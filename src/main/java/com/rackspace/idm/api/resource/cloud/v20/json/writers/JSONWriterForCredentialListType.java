package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.JSONConstants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openstack.docs.identity.api.v2.CredentialListType;
import org.openstack.docs.identity.api.v2.CredentialType;
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;

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

import static com.rackspace.idm.api.resource.cloud.JsonWriterHelper.getApiKeyCredentials;
import static com.rackspace.idm.api.resource.cloud.JsonWriterHelper.getPasswordCredentials;


@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriterForCredentialListType implements MessageBodyWriter<CredentialListType> {
    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type == CredentialListType.class;
    }

    @Override
    public long getSize(CredentialListType credentialListType, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return -1;
    }

    @Override
    public void writeTo(CredentialListType credentialListType, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outputStream) throws IOException, WebApplicationException {
        JSONObject outer = new JSONObject();
        JSONArray list = new JSONArray();
        outer.put(JSONConstants.CREDENTIALS, list);

        for (JAXBElement<? extends CredentialType> cred : credentialListType.getCredential()) {
            CredentialType credential = cred.getValue();
            if (credential instanceof ApiKeyCredentials) {
                list.add(getApiKeyCredentials((ApiKeyCredentials) cred.getValue()));
            } else if (credential instanceof PasswordCredentialsBase) {
                list.add(getPasswordCredentials((PasswordCredentialsBase) cred.getValue()));
            }
        }
        String jsonText = JSONValue.toJSONString(outer);
        outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
    }
}
