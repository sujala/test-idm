package com.rackspace.idm.api.resource.cloud;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policies;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Policy;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspacecloud.docs.auth.api.v1.BaseURL;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRef;
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList;
import org.apache.cxf.common.util.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.identity.api.v2.*;
import org.w3._2005.atom.Link;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.List;

import static com.rackspace.idm.RaxAuthConstants.QNAME_PROPAGATE;
import static com.rackspace.idm.RaxAuthConstants.QNAME_WEIGHT;

/**
 * Methods to help create object from json.
 * Most methods just set values if not null.
 */
public final class JsonWriterHelper {

    private JsonWriterHelper(){
        throw new AssertionError();
    }

    public static JSONArray getLinks(List<Object> any) {
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

    public static JSONObject getExtensionWithoutWrapper(Extension extension) {
        JSONObject outer = new JSONObject();

        outer.put(JSONConstants.NAME, extension.getName());
        outer.put(JSONConstants.NAMESPACE, extension.getNamespace());
        outer.put(JSONConstants.ALIAS, extension.getAlias());
        if (extension.getUpdated() != null) {
            outer.put(JSONConstants.UPDATED, extension.getUpdated().toString());
        }
        outer.put(JSONConstants.DESCRIPTION, extension.getDescription());

        if (extension.getAny().size() > 0) {
            JSONArray links = getLinks(extension.getAny());
            if (links.size() > 0) {
                outer.put(JSONConstants.LINKS, links);
            }
        }

        return outer;
    }

    public static JSONObject getApiKeyCredentials(ApiKeyCredentials creds) {
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();
        outer.put(JSONConstants.RAX_KSKEY_API_KEY_CREDENTIALS, inner);
        inner.put(JSONConstants.USERNAME, creds.getUsername());
        inner.put(JSONConstants.API_KEY, creds.getApiKey());
        return outer;
    }

    public static JSONObject getPasswordCredentials(
            PasswordCredentialsBase creds) {
        JSONObject outer = new JSONObject();
        JSONObject inner = new JSONObject();
        outer.put(JSONConstants.PASSWORD_CREDENTIALS, inner);
        inner.put(JSONConstants.USERNAME, creds.getUsername());
        inner.put(JSONConstants.PASSWORD, creds.getPassword());
        return outer;
    }

    public static JSONObject getRole(Role role) {
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
        if (role.getOtherAttributes().containsKey(QNAME_PROPAGATE)) {
            outer.put(JSONConstants.RAX_AUTH_PROPAGATE, role.getOtherAttributes().get(QNAME_PROPAGATE));
        }
        if (role.getOtherAttributes().containsKey(QNAME_WEIGHT)) {
            outer.put(JSONConstants.RAX_AUTH_WEIGHT, role.getOtherAttributes().get(QNAME_WEIGHT));
        }
        return outer;
    }

    public static JSONObject getUser(User user, boolean isMultiFactorGloballyEnabled) {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.ID, user.getId());
        outer.put(JSONConstants.USERNAME, user.getUsername());
        if (user.getEmail() != null) {
            outer.put(JSONConstants.EMAIL, user.getEmail());
        }
        outer.put(JSONConstants.ENABLED, user.isEnabled());
        if (user.getPassword() != null) {
            outer.put(JSONConstants.OS_KSADM_PASSWORD, user.getPassword());
        }

        if (user.getCreated() != null) {
            outer.put(JSONConstants.CREATED, user.getCreated().toString());

        }
        if (user.getUpdated() != null) {
            outer.put(JSONConstants.UPDATED, user.getUpdated().toString());
        }
        if(user.getDomainId() != null) {
            outer.put(JSONConstants.RAX_AUTH_DOMAIN_ID, user.getDomainId());
        }
        if(user.getDefaultRegion() != null){
            outer.put(JSONConstants.RAX_AUTH_DEFAULT_REGION, user.getDefaultRegion());
        }else {
            outer.put(JSONConstants.RAX_AUTH_DEFAULT_REGION, "");
        }

        if (user.getFederatedIdp() != null) {
            outer.put(JSONConstants.RAX_AUTH_FEDERATED_IDP, user.getFederatedIdp());
        }

        if (isMultiFactorGloballyEnabled) {
            boolean mfaEnabledValue = user.isMultiFactorEnabled() == null ? false : user.isMultiFactorEnabled();
            outer.put(JSONConstants.RAX_AUTH_MULTI_FACTOR_ENABLED, mfaEnabledValue);
            if(mfaEnabledValue) {
                outer.put(JSONConstants.RAX_AUTH_MULTI_FACTOR_STATE, user.getMultiFactorState().toString());
            }
        } else if (Boolean.TRUE.equals(user.isMultiFactorEnabled())) {
            outer.put(JSONConstants.RAX_AUTH_MULTI_FACTOR_ENABLED, true);
            outer.put(JSONConstants.RAX_AUTH_MULTI_FACTOR_STATE, user.getMultiFactorState().toString());
        }

        //display the user multifactor enforcement level if it's non-null regardless of the mfa setting
        if (user.getUserMultiFactorEnforcementLevel() != null) {
            outer.put(JSONConstants.RAX_AUTH_USER_MULTI_FACTOR_ENFORCEMENT_LEVEL, user.getUserMultiFactorEnforcementLevel().toString());
        }

        if (user.getTokenFormat() != null) {
            outer.put(JSONConstants.RAX_AUTH_TOKEN_FORMAT, user.getTokenFormat().value());
        }

        if (user.getContactId() != null) {
            outer.put(JSONConstants.RAX_AUTH_CONTACT_ID, user.getContactId());
        }

        if (user.getOtherAttributes().size() != 0) {
            String password = user.getOtherAttributes().get(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "password"));
            if (!StringUtils.isEmpty(password)) {
                outer.put(JSONConstants.OS_KSADM_PASSWORD, password);
            }
        }
        return outer;
    }

    public static JSONObject getToken(Token token) {
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

        if (token.getAuthenticatedBy() != null && token.getAuthenticatedBy().getCredential().size() > 0) {
            JSONArray credentials = new JSONArray();
            for (String credential : token.getAuthenticatedBy().getCredential()) {
                credentials.add(credential);
            }
            tokenInner.put(JSONConstants.RAX_AUTH_AUTHENTICATED_BY, credentials);
        }

        return tokenInner;
    }

    public static JSONArray getServiceCatalog(ServiceCatalog serviceCatalog) {
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

    public static JSONArray getEndpointsForCatalog(List<EndpointForService> endpoints) {
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

    public static JSONObject getTokenUser(UserForAuthenticateResponse user) {
        JSONObject userInner = new JSONObject();
        userInner.put(JSONConstants.ID, user.getId());
        if(user.getContactId() != null) {
            userInner.put(JSONConstants.RAX_AUTH_CONTACT_ID, user.getContactId());
        }
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

        if (!StringUtils.isEmpty(user.getFederatedIdp())) {
            userInner.put(JSONConstants.RAX_AUTH_FEDERATED_IDP, user.getFederatedIdp());
        }

        String defaultRegion = user.getDefaultRegion();
        if (!StringUtils.isEmpty(defaultRegion)) {
            userInner.put(JSONConstants.RAX_AUTH_DEFAULT_REGION, defaultRegion);
        } else {
            userInner.put(JSONConstants.RAX_AUTH_DEFAULT_REGION, "");
        }

        return userInner;
    }

    public static JSONObject getBaseUrl(BaseURL url) {
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

    public static JSONArray getBaseUrls(BaseURLRefList baseList) {
        JSONArray baseUrls = new JSONArray();
        if (baseList != null) {
            for (BaseURLRef url : baseList.getBaseURLRef()) {
                JSONObject urlItem = new JSONObject();
                urlItem.put(JSONConstants.ID, url.getId());
                urlItem.put(JSONConstants.HREF, url.getHref());
                urlItem.put(JSONConstants.V1_DEFAULT, url.isV1Default());
                baseUrls.add(urlItem);
            }
        }
        return baseUrls;
    }

    public static JSONObject getPoliciesWithoutWrapper(Policies policies) {
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

    public static JSONObject getServiceCatalog11(com.rackspacecloud.docs.auth.api.v1.ServiceCatalog serviceCatalog) {
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

    public static JSONArray getEndpointsForCatalog11(List<com.rackspacecloud.docs.auth.api.v1.Endpoint> endpoints) {
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
                endpointItem.put(JSONConstants.ADMIN_URL, endpoint.getAdminURL());
            }
            if (endpoint.isV1Default()) {
                endpointItem.put(JSONConstants.V1_DEFAULT, endpoint.isV1Default());
            }
            endpointList.add(endpointItem);
        }
        return endpointList;
    }

    public static JSONObject getToken11(com.rackspacecloud.docs.auth.api.v1.Token token) {
        if(token == null || token.getExpires() == null){
            throw new BadRequestException("Invalid token.");
        }

        JSONObject tokenInner = new JSONObject();
        tokenInner.put(JSONConstants.ID, token.getId());
        tokenInner.put(JSONConstants.EXPIRES, token.getExpires().toString());
        return tokenInner;
    }
}
