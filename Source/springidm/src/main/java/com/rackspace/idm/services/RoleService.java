package com.rackspace.idm.services;

import java.util.List;

import com.rackspace.idm.entities.Role;
import com.rackspace.idm.entities.User;

public interface RoleService {

    void addUserToRole(User user, Role role);

    void deleteUserFromRole(User user, Role role);

    Role getRole(String roleName, String customerId);

    List<Role> getRolesForUser(String username);

    List<Role> getByCustomerId(String customerId);
}
