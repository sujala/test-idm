package com.rackspace.idm.domain.config;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;

@Provider
public class MyJAXBContextResolver implements ContextResolver<JAXBContext> {

    private JAXBContext context;
    //private Class[] types = {AuthCredentials.class};
    
    public MyJAXBContextResolver() throws Exception {
        this.context = new JSONJAXBContext(
                JSONConfiguration.natural().build(),
                "com.rackspace.idm.jaxb");
    }

    public JAXBContext getContext(Class<?> objectType) {
        return context;
    }
}