package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.google.gson.JsonArray;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorDevices;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JsonArrayEntryTransformer;
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
import java.util.HashMap;
import java.util.LinkedHashMap;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxAuthMultiFactorDevices extends JSONWriterForEntity<MultiFactorDevices> {

    @Override
    public void writeTo(MultiFactorDevices multiFactor, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.MULTIFACTOR_DEVICES, JSONConstants.RAX_AUTH_MULTIFACTOR_DEVICES);
        write(multiFactor, entityStream, prefixValues, ALWAYS_PLURALIZE_HANDLER, new EntryTransformer());
    }

    private class EntryTransformer implements JsonArrayEntryTransformer {
        @Override
        public void transform(JSONObject entry) {
            JSONObject outer = (JSONObject) entry.get(JSONConstants.RAX_AUTH_MULTIFACTOR_DEVICES);

            if (outer == null || outer.get(JSONConstants.MOBILE_PHONES) == null) {
                outer.put(JSONConstants.MOBILE_PHONES, new JsonArray());
            }

            if (outer == null || outer.get(JSONConstants.OTP_DEVICES) == null) {
                outer.put(JSONConstants.OTP_DEVICES, new JsonArray());
            }
        }
    }
}