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
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;
import org.openstack.docs.identity.api.v2.CredentialListType;
import org.openstack.docs.identity.api.v2.CredentialType;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriter implements
    MessageBodyWriter<JAXBElement<?>> {

    @Override
    public long getSize(JAXBElement<?> arg0, Class<?> arg1, Type arg2,
        Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType) {
        return type == JAXBElement.class;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeTo(JAXBElement<?> object, Class<?> type, Type genericType,
        Annotation[] annotations, MediaType mediaType,
        MultivaluedMap<String, Object> httpHeaders, OutputStream outputStream)
        throws IOException, WebApplicationException {
        
        if (object.getDeclaredType().isAssignableFrom(Service.class)) {

            Service service = (Service) object.getValue();
            String jsonText = JSONValue.toJSONString(getService(service));
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
            
        } else if (object.getDeclaredType().isAssignableFrom(ServiceList.class)) {

            ServiceList services = (ServiceList) object.getValue();
            String jsonText = JSONValue.toJSONString(getServiceList(services));
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
            
        } else if (object.getDeclaredType().isAssignableFrom(SecretQA.class)) {

            SecretQA secrets = (SecretQA) object.getValue();
            String jsonText = JSONValue.toJSONString(getSecretQA(secrets));
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
            
        } else if (object.getDeclaredType().isAssignableFrom(EndpointTemplate.class)) {

            EndpointTemplate template = (EndpointTemplate) object.getValue();
            String jsonText = JSONValue.toJSONString(getEndpointTemplate(template));
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
            
        } else if (object.getDeclaredType().isAssignableFrom(EndpointTemplateList.class)) {

            EndpointTemplateList templates = (EndpointTemplateList) object.getValue();
            String jsonText = JSONValue.toJSONString(getEndpointTemplateList(templates));
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
            
        } else if (object.getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)) {

            ApiKeyCredentials creds = (ApiKeyCredentials) object.getValue();
            String jsonText = JSONValue.toJSONString(getApiKeyCredentials(creds));
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
            
        } else if (object.getDeclaredType().isAssignableFrom(Groups.class)) {

            Groups groups = (Groups) object.getValue();
            String jsonText = JSONValue.toJSONString(getGroups(groups));
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        }else if (object.getDeclaredType().isAssignableFrom(CredentialListType.class)) {
            
            JSONObject outer = new JSONObject();
            JSONArray list = new JSONArray();
            
            CredentialListType credsList = (CredentialListType) object.getValue();
            outer.put(JSONConstants.CREDENTIALS, list);
            
            for ( JAXBElement<? extends CredentialType> cred : credsList.getCredential()) {
                if (cred.getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)) {
                    list.add(getApiKeyCredentials((ApiKeyCredentials)cred.getValue()));
                } else if (cred.getDeclaredType().isAssignableFrom(PasswordCredentialsRequiredUsername.class)){
                    list.add(getPasswordCredentials((PasswordCredentialsRequiredUsername)cred.getValue()));
                }
            }
            
            String jsonText = JSONValue.toJSONString(outer);
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
            
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

        outer.put(JSONConstants.APIKEY_CREDENTIALS, inner);
        inner.put(JSONConstants.USERNAME, creds.getUsername());
        inner.put(JSONConstants.API_KEY, creds.getApiKey());
        return outer;
    }
    
    @SuppressWarnings("unchecked")
    private JSONObject getPasswordCredentials(PasswordCredentialsRequiredUsername creds) {
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();

        outer.put(JSONConstants.PASSWORD_CREDENTIALS, inner);
        inner.put(JSONConstants.USERNAME, creds.getUsername());
        inner.put(JSONConstants.PASSWORD, creds.getPassword());
        return outer;
    }
    
    @SuppressWarnings("unchecked")
    private JSONObject getSecretQA(SecretQA secrets) {
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();

        outer.put(JSONConstants.SECRET_QA, inner);
        inner.put(JSONConstants.ANSWER, secrets.getAnswer());
        inner.put(JSONConstants.QUESTION, secrets.getQuestion());
        return outer;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getGroups(Groups groups){
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();
        JSONArray list = new JSONArray();

        outer.put(JSONConstants.GROUPS, inner);
        inner.put(JSONConstants.GROUP, list);

        for (Group group : groups.getGroup()) {
            list.add(getGroupWithoutWrapper(group));
        }

        return outer;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getGroup(Group group){
        JSONObject outer = new JSONObject();

        outer.put(JSONConstants.GROUP, getGroupWithoutWrapper(group));
        return outer;
    }
    
    @SuppressWarnings("unchecked")
    private JSONObject getGroupWithoutWrapper(Group group){
        JSONObject outer = new JSONObject();

        outer.put(JSONConstants.ID, group.getId());
        outer.put(JSONConstants.DESCRIPTION, group.getDescription());
        outer.put(JSONConstants.NAME, group.getName());
        return outer;
    }

    
    @SuppressWarnings("unchecked")
    private JSONObject getService(Service service) {
        JSONObject outer = new JSONObject();
        
        outer.put(JSONConstants.SERVICE, getServiceWithoutWrapper(service));
        return outer;
    }
    
    @SuppressWarnings("unchecked")
    private JSONObject getServiceWithoutWrapper(Service service) {
        JSONObject outer = new JSONObject();
        
        outer.put(JSONConstants.ID, service.getId());
        outer.put(JSONConstants.NAME, service.getName());
        outer.put(JSONConstants.TYPE, service.getType());
        outer.put(JSONConstants.DESCRIPTION, service.getDescription());
        return outer;
    }
    
    @SuppressWarnings("unchecked")
    private JSONObject getServiceList(ServiceList serviceList) {
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();
        JSONArray list = new JSONArray();

        outer.put(JSONConstants.SERVICES, inner);
        inner.put(JSONConstants.SERVICE, list);
        
        for (Service service : serviceList.getService()) {
            list.add(getServiceWithoutWrapper(service));
        }

        return outer;
    }
    
    @SuppressWarnings("unchecked")
    private JSONObject getEndpointTemplate(EndpointTemplate template) {
        JSONObject outer = new JSONObject();

        outer.put(JSONConstants.ENDPOINT_TEMPLATE, getEndpointTemplateWithoutWrapper(template));

        return outer;
    }
    
    @SuppressWarnings("unchecked")
    private JSONObject getEndpointTemplateWithoutWrapper(EndpointTemplate template) {
        JSONObject outer = new JSONObject();

        outer.put(JSONConstants.ID, template.getId());
        outer.put(JSONConstants.ADMIN_URL, template.getAdminURL());
        outer.put(JSONConstants.INTERNAL_URL, template.getInternalURL());
        outer.put(JSONConstants.NAME, template.getName());
        outer.put(JSONConstants.PUBLIC_URL, template.getPublicURL());
        outer.put(JSONConstants.TYPE, template.getType());
        outer.put(JSONConstants.REGION, template.getRegion());
        outer.put(JSONConstants.GLOBAL, template.isGlobal());
        outer.put(JSONConstants.ENABLED, template.isEnabled());
        if (template.getVersion() != null) {
            outer.put(JSONConstants.VERSION_ID, template.getVersion().getId());
            outer.put(JSONConstants.VERSION_INFO, template.getVersion().getInfo());
            outer.put(JSONConstants.VERSION_LIST, template.getVersion().getList());
        }
        return outer;
    }
    
    @SuppressWarnings("unchecked")
    private JSONObject getEndpointTemplateList(EndpointTemplateList templateList) {
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();
        JSONArray list = new JSONArray();

        outer.put(JSONConstants.ENDPOINT_TEMPLATES, inner);
        inner.put(JSONConstants.ENDPOINT_TEMPLATE, list);
        
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
