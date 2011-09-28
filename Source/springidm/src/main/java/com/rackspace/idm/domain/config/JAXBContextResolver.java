package com.rackspace.idm.domain.config;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;

@Provider
public class JAXBContextResolver implements ContextResolver<JAXBContext> {

    private final JAXBContext context;

    public JAXBContextResolver() throws Exception {
        this.context = new JSONJAXBContext(
            JSONConfiguration.natural().rootUnwrapping(false).build(),
            "com.rackspace.api.idm.v1:com.rackspacecloud.docs.auth.api.v1:org.openstack.docs.common.api.v1:org.openstack.docs.compute.api.v1:org.openstack.docs.identity.api.v2:com.rackspace.docs.identity.api.ext.rax_ksadm.v1:com.rackspace.docs.identity.api.ext.rax_ksgrp.v1:com.rackspace.docs.identity.api.ext.rax_kskey.v1:org.openstack.docs.identity.api.ext.os_ksadm.v1:org.openstack.docs.identity.api.ext.os_kscatalog.v1:org.openstack.docs.identity.api.ext.os_ksec2.v1:org.w3._2005.atom");
    }

    @Override
    public JAXBContext getContext(Class<?> objectType) {
        return context;
    }
}