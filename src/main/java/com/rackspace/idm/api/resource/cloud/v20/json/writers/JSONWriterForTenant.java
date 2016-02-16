package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.idm.JSONConstants;
import org.openstack.docs.identity.api.v2.Tenant;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;


@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForTenant extends JSONWriterForEntity<Tenant> {

    @Override
    public void writeTo(Tenant tenant, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        Map<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.TENANT_DOMAIN_ID_PATH, JSONConstants.RAX_AUTH_DOMAIN_ID);
        write(tenant, entityStream, prefixValues);
    }
}
