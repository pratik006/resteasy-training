package com.prapps.tutorial.ejb.rest.service;

import java.util.Collection;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import com.prapps.tutorial.ejb.rest.exception.ServiceException;
import com.prapps.tutorial.ejb.rest.model.Book;

@Path("/library")
public interface LibraryService {

	@RolesAllowed({"user", "admin"})
	@GET
	@Path("/books")
	@Produces({"application/json", "application/xml"})
	Collection<Book> getBooks();

	@RolesAllowed({"user", "admin"})
	@GET
	@Path("/books/{isbn}")
	@Produces({"application/json", "application/xml"})
	Book getBook(@PathParam("isbn") String isbn);

	@RolesAllowed({"user", "admin"})
	@GET
	@Path("/books/{author}/{title}")
	@Produces({"application/json", "application/xml"})
	Book getBook(@PathParam("author") String author, @PathParam("title") String title);

	@RolesAllowed({"admin"})
	@PUT
	@Path("/books")
	@Produces({"application/json", "application/xml"})
	Book addBook(Book book) throws ServiceException;

	@RolesAllowed("admin")
	@DELETE
	@Path("/books/{isbn}")
	@Produces({"application/json", "application/xml"})
	void deleteBook(@PathParam("isbn") String isbn);

}