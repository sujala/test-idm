package com.rackspace.idm.api.resource.cloud;

import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;

import javax.xml.bind.*;
import java.io.StringReader;
import java.io.StringWriter;

public class ObjectMarshaller <T> {

    public String marshal(Object jaxbObject, Class<?> type) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(type);
        Marshaller marshaller = context.createMarshaller();

        StringWriter sw = new StringWriter();
        marshaller.marshal(jaxbObject, sw);
        return sw.toString();
    }

    public T unmarshal(String objString, Class<?> type) throws JAXBException {

        JSONConfiguration jsonConfiguration = JSONConfiguration.natural().rootUnwrapping(false).build();

        JAXBContext context = new JSONJAXBContext(
            jsonConfiguration,
            "com.rackspace.api.common.fault.v1:" +
            "com.rackspace.api.idm.v1:" +
            "com.rackspace.docs.identity.api.ext.rax_ksgrp.v1:" +
            "com.rackspace.docs.identity.api.ext.rax_kskey.v1:" +
            "com.rackspace.docs.identity.api.ext.rax_ksqa.v1:" +
            "com.rackspacecloud.docs.auth.api.v1:" +
            "org.openstack.docs.common.api.v1:" +
            "org.openstack.docs.compute.api.v1:" +
            "org.openstack.docs.identity.api.ext.os_ksadm.v1:" +
            "org.openstack.docs.identity.api.ext.os_kscatalog.v1:" +
            "org.openstack.docs.identity.api.ext.os_ksec2.v1:" +
            "org.openstack.docs.identity.api.v2:" +
            "com.rackspace.docs.identity.api.ext.rax_auth.v1:" +
            "org.w3._2005.atom"
        );

        Unmarshaller unmarshaller = context.createUnmarshaller();
		JAXBElement<T> jaxbObject = (JAXBElement<T>)unmarshaller.unmarshal(new StringReader(objString));
        return jaxbObject.getValue();
    }
}
