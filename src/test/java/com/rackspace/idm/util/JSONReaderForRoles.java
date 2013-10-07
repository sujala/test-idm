package com.rackspace.idm.util;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForArrayEntity;
import org.openstack.docs.identity.api.v2.EndpointList;
import org.openstack.docs.identity.api.v2.RoleList;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Wasn't provided in production code, but is useful for tests that require reading in json responses. Rather than add to production, which could affect
 * other code unexpectedly, just added to test package.
 */
public class JSONReaderForRoles extends JSONReaderForArrayEntity<RoleList> {

    @Override
    public RoleList readFrom(Class<RoleList> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        return read(JSONConstants.ROLES, JSONConstants.ROLE, inputStream);
    }
}
