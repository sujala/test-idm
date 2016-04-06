package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import com.rackspace.idm.domain.service.RoleLevelEnum;
import com.rackspace.idm.exception.ErrorCodeIdmException;
import com.rackspace.idm.exception.IdmException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.dozer.Mapper;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.RoleList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RoleConverterCloudV20 {
    private static final Logger logger = LoggerFactory.getLogger(RoleConverterCloudV20.class);

    @Autowired
    private Mapper mapper;

    @Autowired
    private JAXBObjectFactories objFactories;

    @Autowired
    private Configuration config;

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
                    jaxbRoles.getRole().add(jaxbRole);
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
        IdentityUserTypeEnum administratorRole = IdentityUserTypeEnum.fromRoleName(role.getAdministratorRole());

        if (administratorRole == null) {
            throw new IdmException(String.format("Specified administrator role '%s' is not valid", role.getAdministratorRole()), ErrorCodes.ERROR_CODE_INVALID_ATTRIBUTE );
        }

        int roleWeight = 0;
        if (administratorRole == IdentityUserTypeEnum.SERVICE_ADMIN) {
            roleWeight = RoleLevelEnum.LEVEL_50.getLevelAsInt();
        } else if (administratorRole == IdentityUserTypeEnum.IDENTITY_ADMIN) {
            roleWeight = RoleLevelEnum.LEVEL_500.getLevelAsInt();
        } else if (administratorRole == IdentityUserTypeEnum.USER_MANAGER) {
            roleWeight = RoleLevelEnum.LEVEL_1000.getLevelAsInt();
        } else {
            throw new IllegalArgumentException(String.format("Unrecognized administrator role of '%s'", role.getAdministratorRole()));
        }
        clientRole.setRsWeight(roleWeight);

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
        jaxbRole.setPropagate(role.getPropagate());
        jaxbRole.setServiceId(role.getClientId());

        return jaxbRole;
    }

    public Role toRoleFromClientRole(com.rackspace.idm.domain.entity.ClientRole role) {
        Role roleEntity = mapper.map(role, Role.class);
        roleEntity.setPropagate(role.getPropagate());
        roleEntity.setServiceId(role.getClientId());

        RoleLevelEnum roleLevelEnum = RoleLevelEnum.fromInt(role.getRsWeight());

        IdentityUserTypeEnum administratorRole = null;
        if (roleLevelEnum == RoleLevelEnum.LEVEL_50) {
            administratorRole = IdentityUserTypeEnum.SERVICE_ADMIN;
        } else if (roleLevelEnum == RoleLevelEnum.LEVEL_500) {
            administratorRole = IdentityUserTypeEnum.IDENTITY_ADMIN;
        } else if (roleLevelEnum == RoleLevelEnum.LEVEL_1000) {
            administratorRole = IdentityUserTypeEnum.USER_MANAGER;
        } else if (roleLevelEnum == IdentityUserTypeEnum.IDENTITY_ADMIN.getLevel()) {
            administratorRole = IdentityUserTypeEnum.SERVICE_ADMIN;
        } else if (roleLevelEnum == IdentityUserTypeEnum.USER_ADMIN.getLevel()) {
            administratorRole = IdentityUserTypeEnum.IDENTITY_ADMIN;
        } else if (roleLevelEnum == IdentityUserTypeEnum.USER_MANAGER.getLevel()) {
            administratorRole = IdentityUserTypeEnum.USER_ADMIN;
        } else if (roleLevelEnum == IdentityUserTypeEnum.DEFAULT_USER.getLevel()) {
            administratorRole = IdentityUserTypeEnum.USER_MANAGER;
        } else {
            logger.error(String.format("Client role with id '%s' has an invalid weight of %d", role.getId(), role.getRsWeight()));
            administratorRole = null;
        }

        if (administratorRole != null) {
            roleEntity.setAdministratorRole(administratorRole.getRoleName());
        }

        return roleEntity;
    }

    public List<TenantRole> toTenantRoles(org.openstack.docs.identity.api.v2.RoleList roleList) {
        if (roleList == null) {
            return null;
        }

        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        for (Role role : roleList.getRole()) {
            TenantRole tenantRole = new TenantRole();
            tenantRole.setName(role.getName());
            if (StringUtils.isNotBlank(role.getTenantId())) {
                tenantRole.getTenantIds().add(role.getTenantId());
            }
            tenantRoles.add(tenantRole);
        }

        return tenantRoles;
    }
}
