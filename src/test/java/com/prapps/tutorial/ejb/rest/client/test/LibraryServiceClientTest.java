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
		Book addedBook = addBook(newbook);
		
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
		boolean found = false;
		for (Book book : books) {
			if (ISBN.equals(book.getIsbn())) {
				Assert.assertEquals(AUTHOR, book.getAuthor());
				Assert.assertEquals(TITLE, book.getTitle());
				found = true;
			}
		}
		Assert.assertTrue(found);
		deleteBook(addedBook);
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
			 
			requestContext.getHeaders().add("Authorization", "Basic " + base64Token);
			// requestContext.getHeaders().add("X-Requested-With","XMLHttpRequest");
			requestContext.getHeaders().add("Accept", "application/json");

		}
	}
}
