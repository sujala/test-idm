package com.rackspace.idm.domain.config.providers.cloudv11;

import com.rackspacecloud.docs.auth.api.v1.User;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.openstack.docs.identity.api.v2.Tenant;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.*;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: May 21, 2012
 * Time: 4:59:02 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Consumes(MediaType.APPLICATION_XML)
public class XMLReaderForUser11 implements MessageBodyReader<User> {

    public static final Logger LOG = Logger.getLogger(XMLReaderForUser11.class);

    private static JAXBContext jaxbContext;

    static {
        try {
            JSONConfiguration jsonConfiguration = JSONConfiguration.natural().rootUnwrapping(false).build();

            jaxbContext = new JSONJAXBContext(jsonConfiguration,
                    "com.rackspacecloud.docs.auth.api.v1");

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
        return type == User.class;
    }

    @Override
    public User readFrom(Class<User> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        try {
            StringWriter writer = new StringWriter();
            IOUtils.copy(entityStream, writer);
            String en = writer.toString();

            Unmarshaller m = getContext().createUnmarshaller();
            JAXBElement<User> jaxbObject = (JAXBElement<User>) m.unmarshal(new StringReader(en));
            return jaxbObject.getValue();
        } catch (JAXBException e) {
            throw new WebApplicationException(e);
        }
    }

}
