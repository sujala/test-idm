package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.ClientRole;

public interface RoleService {

    ClientRole getRoleByName(String roleName);
    ClientRole getSuperUserAdminRole();
    ClientRole getUserAdminRole();
    ClientRole getUserManageRole();
    ClientRole getIdentityAdminRole();
    ClientRole getDefaultRole();
    ClientRole getComputeDefaultRole();
    ClientRole getObjectStoreDefaultRole();
}
