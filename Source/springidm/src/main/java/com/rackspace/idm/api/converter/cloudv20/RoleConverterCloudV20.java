package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.TenantRole;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.RoleList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RoleConverterCloudV20 {

    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;

    public RoleList toRoleListJaxb(List<TenantRole> roles) {
        RoleList jaxbRoles = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRoleList();

        if (roles == null || roles.size() == 0) {
            return jaxbRoles;
        }
        for (TenantRole role : roles) {

            if (role.getTenantIds() != null && role.getTenantIds().length > 0) {
                for (String tenantId : role.getTenantIds()) {
                    Role jaxbRole = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRole();
                    jaxbRole.setDescription(role.getDescription());
                    jaxbRole.setName(role.getName());
                    jaxbRole.setId(role.getRoleRsId());
                    jaxbRole.setServiceId(role.getClientId());
                    jaxbRole.setTenantId(tenantId);
                    jaxbRoles.getRole().add(jaxbRole);
                }
            } else {
                Role jaxbRole = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRole();
                jaxbRole.setDescription(role.getDescription());
                jaxbRole.setName(role.getName());
                jaxbRole.setId(role.getRoleRsId());
                jaxbRole.setServiceId(role.getClientId());
                jaxbRoles.getRole().add(jaxbRole);
            }
        }

        return jaxbRoles;
    }

    public RoleList toRoleListFromClientRoles(List<ClientRole> roles) {
        RoleList jaxbRoles = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRoleList();
        
        if (roles == null || roles.size() == 0) {
            return jaxbRoles;
        }

        for (ClientRole role : roles) {
            Role jaxbRole = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRole();
            jaxbRole.setId(role.getId());
            jaxbRole.setName(role.getName());
            jaxbRole.setDescription(role.getDescription());
            jaxbRole.setServiceId(role.getClientId());
            jaxbRoles.getRole().add(jaxbRole);
        }

        return jaxbRoles;
    }

    public Role toRole(com.rackspace.idm.domain.entity.TenantRole role) {
        if(role == null){
            throw new IllegalArgumentException("TenantRole cannot be null");
        }
            Role jaxbRole = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRole();
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
        Role jaxbRole = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRole();
        jaxbRole.setDescription(role.getDescription());
        jaxbRole.setId(role.getId());
        //jaxbRole.setServiceId(role.getClientId()); // ToDo: Removed from displaying for now.
        return jaxbRole;
    }

    public void setObjFactories(JAXBObjectFactories OBJ_FACTORIES) {
        this.OBJ_FACTORIES = OBJ_FACTORIES;
    }
}
