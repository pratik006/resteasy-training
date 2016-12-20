package com.prapps.tutorial.ejb.rest.client.test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.security.sasl.AuthenticationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.junit.Assert;
import org.junit.Test;

import com.prapps.tutorial.ejb.rest.exception.type.ServiceErrorMessage;
import com.prapps.tutorial.ejb.rest.model.BaseResponse;
import com.prapps.tutorial.ejb.rest.model.Book;

public class LibraryServiceClientTest {
	private static final Logger LOG = Logger.getLogger(LibraryServiceClientTest.class.getName());
	private static final String url = "http://localhost:8080/rest/library/books";
	static String username = "testuser1";
	static String password = "password1";

	static {
		System.setProperty("java.util.logging.config.file", "src/test/resources/logging.properties");
	}
	
	private static final String ISBN = "9788129137708";
	private static final String AUTHOR = "Devdutt Patnaik";
	private static final String TITLE = "My Gita";
	
	private Book addBook(Book book) throws AuthenticationException {
		Client client = ClientBuilder.newClient().register(AddHeadersFilter.INSTANCE);
		Entity<Book> entity = Entity.entity(book, MediaType.APPLICATION_JSON);
		LOG.finest("targetUrl: "+url);
		Response response = client.target(url).request().put(entity);

		LOG.finest("Headers" + response.getHeaders());
		LOG.finest("Status: " + response.getStatus());
		if (response.getStatus() == 401 || response.getStatus() == 403) {
			throw new AuthenticationException("Unauthorized");
		}
		Book addedBook = response.readEntity(Book.class);
		LOG.finest("addedBook: "+addedBook);
		
		Book retrieved = ClientBuilder.newClient().register(AddHeadersFilter.INSTANCE)
				.target(url).path("/{isbn}").resolveTemplate("isbn", book.getIsbn()).request().get().readEntity(Book.class);
		return retrieved;
	}
	
	private boolean deleteBook(Book book) {
		Client client = ClientBuilder.newClient().register(AddHeadersFilter.INSTANCE);
		Response response = client.target(url+"/"+book.getIsbn()).request().delete();

		LOG.fine("Headers" + response.getHeaders());
		LOG.fine("Status: " + response.getStatus());
		
		Book retrieved = ClientBuilder.newClient().register(AddHeadersFilter.INSTANCE)
				.target(url).path("/{isbn}").resolveTemplate("isbn", book.getIsbn()).request().get().readEntity(Book.class);
		return retrieved==null;
	}
	
	private Book findBook(String isbn) {
		Client client = ClientBuilder.newClient().register(AddHeadersFilter.INSTANCE);
		Response response = client.target(url+"/"+isbn).request().get();
		if (response.getStatus() == 200 && response.hasEntity()) {
			return response.readEntity(Book.class);
		}
		
		return null;
	}
	
	@Test
	public void testAddBook() throws AuthenticationException {
		Book book = new Book();
		book.setAuthor("Herbert Schildt");
		book.setIsbn("0071823506, 9780071823500");
		book.setPublishedDate(Calendar.getInstance());
		book.setTitle("Java: The Complete Reference, Ninth Edition");
		Book retrieved = addBook(book);
		Assert.assertEquals(book, retrieved);
		
		Book searchedBook = findBook(book.getIsbn());
		Assert.assertEquals(book.getIsbn(), searchedBook.getIsbn());
		Assert.assertEquals(book.getAuthor(), searchedBook.getAuthor());
		
		//deleteBook(retrieved);
	}

	@Test
	public void testAddBookFailed() {
		Client client = ClientBuilder.newClient().register(AddHeadersFilter.INSTANCE);
		Book book = new Book();
		Entity<Book> entity = Entity.entity(book, MediaType.APPLICATION_JSON);
		Response response = client.target(url).request().put(entity);

		LOG.fine("Headers" + response.getHeaders());
		LOG.fine("Status: " + response.getStatus());
		BaseResponse baseResponse = response.readEntity(BaseResponse.class);
		Assert.assertEquals("failed", baseResponse.getStatus());
		Assert.assertTrue(!baseResponse.getErrors().isEmpty());
		for (ServiceErrorMessage errorMsg : baseResponse.getErrors()) {
			LOG.fine(errorMsg.getErrorCode()+" - "+errorMsg.getErrorCode());
		}
	}

	@Test
	public void testGetBookByIsbn() throws AuthenticationException {
		Book newbook = new Book();
		newbook.setAuthor(AUTHOR);
		newbook.setIsbn(ISBN);
		newbook.setPublishedDate(Calendar.getInstance());
		newbook.setTitle(TITLE);
		Book addedBook = addBook(newbook);
		Client client = ClientBuilder.newClient().register(AddHeadersFilter.INSTANCE);
		WebTarget target = client.target(url).path("/{isbn}").resolveTemplate("isbn", newbook.getIsbn());
		Response response = target.request().get();
		Book searchedBook = response.readEntity(Book.class);

		LOG.fine("Headers" + response.getHeaders());
		LOG.fine("Status: " + response.getStatus());
		LOG.fine("Searched Book: "+searchedBook);
		Assert.assertEquals(AUTHOR, searchedBook.getAuthor());
		Assert.assertEquals(ISBN, searchedBook.getIsbn());
		Assert.assertEquals(TITLE, searchedBook.getTitle());
		
		deleteBook(addedBook);
	}
	
