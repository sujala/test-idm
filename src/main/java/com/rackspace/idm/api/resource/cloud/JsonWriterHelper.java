package com.rackspace.idm.api.resource.cloud;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.AuthenticatedBy;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.JSONConstants;
import com.rackspace.idm.exception.BadRequestException;
import org.apache.cxf.common.util.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.v2.*;
import org.w3._2005.atom.Link;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.List;

import static com.rackspace.idm.RaxAuthConstants.QNAME_PROPAGATE;
import static com.rackspace.idm.RaxAuthConstants.QNAME_WEIGHT;

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/28/13
 * Time: 5:15 PM
 * To change this template use File | Settings | File Templates.
 */
public class JsonWriterHelper {

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

    public static JSONObject getUser(User user) {
        JSONObject outer = new JSONObject();
        outer.put(JSONConstants.ID, user.getId());
        outer.put(JSONConstants.USERNAME, user.getUsername());
        if (user.getEmail() != null) {
            outer.put(JSONConstants.EMAIL, user.getEmail());
        }
        outer.put(JSONConstants.ENABLED, user.isEnabled());
        if (user instanceof UserForCreate && ((UserForCreate) user).getPassword() != null) {
            outer.put(JSONConstants.OS_KSADM_PASSWORD, ((UserForCreate) user).getPassword());
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

        if (user.getOtherAttributes().size() != 0) {
            String defaultRegion = user.getOtherAttributes().get(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "defaultRegion"));
            if (!StringUtils.isEmpty(defaultRegion)) {
                userInner.put(JSONConstants.RAX_AUTH_DEFAULT_REGION, defaultRegion);
            } else {
                userInner.put(JSONConstants.RAX_AUTH_DEFAULT_REGION, "");
            }
        } else {
            userInner.put(JSONConstants.RAX_AUTH_DEFAULT_REGION, "");
        }

        return userInner;
    }
}
