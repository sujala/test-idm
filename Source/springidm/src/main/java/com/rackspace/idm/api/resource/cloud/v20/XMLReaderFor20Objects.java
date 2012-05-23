package com.rackspace.idm.api.resource.cloud.v20;

/**
 * User: alan.erwin
 * Date: 5/23/12
 * Time: 10:47 AM
 */

import com.rackspace.idm.domain.config.providers.PackageClassDiscoverer;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: May 21, 2012
 * Time: 4:59:02 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Consumes(MediaType.APPLICATION_XML)
public class XMLReaderFor20Objects implements MessageBodyReader<Object> {

    public static final Logger LOG = Logger.getLogger(XMLReaderFor20Objects.class);

    private static JAXBContext jaxbContext;

    private static Set<Class<?>> classes;

    static {
        try {
            JSONConfiguration jsonConfiguration = JSONConfiguration.natural().rootUnwrapping(false).build();

            jaxbContext = new JSONJAXBContext(jsonConfiguration,
                    "com.rackspace.api.idm.v1:" +
                            "com.rackspacecloud.docs.auth.api.v1:" +
                            "org.openstack.docs.common.api.v1:" +
                            "org.openstack.docs.compute.api.v1:" +
                            "org.openstack.docs.identity.api.v2:" +
                            "com.rackspace.docs.identity.api.ext.rax_ksgrp.v1:" +
                            "com.rackspace.docs.identity.api.ext.rax_kskey.v1:" +
                            "org.openstack.docs.identity.api.ext.os_ksadm.v1:" +
                            "org.openstack.docs.identity.api.ext.os_kscatalog.v1:" +
                            "org.openstack.docs.identity.api.ext.os_ksec2.v1:" +
                            "org.w3._2005.atom:" +
                            "com.rackspace.docs.identity.api.ext.rax_ksqa.v1:" +
                            "com.rackspace.api.common.fault.v1:" +
                            "com.rackspace.docs.identity.api.ext.rax_ga.v1:" +
                            "com.rackspace.idm.api.resource.cloud.migration");


            classes = PackageClassDiscoverer.findClassesIn(
                    "com.rackspace.api.idm.v1",
                    "com.rackspacecloud.docs.auth.api.v1",
                    "org.openstack.docs.common.api.v1",
                    "org.openstack.docs.compute.api.v1",
                    "org.openstack.docs.identity.api.v2",
                    "com.rackspace.docs.identity.api.ext.rax_ksgrp.v1",
                    "com.rackspace.docs.identity.api.ext.rax_kskey.v1",
                    "org.openstack.docs.identity.api.ext.os_ksadm.v1",
                    "org.openstack.docs.identity.api.ext.os_kscatalog.v1",
                    "org.openstack.docs.identity.api.ext.os_ksec2.v1",
                    "org.w3._2005.atom",
                    "com.rackspace.docs.identity.api.ext.rax_ksqa.v1",
                    "com.rackspace.api.common.fault.v1",
                    "com.rackspace.docs.identity.api.ext.rax_ga.v1",
                    "com.rackspace.idm.api.resource.cloud.migration");
        } catch (Exception e) {
            LOG.error("Error in static initializer.  - " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private JAXBContext getContext() throws JAXBException {
        return jaxbContext;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        Class genClass = (Class) genericType;
        return classes.contains(genClass);
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        try {
            StringWriter writer = new StringWriter();
            IOUtils.copy(entityStream, writer);
            String en = writer.toString();
            Unmarshaller m = getContext().createUnmarshaller();
            JAXBElement unmarshal = (JAXBElement)m.unmarshal(new StringReader(en));

            return unmarshal.getValue();
        } catch (JAXBException e) {
            throw new WebApplicationException(e);
        }


    }
}