package com.prapps.tutorial.ejb.rest.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

import com.prapps.tutorial.ejb.persistence.api.BookSearchCriteria;
import com.prapps.tutorial.ejb.persistence.dao.LibraryDao;
import com.prapps.tutorial.ejb.rest.exception.ServiceException;
import com.prapps.tutorial.ejb.rest.exception.type.ErrorCodeType;
import com.prapps.tutorial.ejb.rest.model.Book;

public class LibraryServiceImpl implements LibraryService {
	private static final Logger LOG = Logger.getLogger(LibraryServiceImpl.class.getName());
	
	@Inject private LibraryDao libraryDao;
	
	/* (non-Javadoc)
	 * @see com.prapps.tutorial.ejb.rest.service.LibraryService#getBooks()
	 */
	@Override	
	public Collection<Book> getBooks() {
		BookSearchCriteria bookSearchCriteria = new BookSearchCriteria();
		return libraryDao.search(bookSearchCriteria);
	}
	
	/* (non-Javadoc)
	 * @see com.prapps.tutorial.ejb.rest.service.LibraryService#getBook(java.lang.String)
	 */
	@Override
	public Book getBook(String isbn) {
		if (LOG.isLoggable(Level.FINEST)) {
			LOG.finest("retrieving isbn: "+isbn);
		}
		
		BookSearchCriteria criteria = new BookSearchCriteria();
		criteria.setIsbn(isbn);
		Book book = libraryDao.searchOne(criteria);
		return book;
	}
	
	/* (non-Javadoc)
	 * @see com.prapps.tutorial.ejb.rest.service.LibraryService#getBook(java.lang.String, java.lang.String)
	 */
	@Override
	
	public Book getBook(String author, String title) {
		if (LOG.isLoggable(Level.INFO)) {
			LOG.info("author: " + author+"\ttitle: "+title);
		}
		BookSearchCriteria criteria = new BookSearchCriteria();
		criteria.setAuthor(author);
		criteria.setTitle(title);
		return libraryDao.searchOne(criteria);
	}
	
	/* (non-Javadoc)
	 * @see com.prapps.tutorial.ejb.rest.service.LibraryService#addBook(com.prapps.tutorial.ejb.rest.model.Book)
	 */
	@Override
	public Book addBook(Book book) throws ServiceException {
		if (LOG.isLoggable(Level.FINE)) {
			LOG.fine("New book: " + book);
		}
		
		Collection<ErrorCodeType> errors = new ArrayList<>();
		if (null == book.getIsbn() || book.getIsbn().isEmpty()) {
			errors.add(ErrorCodeType.MANDATORY_ISBN);
		}
		if (null == book.getAuthor() || book.getAuthor().isEmpty()) {
			errors.add(ErrorCodeType.MANDATORY_AUTHOR);
		}
		
		if (!errors.isEmpty()) {
			throw new ServiceException(errors);
		}
		
		return libraryDao.addBook(book);
	}
	
	/* (non-Javadoc)
	 * @see com.prapps.tutorial.ejb.rest.service.LibraryService#deleteBook(java.lang.String)
	 */
	@Override
	public void deleteBook(String isbn) {
		if (LOG.isLoggable(Level.INFO)) {
			LOG.info("Delete book: " + isbn);
		}
		libraryDao.delete(isbn);
	}
}
