package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.rackspace.idm.dao.RoleDao;
import com.rackspace.idm.dao.UserDao;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.NotFoundException;

public class DefaultRoleService implements RoleService {

    private RoleDao roleDao;
    private UserDao userDao;
    private Logger logger;

    public DefaultRoleService(RoleDao roleDao, UserDao userDao, Logger logger) {
        this.roleDao = roleDao;
        this.userDao = userDao;
        this.logger = logger;
    }

    public void addUserToRole(User user, Role role) {
        logger.info("Adding user {} to Role {}", user, role);
        if (user == null) {
            logger.error("Null user passed");
            throw new IllegalArgumentException("Null user passed");
        }
        if (role == null) {
            logger.error("Null role passed");
            throw new IllegalArgumentException("Null role passed");
        }
        try {
            roleDao.addUserToRole(user.getUniqueId(), role.getUniqueId());
        } catch (DuplicateException drx) {
            logger.warn("User {} already in role {}", user, role);
            return;
        }
        logger.info("Added user {} to Role {}", user, role);
    }

    public void deleteUserFromRole(User user, Role role) {
        logger.info("Adding user {} to Role {}", user, role);
        if (user == null) {
            logger.error("Null user passed");
            throw new IllegalArgumentException("Null user passed");
        }
        if (role == null) {
            logger.error("Null role passed");
            throw new IllegalArgumentException("Null role passed");
        }
        try {
            roleDao.deleteUserFromRole(user.getUniqueId(), role.getUniqueId());
        } catch (NotFoundException nfe) {
            logger.warn("User {} doesn't have role {}", user, role);
            return;
        }
        logger.info("Added user {} to Role {}", user, role);
    }

    public Role getRole(String roleName, String customerId) {
        logger.debug("Getting role {} for customer {}", roleName, customerId);
        Role role = roleDao.findByRoleNameAndCustomerId(roleName, customerId);
        logger.debug("Got role {}", role);
        return role;
    }

    public List<Role> getRolesForUser(String username) {
        logger.info("Getting Roles for User: {}", username);
        String[] roleIds = userDao.getGroupIdsForUser(username);

        if (roleIds == null) {
            return null;
        }

        List<Role> roles = new ArrayList<Role>();

        for (String roleId : roleIds) {
            Role role = roleDao.findRoleByUniqueId(roleId);
            if (role != null) {
                roles.add(role);
            }
        }

        logger.info("Got Roles for User: {} - {}", username, roles);
        return roles;
    }

    public List<Role> getByCustomerId(String customerId) {
        logger.debug("Getting Roles for Customer: {}", customerId);
        List<Role> roles = this.roleDao.findByCustomerId(customerId);
        logger.debug("Got Roles for Customer: {}", customerId);
        return roles;
    }
}
