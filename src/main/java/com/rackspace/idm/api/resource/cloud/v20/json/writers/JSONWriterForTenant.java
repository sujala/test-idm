package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JsonArrayEntryTransformer;
import com.rackspace.idm.api.resource.cloud.JsonArrayTransformerHandler;
import org.json.simple.JSONObject;
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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForTenant extends JSONWriterForEntity<Tenant> {

    public static final TenantJsonArrayTransformerHandler TENANT_PLURALIZE_HANDLER = new TenantJsonArrayTransformerHandler();

    @Override
    public void writeTo(Tenant tenant, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        Map<String, String> prefixValues = new LinkedHashMap<String, String>();
        List<String> removeIfEmpty = new ArrayList<>();
        prefixValues.put(JSONConstants.TENANT_DOMAIN_ID_PATH, JSONConstants.RAX_AUTH_DOMAIN_ID);
        prefixValues.put(JSONConstants.TENANT_TYPES_PATH, JSONConstants.RAX_AUTH_TYPES);
        removeIfEmpty.add(JSONConstants.RAX_AUTH_TYPES);
        write(tenant, entityStream, prefixValues, new TenantJsonArrayTransformerHandler(), new TenantJsonArrayEntryTransformer(tenant));
    }

    public static class TenantJsonArrayTransformerHandler implements JsonArrayTransformerHandler {

        @Override
        public boolean pluralizeJSONArrayWithName(String elementName) {
            return true;
        }

        @Override
        public String getPluralizedNamed(String elementName) {
            if (JSONConstants.TYPE.equals(elementName)) {
                return JSONConstants.RAX_AUTH_TYPES;
            }
            return elementName + "s";
        }
    }

    private static class  TenantJsonArrayEntryTransformer implements JsonArrayEntryTransformer {

        private Tenant tenant;

        public TenantJsonArrayEntryTransformer(Tenant tenant) {
            this.tenant = tenant;
        }

        @Override
        public void transform(JSONObject arrayEntry) {
            if (arrayEntry.containsKey(JSONConstants.RAX_AUTH_TYPES)) {
                if (tenant.getTypes() == null) {
                    arrayEntry.remove(JSONConstants.RAX_AUTH_TYPES);
                }
            }
        }
    }
}
