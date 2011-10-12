package com.rackspace.idm.api.resource.cloud.v20;

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
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForAuthenticationRequest implements
    MessageBodyReader<AuthenticationRequest> {

    private static final com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory OBJ_FACTORY_API_KEY = new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory();
    private static final org.openstack.docs.identity.api.v2.ObjectFactory OBJ_FACTORY_PASSWORD = new org.openstack.docs.identity.api.v2.ObjectFactory();

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == AuthenticationRequest.class;
    }

    @Override
    public AuthenticationRequest readFrom(Class<AuthenticationRequest> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException, WebApplicationException {

        String jsonBody = IOUtils.toString(inputStream, "UTF-8");

        AuthenticationRequest auth = new AuthenticationRequest();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey("auth")) {
                JSONObject obj3;

                obj3 = (JSONObject) parser.parse(outer.get("auth").toString());
                Object tenantId = obj3.get("tenantId");
                Object tenantName = obj3.get("tenantName");

                if (tenantId != null) {
                    auth.setTenantId(tenantId.toString());
                }
                if (tenantName != null) {
                    auth.setTenantName(tenantName.toString());
                }
                JSONObject obj4;
                if (obj3.containsKey("RAX-KSKEY:apiKeyCredentials")) {
                    obj4 = (JSONObject) parser.parse(obj3.get(
                        "RAX-KSKEY:apiKeyCredentials").toString());

                    ApiKeyCredentials creds = new ApiKeyCredentials();

                    Object username = obj4.get("username");
                    Object apiKey = obj4.get("apiKey");

                    if (username != null) {
                        creds.setUsername(username.toString());
                    }
                    if (apiKey != null) {
                        creds.setApiKey(apiKey.toString());
                    }

                    auth.setCredential(OBJ_FACTORY_API_KEY
                        .createApiKeyCredentials(creds));

                } else if (obj3.containsKey("passwordCredentials")) {
                    obj4 = (JSONObject) parser.parse(obj3.get(
                        "passwordCredentials").toString());

                    PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername();

                    Object username = obj4.get("username");
                    Object password = obj4.get("password");

                    if (username != null) {
                        creds.setUsername(username.toString());
                    }
                    if (password != null) {
                        creds.setPassword(password.toString());
                    }

                    auth.setCredential(OBJ_FACTORY_PASSWORD
                        .createPasswordCredentials(creds));
                }
            }
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return auth;
    }
}
