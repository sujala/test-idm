package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.domain.config.providers.PackageClassDiscoverer;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.IdmException;
import com.sun.jersey.core.provider.EntityHolder;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
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
public class XMLReader implements MessageBodyReader<Object> {

    public static final Logger LOG = Logger.getLogger(XMLReader.class);

    private static JAXBContext jaxbContext;

    private static Set<Class<?>> classes;

    static {
        try {

            jaxbContext = JAXBContext.newInstance(
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
                            "com.rackspace.docs.identity.api.ext.rax_auth.v1:" +
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
                    "com.rackspace.docs.identity.api.ext.rax_auth.v1",
                    "com.rackspace.idm.api.resource.cloud.migration");
        } catch (Exception e) {
            LOG.error("Error in static initializer.  - " + e.getMessage());
            throw new IdmException(e);
        }
    }

    JAXBContext getContext() throws JAXBException {
        return jaxbContext;
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (!type.equals(EntityHolder.class)) {
            Class genClass = (Class) genericType;
            return classes.contains(genClass);
        }
        return false;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException {
        try {
            StringWriter writer = new StringWriter();
            IOUtils.copy(entityStream, writer);
            String en = writer.toString();
            Unmarshaller m = getContext().createUnmarshaller();
            JAXBElement<?> unMarshal = m.unmarshal(new StreamSource(new StringReader(en)), (Class) genericType);

            return unMarshal.getValue();
        } catch (JAXBException e) {
            throw new BadRequestException(e.toString(), e);
        }


    }
}