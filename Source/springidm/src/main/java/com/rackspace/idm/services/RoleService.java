package com.rackspace.idm.services;

import java.util.List;

import com.rackspace.idm.domain.entity.Role;
import com.rackspace.idm.domain.entity.User;

public interface RoleService {

    void addUserToRole(User user, Role role);

    void deleteUserFromRole(User user, Role role);

    List<Role> getByCustomerId(String customerId);

    Role getRole(String roleName, String customerId);

    List<Role> getRolesForUser(String username);
}
