package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.idm.JSONConstants;
import org.openstack.docs.identity.api.v2.Tenant;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.rackspace.idm.JSONConstants.*;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForTenant extends JSONReaderForEntity<Tenant> {

    @Override
    public Tenant readFrom(Class<Tenant> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(TENANT_RAX_AUTH_DOMAIN_ID_PATH, DOMAIN_ID);
        prefixValues.put(TENANT_RAX_AUTH_TYPES_PATH, TYPES);

        return read(inputStream, JSONConstants.TENANT, prefixValues);
    }
}
