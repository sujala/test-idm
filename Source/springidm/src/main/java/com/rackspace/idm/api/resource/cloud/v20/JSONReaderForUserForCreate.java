package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForUserForCreate implements MessageBodyReader<UserForCreate> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations,
                              MediaType mediaType) {
        return type == UserForCreate.class;
    }

    @Override
    public UserForCreate readFrom(Class<UserForCreate> type,
                                  Type genericType, Annotation[] annotations, MediaType mediaType,
                                  MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
            throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        UserForCreate object = getUserFromJSONString(jsonBody);

        return object;
    }

    public static UserForCreate getUserFromJSONString(String jsonBody) {
        UserForCreate user = new UserForCreate();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.USER)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(JSONConstants.USER).toString());

                Object id = obj3.get(JSONConstants.ID);
                Object username = obj3.get(JSONConstants.USERNAME);
                Object email = obj3.get(JSONConstants.EMAIL);
                Object displayname = obj3.get(JSONConstants.DISPLAY_NAME_CLOUD);
                Object enabled = obj3.get(JSONConstants.ENABLED);
                Object password = obj3.get(JSONConstants.OS_KSADM_PASSWORD);
                Object defaultRegion = obj3.get(JSONConstants.OS_KSADM_DEFAULT_REGION);

                if (defaultRegion != null) {
                    user.getOtherAttributes().put(new QName(JSONConstants.OS_KSADM_DEFAULT_REGION.toString()), defaultRegion.toString());
                }

                if (password != null) {
                    user.setPassword(password.toString());
                }
                if (id != null) {
                    user.setId(id.toString());
                }
                if (enabled != null) {
                    user.setEnabled(Boolean.valueOf(enabled.toString()));
                }
                if (username != null) {
                    user.setUsername(username.toString());
                }
                if (email != null) {
                    user.setEmail(email.toString());
                }
                if (displayname != null) {
                    user.setDisplayName(displayname.toString());
                }


            }
        } catch (ParseException e) {
            throw new BadRequestException("Unable to parse request");
        }

        return user;
    }
}

