package com.rackspace.idm.domain.config;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;

@Provider
public class JAXBContextResolver implements ContextResolver<JAXBContext> {

    private final JAXBContext context;
    //private Class[] types = {AuthCredentials.class};
    
    public JAXBContextResolver() throws Exception {
        this.context = new JSONJAXBContext(
                JSONConfiguration.natural().rootUnwrapping(false).build(),
                "com.rackspacecloud.docs.auth.api.v1:com.rackspace.idm.jaxb");
    }

    @Override
    public JAXBContext getContext(Class<?> objectType) {
        return context;
    }
}