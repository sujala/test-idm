package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import org.apache.commons.collections4.CollectionUtils;


public class CreateUserUtil {

    public static void attachRoleToUser(ClientRole role, User user) {
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName(role.getName());

        if (!user.getRoles().contains(tenantRole)) {
            user.getRoles().add(tenantRole);
        }
    }

    public static boolean isCreateUserOneCall(org.openstack.docs.identity.api.v2.User user) {
        return user.getSecretQA() != null || user.getGroups() != null || user.getRoles() != null;
    }

}
