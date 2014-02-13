package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.idm.api.resource.cloud.JsonPrefixMapper;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.exception.BadRequestException;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.CredentialType;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.rackspace.idm.JSONConstants.*;


@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForAuthenticationRequest implements MessageBodyWriter<AuthenticationRequest> {

    private JsonPrefixMapper prefixMapper = new JsonPrefixMapper();

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == AuthenticationRequest.class;
    }

    @Override
    public long getSize(AuthenticationRequest authenticationRequest, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(AuthenticationRequest authenticationRequest, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        HashMap<String, String> prefixValues = new LinkedHashMap<String, String>();
        prefixValues.put(AUTH_API_KEY_CREDENTIALS_PATH, RAX_KSKEY_API_KEY_CREDENTIALS);
        prefixValues.put(AUTH_RSA_CREDENTIALS_PATH, RAX_AUTH_RSA_CREDENTIALS);
        prefixValues.put(AUTH_DOMAIN_PATH, RAX_AUTH_DOMAIN);
        prefixValues.put(AUTH_PASSCODE_CREDENTIALS_PATH, RAX_AUTH_PASSCODE_CREDENTIALS);

        write(authenticationRequest, entityStream, prefixValues);
    }

    protected void write(AuthenticationRequest entity, OutputStream entityStream, HashMap prefixValues) {

        try {
            JSONObject cred = null;
            if(entity.getCredential() != null){
                CredentialType credentialType = entity.getCredential().getValue();
                entity.setCredential(null);
                cred = getObject(credentialType);
            }

            JSONObject auth = getObject(entity);

            if(cred != null){
                String key = null;
                Object value = null;
                if(cred.get(API_KEY_CREDENTIALS) != null){
                    key = API_KEY_CREDENTIALS;
                    value = cred.get(API_KEY_CREDENTIALS);
                } else if(cred.get(PASSWORD_CREDENTIALS) != null){
                    key = PASSWORD_CREDENTIALS;
                    value = cred.get(PASSWORD_CREDENTIALS);
                } else if(cred.get(RSA_CREDENTIALS) != null){
                    key = RSA_CREDENTIALS;
                    value = cred.get(RSA_CREDENTIALS);
                } else if(cred.get(PASSCODE_CREDENTIALS) != null){
                    key = PASSCODE_CREDENTIALS;
                    value = cred.get(PASSCODE_CREDENTIALS);
                }
                JSONObject authVal = (JSONObject) auth.get(AUTH);
                if (authVal == null) {
                    authVal = new JSONObject();
                    auth.put(AUTH, authVal);
                }
                if (key != null) {
                    ((JSONObject) auth.get(AUTH)).put(key, value);
                }
            }

            JSONObject jsonObject;

            if(prefixValues != null){
                jsonObject = prefixMapper.mapPrefix(auth, prefixValues);
            }else{
                jsonObject = auth;
            }

            String newJsonString = jsonObject.toString();
            entityStream.write(newJsonString.getBytes(UTF_8));
        } catch (UnsupportedEncodingException e) {
            throw new BadRequestException("Parameters are not valid.", e);
        } catch (IOException e) {
            throw new BadRequestException("Parameters are not valid.", e);
        }
    }

    JSONObject getObject(Object object){
        try {
            OutputStream outputStream = new ByteArrayOutputStream();
            getMarshaller().marshallToJSON(object, outputStream);
            JSONParser parser = new JSONParser();
            String jsonString = outputStream.toString();
            return (JSONObject) parser.parse(jsonString);
        } catch (JAXBException e){
            throw new BadRequestException("Parameters are not valid.", e);
        } catch (ParseException e) {
            throw new BadRequestException("Parameters are not valid.", e);
        }

    }

    JSONMarshaller getMarshaller() throws JAXBException {
        return ((JSONJAXBContext) JAXBContextResolver.get()).createJSONMarshaller();
    }
}
