package com.rackspace.idm.domain.config.providers.cloudv20;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: Apr 25, 2012
 * Time: 3:12:22 PM
 * To change this template use File | Settings | File Templates.
 */

import com.rackspace.idm.domain.config.providers.PackageClassDiscoverer;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * This is a JAXBElement provider with awareness of atom links and other
 * extended types not found in the XSD 1.0 version of the schema. Additionally,
 * it allows setting custom namespace prefixes.
 */

@Provider
@Produces("application/xml")
@Component
public class RAXKSQA_XMLWriter extends NamespacePrefixMapper implements
        MessageBodyWriter<Object> {
    public static final Logger LOG = Logger.getLogger(RAXKSQA_XMLWriter.class);

    @Resource(name = "raxksqaNsPrefixMap")
    private Map<String, String> raxksqaNsPrefixMap;

    private static Set<Class<?>> classes = new HashSet<Class<?>>();

    private static final String PREFIX_MAPPER_PROP = "com.sun.xml.bind.namespacePrefixMapper";

    private static JAXBContext jaxbContext;

    static {
        try {
            JSONConfiguration jsonConfiguration = JSONConfiguration.natural().rootUnwrapping(false).build();

            jaxbContext = new JSONJAXBContext(jsonConfiguration,
                    "org.openstack.docs.identity.api.ext.os_kscatalog.v1" );


            classes = PackageClassDiscoverer.findClassesIn(
                    "org.openstack.docs.identity.api.ext.os_kscatalog.v1");

        } catch (Exception e) {
            LOG.error("Error in static initializer.  - " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private JAXBContext getContext() throws JAXBException {
        return jaxbContext;
    }

//
// MessageBodyWriter
//

    private boolean isCorrectClass(Type genericType) {
        boolean ret = false;
        if (genericType instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) genericType;
            Type[] args = ptype.getActualTypeArguments();
            if (args.length == 1) {
                Class elmClass = (Class) args[0];
                ret = classes.contains(elmClass);
            }
        } else {
            Class genClass = (Class) genericType;
            ret = classes.contains(genClass);
        }

        return ret;
    }

    public boolean isWriteable(Class<?> type, Type genericType,
                               Annotation[] annotations, MediaType mediaType) {
        return isCorrectClass(genericType);
    }

    @Override
    public long getSize(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;  //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        //To change body of implemented methods use File | Settings | File Templates.
        try {
            Marshaller m = getContext().createMarshaller();
            m.setProperty(PREFIX_MAPPER_PROP, this);
            m.marshal(o, entityStream);
        } catch (JAXBException e) {
            throw new WebApplicationException(e);
        }
    }
//
// Prefix mapper
//

    public String getPreferredPrefix(String namespaceUri, String suggestion,
                                     boolean requirePrefix) {
        return raxksqaNsPrefixMap.get(namespaceUri);
    }
}
