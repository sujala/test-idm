package com.rackspace.idm.api.resource.cloud.v20.JSONReaders;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
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
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.LinkedHashMap;

import static com.rackspace.idm.JSONConstants.*;

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
        prefixValues.put("auth.RAX-KSKEY:apiKeyCredentials", API_KEY_CREDENTIALS);
        prefixValues.put("auth.RAX-AUTH:rsaCredentials", RSA_CREDENTIALS);
        prefixValues.put("auth.RAX-AUTH:domain", DOMAIN);

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
                jsonObject = prefixMapper.addPrefix(outer, prefixValues);
            }else {
                jsonObject = outer;
            }

            ObjectMapper om = new ObjectMapper();
            om.setAnnotationIntrospector(new JaxbAnnotationIntrospector(TypeFactory.defaultInstance()));
            om.configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true);

            CredentialType credentialType = null;

            for(Object object : jsonObject.values()){
                if(((JSONObject)object).containsKey(API_KEY_CREDENTIALS)){
                    JSONObject innerObject = (JSONObject) object;
                    String string = innerObject.toString();
                    credentialType = om.readValue(string.getBytes(), ApiKeyCredentials.class);
                    ((JSONObject)jsonObject.get(AUTH)).remove(API_KEY_CREDENTIALS);
                }
                if(((JSONObject)object).containsKey(PASSWORD_CREDENTIALS)){
                    JSONObject innerObject = (JSONObject) object;
                    String string = innerObject.toString();
                    credentialType = om.readValue(string.getBytes(), PasswordCredentialsBase.class);
                    ((JSONObject)jsonObject.get(AUTH)).remove(PASSWORD_CREDENTIALS);
                }
                if(((JSONObject)object).containsKey(RSA_CREDENTIALS)){
                    JSONObject innerObject = (JSONObject) object;
                    String string = innerObject.toString();
                    credentialType = om.readValue(string.getBytes(), RsaCredentials.class);
                    ((JSONObject)jsonObject.get(AUTH)).remove(RSA_CREDENTIALS);
                }
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
}
