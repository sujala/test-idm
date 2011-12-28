package com.rackspace.idm.api.resource.cloud;

import com.rackspacecloud.docs.auth.api.v1.Credentials;
import org.apache.log4j.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

@Provider
@Consumes("application/xml")
public class CredentialProvider implements MessageBodyReader<JAXBElement<? extends Credentials>> {
    static final Logger LOG = Logger.getLogger(CredentialProvider.class);
    static JAXBContext jaxbContext;

    static {
        try {
            jaxbContext = JAXBContext.newInstance("com.rackspacecloud.docs.auth.api.v1");
        } catch (Exception e) {
            LOG.error("Couldn't create jaxbContext...");
        }
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
                              Annotation[] annotations, MediaType mediaType) {

        //
        //  Basically we want to return true only if the media type is
        //  compatible with application/xml and the genericType is
        //  JAXBElement<?  extends Credentials>
        //
        if (!(mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE) && genericType instanceof ParameterizedType)) {
            return false;
        }

        ParameterizedType ptype = (ParameterizedType) genericType;
        Type rawType = ptype.getRawType();
        Type[] args = ptype.getActualTypeArguments();

        if (!(rawType instanceof Class)) {return false;}
        if (!((Class<?>) rawType).getCanonicalName().equals("javax.xml.bind.JAXBElement")) {return false;}
        if (args.length != 1) {return false;}
        if (!(args[0] instanceof WildcardType)) {return false;}

        Type[] upperBounds = ((WildcardType) args[0]).getUpperBounds();
        
        return upperBounds.length == 1 && upperBounds[0] instanceof Class
                && ((Class<?>) upperBounds[0]).getCanonicalName().equals("com.rackspace.idm.cloudv11.jaxb.Credentials");

    }

    @Override
    @SuppressWarnings("unchecked")
    public JAXBElement<? extends Credentials> readFrom(Class<JAXBElement<? extends Credentials>> type,
                                                       Type genericType, Annotation[] annotations, MediaType mediaType,
                                                       MultivaluedMap<String, String> httpHeaders, InputStream entityStream)
            throws WebApplicationException, IOException {
        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            return (JAXBElement<? extends Credentials>) unmarshaller.unmarshal(entityStream);
        } catch (Exception e) {
            throw new WebApplicationException(400);
        }
    }
}
