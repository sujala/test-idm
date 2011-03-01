package com.rackspace.idm.domain.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.domain.dao.RoleDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Role;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.RoleService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;

public class DefaultRoleService implements RoleService {

    private RoleDao roleDao;
    private UserDao userDao;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultRoleService(RoleDao roleDao, UserDao userDao) {
        this.roleDao = roleDao;
        this.userDao = userDao;
    }

    public void addUserToRole(User user, Role role) {
        logger.info("Adding user {} to Role {}", user, role);
        if (user == null) {
            logger.warn("Null user passed");
            throw new IllegalArgumentException("Null user passed");
        }
        if (role == null) {
            logger.warn("Null role passed");
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
            logger.warn("Null user passed");
            throw new IllegalArgumentException("Null user passed");
        }
        if (role == null) {
            logger.warn("Null role passed");
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
