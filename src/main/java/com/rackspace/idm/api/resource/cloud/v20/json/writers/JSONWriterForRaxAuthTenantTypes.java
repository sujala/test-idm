package com.rackspace.idm.api.resource.cloud.v20.json.writers;


import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantTypes;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.rackspace.idm.JSONConstants.*;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxAuthTenantTypes extends JSONWriterForArrayEntity<TenantTypes> {

    @Override
    public void writeTo(TenantTypes tenantTypes, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(tenantTypes, TENANT_TYPES, RAX_AUTH_TENANT_TYPES, entityStream);
    }
}
