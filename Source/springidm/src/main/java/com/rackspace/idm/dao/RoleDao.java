package com.rackspace.idm.dao;

import java.util.List;

import com.rackspace.idm.entities.Role;

public interface RoleDao {
    void add(Role role);

    void addUserToRole(String userDN, String roleDN);

    void delete(String name, String customerId);

    void deleteUserFromRole(String userDN, String roleDN);

    Role findByRoleNameAndCustomerId(String roleName, String customerId);

    Role findByInum(String inum);

    void save(Role role);

    List<Role> findByCustomerId(String customerId);

    String getUnusedRoleInum(String customerInum);
}
