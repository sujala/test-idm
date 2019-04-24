package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ValidatePassword;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.lang.StringUtils;
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
import java.util.LinkedHashMap;
import java.util.Map;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForRaxAuthValidatePassword extends JSONWriterForEntity<ValidatePassword> {

    public static final String CHECK_LIST = "nonPassingCheckNames";

    @Override
    public void writeTo(ValidatePassword validatePassword, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        Map<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(JSONConstants.VALIDATE_PWD, JSONConstants.RAX_AUTH_VALIDATE_PWD);
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        write(validatePassword, buffer, prefixValues, NEVER_PLURALIZE_HANDLER);

        try {
            final JSONParser parser = new JSONParser();
            final JSONObject object = (JSONObject) parser.parse(new String(buffer.toByteArray(), "UTF8"));
            final JSONObject inner = (JSONObject) object.get(JSONConstants.RAX_AUTH_VALIDATE_PWD);
            final String checks = (String) inner.get(CHECK_LIST);

            if (checks != null) {
                // Workaround to fix the "nonPassingCheckNames" XML attribute JSON representation
                final JSONArray checksArray = new JSONArray();
                inner.put(CHECK_LIST, checksArray);

                // Workaround to send empty array without blank character [" "] in response
                if (StringUtils.isNotBlank(checks)) {
                    for (String code : checks.split(" ")) {
                        checksArray.add(code);
                    }
                }
            }

            entityStream.write(object.toJSONString().getBytes(JSONConstants.UTF_8));
        } catch (Exception e) {
            throw new BadRequestException("Parameters are not valid.", e);
        }
    }

}
