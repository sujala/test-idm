package com.rackspace.idm.domain.config;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;

@Provider
@Produces("application/xml")
public class JAXBXMLContextResolver implements ContextResolver<JAXBContext> {

    private static JAXBContext context;

    public JAXBXMLContextResolver() throws Exception {
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
    
    private static void init() throws Exception {

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
                "com.rackspace.docs.identity.api.ext.rax_ga.v1:" +
                "com.rackspace.idm.api.resource.cloud.migration"
        );

    }
}
