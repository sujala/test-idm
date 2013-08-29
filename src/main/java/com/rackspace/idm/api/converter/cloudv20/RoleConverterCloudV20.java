package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.TenantRole;
import org.apache.commons.configuration.Configuration;
import org.dozer.Mapper;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.RoleList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.rackspace.idm.RaxAuthConstants.QNAME_PROPAGATE;
import static com.rackspace.idm.RaxAuthConstants.QNAME_WEIGHT;

@Component
public class RoleConverterCloudV20 {
    @Autowired
    Mapper mapper;

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

            if (role.getTenantIds() != null && role.getTenantIds().size() > 0) {

                for (String tenantId : role.getTenantIds()) {
                    Role jaxbRole = mapper.map(role, Role.class);
                    jaxbRole.setTenantId(tenantId);
                }
            } else {
                Role jaxbRole = mapper.map(role, Role.class);
                jaxbRoles.getRole().add(jaxbRole);
            }
        }

        return jaxbRoles;
    }

    public ClientRole fromRole(Role role, String clientId) {
        ClientRole clientRole = mapper.map(role, ClientRole.class);
        clientRole.setPropagate(role.isPropagate());
        clientRole.setClientId(clientId);

        if (clientRole.getRsWeight() < 1) {
            clientRole.setRsWeight(config.getInt("cloudAuth.special.rsWeight"));
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

        return jaxbRole;
    }

    public Role toRoleFromClientRole(com.rackspace.idm.domain.entity.ClientRole role) {
        return mapper.map(role, Role.class);
    }
}
