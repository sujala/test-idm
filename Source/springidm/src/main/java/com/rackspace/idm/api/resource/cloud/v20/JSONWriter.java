package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DefaultRegionServices;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.config.providers.PackageClassDiscoverer;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import org.apache.cxf.common.util.StringUtils;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.common.api.v1.Extensions;
import org.openstack.docs.common.api.v1.MediaTypeList;
import org.openstack.docs.common.api.v1.VersionChoice;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.ServiceList;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplateList;
import org.openstack.docs.identity.api.v2.*;
import org.openstack.docs.identity.api.v2.Endpoint;
import org.openstack.docs.identity.api.v2.ServiceCatalog;
import org.openstack.docs.identity.api.v2.Token;
import org.openstack.docs.identity.api.v2.User;
import org.slf4j.LoggerFactory;
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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriter implements MessageBodyWriter<Object> {

    public static final Logger LOG = Logger.getLogger(JSONWriter.class);
    private static Set<Class<?>> classes = new HashSet<Class<?>>();

    static {
        try {
            classes = PackageClassDiscoverer.findClassesIn(
                    "com.rackspace.api.idm.v1",
                    "com.rackspacecloud.docs.auth.api.v1",
                    "org.openstack.docs.common.api.v1",
                    "org.openstack.docs.compute.api.v1",
                    "org.openstack.docs.identity.api.v2",
                    "com.rackspace.docs.identity.api.ext.rax_ksgrp.v1",
                    "com.rackspace.docs.identity.api.ext.rax_kskey.v1",
                    "org.openstack.docs.identity.api.ext.os_ksadm.v1",
                    "org.openstack.docs.identity.api.ext.os_kscatalog.v1",
                    "org.openstack.docs.identity.api.ext.os_ksec2.v1",
                    "org.w3._2005.atom", "com.rackspace.docs.identity.api.ext.rax_ksqa.v1",
                    "com.rackspace.api.common.fault.v1",
                    "com.rackspace.docs.identity.api.ext.rax_auth.v1",
                    "com.rackspace.idm.api.resource.cloud.migration");

        } catch (Exception e) {
            LOG.error("Error in static initializer.  - " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private boolean isCorrectClass(Type genericType) {
        boolean ret = false;
        if (genericType instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) genericType;
            Type[] args = ptype.getActualTypeArguments();
            if (args.length == 1) {
                Class elmClass = (Class) args[0];
                ret = classes.contains(elmClass);
            }
        } else {
            Class genClass = (Class) genericType;
            ret = classes.contains(genClass);
        }

        return ret;
    }

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JSONWriter.class);

    @Override
    public long getSize(Object arg0, Class<?> arg1, Type arg2, Annotation[] arg3, MediaType arg4) {
        return -1;
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return isCorrectClass(genericType);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void writeTo(Object object, Class<?> type, Type genericType, Annotation[] annotations,
                        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders, OutputStream outputStream)
            throws IOException, WebApplicationException {
        String jsonText = "";
        if (object.getClass().equals(Extension.class)) {
            Extension extension = (Extension) object;
            jsonText = JSONValue.toJSONString(getExtension(extension));
        } else if (object.getClass().equals(VersionChoice.class)) {
            VersionChoice versionChoice = (VersionChoice) object;
            jsonText = JSONValue.toJSONString(getVersionChoice(versionChoice));
        } else if (object.getClass().equals(Extensions.class)) {
            Extensions extensions = (Extensions) object;
            jsonText = JSONValue.toJSONString(getExtensionList(extensions));
        } else if (object.getClass().equals(Tenants.class)) {
            JSONObject outer = new JSONObject();
            JSONArray list = new JSONArray();
            Tenants tenants = (Tenants) object;
            for (Tenant tenant : tenants.getTenant()) {
                list.add(getTenantWithoutWrapper(tenant));
            }
            outer.put(JSONConstants.TENANTS, list);
            jsonText = JSONValue.toJSONString(outer);

        } else if (object.getClass().equals(Service.class)) {
            Service service = (Service) object;
            jsonText = JSONValue.toJSONString(getService(service));

        } else if (object.getClass().equals(ServiceList.class)) {
            JSONObject outer = new JSONObject();
            JSONArray list = new JSONArray();
            ServiceList serviceList = (ServiceList) object;
            for (Service service : serviceList.getService()) {
                list.add(getServiceWithoutWrapper(service));
            }
            outer.put(JSONConstants.SERVICES, list);
            jsonText = JSONValue.toJSONString(outer);
        } else if (object.getClass().equals(SecretQA.class)) {

            SecretQA secrets = (SecretQA) object;
            jsonText = JSONValue.toJSONString(getSecretQA(secrets));
        } else if (object.getClass().equals(EndpointTemplate.class)) {

            EndpointTemplate template = (EndpointTemplate) object;
            jsonText = JSONValue.toJSONString(getEndpointTemplate(template));
        } else if (object.getClass().equals(Endpoint.class)) {
            JSONObject outer = new JSONObject();
            outer.put(JSONConstants.ENDPOINT, getEndpoint((Endpoint) object));
            jsonText = JSONValue.toJSONString(outer);
        } else if (object.getClass().equals(EndpointList.class)) {
            JSONObject outerList = new JSONObject();
            JSONArray endpoints = new JSONArray();
            EndpointList endpointList = (EndpointList) object;
            outerList.put(JSONConstants.ENDPOINTS, endpoints);
            for (Endpoint endpoint : endpointList.getEndpoint()) {
                endpoints.add(getEndpoint(endpoint));
            }
            jsonText = JSONValue.toJSONString(outerList);
        } else if (object.getClass().equals(EndpointTemplateList.class)) {
            JSONObject endpointTemplate = new JSONObject();
            JSONArray endpoints = new JSONArray();
            endpointTemplate.put(JSONConstants.ENDPOINT_TEMPLATES, endpoints);
            EndpointTemplateList templateList = (EndpointTemplateList) object;
            for (EndpointTemplate template : templateList.getEndpointTemplate()) {
                JSONObject templateItem = new JSONObject();
                templateItem.put(JSONConstants.ID, template.getId());
                templateItem.put(JSONConstants.ENABLED, template.isEnabled());
                if (template.getRegion() != null) {
                    templateItem.put(JSONConstants.REGION, template.getRegion());
                }
                if (template.getPublicURL() != null) {
                    templateItem.put(JSONConstants.PUBLIC_URL, template.getPublicURL());
                }
                if (template.getRegion() != null) {
                    templateItem.put(JSONConstants.GLOBAL, template.isGlobal());
                }
                if (template.getName() != null) {
                    templateItem.put(JSONConstants.NAME, template.getName());
                }
                if (template.getAdminURL() != null) {
                    templateItem.put(JSONConstants.ADMIN_URL, template.getAdminURL());
                }
                if (template.getType() != null) {
                    templateItem.put(JSONConstants.TYPE, template.getType());
                }
                if (template.getInternalURL() != null) {
                    templateItem.put(JSONConstants.INTERNAL_URL, template.getInternalURL());
                }
                if (template.getVersion() != null) {
                    templateItem.put(JSONConstants.VERSION_ID, template.getVersion().getId());
                    templateItem.put(JSONConstants.VERSION_INFO, template.getVersion().getInfo());
                    templateItem.put(JSONConstants.VERSION_LIST, template.getVersion().getList());
                }
                endpoints.add(templateItem);
            }
            jsonText = JSONValue.toJSONString(endpointTemplate);
        } else if (object instanceof CredentialType) {
            CredentialType cred = (CredentialType) object;
            if (cred instanceof ApiKeyCredentials) {
                ApiKeyCredentials creds = (ApiKeyCredentials) cred;
                jsonText = JSONValue.toJSONString(getApiKeyCredentials(creds));
            } else if (cred instanceof PasswordCredentialsBase) {
                PasswordCredentialsBase creds = (PasswordCredentialsBase) cred;
                jsonText = JSONValue.toJSONString(getPasswordCredentials(creds));
            } else {
                throw new BadRequestException("Credential Type must be API Key Credentials, Password Credentials, or SecretQA.");
            }

        } else if (object.getClass().equals(Groups.class)) {
            Groups groups = (Groups) object;
            jsonText = JSONValue.toJSONString(getGroups(groups));
        } else if (object.getClass().equals(Group.class)) {
            Group group = (Group) object;
            jsonText = JSONValue.toJSONString(getGroup(group));
        } else if (object.getClass().equals(GroupsList.class)) {
            GroupsList groupsList = (GroupsList) object;
            jsonText = JSONValue.toJSONString(getGroupsList(groupsList));
        } else if (object.getClass().equals(CredentialListType.class)) {
            JSONObject outer = new JSONObject();
            JSONArray list = new JSONArray();

            CredentialListType credsList = (CredentialListType) object;
            outer.put(JSONConstants.CREDENTIALS, list);
            for (JAXBElement<? extends CredentialType> cred : credsList.getCredential()) {
                CredentialType credential = cred.getValue();
                if (credential instanceof ApiKeyCredentials) {
                    list.add(getApiKeyCredentials((ApiKeyCredentials) cred.getValue()));
                } else if (credential instanceof PasswordCredentialsBase) {
                    list.add(getPasswordCredentials((PasswordCredentialsBase) cred.getValue()));
                }
            }
            jsonText = JSONValue.toJSONString(outer);
        } else if(object instanceof DefaultRegionServices){
            JSONObject outer = new JSONObject();
            JSONArray list = new JSONArray();
            DefaultRegionServices defaultRegionServices = (DefaultRegionServices) object;
            for (String serviceName : defaultRegionServices.getServiceName()) {
                list.add(serviceName);
            }
            outer.put(JSONConstants.RAX_AUTH_DEFAULT_REGION_SERVICES, list);
            jsonText = JSONValue.toJSONString(outer);
        } else if (object.getClass().equals(RoleList.class)) {
            JSONObject outer = new JSONObject();
            JSONArray list = new JSONArray();
            RoleList roleList = (RoleList) object;
            for (Role role : roleList.getRole()) {
                list.add(getRole(role));
            }
            outer.put(JSONConstants.ROLES, list);
            jsonText = JSONValue.toJSONString(outer);
        } else if (object.getClass().equals(UserList.class)) {
            JSONObject outer = new JSONObject();
            JSONArray list = new JSONArray();
            UserList userList = (UserList) object;
            for (User user : userList.getUser()) {
                list.add(getUser(user));
            }
            outer.put(JSONConstants.USERS, list);
            jsonText = JSONValue.toJSONString(outer);
        } else if (object.getClass().equals(AuthenticateResponse.class)) {
            JSONObject outer = new JSONObject();
            JSONObject access = new JSONObject();
            AuthenticateResponse authenticateResponse = (AuthenticateResponse) object;
            access.put(JSONConstants.TOKEN, getToken(authenticateResponse.getToken()));

            if (authenticateResponse.getServiceCatalog() != null)
                access.put(JSONConstants.SERVICECATALOG, getServiceCatalog(authenticateResponse.getServiceCatalog()));

            if (authenticateResponse.getUser() != null) {
                access.put(JSONConstants.USER, getTokenUser(authenticateResponse.getUser()));
            }
            outer.put(JSONConstants.ACCESS, access);
            if (authenticateResponse.getAny().size() > 0) {
                for (Object response : authenticateResponse.getAny()) {
                    if (response instanceof JAXBElement && ((JAXBElement) response).getDeclaredType().isAssignableFrom(UserForAuthenticateResponse.class)) {
                        UserForAuthenticateResponse userForAuthenticateResponse = (UserForAuthenticateResponse) ((JAXBElement) response).getValue();

                        JSONObject subAccess = new JSONObject();
                        subAccess.put(JSONConstants.ID, userForAuthenticateResponse.getId());
                        subAccess.put(JSONConstants.NAME, userForAuthenticateResponse.getName());

                        JSONArray subRoles = new JSONArray();

                        for (Role role : userForAuthenticateResponse.getRoles().getRole()) {
                            JSONObject subRole = new JSONObject();
                            subRole.put(JSONConstants.SERVICE_ID, role.getServiceId());
                            subRole.put(JSONConstants.DESCRIPTION, role.getDescription());
                            subRole.put(JSONConstants.NAME, role.getName());
                            subRole.put(JSONConstants.ID, role.getId());

                            subRoles.add(subRole);
                        }

                        subAccess.put(JSONConstants.ROLES, subRoles);
                        access.put(JSONConstants.IMPERSONATOR, subAccess);
                        break;
                    }
                }
            }
            jsonText = JSONValue.toJSONString(outer);
        } else if (object.getClass().equals(ImpersonationResponse.class)) {
            JSONObject outer = new JSONObject();
            JSONObject access = new JSONObject();
            ImpersonationResponse authenticateResponse = (ImpersonationResponse) object;
            access.put(JSONConstants.TOKEN, getToken(authenticateResponse.getToken()));

            outer.put(JSONConstants.ACCESS, access);
            jsonText = JSONValue.toJSONString(outer);
        } else if (object.getClass().equals(BaseURLList.class)) {
            JSONObject outer = new JSONObject();
            JSONArray list = new JSONArray();
            BaseURLList baseList = (BaseURLList) object;
            for (BaseURL url : baseList.getBaseURL()) {
                list.add(getBaseUrl(url));
            }
            outer.put(JSONConstants.BASE_URLS, list);
            jsonText = JSONValue.toJSONString(outer);
            // Version 1.1 specific
        } else if (object.getClass().equals(com.rackspacecloud.docs.auth.api.v1.User.class)) {
            com.rackspacecloud.docs.auth.api.v1.User user = (com.rackspacecloud.docs.auth.api.v1.User) object;
            JSONObject outer = new JSONObject();
            JSONObject inner = new JSONObject();
            inner.put(JSONConstants.ID, user.getId());
            inner.put(JSONConstants.ENABLED, user.isEnabled());
            if (user.getKey() != null) {
                inner.put(JSONConstants.KEY, user.getKey());
            }
            if (user.getMossoId() != null) {
                inner.put(JSONConstants.MOSSO_ID, user.getMossoId());
            }
            if (user.getNastId() != null) {
                inner.put(JSONConstants.NAST_ID, user.getNastId());
            }
            //inner.put(JSONConstants.CREATED, user.getCreated());
            //inner.put(JSONConstants.UPDATED, user.getUpdated());
            JSONArray baseUrls = new JSONArray();
            BaseURLRefList baseList = user.getBaseURLRefs();
            if (baseList != null) {
                for (BaseURLRef url : baseList.getBaseURLRef()) {
                    JSONObject urlItem = new JSONObject();
                    urlItem.put(JSONConstants.ID, url.getId());
                    urlItem.put(JSONConstants.HREF, url.getHref());
                    urlItem.put(JSONConstants.V1_DEFAULT, url.isV1Default());
                    baseUrls.add(urlItem);
                }
            }
            inner.put(JSONConstants.BASE_URL_REFS, baseUrls);
            outer.put(JSONConstants.USER, inner);
            jsonText = JSONValue.toJSONString(outer);

        } else if (object.getClass().equals(User.class)) {
            User user = (User) object;
            JSONObject outer = new JSONObject();
            outer.put(JSONConstants.USER, getUser(user));

            jsonText = JSONValue.toJSONString(outer);
        } else {
            try {
                getMarshaller().marshallToJSON(object, outputStream);
            } catch (JAXBException e) {
                logger.info(e.toString());
                throw new BadRequestException("Parameters are not valid.", e);
            }
        }
        outputStream.write(jsonText.getBytes(JSONConstants.UTF_8));
    }

    @SuppressWarnings("unchecked")
    static JSONArray getLinks(List<Object> any) {
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
                    if (l.getHref() != null) {
                        jlink.put("href", l.getHref());
                    }
                    linkArray.add(jlink);
                }
            }
        }
        return linkArray;
    }

    JSONObject getVersionChoice(VersionChoice versionChoice) {

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
    JSONObject getTokenUser(UserForAuthenticateResponse user) {
        JSONObject userInner = new JSONObject();
        userInner.put(JSONConstants.ID, user.getId());
        if (user.getName() != null) {
            userInner.put(JSONConstants.NAME, user.getName());
        }
        JSONArray roleInner = new JSONArray();
        userInner.put(JSONConstants.ROLES, roleInner);
        if (user.getRoles() != null) {
            RoleList roleList = user.getRoles();
            for (Role role : roleList.getRole()) {
                roleInner.add(getRole(role));
            }
        }
        return userInner;
    }

    @SuppressWarnings("unchecked")
    JSONObject getTenantWithoutWrapper(Tenant tenant) {
        JSONObject userInner = new JSONObject();
        userInner.put(JSONConstants.ID, tenant.getId());
        userInner.put(JSONConstants.ENABLED, tenant.isEnabled());
        if (tenant.getName() != null) {
            userInner.put(JSONConstants.NAME, tenant.getName());
        }
        if (tenant.getDescription() != null) {
            userInner.put(JSONConstants.DESCRIPTION, tenant.getDescription());
        }
        //userInner.put(JSONConstants.DISPLAY_NAME, tenant.getDisplayName());
        //userInner.put(JSONConstants.CREATED, tenant.getCreated().toString());
        return userInner;
    }

    @SuppressWarnings("unchecked")
    JSONArray getServiceCatalog(ServiceCatalog serviceCatalog) {
        JSONArray serviceInner = new JSONArray();
        if (serviceCatalog != null) {
            for (ServiceForCatalog service : serviceCatalog.getService()) {
                JSONObject catalogItem = new JSONObject();
                catalogItem.put(JSONConstants.ENDPOINTS, getEndpointsForCatalog(service.getEndpoint()));
                if (service.getName() != null) {
                    catalogItem.put(JSONConstants.NAME, service.getName());
                }
                if (service.getType() != null) {
                    catalogItem.put(JSONConstants.TYPE, service.getType());
                }
                serviceInner.add(catalogItem);
            }
        }
        return serviceInner;
    }

    @SuppressWarnings("unchecked")
    JSONObject getToken(Token token) {
        JSONObject tokenInner = new JSONObject();
        try {
            tokenInner.put(JSONConstants.ID, token.getId());
            tokenInner.put(JSONConstants.EXPIRES, token.getExpires().toString());
        } catch (NullPointerException e) {
            throw new BadRequestException("Expected \"id\" and \"expired\" to not be null.", e);
        }

        if (token.getTenant() != null) {
            JSONObject tenantInner = new JSONObject();
            tokenInner.put(JSONConstants.TENANT, tenantInner);
            tenantInner.put(JSONConstants.ID, token.getTenant().getId());
            tenantInner.put(JSONConstants.NAME, token.getTenant().getName());
        }
        return tokenInner;
    }

    @SuppressWarnings("unchecked")
    JSONArray getTenants(List<TenantForAuthenticateResponse> tenants) {
        JSONArray tenantList = new JSONArray();
        for (TenantForAuthenticateResponse tenant : tenants) {
            JSONObject tenantItem = new JSONObject();
            tenantItem.put(JSONConstants.ID, tenant.getId());
            tenantItem.put(JSONConstants.NAME, tenant.getName());
            tenantList.add(tenantItem);
        }
        return tenantList;
    }

    @SuppressWarnings("unchecked")
    JSONArray getEndpointsForCatalog(List<EndpointForService> endpoints) {
        JSONArray endpointList = new JSONArray();
        for (EndpointForService endpoint : endpoints) {
            JSONObject endpointItem = new JSONObject();
            if (endpoint.getTenantId() != null) {
                endpointItem.put(JSONConstants.TENANT_ID, endpoint.getTenantId());
            }
            if (endpoint.getPublicURL() != null) {
                endpointItem.put(JSONConstants.PUBLIC_URL, endpoint.getPublicURL());
            }
            if (endpoint.getInternalURL() != null) {
                endpointItem.put(JSONConstants.INTERNAL_URL, endpoint.getInternalURL());
            }
            if (endpoint.getRegion() != null) {
                endpointItem.put(JSONConstants.REGION, endpoint.getRegion());
            }
            if (endpoint.getVersion() != null) {
                endpointItem.put(JSONConstants.VERSION_INFO, endpoint.getVersion().getInfo());
                endpointItem.put(JSONConstants.VERSION_LIST, endpoint.getVersion().getList());
                endpointItem.put(JSONConstants.VERSION_ID, endpoint.getVersion().getId());
            }
            endpointList.add(endpointItem);
        }
        return endpointList;
    }

    @SuppressWarnings("unchecked")
    JSONObject getApiKeyCredentials(ApiKeyCredentials creds) {
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();
        outer.put(JSONConstants.APIKEY_CREDENTIALS, inner);
        inner.put(JSONConstants.USERNAME, creds.getUsername());
        inner.put(JSONConstants.API_KEY, creds.getApiKey());
        return outer;
    }

    @SuppressWarnings("unchecked")
    JSONObject getPasswordCredentials(
            PasswordCredentialsBase creds) {
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();
        outer.put(JSONConstants.PASSWORD_CREDENTIALS, inner);
        inner.put(JSONConstants.USERNAME, creds.getUsername());
        inner.put(JSONConstants.PASSWORD, creds.getPassword());
        return outer;
    }

    @SuppressWarnings("unchecked")
    JSONObject getSecretQA(SecretQA secrets) {
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();
        outer.put(JSONConstants.SECRET_QA, inner);
        inner.put(JSONConstants.ANSWER, secrets.getAnswer());
        inner.put(JSONConstants.QUESTION, secrets.getQuestion());
        return outer;
    }

    @SuppressWarnings("unchecked")
    JSONObject getUser(User user) {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.ID, user.getId());
        outer.put(JSONConstants.USERNAME, user.getUsername());
        outer.put(JSONConstants.EMAIL, user.getEmail());
        outer.put(JSONConstants.ENABLED, user.isEnabled());
        if (user instanceof UserForCreate) {
            if (((UserForCreate) user).getPassword() != null) {
                outer.put(JSONConstants.OS_KSADM_PASSWORD, ((UserForCreate) user).getPassword());
            }
        }
        if (user.getCreated() != null) {
            outer.put(JSONConstants.CREATED, user.getCreated().toString());

        }
        if (user.getUpdated() != null) {
            outer.put(JSONConstants.UPDATED, user.getUpdated().toString());
        }
        if (user.getOtherAttributes().size() != 0) {

            String defaultRegion = user.getOtherAttributes().get(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "defaultRegion"));
            if (!StringUtils.isEmpty(defaultRegion)) {
                outer.put(JSONConstants.RAX_AUTH_DEFAULT_REGION, defaultRegion);
            }
            String password = user.getOtherAttributes().get(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"));
            if (!StringUtils.isEmpty(password)) {
                outer.put(JSONConstants.OS_KSADM_PASSWORD, password);
            }
        }
        return outer;
    }

    @SuppressWarnings("unchecked")
    JSONObject getRole(Role role) {
        JSONObject outer = new JSONObject();
        if (role.getId() != null) {
            outer.put(JSONConstants.ID, role.getId());
        }
        if (role.getDescription() != null) {
            outer.put(JSONConstants.DESCRIPTION, role.getDescription());
        }
        if (role.getName() != null) {
            outer.put(JSONConstants.NAME, role.getName());
        }
        if (role.getServiceId() != null) {
            outer.put(JSONConstants.SERVICE_ID, role.getServiceId());
        }
        if (role.getTenantId() != null) {
            outer.put(JSONConstants.TENANT_ID, role.getTenantId());
        }
        return outer;
    }

    @SuppressWarnings("unchecked")
    JSONObject getGroups(Groups groups) {
        JSONObject outer = new JSONObject();
        JSONArray list = new JSONArray();
        outer.put(JSONConstants.GROUPS, list);
        for (Group group : groups.getGroup()) {
            list.add(getGroupWithoutWrapper(group));
        }
        return outer;
    }

    JSONObject getGroupsList(GroupsList groupsList) {
        JSONObject outer = new JSONObject();
        JSONObject values = new JSONObject();
        JSONArray list = new JSONArray();
        outer.put(JSONConstants.GROUPSLIST, values);
        for (com.rackspacecloud.docs.auth.api.v1.Group group : groupsList.getGroup()) {
            list.add(get11Group(group));
        }
        values.put("values", list);
        return outer;
    }

    @SuppressWarnings("unchecked")
    JSONObject getGroup(Group group) {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.GROUP, getGroupWithoutWrapper(group));
        return outer;
    }

    @SuppressWarnings("unchecked")
    JSONObject getGroupWithoutWrapper(Group group) {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.ID, group.getId());
        outer.put(JSONConstants.NAME, group.getName());
        if (group.getDescription() != null) {
            outer.put(JSONConstants.DESCRIPTION, group.getDescription());
        }
        return outer;
    }

    JSONObject get11Group(com.rackspacecloud.docs.auth.api.v1.Group group) {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.ID, group.getId());
        if (group.getDescription() != null) {
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
    JSONObject getServiceWithoutWrapper(Service service) {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.ID, service.getId());
        outer.put(JSONConstants.NAME, service.getName());
        outer.put(JSONConstants.TYPE, service.getType());
        if (service.getDescription() != null) {
            outer.put(JSONConstants.DESCRIPTION, service.getDescription());
        }
        return outer;
    }

    @SuppressWarnings("unchecked")
    JSONObject getServiceList(ServiceList serviceList) {
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
    JSONObject getEndpointTemplateWithoutWrapper(
            EndpointTemplate template) {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.ID, template.getId());
        if (template.getAdminURL() != null) {
            outer.put(JSONConstants.ADMIN_URL, template.getAdminURL());
        }
        if (template.getInternalURL() != null) {
            outer.put(JSONConstants.INTERNAL_URL, template.getInternalURL());
        }
        if (template.getName() != null) {
            outer.put(JSONConstants.NAME, template.getName());
        }
        if (template.getPublicURL() != null) {
            outer.put(JSONConstants.PUBLIC_URL, template.getPublicURL());
        }
        if (template.getType() != null) {
            outer.put(JSONConstants.TYPE, template.getType());
        }
        if (template.getRegion() != null) {
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
    JSONObject getEndpointTemplateList(EndpointTemplateList templateList) {
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
    JSONObject getBaseUrl(BaseURL url) {
        JSONObject baseURL = new JSONObject();
        baseURL.put(JSONConstants.ENABLED, url.isEnabled());
        baseURL.put(JSONConstants.DEFAULT, url.isDefault());
        if (url.getInternalURL() != null) {
            baseURL.put(JSONConstants.INTERNAL_URL, url.getInternalURL());
        }
        if (url.getPublicURL() != null) {
            baseURL.put(JSONConstants.PUBLIC_URL, url.getPublicURL());
        }
        if (url.getRegion() != null) {
            baseURL.put(JSONConstants.REGION, url.getRegion());
        }
        if (url.getServiceName() != null) {
            baseURL.put(JSONConstants.SERVICE_NAME, url.getServiceName());
        }
        if (url.getUserType() != null) {
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
    JSONObject getExtensionList(Extensions extensions) {
        JSONObject outer = new JSONObject();
        JSONArray list = new JSONArray();
        outer.put(JSONConstants.EXTENSIONS, list);
        for (Extension extension : extensions.getExtension()) {
            list.add(getExtensionWithoutWrapper(extension));
        }
        return outer;
    }

    @SuppressWarnings("unchecked")
    JSONObject getEndpoint(Endpoint endpoint) {
        JSONObject endpointItem = new JSONObject();
        endpointItem.put(JSONConstants.ID, endpoint.getId());
        if (endpoint.getRegion() != null) {
            endpointItem.put(JSONConstants.REGION, endpoint.getRegion());
        }
        if (endpoint.getPublicURL() != null) {
            endpointItem.put(JSONConstants.PUBLIC_URL, endpoint.getPublicURL());
        }
        if (endpoint.getName() != null) {
            endpointItem.put(JSONConstants.NAME, endpoint.getName());
        }
        if (endpoint.getAdminURL() != null) {
            endpointItem.put(JSONConstants.ADMIN_URL, endpoint.getAdminURL());
        }
        if (endpoint.getType() != null) {
            endpointItem.put(JSONConstants.TYPE, endpoint.getType());
        }
        if (endpoint.getTenantId() != null) {
            endpointItem.put(JSONConstants.TENANT_ID, endpoint.getTenantId());
        }
        if (endpoint.getInternalURL() != null) {
            endpointItem.put(JSONConstants.INTERNAL_URL, endpoint.getInternalURL());
        }
        if (endpoint.getVersion() != null) {
            endpointItem.put(JSONConstants.VERSION_ID, endpoint.getVersion().getId());
            endpointItem.put(JSONConstants.VERSION_INFO, endpoint.getVersion().getInfo());
            endpointItem.put(JSONConstants.VERSION_LIST, endpoint.getVersion().getList());
        }
        return endpointItem;
    }

    @SuppressWarnings("unchecked")
    JSONObject getExtensionWithoutWrapper(Extension extension) {
        JSONObject outer = new JSONObject();

        outer.put(JSONConstants.NAME, extension.getName());
        outer.put(JSONConstants.NAMESPACE, extension.getNamespace());
        outer.put(JSONConstants.ALIAS, extension.getAlias());
        if (extension.getUpdated() != null) {
            outer.put(JSONConstants.UPDATED, extension.getUpdated().toString());
        }
        outer.put(JSONConstants.DESCRIPTION, extension.getDescription());

        if (extension.getAny().size() > 0) {
            JSONArray links = JSONWriter.getLinks(extension.getAny());
            if (links.size() > 0) {
                outer.put(JSONConstants.LINKS, links);
            }
        }

        return outer;
    }

    JSONMarshaller getMarshaller() throws JAXBException {
        return ((JSONJAXBContext) JAXBContextResolver.get()).createJSONMarshaller();
    }
}
