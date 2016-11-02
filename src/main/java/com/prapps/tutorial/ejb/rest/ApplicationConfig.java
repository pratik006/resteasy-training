package com.prapps.tutorial.ejb.rest;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.prapps.tutorial.ejb.rest.exception.ServiceExceptionMapper;
import com.prapps.tutorial.ejb.rest.interceptor.ResponseHelperInterceptor;
import com.prapps.tutorial.ejb.rest.interceptor.RestSecurityInterceptor;
import com.prapps.tutorial.ejb.rest.service.LibraryServiceImpl;

@ApplicationPath("/")
public class ApplicationConfig extends Application {
	
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>(Arrays.asList(
        		LibraryServiceImpl.class, 
        		RestSecurityInterceptor.class, 
        		ResponseHelperInterceptor.class, 
        		ServiceExceptionMapper.class)
        	);
    }
}
