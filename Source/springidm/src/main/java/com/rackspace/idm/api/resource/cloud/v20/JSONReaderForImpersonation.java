package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_ga.v1.ImpersonationRequest;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openstack.docs.identity.api.v2.User;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 3/19/12
 * Time: 3:01 PM
 * To change this template use File | Settings | File Templates.
 */

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForImpersonation implements MessageBodyReader<ImpersonationRequest> {

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == ImpersonationRequest.class;
    }

    @Override
    public ImpersonationRequest readFrom(Class<ImpersonationRequest> type, Type genericType,
                                         Annotation[] annotations, MediaType mediaType,
                                         MultivaluedMap<String, String> httpHeaders,
                                         InputStream entityStream) throws IOException, WebApplicationException {
        String jsonBody = IOUtils.toString(entityStream, JSONConstants.UTF_8);
        ImpersonationRequest request = getImpersonationFromJSONString(jsonBody);
        return request;
    }

    public static ImpersonationRequest getImpersonationFromJSONString(String jsonBody) {
        ImpersonationRequest request = new ImpersonationRequest();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.IMPERSONATION)) {
                JSONObject jsonImpersonation = (JSONObject) parser.parse(outer.get(JSONConstants.IMPERSONATION).toString());
                JSONObject jsonUser = (JSONObject) parser.parse(jsonImpersonation.get(JSONConstants.USER).toString());
                Object username = jsonUser.get(JSONConstants.USERNAME);

                User user = new User();
                if (username != null) {
                    user.setUsername(username.toString());
                }

                Object expireInSecondsObject = jsonImpersonation.get(JSONConstants.IMPERSONATION_EXPIRE_IN_SECONDS);
                if (expireInSecondsObject != null) {
                    String expireInSeconds = expireInSecondsObject.toString();
                    if (expireInSeconds != null) {
                        request.setExpireInSeconds(Integer.parseInt(expireInSeconds));
                    }
                }
                request.setUser(user);
            }
        } catch (NumberFormatException e) {
            throw new BadRequestException("Expire-in element should be an integer.");
        } catch (Exception e) {
            throw new BadRequestException("Invalid json request body");
        }

        return request;
    }
}
