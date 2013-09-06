package com.rackspace.idm.api.resource.cloud.v20.JSONReaders;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RsaCredentials;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import org.openstack.docs.identity.api.v2.TokenForAuthenticationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class JSONReaderForAuthenticationRequest implements
    MessageBodyReader<AuthenticationRequest> {

    private static final com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory OBJ_FACTORY_API_KEY = new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory();
    private static final org.openstack.docs.identity.api.v2.ObjectFactory OBJ_FACTORY_PASSWORD = new org.openstack.docs.identity.api.v2.ObjectFactory();
    private static final Logger LOGGER = LoggerFactory.getLogger(JSONReaderForAuthenticationRequest.class);
    private static final ObjectFactory OBJECT_FACTORY_RAX_AUTH = new ObjectFactory();

    @Override
    public boolean isReadable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == AuthenticationRequest.class;
    }

    @Override
    public AuthenticationRequest readFrom(Class<AuthenticationRequest> type,
        Type genericType, Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

        String jsonBody = IOUtils.toString(inputStream, JSONConstants.UTF_8);

        AuthenticationRequest auth = getAuthenticationRequestFromJSONString(jsonBody);

        return auth;
    }

    public static AuthenticationRequest getAuthenticationRequestFromJSONString(String jsonBody) {
        AuthenticationRequest auth = new AuthenticationRequest();

        try {
            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);

            if (outer.containsKey(JSONConstants.AUTH)) {
                JSONObject objAuth;

                objAuth = (JSONObject) parser.parse(outer.get(JSONConstants.AUTH).toString());
                Object tenantId = objAuth.get(JSONConstants.TENANT_ID);
                Object tenantName = objAuth.get(JSONConstants.TENANT_NAME);

                if (tenantId != null) {
                    auth.setTenantId(tenantId.toString());
                }
                if (tenantName != null) {
                    auth.setTenantName(tenantName.toString());
                }

                if(objAuth.containsKey(JSONConstants.DOMAIN)){
                    JSONObject domainObj = (JSONObject)objAuth.get(JSONConstants.DOMAIN);
                    Domain domain = JSONReaderForDomain.getDomainFromInnerJSONString(domainObj);
                    auth.getAny().add(domain);
                }

                if (objAuth.containsKey(JSONConstants.TOKEN)) {
                    JSONObject objToken = (JSONObject) parser.parse(objAuth.get(JSONConstants.TOKEN).toString());
                    TokenForAuthenticationRequest token = new TokenForAuthenticationRequest();
                    Object id = objToken.get(JSONConstants.ID);
                    if (id != null) {
                        token.setId(id.toString());
                    }
                    auth.setToken(token);
                }

                if (objAuth.containsKey(JSONConstants.PASSWORD_CREDENTIALS)) {
                    JSONObject credObj = (JSONObject)objAuth.get(JSONConstants.PASSWORD_CREDENTIALS);
                    PasswordCredentialsRequiredUsername creds = JSONReaderForPasswordCredentials
                        .getPasswordCredentialsFromInnerJSONObject(credObj);
                    auth.setCredential(OBJ_FACTORY_PASSWORD.createPasswordCredentials(creds));
                } else if (objAuth.containsKey(JSONConstants.RAX_AUTH_RSA)) {
                    JSONObject credObj = (JSONObject)objAuth.get(JSONConstants.RAX_AUTH_RSA);
                    RsaCredentials creds = JSONReaderForRSACredentials.getRSACredentialsFromInnerJSONObject(credObj);
                    auth.setCredential(OBJECT_FACTORY_RAX_AUTH.createRsaCredentials(creds));
                }
            }
        } catch (ParseException e) {
            LOGGER.info(e.toString());
            throw new BadRequestException("JSON Parsing error", e);
        }

        return auth;
    }
}
