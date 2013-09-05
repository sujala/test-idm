package com.rackspace.idm.api.resource.cloud.v20.json.writers;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group;
import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.config.providers.PackageClassDiscoverer;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.IdmException;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONMarshaller;
import org.apache.cxf.common.util.StringUtils;
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
import org.openstack.docs.identity.api.v2.*;
import org.openstack.docs.identity.api.v2.Endpoint;
import org.openstack.docs.identity.api.v2.ServiceCatalog;
import org.openstack.docs.identity.api.v2.Token;
import org.openstack.docs.identity.api.v2.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3._2005.atom.Link;

import javax.ws.rs.Produces;
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

import static com.rackspace.idm.RaxAuthConstants.QNAME_PROPAGATE;
import static com.rackspace.idm.RaxAuthConstants.QNAME_WEIGHT;

@Provider
@Produces(MediaType.APPLICATION_JSON)
public class JSONWriter implements MessageBodyWriter<Object> {

    public static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(JSONWriter.class);
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
                    "com.rackspace.docs.identity.api.ext.rax_auth.v1");

        } catch (Exception e) {
            LOG.error("Error in static initializer.  - " + e.getMessage());
            throw new IdmException(e);
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

    private static final Logger LOGGER = LoggerFactory.getLogger(JSONWriter.class);

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
            throws IOException {
        String jsonText = "";
      if (object.getClass().equals(Policy.class)) {
            Policy policy = (Policy) object;
            JSONObject outer = new JSONObject();
            outer.put(JSONConstants.RAX_AUTH_POLICY, getPolicyWithoutWrapper(policy));
            jsonText = JSONValue.toJSONString(outer);
        } else if (object.getClass().equals(Policies.class)) {
            Policies policies = (Policies) object;
            JSONObject outer = new JSONObject();
            outer.put(JSONConstants.RAX_AUTH_POLICIES, getPoliciesWithoutWrapper(policies));
            jsonText = JSONValue.toJSONString(outer);
        }else if (object.getClass().equals(AuthData.class)) {
            JSONObject outer = new JSONObject();
            JSONObject inner = new JSONObject();
            AuthData authData = (AuthData) object;

            if (authData.getServiceCatalog() != null) {
                inner.put(JSONConstants.SERVICECATALOG, getServiceCatalog11(authData.getServiceCatalog()));
            }
            inner.put(JSONConstants.TOKEN, getToken11(authData.getToken()));
            outer.put(JSONConstants.AUTH, inner);
            jsonText = JSONValue.toJSONString(outer);
        } else if (object.getClass().equals(com.rackspacecloud.docs.auth.api.v1.ServiceCatalog.class)) {
            com.rackspacecloud.docs.auth.api.v1.ServiceCatalog serviceCatalog = (com.rackspacecloud.docs.auth.api.v1.ServiceCatalog) object;
            JSONObject outer = new JSONObject();
            outer.put(JSONConstants.SERVICECATALOG, getServiceCatalog11(serviceCatalog));
            jsonText = JSONValue.toJSONString(outer);
        } else {
            try {
                getMarshaller().marshallToJSON(object, outputStream);
            } catch (JAXBException e) {
                LOGGER.info(e.toString());
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

    @SuppressWarnings("unchecked")
    JSONObject getPolicyWithoutWrapper(Policy policy) {
        JSONObject policyInner = new JSONObject();
        policyInner.put(JSONConstants.ID, policy.getId());
        policyInner.put(JSONConstants.ENABLED, policy.isEnabled());
        policyInner.put(JSONConstants.GLOBAL, policy.isGlobal());
        policyInner.put(JSONConstants.BLOB, policy.getBlob());
        if( policy.getType() != null){
            policyInner.put(JSONConstants.TYPE, policy.getType());
        }
        if (policy.getName() != null) {
            policyInner.put(JSONConstants.NAME, policy.getName());
        }
        if (policy.getDescription() != null) {
            policyInner.put(JSONConstants.DESCRIPTION, policy.getDescription());
        }
        return policyInner;
    }

    @SuppressWarnings("unchecked")
    JSONObject getPoliciesWithoutWrapper(Policies policies) {
        JSONObject policyOuter = new JSONObject();
        JSONArray policyInner = new JSONArray();
        if(policies != null){
            for(Policy policy : policies.getPolicy()){
                JSONObject policySave = new JSONObject();
                if(policy.getId() != null){
                    policySave.put(JSONConstants.ID,policy.getId());
                }
                if(policy.getName() != null){
                    policySave.put(JSONConstants.NAME, policy.getName());
                }
                if(policy.getType() != null){
                    policySave.put(JSONConstants.TYPE, policy.getType());
                }
                policySave.put(JSONConstants.ENABLED, policy.isEnabled());
                policySave.put(JSONConstants.GLOBAL, policy.isGlobal());

                policyInner.add(policySave);
            }
        }
        policyOuter.put(JSONConstants.RAX_AUTH_POLICY, policyInner);
        if (policies.getAlgorithm() != null) {
            policyOuter.put(JSONConstants.POLICIES_ALGORITHM, policies.getAlgorithm().value());
        }
        return policyOuter;
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
    JSONObject getServiceCatalog11(com.rackspacecloud.docs.auth.api.v1.ServiceCatalog serviceCatalog) {
        JSONObject catalogItem = new JSONObject();
        if (serviceCatalog != null) {
            for (com.rackspacecloud.docs.auth.api.v1.Service service : serviceCatalog.getService()) {
                if (service.getName() != null) {
                    catalogItem.put(service.getName(), getEndpointsForCatalog11(service.getEndpoint()));
                }
            }
        }
        return catalogItem;
    }

    @SuppressWarnings("unchecked")
    JSONObject getToken(Token token) {
        if(token == null || token.getExpires() == null){
            throw new BadRequestException("Invalid token.");
        }

        JSONObject tokenInner = new JSONObject();
        tokenInner.put(JSONConstants.ID, token.getId());
        tokenInner.put(JSONConstants.EXPIRES, token.getExpires().toString());

        if (token.getTenant() != null) {
            JSONObject tenantInner = new JSONObject();
            tokenInner.put(JSONConstants.TENANT, tenantInner);
            tenantInner.put(JSONConstants.ID, token.getTenant().getId());
            tenantInner.put(JSONConstants.NAME, token.getTenant().getName());
        }

        if (token.getAny().size() > 0) {
            for (Object response : token.getAny()) {
                if (response instanceof JAXBElement) {
                    JAXBElement jaxbElement = ((JAXBElement)response);

                    if (jaxbElement.getDeclaredType().isAssignableFrom(AuthenticatedBy.class)) {
                        AuthenticatedBy authenticatedBy = (AuthenticatedBy) ((JAXBElement) response).getValue();
                        JSONArray credentials = new JSONArray();
                        for (String credential : authenticatedBy.getCredential()) {
                            credentials.add(credential);
                        }
                        tokenInner.put(JSONConstants.RAX_AUTH_AUTHENTICATED_BY, credentials);
                    }
                }
            }
        }

        return tokenInner;
    }

    @SuppressWarnings("unchecked")
    JSONObject getToken11(com.rackspacecloud.docs.auth.api.v1.Token token) {
        if(token == null || token.getExpires() == null){
            throw new BadRequestException("Invalid token.");
        }

        JSONObject tokenInner = new JSONObject();
        tokenInner.put(JSONConstants.ID, token.getId());
        tokenInner.put(JSONConstants.EXPIRES, token.getExpires().toString());
        return tokenInner;
    }

    @SuppressWarnings("unchecked")
    JSONArray getEndpointsForCatalog11(List<com.rackspacecloud.docs.auth.api.v1.Endpoint> endpoints) {
        JSONArray endpointList = new JSONArray();
        for (com.rackspacecloud.docs.auth.api.v1.Endpoint endpoint : endpoints) {
            JSONObject endpointItem = new JSONObject();
            if (endpoint.getPublicURL() != null) {
                endpointItem.put(JSONConstants.PUBLIC_URL, endpoint.getPublicURL());
            }
            if (endpoint.getInternalURL() != null) {
                endpointItem.put(JSONConstants.INTERNAL_URL, endpoint.getInternalURL());
            }
            if (endpoint.getRegion() != null) {
                endpointItem.put(JSONConstants.REGION, endpoint.getRegion());
            }
            if (endpoint.getAdminURL() != null) {
                endpointItem.put(JSONConstants.PUBLIC_URL, endpoint.getPublicURL());
            }
            if (endpoint.isV1Default()) {
                endpointItem.put(JSONConstants.V1_DEFAULT, endpoint.isV1Default());
            }
            endpointList.add(endpointItem);
        }
        return endpointList;
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

    JSONMarshaller getMarshaller() throws JAXBException {
        return ((JSONJAXBContext) JAXBContextResolver.get()).createJSONMarshaller();
    }
}
