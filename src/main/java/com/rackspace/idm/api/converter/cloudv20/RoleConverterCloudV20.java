package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.TenantRole;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.RoleList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.rackspace.idm.RaxAuthConstants.*;

@Component
public class RoleConverterCloudV20 {

    @Autowired
    private JAXBObjectFactories objFactories;

    @Autowired
    Configuration config;

    public RoleList toRoleListJaxb(List<TenantRole> roles) {
        RoleList jaxbRoles = objFactories.getOpenStackIdentityV2Factory().createRoleList();

        if (roles == null || roles.size() == 0) {
            return jaxbRoles;
        }
        for (TenantRole role : roles) {

            if (role.getTenantIds() != null && role.getTenantIds().length > 0) {
                for (String tenantId : role.getTenantIds()) {
                    Role jaxbRole = objFactories.getOpenStackIdentityV2Factory().createRole();
                    jaxbRole.setDescription(role.getDescription());
                    jaxbRole.setName(role.getName());
                    jaxbRole.setId(role.getRoleRsId());
                    jaxbRole.setServiceId(role.getClientId());
                    jaxbRole.setTenantId(tenantId);
                    jaxbRoles.getRole().add(jaxbRole);
                }
            } else {
                Role jaxbRole = objFactories.getOpenStackIdentityV2Factory().createRole();
                jaxbRole.setDescription(role.getDescription());
                jaxbRole.setName(role.getName());
                jaxbRole.setId(role.getRoleRsId());
                jaxbRole.setServiceId(role.getClientId());
                jaxbRoles.getRole().add(jaxbRole);
            }
        }

        return jaxbRoles;
    }

    public ClientRole toClientRoleFromRole(Role role, String clientId) {
        ClientRole clientRole = new ClientRole();
        clientRole.setClientId(clientId);
        clientRole.setDescription(role.getDescription());
        clientRole.setName(role.getName());

        if (role.getOtherAttributes().containsKey(QNAME_WEIGHT)) {
            String weight = role.getOtherAttributes().get(QNAME_WEIGHT);
            clientRole.setRsWeight(Integer.parseInt(weight));
        } else {
            clientRole.setRsWeight(config.getInt("cloudAuth.special.rsWeight"));
        }

        if (role.getOtherAttributes().containsKey(QNAME_PROPAGATE)) {
            String propagate = role.getOtherAttributes().get(QNAME_PROPAGATE);
            clientRole.setPropagate(Boolean.parseBoolean(propagate));
        }

        return clientRole;
    }

    public RoleList toRoleListFromClientRoles(List<ClientRole> roles) {
        RoleList jaxbRoles = objFactories.getOpenStackIdentityV2Factory().createRoleList();
        
        if (roles == null || roles.size() == 0) {
            return jaxbRoles;
        }

        for (ClientRole role : roles) {
            Role jaxbRole = toRoleFromClientRole(role);
            jaxbRoles.getRole().add(jaxbRole);
        }

        return jaxbRoles;
    }

    public Role toRole(com.rackspace.idm.domain.entity.TenantRole role) {
        if(role == null){
            throw new IllegalArgumentException("TenantRole cannot be null");
        }
            Role jaxbRole = objFactories.getOpenStackIdentityV2Factory().createRole();
            jaxbRole.setDescription(role.getDescription());
            jaxbRole.setId(role.getRoleRsId());
            //jaxbRole.setServiceId(role.getClientId()); // ToDo: Removed from displaying for now.

        return jaxbRole;
    }

    public Role toRoleFromClientRole(
        com.rackspace.idm.domain.entity.ClientRole role) {
        if(role == null){
            throw new IllegalArgumentException("TenantRole cannot be null");
        }
        Role jaxbRole = objFactories.getOpenStackIdentityV2Factory().createRole();
        jaxbRole.setDescription(role.getDescription());
        jaxbRole.setId(role.getId());
        jaxbRole.setName(role.getName());
        jaxbRole.setServiceId(role.getClientId());

        jaxbRole.getOtherAttributes().put(QNAME_WEIGHT, Integer.toString(role.getRsWeight()));

        if (role.getPropagate() != null) {
            jaxbRole.getOtherAttributes().put(QNAME_PROPAGATE, Boolean.toString(role.getPropagate()));
        }

        return jaxbRole;
    }

    public void setObjFactories(JAXBObjectFactories objFactories) {
        this.objFactories = objFactories;
    }
}
