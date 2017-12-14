package com.rackspace.idm.modules.usergroups.api.resource.json;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment;
import com.rackspace.idm.api.resource.cloud.JsonArrayTransformerHandler;
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForEntity;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.rackspace.idm.modules.usergroups.Constants.RAX_AUTH_TENANT_ASSIGNMENT;
import static com.rackspace.idm.modules.usergroups.Constants.TENANT_ASSIGNMENT;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxAuthTenantAssignment extends JSONReaderForEntity<TenantAssignment> {
    private JsonArrayTransformerHandler arrayHandler = new TenantAssignmentArrayTranformerHandler();

    @Override
    public TenantAssignment readFrom(Class<TenantAssignment> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(RAX_AUTH_TENANT_ASSIGNMENT, TENANT_ASSIGNMENT);
        return read(entityStream, RAX_AUTH_TENANT_ASSIGNMENT, prefixValues, arrayHandler);
    }
}
