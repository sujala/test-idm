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
    boolean isIdentityAccessRole(String rolename);
    List<ClientRole> getIdentityAccessRoles();

    /**
     * Get a list of all the Identity roles (prefixed with "identity:") that the identity application uses to make
     * authorization decisions.
     *
     * @return
     */
    List<ClientRole> getAllIdentityRoles();

    /**
     * Returns true if role is assigned to users, user group, delegation agreements or anything else.
     *
     * @param roleId
     * @throws IllegalArgumentException if roleId is null
     * @return boolean
     */
    boolean isRoleAssigned (String roleId);

}
