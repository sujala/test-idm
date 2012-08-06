package com.rackspace.idm.domain.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

@Provider
@Produces("application/xml")
public class JAXBXMLContextResolver implements ContextResolver<JAXBContext> {

    private static JAXBContext context;
    private static Logger logger = LoggerFactory.getLogger(JAXBXMLContextResolver.class);

    public JAXBXMLContextResolver() throws JAXBException{
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
    		} catch (Exception e) {
                logger.info("failed to create JAXBXMLContext: " + e.getMessage());
            }
    	}
    	return context;
    }
    
    private static void init() throws JAXBException {

        context = JAXBContext.newInstance(
	            "com.rackspace.api.idm.v1:"+
                "com.rackspacecloud.docs.auth.api.v1:" +
                "org.openstack.docs.common.api.v1:"+
                "org.openstack.docs.compute.api.v1:" +
                "org.openstack.docs.identity.api.v2:" +
                "com.rackspace.docs.identity.api.ext.rax_ksgrp.v1:" +
                "com.rackspace.docs.identity.api.ext.rax_kskey.v1:"+
                "org.openstack.docs.identity.api.ext.os_ksadm.v1:" +
                "org.openstack.docs.identity.api.ext.os_kscatalog.v1:" +
                "org.openstack.docs.identity.api.ext.os_ksec2.v1:" +
                "org.w3._2005.atom:" +
                "com.rackspace.docs.identity.api.ext.rax_ksqa.v1:" +
                "com.rackspace.api.common.fault.v1:" +
                "com.rackspace.docs.identity.api.ext.rax_auth.v1:" +
                "com.rackspace.idm.api.resource.cloud.migration"
        );

    }

    public static void setContext(JAXBContext context) {
        JAXBXMLContextResolver.context = context;
    }
}
