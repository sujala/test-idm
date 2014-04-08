package com.rackspace.idm.domain.config.providers.cloudv20;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: Apr 25, 2012
 * Time: 3:12:22 PM
 * To change this template use File | Settings | File Templates.
 */

import com.rackspace.idm.api.resource.cloud.v20.MultiFactorCloud20Service;
import com.rackspace.idm.domain.config.providers.PackageClassDiscoverer;
import com.rackspace.idm.exception.IdmException;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.xml.bind.marshaller.NamespacePrefixMapper;
import org.apache.log4j.Logger;
import org.openstack.docs.identity.api.v2.User;
import org.openstack.docs.identity.api.v2.UserList;
import org.springframework.beans.factory.annotation.Autowired;
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
public class Core20XMLWriter extends NamespacePrefixMapper implements MessageBodyWriter<Object> {
    public static final Logger LOG = Logger.getLogger(Core20XMLWriter.class);

    @Resource(name = "corev20NsPrefixMap")
    private Map<String, String> corev20NsPrefixMap;

    private static Set<Class<?>> classes = new HashSet<Class<?>>();

    private static final String PREFIX_MAPPER_PROP = "com.sun.xml.bind.namespacePrefixMapper";

    private static JAXBContext jaxbContext;

    @Autowired
    private MultiFactorCloud20Service multiFactorCloud20Service;

    static {
        try {
            JSONConfiguration jsonConfiguration = JSONConfiguration.natural().rootUnwrapping(false).build();

            jaxbContext = new JSONJAXBContext(jsonConfiguration,
                    "org.openstack.docs.identity.api.v2:" +
                    "com.rackspace.docs.identity.api.ext.rax_auth.v1"
            );

            classes.addAll(PackageClassDiscoverer.findClassesIn("org.openstack.docs.identity.api.v2"));
            classes.addAll(PackageClassDiscoverer.findClassesIn("com.rackspace.docs.identity.api.ext.rax_auth.v1"));
        } catch (Exception e) {
            LOG.error("Error in static initializer.  - " + e.getMessage());
            throw new IdmException(e);
        }
    }

    private JAXBContext getContext() throws JAXBException {
        return jaxbContext;
    }

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
    public void writeTo(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException {
        //To change body of implemented methods use File | Settings | File Templates.
        if (o instanceof User) {
            cleanNullBooleans((User) o);
        }
        if (o instanceof UserList) {
            for (User user : ((UserList) o).getUser()) {
                cleanNullBooleans(user);
            }
        }
        try {
            Marshaller m = getContext().createMarshaller();
            m.setProperty(PREFIX_MAPPER_PROP, this);
            m.marshal(o, entityStream);
        } catch (JAXBException e) {
            throw new WebApplicationException(e);
        }
    }

    // Workaround to avoid null values from JAXB generated classes
    private void cleanNullBooleans(User user) {
        if (multiFactorCloud20Service.isMultiFactorGloballyEnabled()) {
            user.setMultiFactorEnabled(user.isMultiFactorEnabled() == null ? false : user.isMultiFactorEnabled());
            user.setEnabled(user.isEnabled());
        } else if (Boolean.FALSE.equals(user.isMultiFactorEnabled())) {
            user.setMultiFactorEnabled(null);
        }
    }

    public String getPreferredPrefix(String namespaceUri, String suggestion,  boolean requirePrefix) {
        return corev20NsPrefixMap.get(namespaceUri);
    }

    public void setNsPrefixMap(Map<String, String> corev20NsPrefixMap) {
        this.corev20NsPrefixMap = corev20NsPrefixMap;
    }

}
