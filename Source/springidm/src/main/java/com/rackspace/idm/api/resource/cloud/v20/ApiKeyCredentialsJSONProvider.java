package com.rackspace.idm.api.resource.cloud.v20;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

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
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;
import org.openstack.docs.identity.api.v2.CredentialListType;
import org.openstack.docs.identity.api.v2.CredentialType;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class ApiKeyCredentialsJSONProvider implements
    MessageBodyWriter<JAXBElement<?>> {

    @Override
    public long getSize(JAXBElement<?> arg0, Class<?> arg1, Type arg2,
        Annotation[] arg3, MediaType arg4) {
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

        if (object.getDeclaredType().isAssignableFrom(EndpointTemplate.class)) {

            EndpointTemplate template = (EndpointTemplate) object.getValue();
            String jsonText = JSONValue.toJSONString(getEndpointTemplate(template));
            outputStream.write(jsonText.getBytes("UTF-8"));
            
        } else if (object.getDeclaredType().isAssignableFrom(EndpointTemplateList.class)) {

            EndpointTemplateList templates = (EndpointTemplateList) object.getValue();
            String jsonText = JSONValue.toJSONString(getEndpointTemplateList(templates));
            outputStream.write(jsonText.getBytes("UTF-8"));
            
        } else if (object.getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)) {

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
    
    @SuppressWarnings("unchecked")
    private JSONObject getEndpointTemplate(EndpointTemplate template) {
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();

        outer.put("OS-KSCATALOG:endpointTemplate", inner);
        inner.put("id", template.getId());
        inner.put("adminURL", template.getAdminURL());
        inner.put("internalURL", template.getInternalURL());
        inner.put("name", template.getName());
        inner.put("publicURL", template.getPublicURL());
        inner.put("type", template.getType());
        inner.put("region", template.getRegion());
        inner.put("global", template.isGlobal());
        inner.put("enabled", template.isEnabled());
        if (template.getVersion() != null) {
            inner.put("versionId", template.getVersion().getId());
            inner.put("versionInfo", template.getVersion().getInfo());
            inner.put("versionList", template.getVersion().getList());
        }
        return outer;
    }
    
    @SuppressWarnings("unchecked")
    private JSONObject getEndpointTemplateList(EndpointTemplateList templateList) {
        JSONObject outer = new JSONObject();
        JSONArray list = new JSONArray();

        outer.put("OS-KSCATALOG:endpointTemplates", list);
        
        for (EndpointTemplate template : templateList.getEndpointTemplate()) {
            list.add(getEndpointTemplate(template));
        }

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
}
