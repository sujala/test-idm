package com.rackspace.idm.api.converter.cloudv20;

import java.util.List;

import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.RoleList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.TenantRole;

@Component
public class RoleConverterCloudV20 {

    @Autowired
    private JAXBObjectFactories OBJ_FACTORIES;

    public RoleList toRoleListJaxb(List<TenantRole> roles) {
        RoleList jaxbRoles = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRoleList();
        
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
        
        for (ClientRole role : roles) {
                Role jaxbRole = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRole();
                jaxbRole.setDescription(role.getDescription());
                jaxbRole.setId(role.getId());
                jaxbRole.setServiceId(role.getClientId());
                jaxbRoles.getRole().add(jaxbRole);
        }

        return jaxbRoles;
    }
    
    public Role toRole(com.rackspace.idm.domain.entity.TenantRole role) {
        Role jaxbRole = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRole();
        jaxbRole.setDescription(role.getDescription());
        jaxbRole.setId(role.getRoleRsId());
        jaxbRole.setServiceId(role.getClientId());
        return jaxbRole;
    }
    
    public Role toRoleFromClientRole(com.rackspace.idm.domain.entity.ClientRole role) {
        Role jaxbRole = OBJ_FACTORIES.getOpenStackIdentityV2Factory().createRole();
        jaxbRole.setDescription(role.getDescription());
        jaxbRole.setId(role.getId());
        jaxbRole.setServiceId(role.getClientId());
        return jaxbRole;
    }
}
