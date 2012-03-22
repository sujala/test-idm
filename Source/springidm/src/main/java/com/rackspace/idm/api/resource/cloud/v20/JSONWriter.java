package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspacecloud.docs.auth.api.v1.BaseURL;
import com.rackspacecloud.docs.auth.api.v1.BaseURLList;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.common.api.v1.Extensions;
import org.openstack.docs.common.api.v1.MediaTypeList;
import org.openstack.docs.common.api.v1.VersionChoice;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;
import org.openstack.docs.identity.api.v2.*;
import org.w3._2005.atom.Link;

import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriter implements MessageBodyWriter<JAXBElement<?>> {

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

        if (object.getDeclaredType().isAssignableFrom(Extension.class)) {
            Extension extension = (Extension) object.getValue();
            String jsonText = JSONValue.toJSONString(getExtension(extension));
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        }else  if (object.getDeclaredType().isAssignableFrom(VersionChoice.class)) {
            VersionChoice versionChoice = (VersionChoice) object.getValue();
            String jsonText = JSONValue.toJSONString(getVersionChoice(versionChoice));
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
        }

        else if (object.getDeclaredType().isAssignableFrom(Extensions.class)) {
            Extensions extensions = (Extensions) object.getValue();
            String jsonText = JSONValue.toJSONString(getExtensionList(extensions));
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        } else if (object.getDeclaredType().isAssignableFrom(Tenants.class)) {
            JSONObject outer = new JSONObject();
            JSONArray list = new JSONArray();
            Tenants tenants = (Tenants)object.getValue();
            for (Tenant tenant : tenants.getTenant()){
                list.add(getTenantWithoutWrapper(tenant));
            }
            outer.put(JSONConstants.TENANTS, list);
            String jsonText = JSONValue.toJSONString(outer);
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        } else if (object.getDeclaredType().isAssignableFrom(Service.class)) {

            Service service = (Service) object.getValue();
            String jsonText = JSONValue.toJSONString(getService(service));
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        } else if (object.getDeclaredType().isAssignableFrom(ServiceList.class)) {
            JSONObject outer = new JSONObject();
            JSONArray list = new JSONArray();
            ServiceList serviceList = (ServiceList)object.getValue();
            for (Service service : serviceList.getService()){
                list.add(getServiceWithoutWrapper(service));
            }
            outer.put(JSONConstants.SERVICES, list);
            String jsonText = JSONValue.toJSONString(outer);
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        } else if (object.getDeclaredType().isAssignableFrom(SecretQA.class)) {

            SecretQA secrets = (SecretQA) object.getValue();
            String jsonText = JSONValue.toJSONString(getSecretQA(secrets));
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        } else if (object.getDeclaredType().isAssignableFrom(EndpointTemplate.class)) {

            EndpointTemplate template = (EndpointTemplate) object.getValue();
            String jsonText = JSONValue.toJSONString(getEndpointTemplate(template));
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        } else if (object.getDeclaredType().isAssignableFrom(Endpoint.class)) {
            JSONObject outer = new JSONObject();
            outer.put(JSONConstants.ENDPOINT, getEndpoint((Endpoint)object.getValue()));
            String jsonText = JSONValue.toJSONString(outer);
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        } else if (object.getDeclaredType().isAssignableFrom(EndpointList.class)) {
            JSONObject outerList = new JSONObject();
            JSONArray endpoints = new JSONArray();
            EndpointList endpointList = (EndpointList)object.getValue();
            outerList.put(JSONConstants.ENDPOINTS, endpoints);
            for(Endpoint endpoint : endpointList.getEndpoint()){
                endpoints.add(getEndpoint(endpoint));
            }
            String jsonText = JSONValue.toJSONString(outerList);
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        } else if (object.getDeclaredType().isAssignableFrom(EndpointTemplateList.class)) {
            JSONObject endpointTemplate = new JSONObject();
            JSONArray endpoints = new JSONArray();
            endpointTemplate.put(JSONConstants.ENDPOINT_TEMPLATES, endpoints);
            EndpointTemplateList templateList = (EndpointTemplateList)object.getValue();
            for(EndpointTemplate template : templateList.getEndpointTemplate()){
                JSONObject templateItem = new JSONObject();
                templateItem.put(JSONConstants.ID, template.getId());
                templateItem.put(JSONConstants.ENABLED, template.isEnabled());
                if(template.getRegion() != null){
                    templateItem.put(JSONConstants.REGION, template.getRegion());
                }
                if(template.getPublicURL() != null){
                    templateItem.put(JSONConstants.PUBLIC_URL, template.getPublicURL());
                }
                if(template.getRegion() != null){
                    templateItem.put(JSONConstants.GLOBAL, template.isGlobal());
                }
                if(template.getName() != null){
                    templateItem.put(JSONConstants.NAME, template.getName());
                }
                if(template.getAdminURL() != null){
                    templateItem.put(JSONConstants.ADMIN_URL, template.getAdminURL());
                }
                if(template.getType() != null){
                    templateItem.put(JSONConstants.TYPE, template.getType());
                }
                if(template.getInternalURL() != null){
                    templateItem.put(JSONConstants.INTERNAL_URL, template.getInternalURL());
                }
                if(template.getVersion() != null){
                    templateItem.put(JSONConstants.VERSION_ID, template.getVersion().getId());
                    templateItem.put(JSONConstants.VERSION_INFO, template.getVersion().getInfo());
                    templateItem.put(JSONConstants.VERSION_LIST, template.getVersion().getList());
                }
                endpoints.add(templateItem);
            }
            String jsonText = JSONValue.toJSONString(endpointTemplate);
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        } else if (object.getDeclaredType().isAssignableFrom(
            CredentialType.class) || object.getDeclaredType().isAssignableFrom(ApiKeyCredentials.class)) {

            CredentialType cred = (CredentialType) object.getValue();

            if (cred instanceof ApiKeyCredentials) {
                ApiKeyCredentials creds = (ApiKeyCredentials) cred;
                String jsonText = JSONValue.toJSONString(getApiKeyCredentials(creds));
                outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
            } else {
                PasswordCredentialsRequiredUsername creds = (PasswordCredentialsRequiredUsername) cred;
                String jsonText = JSONValue.toJSONString(getPasswordCredentials(creds));
                outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
            }

        } else if (object.getDeclaredType().isAssignableFrom(Groups.class)) {
            Groups groups = (Groups) object.getValue();
            String jsonText = JSONValue.toJSONString(getGroups(groups));
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        } else if (object.getDeclaredType().isAssignableFrom(Group.class)) {
            Group group = (Group) object.getValue();
            String jsonText = JSONValue.toJSONString(getGroup(group));
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        }
        else if (object.getDeclaredType().isAssignableFrom(CredentialListType.class)) {

            JSONObject outer = new JSONObject();
            JSONArray list = new JSONArray();

            CredentialListType credsList = (CredentialListType) object.getValue();
            outer.put(JSONConstants.CREDENTIALS, list);

            for (JAXBElement<? extends CredentialType> cred : credsList
                .getCredential()) {
                if (cred.getDeclaredType().isAssignableFrom(
                    ApiKeyCredentials.class)) {
                    list.add(getApiKeyCredentials((ApiKeyCredentials) cred.getValue()));
                } else if (cred.getDeclaredType().isAssignableFrom(
                    PasswordCredentialsRequiredUsername.class)) {
                    list.add(getPasswordCredentials((PasswordCredentialsRequiredUsername) cred.getValue()));
                }
            }

            String jsonText = JSONValue.toJSONString(outer);
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        } else if (object.getDeclaredType().isAssignableFrom(RoleList.class)) {
            JSONObject outer = new JSONObject();
            JSONArray list = new JSONArray();

            RoleList roleList = (RoleList)object.getValue();

            for (Role role : roleList.getRole()){
                list.add(getRole(role));
            }
            outer.put(JSONConstants.ROLES, list);

            String jsonText = JSONValue.toJSONString(outer);
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        } else if (object.getDeclaredType().isAssignableFrom(UserList.class)) {
            JSONObject outer = new JSONObject();
            JSONArray list = new JSONArray();

            UserList userList = (UserList)object.getValue();

            for (User user : userList.getUser()){
                list.add(getUser(user));
            }
            outer.put(JSONConstants.USERS, list);

            String jsonText = JSONValue.toJSONString(outer);
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        } else if (object.getDeclaredType().isAssignableFrom(AuthenticateResponse.class)) {
            JSONObject outer = new JSONObject();
            JSONObject access = new JSONObject();
            AuthenticateResponse authenticateResponse = (AuthenticateResponse)object.getValue();
            access.put(JSONConstants.TOKEN, getToken(authenticateResponse.getToken()));
            access.put(JSONConstants.SERVICECATALOG, getServiceCatalog(authenticateResponse.getServiceCatalog()));
            if(authenticateResponse.getUser() != null)
                access.put(JSONConstants.USER, getTokenUser(authenticateResponse.getUser()));
            outer.put(JSONConstants.ACCESS, access);

            String jsonText = JSONValue.toJSONString(outer);
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        } else if (object.getDeclaredType().isAssignableFrom(BaseURLList.class)) {
            JSONObject outer = new JSONObject();
            JSONArray list = new JSONArray();

            BaseURLList baseList = (BaseURLList)object.getValue();
            for (BaseURL url : baseList.getBaseURL()){
                list.add(getBaseUrlList(url));
            }
            outer.put(JSONConstants.BASE_URLS, list);

            String jsonText = JSONValue.toJSONString(outer);
            outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));

        // Version 1.1 specific
        } else if (object.getDeclaredType().isAssignableFrom(com.rackspacecloud.docs.auth.api.v1.User.class)) {
            com.rackspacecloud.docs.auth.api.v1.User user = (com.rackspacecloud.docs.auth.api.v1.User)object.getValue();
            JSONObject outer = new JSONObject();
            JSONObject inner = new JSONObject();
            inner.put(JSONConstants.ID, user.getId());
            inner.put(JSONConstants.ENABLED, user.isEnabled());
            if(user.getKey() != null){
                inner.put(JSONConstants.KEY, user.getKey());
            }
            if(user.getMossoId() != null){
                inner.put(JSONConstants.MOSSO_ID, user.getMossoId());
            }
            if(user.getNastId() != null){
                inner.put(JSONConstants.NAST_ID, user.getNastId());
            }
            //inner.put(JSONConstants.CREATED, user.getCreated());
            //inner.put(JSONConstants.UPDATED, user.getUpdated());
            JSONArray baseUrls = new JSONArray();
            BaseURLRefList baseList = user.getBaseURLRefs();
            if(baseList != null){
                for (BaseURLRef url : baseList.getBaseURLRef()){
                    JSONObject urlItem = new JSONObject();
                    urlItem.put(JSONConstants.ID, url.getId());
                    urlItem.put(JSONConstants.HREF, url.getHref());
                    urlItem.put(JSONConstants.V1_DEFAULT, url.isV1Default());
                    baseUrls.add(urlItem);
                }
            }
            inner.put(JSONConstants.BASE_URL_REFS, baseUrls);
            outer.put(JSONConstants.USER, inner);
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
    private static JSONArray getLinks(List<Object> any) {
        JSONArray linkArray = new JSONArray();
        for (Object o : any) {
            if (o instanceof JAXBElement) {
                Object elmType = ((JAXBElement<Object>) o).getValue();
                if (elmType instanceof Link) {
                    Link l = (Link) elmType;
                    JSONObject jlink = new JSONObject();
                    if (l.getRel() != null) {
                        jlink.put("rel", l.getRel().value());
                    }
                    if (l.getType() != null) {
                        jlink.put("type", l.getType());
                    }
                    jlink.put("href", l.getHref());
                    linkArray.add(jlink);
                }
            }
        }
        return linkArray;
    }

    private JSONObject getVersionChoice(VersionChoice versionChoice) {

        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();

        outer.put("version", inner);

        inner.put("id", versionChoice.getId());
        if (versionChoice.getStatus() != null) {
            inner.put("status", versionChoice.getStatus().toString());
        }

        XMLGregorianCalendar updated = versionChoice.getUpdated();

        if (updated != null) {
            inner.put("updated", updated.toXMLFormat());
        }

        if (!versionChoice.getAny().isEmpty()) {
            JSONArray linkArray = getLinks(versionChoice.getAny());
            if (!linkArray.isEmpty()) {
                inner.put("links", linkArray);
            }
        }


        MediaTypeList mtl = versionChoice.getMediaTypes();
        if (mtl != null && !mtl.getMediaType().isEmpty()) {
            JSONArray typeArray = new JSONArray();
            for (org.openstack.docs.common.api.v1.MediaType mt : versionChoice.getMediaTypes().getMediaType()) {
                JSONObject jtype = new JSONObject();
                jtype.put("base", mt.getBase());
                jtype.put("type", mt.getType());
                typeArray.add(jtype);
            }
            JSONObject type_values = new JSONObject();
            type_values.put("values", typeArray);
            inner.put("media-types", type_values);
        }
        return outer;

    }

    @SuppressWarnings("unchecked")
    private JSONObject getTokenUser(UserForAuthenticateResponse user){
        JSONObject userInner = new JSONObject();
        userInner.put(JSONConstants.ID, user.getId());
        if(user.getName() != null){
            userInner.put(JSONConstants.NAME, user.getName());
        }
        JSONArray roleInner = new JSONArray();
        userInner.put(JSONConstants.ROLES, roleInner);
        RoleList roleList = user.getRoles();
        for (Role role : roleList.getRole()){
            roleInner.add(getRole(role));
        }
        return userInner;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getTenantWithoutWrapper(Tenant tenant){
        JSONObject userInner = new JSONObject();
        userInner.put(JSONConstants.ID, tenant.getId());
        userInner.put(JSONConstants.ENABLED, tenant.isEnabled());
        if(tenant.getName() != null){
            userInner.put(JSONConstants.NAME, tenant.getName());
        }
        if(tenant.getDescription() != null){
            userInner.put(JSONConstants.DESCRIPTION, tenant.getDescription());
        }
        //userInner.put(JSONConstants.DISPLAY_NAME, tenant.getDisplayName());
        //userInner.put(JSONConstants.CREATED, tenant.getCreated().toString());
        return userInner;
    }

    @SuppressWarnings("unchecked")
    private JSONArray getServiceCatalog(ServiceCatalog serviceCatalog){
        JSONArray serviceInner = new JSONArray();
        if(serviceCatalog != null) {
            for(ServiceForCatalog service : serviceCatalog.getService()){
                JSONObject catalogItem = new JSONObject();
                catalogItem.put(JSONConstants.ENDPOINTS, getEndpointsForCatalog(service.getEndpoint()));
                if(service.getName() != null){
                    catalogItem.put(JSONConstants.NAME, service.getName());
                }
                if(service.getType() != null){
                    catalogItem.put(JSONConstants.TYPE, service.getType());
                }
                serviceInner.add(catalogItem);
            }
        }
        return serviceInner;        
    }

    @SuppressWarnings("unchecked")
    private JSONObject getToken(Token token){
        JSONObject tokenInner = new JSONObject();
        tokenInner.put(JSONConstants.ID, token.getId());
        tokenInner.put(JSONConstants.EXPIRES, token.getExpires().toString());

        // Check for impersonation only?
        for (QName key : token.getOtherAttributes().keySet()) {
            tokenInner.put(key.toString(), token.getOtherAttributes().get(key));
        }

        return tokenInner;
    }

    @SuppressWarnings("unchecked")
    private JSONArray getEndpointsForCatalog(List<EndpointForService> endpoints){
        JSONArray endpointList = new JSONArray();
        for(EndpointForService endpoint : endpoints){
            JSONObject endpointItem = new JSONObject();
            if(endpoint.getTenantId() != null){
                endpointItem.put(JSONConstants.TENANT_ID, endpoint.getTenantId());
            }
            if(endpoint.getPublicURL() != null){
                endpointItem.put(JSONConstants.PUBLIC_URL, endpoint.getPublicURL());
            }
            if(endpoint.getInternalURL() != null){
                endpointItem.put(JSONConstants.INTERNAL_URL, endpoint.getInternalURL());
            }
            if(endpoint.getRegion() != null){
                endpointItem.put(JSONConstants.REGION, endpoint.getRegion());
            }
            if(endpoint.getVersion() != null){
                endpointItem.put(JSONConstants.VERSION_INFO, endpoint.getVersion().getInfo());
                endpointItem.put(JSONConstants.VERSION_LIST, endpoint.getVersion().getList());
                endpointItem.put(JSONConstants.VERSION_ID, endpoint.getVersion().getId());
            }
            endpointList.add(endpointItem);
            }
        return endpointList;
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
    private JSONObject getPasswordCredentials(
        PasswordCredentialsRequiredUsername creds) {
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
    private JSONObject getUser(User user) {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.ID, user.getId());
        outer.put(JSONConstants.USERNAME, user.getUsername());
        outer.put(JSONConstants.EMAIL, user.getEmail());
        outer.put(JSONConstants.ENABLED, user.isEnabled());
        return outer;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getRole(Role role) {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.ID, role.getId());
        if(role.getDescription() != null){
            outer.put(JSONConstants.DESCRIPTION, role.getDescription());
        }
        if(role.getName() != null){
            outer.put(JSONConstants.NAME, role.getName());
        }
        if(role.getServiceId() != null){
            outer.put(JSONConstants.SERVICE_ID, role.getServiceId());
        }
        return outer;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getGroups(Groups groups) {
        JSONObject outer = new JSONObject();
        JSONArray list = new JSONArray();
        outer.put(JSONConstants.GROUPS, list);
        for (Group group : groups.getGroup()) {
            list.add(getGroupWithoutWrapper(group));
        }
        return outer;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getGroup(Group group) {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.GROUP, getGroupWithoutWrapper(group));
        return outer;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getGroupWithoutWrapper(Group group) {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.ID, group.getId());
        outer.put(JSONConstants.NAME, group.getName());
        if(group.getDescription() != null){
            outer.put(JSONConstants.DESCRIPTION, group.getDescription());
        }
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
        if(service.getDescription() != null){
            outer.put(JSONConstants.DESCRIPTION, service.getDescription());
        }
        return outer;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getServiceList(ServiceList serviceList) {
        JSONObject outer = new JSONObject();
        JSONArray list = new JSONArray();
        for (Service service : serviceList.getService()) {
            list.add(getServiceWithoutWrapper(service));
        }
        outer.put(JSONConstants.SERVICES, list);
        return outer;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getEndpointTemplate(EndpointTemplate template) {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.ENDPOINT_TEMPLATE, getEndpointTemplateWithoutWrapper(template));
        return outer;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getEndpointTemplateWithoutWrapper(
        EndpointTemplate template) {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.ID, template.getId());
        if(template.getAdminURL() != null){
            outer.put(JSONConstants.ADMIN_URL, template.getAdminURL());
        }
        if(template.getInternalURL() != null){
            outer.put(JSONConstants.INTERNAL_URL, template.getInternalURL());
        }
        if(template.getName() != null){
            outer.put(JSONConstants.NAME, template.getName());
        }
        if(template.getPublicURL() != null){
            outer.put(JSONConstants.PUBLIC_URL, template.getPublicURL());
        }
        if(template.getType() != null){
            outer.put(JSONConstants.TYPE, template.getType());
        }
        if(template.getRegion() != null){
            outer.put(JSONConstants.REGION, template.getRegion());
        }
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
            list.add(getEndpointTemplateWithoutWrapper(template));
        }
        return outer;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getBaseUrlList(BaseURL url){
        JSONObject baseURL = new JSONObject();
        baseURL.put(JSONConstants.ENABLED, url.isEnabled());
        baseURL.put(JSONConstants.DEFAULT, url.isDefault());
        if(url.getInternalURL() != null){
        baseURL.put(JSONConstants.INTERNAL_URL, url.getInternalURL());
        }
        if(url.getPublicURL() != null){
            baseURL.put(JSONConstants.PUBLIC_URL, url.getPublicURL());
        }
        if(url.getRegion() != null){
            baseURL.put(JSONConstants.REGION, url.getRegion());
        }
        if(url.getServiceName() != null){
            baseURL.put(JSONConstants.SERVICE_NAME, url.getServiceName());
        }
        if(url.getUserType() != null){
            baseURL.put(JSONConstants.USER_TYPE, url.getUserType().name());
        }
        baseURL.put(JSONConstants.ID, url.getId());
        return baseURL;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getExtension(Extension extension) {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.EXTENSION, getExtensionWithoutWrapper(extension));
        return outer;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getExtensionList(Extensions extensions) {
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();
        JSONArray list = new JSONArray();
        outer.put(JSONConstants.EXTENSIONS, list);
        for (Extension extension : extensions.getExtension()) {
            list.add(getExtensionWithoutWrapper(extension));
        }
        return outer;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getEndpoint(Endpoint endpoint){
        JSONObject endpointItem = new JSONObject();
        endpointItem.put(JSONConstants.ID, endpoint.getId());
        if(endpoint.getRegion() != null){
            endpointItem.put(JSONConstants.REGION, endpoint.getRegion());
        }
        if(endpoint.getPublicURL() != null){
            endpointItem.put(JSONConstants.PUBLIC_URL, endpoint.getPublicURL());
        }
        if(endpoint.getName() != null){
            endpointItem.put(JSONConstants.NAME, endpoint.getName());
        }
        if(endpoint.getAdminURL() != null){
            endpointItem.put(JSONConstants.ADMIN_URL, endpoint.getAdminURL());
        }
        if(endpoint.getType() != null){
            endpointItem.put(JSONConstants.TYPE, endpoint.getType());
        }
        if(endpoint.getInternalURL() != null){
            endpointItem.put(JSONConstants.INTERNAL_URL, endpoint.getInternalURL());
        }
        if(endpoint.getVersion() != null){
            endpointItem.put(JSONConstants.VERSION_ID, endpoint.getVersion().getId());
            endpointItem.put(JSONConstants.VERSION_INFO, endpoint.getVersion().getInfo());
            endpointItem.put(JSONConstants.VERSION_LIST, endpoint.getVersion().getList());
        }
        return endpointItem;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getExtensionWithoutWrapper(Extension extension) {
        JSONObject outer = new JSONObject();

        outer.put(JSONConstants.NAME, extension.getName());
        outer.put(JSONConstants.NAMESPACE, extension.getNamespace());
        outer.put(JSONConstants.ALIAS, extension.getAlias());
        outer.put(JSONConstants.UPDATED, extension.getUpdated().toString());
        outer.put(JSONConstants.DESCRIPTION, extension.getDescription());
        List<Link> links = new ArrayList<Link>();
        if (extension.getAny() != null && extension.getAny().size() > 0) {
            for (Object obj : extension.getAny()) {
                if (Object.class.isAssignableFrom(Link.class)) {
                    links.add(((JAXBElement<Link>) obj).getValue());
                }
            }
        }
        if (links.size() > 0) {
            JSONArray list = new JSONArray();
            outer.put(JSONConstants.LINKS, list);
            for (Link link : links) {
                list.add(getLinkWithoutWrapper(link));
            }
        }
        return outer;
    }

    @SuppressWarnings("unchecked")
    private JSONObject getLinkWithoutWrapper(Link link) {
        JSONObject outer = new JSONObject();
        if(link.getRel() != null){
            outer.put(JSONConstants.REL, link.getRel().toString());
        }
        if(link.getType() != null){
            outer.put(JSONConstants.TYPE, link.getType());
        }
        if(link.getHref() != null){
            outer.put(JSONConstants.HREF, link.getHref());
        }
        return outer;
    }

    private JSONMarshaller getMarshaller() throws JAXBException {
        return ((JSONJAXBContext) JAXBContextResolver.get()).createJSONMarshaller();
    }
}
