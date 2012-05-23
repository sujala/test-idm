package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspacecloud.docs.auth.api.v1.BaseURLRef;
import com.rackspacecloud.docs.auth.api.v1.User;
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyEnabled;
import com.rackspacecloud.docs.auth.api.v1.UserWithOnlyKey;
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

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: May 21, 2012
 * Time: 4:59:02 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Consumes(MediaType.APPLICATION_XML)
public class XMLReaderFor11Objects implements MessageBodyReader<Object> {

    public static final Logger LOG = Logger.getLogger(XMLReaderFor11Objects.class);

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
        return type == BaseURLRef.class || type == User.class || type == UserWithOnlyEnabled.class || type == UserWithOnlyKey.class;
    }

    @Override
    public Object readFrom(Class<Object> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        try {
            StringWriter writer = new StringWriter();
            IOUtils.copy(entityStream, writer);
            String en = writer.toString();

            Unmarshaller m = getContext().createUnmarshaller();
            if (type.equals(BaseURLRef.class)) {
                JAXBElement<BaseURLRef> jaxbObject = (JAXBElement<BaseURLRef>) m.unmarshal(new StringReader(en));
                return jaxbObject.getValue();
            } else if (type.equals(User.class)) {
                JAXBElement<User> jaxbObject = (JAXBElement<User>) m.unmarshal(new StringReader(en));
                return jaxbObject.getValue();
            } else if (type.equals(UserWithOnlyEnabled.class)) {
                //Root Element is found and sets UserWithOnly as User
                JAXBElement<User> jaxbObject = (JAXBElement<User>) m.unmarshal(new StringReader(en));
                UserWithOnlyEnabled userWithOnlyEnabled = new UserWithOnlyEnabled();
                Boolean enabled = jaxbObject.getValue().isEnabled();
                userWithOnlyEnabled.setEnabled(enabled);
                return userWithOnlyEnabled;
            } else if (type.equals(UserWithOnlyKey.class)) {
                //Root Element is found and sets UserWithOnly as User
                JAXBElement<User> jaxbObject = (JAXBElement<User>) m.unmarshal(new StringReader(en));
                UserWithOnlyKey userWithOnlyKey = new UserWithOnlyKey();
                userWithOnlyKey.setKey(jaxbObject.getValue().getKey());
                return userWithOnlyKey;
            } else {
                return type;
            }

        } catch (JAXBException e) {
            throw new WebApplicationException(e);
        }
    }

}
