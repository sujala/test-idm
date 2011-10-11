package com.rackspace.idm.api.resource.cloud.v20;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openstack.docs.identity.api.v2.CredentialListType;
import org.openstack.docs.identity.api.v2.CredentialType;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ApiKeyCredentialsJSONProvider implements
    MessageBodyWriter<JAXBElement<?>> {

    @Override
    public long getSize(JAXBElement<?> arg0, Class<?> arg1, Type arg2,
        Annotation[] arg3, MediaType arg4) {
        // TODO Auto-generated method stub
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {

        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeTo(JAXBElement<?> object, Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, Object> httpHeaders, OutputStream outputStream)
        throws IOException, WebApplicationException {

        if (object.getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)) {

            ApiKeyCredentials creds = (ApiKeyCredentials) object.getValue();

            String jsonText = JSONValue.toJSONString(getApiKeyCredentials(creds));

            outputStream.write(jsonText.getBytes("UTF-8"));
        } else if (object.getDeclaredType().isAssignableFrom(CredentialListType.class)) {
            JSONObject outer = new JSONObject();
            JSONArray list = new JSONArray();
            
            CredentialListType credsList = (CredentialListType) object.getValue();
            
            outer.put("credentials", list);
            
            for ( JAXBElement<? extends CredentialType> cred : credsList.getCredential()) {
                if (cred.getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)) {
                    list.add(getApiKeyCredentials((ApiKeyCredentials)cred.getValue()));
                } else if (cred.getDeclaredType().isAssignableFrom(PasswordCredentialsRequiredUsername.class)){
                    list.add(getPasswordCredentials((PasswordCredentialsRequiredUsername)cred.getValue()));
                }
            }
            
            String jsonText = JSONValue.toJSONString(outer);

            outputStream.write(jsonText.getBytes("UTF-8"));
            
        } else {
            try {
                getMarshaller().marshallToJSON(object, outputStream);
            } catch (JAXBException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private JSONObject getApiKeyCredentials(ApiKeyCredentials creds) {
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();

        outer.put("RAX-KSKEY:apiKeyCredentials", inner);
        inner.put("username", creds.getUsername());
        inner.put("apiKey", creds.getApiKey());
        return outer;
    }
    
    @SuppressWarnings("unchecked")
    private JSONObject getPasswordCredentials(PasswordCredentialsRequiredUsername creds) {
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();

        outer.put("passwordCredentials", inner);
        inner.put("username", creds.getUsername());
        inner.put("password", creds.getPassword());
        return outer;
    }

    private JSONMarshaller marshaller;

    private JSONMarshaller getMarshaller() throws JAXBException {
        if (marshaller == null) {
            JSONJAXBContext context = (JSONJAXBContext) JAXBContextResolver
                .get();
                marshaller = context.createJSONMarshaller();
        }
        return marshaller;
    }

    // @Override
    // public boolean isReadable(Class<?> type, Type genericType,
    // Annotation[] annotations, MediaType mediaType) {
    // return ApiKeyCredentials.class == type;
    // }
    //
    // @Override
    // public JAXBElement<ApiKeyCredentials>
    // readFrom(Class<JAXBElement<ApiKeyCredentials>> type,
    // Type genericType, Annotation[] annotations, MediaType mediaType,
    // MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
    // throws IOException, WebApplicationException {
    //
    // String jsonBody = IOUtils.toString(inputStream, "UTF-8");
    //
    // JSONParser parser = new JSONParser();
    //
    // ApiKeyCredentials userCreds = new ApiKeyCredentials();
    //
    // try {
    // JSONObject obj = (JSONObject) parser.parse(jsonBody);
    // JSONObject obj3 = (JSONObject) parser.parse(obj.get(
    // "RAX-KSKEY:apiKeyCredentials").toString());
    // String username = obj3.get("username").toString();
    // String apikey = obj3.get("apiKey").toString();
    // if (StringUtils.isBlank(username)) {
    // throw new BadRequestException("Expecting username");
    // }
    // if (StringUtils.isBlank(apikey)) {
    // throw new BadRequestException("Expecting apiKey");
    // }
    // userCreds.setUsername(username);
    // userCreds.setApiKey(apikey);
    //
    // } catch (ParseException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // return
    // OBJ_FACTORIES.getRackspaceIdentityExtKskeyV1Factory().createApiKeyCredentials(userCreds);
    // }
}