	@Test
	public void testGetBooks() throws AuthenticationException {
		Book newbook = new Book();
		newbook.setAuthor(AUTHOR);
		newbook.setIsbn(ISBN);
		newbook.setPublishedDate(Calendar.getInstance());
		newbook.setTitle(TITLE);
		//Book addedBook = addBook(newbook);
		
		Client client = ClientBuilder.newClient().register(AddHeadersFilter.INSTANCE);
		WebTarget target = client.target(url);
		Response response = target.request().get();
		LOG.fine("Headers" + response.getHeaders());
		LOG.fine("Status: " + response.getStatus());
		if (response.getStatus() == 401) {
			throw new AuthenticationException("Unauthorized");
		}
		GenericType<List<Book>> bookListType = new GenericType<List<Book>>() {};
		List<Book> books = response.readEntity(bookListType);
		Assert.assertTrue(books.size() > 0);
		/*boolean found = false;
		for (Book book : books) {
			if (ISBN.equals(book.getIsbn())) {
				Assert.assertEquals(AUTHOR, book.getAuthor());
				Assert.assertEquals(TITLE, book.getTitle());
				found = true;
			}
		}
		Assert.assertTrue(found);*/
		//deleteBook(addedBook);
	}

	@Test
	public void testGetBookByAuthorAndTitle() throws AuthenticationException {
		Book newbook = new Book();
		newbook.setAuthor(AUTHOR);
		newbook.setIsbn(ISBN);
		newbook.setPublishedDate(Calendar.getInstance());
		newbook.setTitle(TITLE);
		Book addedBook = addBook(newbook);
		
		Client client = ClientBuilder.newClient().register(AddHeadersFilter.INSTANCE);
		Map<String, Object> map = new HashMap<>();
		map.put("author", AUTHOR);
		map.put("title", TITLE);
		WebTarget target = client.target(url).path("/{author}/{title}").resolveTemplates(map);
		Response response = target.request().get();
		Book book = response.readEntity(Book.class);

		LOG.fine("Headers" + response.getHeaders());
		LOG.fine("Status: " + response.getStatus());
		LOG.fine("Title: " + book.getTitle() + "\tAuthor: " + book.getAuthor() + "\tISBN: " + book.getIsbn());
		Assert.assertEquals(TITLE, book.getTitle());
		Assert.assertEquals(AUTHOR, book.getAuthor());
		
		deleteBook(addedBook);
	}
	
	@Test(expected = AuthenticationException.class)
	public void testAuthFailed() throws AuthenticationException {
		Book book = new Book();
		book.setAuthor("Herbert Schildt");
		book.setIsbn("0071823506, 9780071823500");
		book.setPublishedDate(Calendar.getInstance());
		book.setTitle("Java: The Complete Reference, Ninth Edition");
		username = "testuser2";
		password = "password2";
		Book retrieved = addBook(book);
		username = "testuser1";
		password = "password1";
	}

	public enum AddHeadersFilter implements ClientRequestFilter {
		INSTANCE;

		private AddHeadersFilter() {
		}

