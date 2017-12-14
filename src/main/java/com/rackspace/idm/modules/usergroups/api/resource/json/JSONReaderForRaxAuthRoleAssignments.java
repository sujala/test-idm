package com.rackspace.idm.modules.usergroups.api.resource.json;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
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

import static com.rackspace.idm.modules.usergroups.Constants.*;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForRaxAuthRoleAssignments extends JSONReaderForEntity<RoleAssignments> {
    private JsonArrayTransformerHandler arrayHandler = new TenantAssignmentArrayTranformerHandler();

    @Override
    public RoleAssignments readFrom(Class<RoleAssignments> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(RAX_AUTH_ROLE_ASSIGNMENTS, ROLE_ASSIGNMENTS);
        prefixValues.put(TENANT_ASSIGNMENT, TENANT_ASSIGNMENTS);
        return read(entityStream, RAX_AUTH_ROLE_ASSIGNMENTS, prefixValues, arrayHandler);
    }
}