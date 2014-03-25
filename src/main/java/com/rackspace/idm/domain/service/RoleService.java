package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.ClientRole;

import java.util.List;

public interface RoleService {

    ClientRole getRoleByName(String roleName);
    ClientRole getSuperUserAdminRole();
    ClientRole getUserAdminRole();
    ClientRole getUserManageRole();
    ClientRole getIdentityAdminRole();
    ClientRole getDefaultRole();
    ClientRole getComputeDefaultRole();
    ClientRole getObjectStoreDefaultRole();
    boolean isIdentityAccessRole(ClientRole role);
    List<ClientRole> getIdentityAccessRoles();
}
