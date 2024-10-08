package com.rackspace.idm.api.resource.cloud.v20.json.readers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationCredentials;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.PasscodeCredentials;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RsaCredentials;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.api.resource.cloud.JsonPrefixMapper;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.commons.io.IOUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.CredentialType;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;

import javax.ws.rs.Consumes;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.rackspace.idm.JSONConstants.*;
import static com.rackspace.idm.JSONConstants.AUTH_RAX_AUTH_DELEGATION_CREDENTIALS_PATH;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
public class JSONReaderForAuthenticationRequest implements MessageBodyReader<AuthenticationRequest> {

    private JsonPrefixMapper prefixMapper = new JsonPrefixMapper();
    private ObjectFactory objectFactory = new ObjectFactory();

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

        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(AUTH_RAX_KSKEY_API_KEY_CREDENTIALS_PATH, API_KEY_CREDENTIALS);
        prefixValues.put(AUTH_RAX_AUTH_RSA_CREDENTIALS_PATH, RSA_CREDENTIALS);
        prefixValues.put(AUTH_RAX_AUTH_PASSCODE_CREDENTIALS_PATH, PASSCODE_CREDENTIALS);
        prefixValues.put(AUTH_RAX_AUTH_DELEGATION_CREDENTIALS_PATH, DELEGATION_CREDENTIALS);
        prefixValues.put(AUTH_RAX_AUTH_DOMAIN_PATH, DOMAIN);
        prefixValues.put(AUTH_RAX_AUTH_SCOPE_PATH, SCOPE);
        prefixValues.put(AUTH_RAX_AUTH_DOMAINID_PATH, DOMAIN_ID);

        return read(inputStream, AUTH, prefixValues);
    }

    protected AuthenticationRequest read(InputStream entityStream, String rootValue, HashMap prefixValues) {
        try {

            String jsonBody = IOUtils.toString(entityStream, UTF_8);

            JSONParser parser = new JSONParser();
            JSONObject outer = (JSONObject) parser.parse(jsonBody);


            if (outer == null || outer.keySet().size() < 1) {
                throw new BadRequestException("Invalid json request body");
            }

            String rootElement = outer.keySet().iterator().next().toString();
            if(!rootElement.equals(rootValue)){
                throw new BadRequestException("Invalid json request body");
            }

            JSONObject jsonObject;

            if(prefixValues != null){
                jsonObject = prefixMapper.mapPrefix(outer, prefixValues);
            }else {
                jsonObject = outer;
            }

            ObjectMapper om = new ObjectMapper();
            om.setAnnotationIntrospector(new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()));
            om.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);

            CredentialType credentialType = null;
            JSONObject innerObject = new JSONObject();

            JSONObject auth = (JSONObject) jsonObject.get(AUTH);

            if(auth.containsKey(API_KEY_CREDENTIALS)){
                innerObject.put(API_KEY_CREDENTIALS, auth.get(API_KEY_CREDENTIALS));
                removeExtraCredentialAttributes((JSONObject)innerObject.get(API_KEY_CREDENTIALS));
                String string = innerObject.toString();
                credentialType = om.readValue(string.getBytes(), ApiKeyCredentials.class);
                ((JSONObject)jsonObject.get(AUTH)).remove(API_KEY_CREDENTIALS);
            } else if(auth.containsKey(PASSWORD_CREDENTIALS)){
                innerObject.put(PASSWORD_CREDENTIALS, auth.get(PASSWORD_CREDENTIALS));
                removeExtraCredentialAttributes((JSONObject)innerObject.get(PASSWORD_CREDENTIALS));
                String string = innerObject.toString();
                credentialType = om.readValue(string.getBytes(), PasswordCredentialsBase.class);
                ((JSONObject)jsonObject.get(AUTH)).remove(PASSWORD_CREDENTIALS);
            } else if(auth.containsKey(RSA_CREDENTIALS)){
                innerObject.put(RSA_CREDENTIALS, auth.get(RSA_CREDENTIALS));
                removeExtraCredentialAttributes((JSONObject)innerObject.get(RSA_CREDENTIALS));
                String string = innerObject.toString();
                credentialType = om.readValue(string.getBytes(), RsaCredentials.class);
                ((JSONObject)jsonObject.get(AUTH)).remove(RSA_CREDENTIALS);
            } else if(auth.containsKey(PASSCODE_CREDENTIALS)){
                innerObject.put(PASSCODE_CREDENTIALS, auth.get(PASSCODE_CREDENTIALS));
                removeExtraCredentialAttributes((JSONObject)innerObject.get(PASSCODE_CREDENTIALS));
                String string = innerObject.toString();
                credentialType = om.readValue(string.getBytes(), PasscodeCredentials.class);
                ((JSONObject)jsonObject.get(AUTH)).remove(PASSCODE_CREDENTIALS);
            } else if(auth.containsKey(DELEGATION_CREDENTIALS)){
                innerObject.put(DELEGATION_CREDENTIALS, auth.get(DELEGATION_CREDENTIALS));
                removeExtraCredentialAttributes((JSONObject)innerObject.get(DELEGATION_CREDENTIALS));
                String string = innerObject.toString();
                credentialType = om.readValue(string.getBytes(), DelegationCredentials.class);
                ((JSONObject)jsonObject.get(AUTH)).remove(DELEGATION_CREDENTIALS);
            }

            String jsonString = jsonObject.toString();

            AuthenticationRequest authenticationRequest = om.readValue(jsonString.getBytes(), AuthenticationRequest.class);

            if(credentialType != null){
                authenticationRequest.setCredential(objectFactory.createCredential(credentialType));
            }

            return authenticationRequest;
        } catch (ParseException e) {
            throw new BadRequestException("Invalid json request body");
        } catch (IOException e) {
            throw new BadRequestException("Invalid json request body");
        }
    }


    /*
     * HACK - remove once auth plugin is fixed
     */
    private void removeExtraCredentialAttributes(JSONObject object){
        object.remove(JSONConstants.TENANT_ID);
        object.remove(JSONConstants.TENANT_NAME);
    }
}
