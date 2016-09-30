package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JsonArrayEntryTransformer;
import org.json.simple.JSONObject;
import org.openstack.docs.identity.api.v2.Tenants;

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
import java.util.HashMap;
import java.util.Map;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForTenants extends JSONWriterForArrayEntity<Tenants> {

    @Override
    public void writeTo(Tenants tenants, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        write(tenants, JSONConstants.TENANTS, JSONConstants.TENANTS, entityStream, new TenantEntryTransformer());
    }

    private static class TenantEntryTransformer implements JsonArrayEntryTransformer {

        @Override
        public void transform(JSONObject arrayEntry) {
            if (arrayEntry.containsKey("domainId")) {
                Object domainId = arrayEntry.get("domainId");
                arrayEntry.remove("domainId");
                arrayEntry.put(JSONConstants.RAX_AUTH_DOMAIN_ID, domainId);
            }

            if (arrayEntry.containsKey(JSONConstants.TYPES)) {
                arrayEntry.remove(JSONConstants.TYPES);
            }
        }
    }
}
