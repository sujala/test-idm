package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.BypassCodes;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;


@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForBypassCodes extends JSONWriterForEntity<BypassCodes> {

    public static final String CODES = "codes";

    @Override
    public void writeTo(BypassCodes bypassCodes, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        final HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.BYPASS_CODES, JSONConstants.RAX_AUTH_BYPASS_CODES);
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        write(bypassCodes, buffer, prefixValues, NEVER_PLURALIZE_HANDLER);

        try {
            final JSONParser parser = new JSONParser();
            final JSONObject object = (JSONObject) parser.parse(new String(buffer.toByteArray(), "UTF8"));
            final JSONObject inner = (JSONObject) object.get(JSONConstants.RAX_AUTH_BYPASS_CODES);
            final JSONArray array = (JSONArray) inner.get(CODES);

            if (array != null) {
                // Workaround to fix the "codes" XML attribute JSON representation
                final String codes = (String) array.get(0);
                final JSONArray newCodes = new JSONArray();
                inner.put(CODES, newCodes);
                for (String code : codes.split(" ")) {
                    newCodes.add(code);
                }
            }

            // Save
            entityStream.write(object.toJSONString().getBytes(JSONConstants.UTF_8));
        } catch (Exception e) {
            throw new BadRequestException("Parameters are not valid.", e);
        }
    }

}