		@Override
		public void filter(ClientRequestContext requestContext) throws IOException {
			String token = username + ":" + password; 
			String base64Token = Base64.getEncoder().encodeToString(token.getBytes(StandardCharsets.UTF_8));
			 
			//requestContext.getHeaders().add("Authorization", "Basic " + base64Token);
			//testuser1-role-admin
			requestContext.getHeaders().add("Authorization", "bearer " + "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJRcFZoOUlqYUFIZ0RTS3ZjUDN2YTVXTllkTnJLNlJZRVRyZkIzRDZtcUdnIn0.eyJqdGkiOiJhZDFjZDEyNC0wNDRmLTQ4NTgtYjE5Yi02YjlmNTkwZTAyNmUiLCJleHAiOjE0ODIyMTk0MTgsIm5iZiI6MCwiaWF0IjoxNDgyMjE1ODE4LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgxODAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoiY3VybCIsInN1YiI6IjI3N2U0OGQ5LWQ5ZTAtNDdjYi1hMmMzLTI1ZmY2N2FlMjZiMiIsInR5cCI6IkJlYXJlciIsImF6cCI6ImN1cmwiLCJhdXRoX3RpbWUiOjAsInNlc3Npb25fc3RhdGUiOiJhOTYwZWRiZi05ZGM5LTRkNzMtODA2Yi1jMDNlOTZkNDBjNzIiLCJhY3IiOiIxIiwiY2xpZW50X3Nlc3Npb24iOiIzM2ZlY2RhYy1kMjY0LTQ3MDUtODM4NS0zODM4YWJjNDczMjUiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsiY3JlYXRlLXJlYWxtIiwiYWRtaW4iLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7Im1hc3Rlci1yZWFsbSI6eyJyb2xlcyI6WyJ2aWV3LXJlYWxtIiwidmlldy1pZGVudGl0eS1wcm92aWRlcnMiLCJtYW5hZ2UtaWRlbnRpdHktcHJvdmlkZXJzIiwiaW1wZXJzb25hdGlvbiIsImNyZWF0ZS1jbGllbnQiLCJtYW5hZ2UtdXNlcnMiLCJ2aWV3LWF1dGhvcml6YXRpb24iLCJtYW5hZ2UtZXZlbnRzIiwibWFuYWdlLXJlYWxtIiwidmlldy1ldmVudHMiLCJ2aWV3LXVzZXJzIiwidmlldy1jbGllbnRzIiwibWFuYWdlLWF1dGhvcml6YXRpb24iLCJtYW5hZ2UtY2xpZW50cyJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsInZpZXctcHJvZmlsZSJdfX0sIm5hbWUiOiIiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ0ZXN0dXNlcjEifQ.KaELfpJ9Uprb3w2sb0vqUqILud7C6WCNWtjGgrvIt3GgVGLybQvtPs2DpA_WSYJgx9RpFyO0YreMKtgV4hVXv5lJp-s8mHzE7CTVO2sHnKEOVE7Ku6ZTVBllX2QSXzEXs_pmuHyaNsVZOieN2oIanLuuNQDh_nXrQnZlEcgYcpa6A7tirJwRDOK5E3VjsrqBxItj_HIPy_I5VhDjPDBJCTPfzAIasW0H8qst8ymmj1vM0HthXry5CGl-spCvEp_nttSVVe4Yhf24COJB0W01RUiO3JO-dwRVX87qXs0GNamBLxfgTLigbUjxoEUEqza5Hq06o1NNNsduhuLVLm41Mw");
			//testuser2-role-user
			//requestContext.getHeaders().add("Authorization", "bearer " + "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJIS3ByYkF1SGd5aHN6WDlQTkRwUzE1Tzh2RU5XaGFGek1QbXpIZ3JrSjFBIn0.eyJqdGkiOiI1NjA1N2VlMy0wY2M3LTRjOTItOTBjNS00YzMyYjNiNWJkYmMiLCJleHAiOjE0ODIyMDMyMDksIm5iZiI6MCwiaWF0IjoxNDgyMTk5NjA5LCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgxODAvYXV0aC9yZWFsbXMvbWFzdGVyIiwiYXVkIjoiY3VybCIsInN1YiI6ImE4NjZhMTY2LTkxNmUtNGRjMC1iYzQ5LTdkZWQ5NTAxYmI4MyIsInR5cCI6IkJlYXJlciIsImF6cCI6ImN1cmwiLCJhdXRoX3RpbWUiOjAsInNlc3Npb25fc3RhdGUiOiIxNjFmYTkwZi03MTBjLTQwNjktYmVlZS00NmRmZWEwZGRkODYiLCJhY3IiOiIxIiwiY2xpZW50X3Nlc3Npb24iOiJmNDdiNGQ3Yi1iNDc1LTQyYTYtYWMzMS00Njk2NzBhMWI2NDAiLCJhbGxvd2VkLW9yaWdpbnMiOltdLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsidW1hX2F1dGhvcml6YXRpb24iLCJ1c2VyIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsic2VydmljZSI6eyJyb2xlcyI6WyJ1c2VyIl19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50Iiwidmlldy1wcm9maWxlIl19fSwibmFtZSI6IiIsInByZWZlcnJlZF91c2VybmFtZSI6InRlc3R1c2VyMiJ9.cYWZVnp9Pm8HYfT_jJfT_RMNT0aCN7Lkk-S7YvaCp0RpTwK3geh1MRSOfGYa1jSKFjuHLyXoOIiakJXpunk2REYHJdaewfIo0PDkckVz_F98LdQl8ZnXWHnQkYp3nRWz_GJxb0vx4eWobq5RXnXYD_BqKpYzwzodUAunQwJj05U9zenSbVp1NQl2BX6X8Bw93ZnRurFudyfJk6VJ34h9zZ2WeN69QY8ZGhMKlV3mA4J8LNjGQzS8PIU7EUY3UNkyV2oCEfafhn1X_sH8pW85rBGtbMIm1TRSL7DH782Id8A8pHgYVFvQhvqS4RF1nbN3c-HAE18Xwfck7hL_uxz-fw");
			
			
			// requestContext.getHeaders().add("X-Requested-With","XMLHttpRequest");
			requestContext.getHeaders().add("Accept", "application/json");

		}
	}
}
