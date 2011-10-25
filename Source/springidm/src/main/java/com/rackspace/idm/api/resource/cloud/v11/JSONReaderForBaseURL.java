package com.rackspace.idm.api.resource.cloud.v11;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.rackspace.idm.JSONConstants;
import com.rackspacecloud.docs.auth.api.v1.BaseURL;
import com.rackspacecloud.docs.auth.api.v1.UserType;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForBaseURL implements MessageBodyReader<BaseURL>{

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == BaseURL.class;
    }

    @Override
    public BaseURL readFrom(Class<BaseURL> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        BaseURL object = getBaseURLFromJSONString(jsonBody);

        return object;
    }
    
    public static BaseURL getBaseURLFromJSONString(String jsonBody) {
        BaseURL baseurl = new BaseURL();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.ENDPOINT_TEMPLATE)) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get(
                    JSONConstants.ENDPOINT_TEMPLATE).toString());
                
                Object id = obj3.get(JSONConstants.ID);
                Object adminURL = obj3.get(JSONConstants.ADMIN_URL);
                Object internalURL = obj3.get(JSONConstants.INTERNAL_URL);
                Object userType = obj3.get(JSONConstants.USER_TYPE);
                Object publicURL = obj3.get(JSONConstants.PUBLIC_URL);
                Object serviceName = obj3.get(JSONConstants.SERVICE_NAME);
                Object region = obj3.get(JSONConstants.REGION);
                Object def = obj3.get(JSONConstants.DEFAULT);
                Object enabled = obj3.get(JSONConstants.ENABLED);

                if (id != null) {
                    baseurl.setId(Integer.parseInt(id.toString()));
                }
                if (adminURL != null) {
                    baseurl.setAdminURL(adminURL.toString());
                }
                if (internalURL != null) {
                    baseurl.setInternalURL(internalURL.toString());
                }
                if (userType != null) {
                    baseurl.setUserType(UserType.valueOf(userType.toString()));
                }
                if (publicURL != null) {
                    baseurl.setPublicURL(publicURL.toString());
                }
                if (serviceName != null) {
                    baseurl.setServiceName(serviceName.toString());
                }
                if (def != null) {
                    baseurl.setDefault(Boolean.valueOf(def.toString()));
                }
                if (region != null) {
                    baseurl.setRegion(region.toString());
                }

                if (enabled != null) {
                    baseurl.setEnabled(Boolean.parseBoolean(enabled.toString()));
                }

            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return baseurl;
    }
}
