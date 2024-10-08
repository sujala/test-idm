package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.openstack.docs.identity.api.v2.User;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;


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
                                         InputStream entityStream) throws IOException {
        String jsonBody = IOUtils.toString(entityStream, JSONConstants.UTF_8);
        return getImpersonationFromJSONString(jsonBody);
    }

    public static ImpersonationRequest getImpersonationFromJSONString(String jsonBody) {
        ImpersonationRequest request = new ImpersonationRequest();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.RAX_AUTH_IMPERSONATION)) {
                JSONObject jsonImpersonation = (JSONObject) parser.parse(outer.get(JSONConstants.RAX_AUTH_IMPERSONATION).toString());
                JSONObject jsonUser = (JSONObject) parser.parse(jsonImpersonation.get(JSONConstants.USER).toString());
                Object username = jsonUser.get(JSONConstants.USERNAME);
                Object federatedIdp = jsonUser.get(JSONConstants.RAX_AUTH_FEDERATED_IDP);

                User user = new User();
                if (username != null) {
                    user.setUsername(username.toString());
                }
                if (federatedIdp != null) {
                    user.setFederatedIdp(federatedIdp.toString());
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
            throw new BadRequestException("Expire-in element should be an integer.", e);
        } catch (Exception e) {
            throw new BadRequestException("Invalid json request body", e);
        }

        return request;
    }
}
