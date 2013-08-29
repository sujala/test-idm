package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.idm.JSONConstants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openstack.docs.identity.api.v2.*;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBElement;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import static com.rackspace.idm.api.resource.cloud.JsonWriterHelper.*;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/8/13
 * Time: 3:25 PM
 * To change this template use File | Settings | File Templates.
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForAuthenticateResponse implements MessageBodyWriter<AuthenticateResponse> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == AuthenticateResponse.class;
    }

    @Override
    public long getSize(AuthenticateResponse authenticateResponse, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(AuthenticateResponse authenticateResponse, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outputStream) throws IOException, WebApplicationException {
        JSONObject outer = new JSONObject();
        JSONObject access = new JSONObject();
        access.put(JSONConstants.TOKEN, getToken(authenticateResponse.getToken()));

        if (authenticateResponse.getServiceCatalog() != null) {
            access.put(JSONConstants.SERVICECATALOG, getServiceCatalog(authenticateResponse.getServiceCatalog()));
        }

        if (authenticateResponse.getUser() != null) {
            access.put(JSONConstants.USER, getTokenUser(authenticateResponse.getUser()));
        }
        outer.put(JSONConstants.ACCESS, access);
        if (authenticateResponse.getAny().size() > 0) {
            for (Object response : authenticateResponse.getAny()) {
                if (response instanceof JAXBElement && ((JAXBElement) response).getDeclaredType().isAssignableFrom(UserForAuthenticateResponse.class)) {
                    UserForAuthenticateResponse userForAuthenticateResponse = (UserForAuthenticateResponse) ((JAXBElement) response).getValue();

                    JSONObject subAccess = new JSONObject();
                    subAccess.put(JSONConstants.ID, userForAuthenticateResponse.getId());
                    subAccess.put(JSONConstants.NAME, userForAuthenticateResponse.getName());

                    JSONArray subRoles = new JSONArray();

                    for (Role role : userForAuthenticateResponse.getRoles().getRole()) {
                        JSONObject subRole = new JSONObject();
                        subRole.put(JSONConstants.SERVICE_ID, role.getServiceId());
                        subRole.put(JSONConstants.DESCRIPTION, role.getDescription());
                        subRole.put(JSONConstants.NAME, role.getName());
                        subRole.put(JSONConstants.ID, role.getId());

                        subRoles.add(subRole);
                    }

                    subAccess.put(JSONConstants.ROLES, subRoles);
                    access.put(JSONConstants.RAX_AUTH_IMPERSONATOR, subAccess);
                    break;
                }
            }
        }
        String jsonText = JSONValue.toJSONString(outer);
        outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
    }
}
