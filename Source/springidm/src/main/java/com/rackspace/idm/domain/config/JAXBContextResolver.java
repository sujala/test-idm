package com.rackspace.idm.domain.config;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

@Provider
@Produces("application/json")
public class JAXBContextResolver implements ContextResolver<JAXBContext> {

    private static JAXBContext context;

    public JAXBContextResolver() throws JAXBException{
    	init();
    }

    @Override
    public JAXBContext getContext(Class<?> objectType) {
        return context;
    }
    
    public static JAXBContext get() {
    	if (context == null) {
    		try {
    			init();
    		} catch (Throwable t) {
    			t.printStackTrace();
    		}
    	}
    	return context;
    }
    
    private static void init() throws JAXBException {
        JSONConfiguration jsonConfiguration = JSONConfiguration.natural().rootUnwrapping(false).build();

        context = new JSONJAXBContext(
                jsonConfiguration,
	            "com.rackspace.api.idm.v1:com.rackspacecloud.docs.auth.api.v1:org.openstack.docs.common.api.v1:org.openstack.docs.compute.api.v1:org.openstack.docs.identity.api.v2:com.rackspace.docs.identity.api.ext.rax_ksgrp.v1:com.rackspace.docs.identity.api.ext.rax_kskey.v1:org.openstack.docs.identity.api.ext.os_ksadm.v1:org.openstack.docs.identity.api.ext.os_kscatalog.v1:org.openstack.docs.identity.api.ext.os_ksec2.v1:org.w3._2005.atom:com.rackspace.docs.identity.api.ext.rax_ksqa.v1:com.rackspace.api.common.fault.v1:com.rackspace.docs.identity.api.ext.rax_auth.v1:com.rackspace.idm.api.resource.cloud.migration");

    }
}
