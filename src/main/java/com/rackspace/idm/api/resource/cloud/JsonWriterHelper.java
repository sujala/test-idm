package com.rackspace.idm.api.resource.cloud;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.JSONConstants;
import org.apache.cxf.common.util.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.User;
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
}
