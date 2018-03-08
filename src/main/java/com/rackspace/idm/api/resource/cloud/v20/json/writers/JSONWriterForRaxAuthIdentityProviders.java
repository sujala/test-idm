package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviders;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.AlwaysPluralizeJsonArrayTransformerHandler;
import com.rackspace.idm.api.resource.cloud.JsonArrayEntryTransformer;
import com.rackspace.idm.domain.config.IdentityConfigHolder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxAuthIdentityProviders extends JSONWriterForArrayEntity<IdentityProviders> {

    @Override
    public void writeTo(IdentityProviders identityProviders, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        if (IdentityConfigHolder.IDENTITY_CONFIG.getReloadableConfig().listIdpsAlwaysReturnsApprovedDomainIds()) {
            write(identityProviders, JSONConstants.IDENTITY_PROVIDERS, JSONConstants.RAX_AUTH_IDENTITY_PROVIDERS, entityStream, new AlwaysPluralizeJsonArrayTransformerHandler(), new EntryTransformer());
        } else {
            write(identityProviders, JSONConstants.IDENTITY_PROVIDERS, JSONConstants.RAX_AUTH_IDENTITY_PROVIDERS, entityStream, new AlwaysPluralizeJsonArrayTransformerHandler());
        }
    }

    private class EntryTransformer implements JsonArrayEntryTransformer {
        @Override
        public void transform(JSONObject entry) {
            if (entry != null) {
                Object domainIds = entry.get(JSONConstants.APPROVED_DOMAIN_IDS);

                if (domainIds == null) {
                    JSONArray ar =  new JSONArray();
                    // Add or overwrite any existing entry in the json object
                    entry.put(JSONConstants.APPROVED_DOMAIN_IDS, ar);
                }
            }
        }
    }
}