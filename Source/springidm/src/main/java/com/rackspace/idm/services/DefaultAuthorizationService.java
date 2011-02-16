package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;

import net.spy.memcached.MemcachedClient;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;

import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.ClientGroup;
import com.rackspace.idm.entities.Permission;

public class DefaultAuthorizationService implements AuthorizationService {

    private MemcachedClient memcached;
    private ClientDao clientDao;
    private Logger logger;
    private Configuration config;

    public DefaultAuthorizationService(ClientDao clientDao,
        MemcachedClient memcached, Configuration config, Logger logger) {
        this.memcached = memcached;
        this.clientDao = clientDao;
        this.logger = logger;
        this.config = config;
    }

    public boolean authorizeRacker(AccessToken token) {
        return token.getIsTrusted();
    }

    public boolean authorizeRackspaceClient(AccessToken token) {
        return token.isClientToken()
            && token.getTokenClient().getCustomerId()
                .equals(getRackspaceCustomerId());
    }

    public boolean authorizeClient(AccessToken token, String verb, String uri) {

        if (!token.hasClientPermissions()) {
            return false;
        }

        List<String> allowedActions = getAllowedMethodsFromPermissions(token
            .getTokenClient().getPermissions());

        return checkPermissions(allowedActions, verb, uri);
    }

    public boolean authorizeUser(AccessToken token, String customerId,
        String username) {

        if (token.isClientToken() || token.isRestrictedToSetPassword()) {
            return false;
        }

        boolean authorized = token.getTokenUser().getUsername()
            .equals(username)
            && token.getTokenUser().getCustomerId().equals(customerId);

        return authorized;
    }

    public boolean authorizeCustomerUser(AccessToken token, String customerId) {

        if (token.isClientToken() || token.isRestrictedToSetPassword()) {
            return false;
        }

        boolean authorized = token.getTokenUser().getCustomerId()
            .equals(customerId);

        return authorized;
    }

    public boolean authorizeAdmin(AccessToken token, String customerId) {

        if (token.isRestrictedToSetPassword() || !token.hasUserGroups()
            || !token.getTokenUser().getCustomerId().equals(customerId)) {

            return false;
        }

        boolean authorized = false;

        for (ClientGroup r : token.getTokenUser().getGroups()) {
            if (r.getClientId().equals(getIdmClientId())
                && r.getName().toLowerCase()
                    .equals(getIdmAdminGroupName().toLowerCase())) {
                authorized = true;
            }
        }

        return authorized;
    }

    public boolean authorizeCustomerIdm(AccessToken authToken) {
        if (!authToken.isClientToken()) {
            return false;
        }

        boolean authorized = getIdmClientId().equalsIgnoreCase(
            authToken.getTokenClient().getClientId())
            && getRackspaceCustomerId().equalsIgnoreCase(
                authToken.getTokenClient().getCustomerId());
        
        return authorized;
    }

    private List<String> getAllowedMethodsFromPermissions(
        List<Permission> permissions) {

        if (permissions == null || permissions.size() < 1) {
            return null;
        }

        List<String> uris = new ArrayList<String>();

        for (Permission perm : permissions) {
            if (perm.getClientId().equals(getIdmClientId())) {
                Permission p = (Permission) memcached.get(perm
                    .getPermissionId());
                if (p == null) {
                    p = clientDao
                        .getDefinedPermissionByClientIdAndPermissionId(
                            perm.getClientId(), perm.getPermissionId());
                    if (p != null) {
                        memcached.set(p.getPermissionId(), 3600, p);
                    }
                }
                if (p != null) {
                    uris.add(p.getValue());
                }
            }
        }

        return uris;
    }

    private boolean checkPermissions(List<String> allowedActions, String verb,
        String uri) {

        String requestedActionURI = verb + " " + uri;
        requestedActionURI = requestedActionURI.toLowerCase();

        boolean result = false;

        if (allowedActions == null || allowedActions.size() == 0) {
            logger.debug("Empty Permission List.");
            return false;
        }

        for (String action : allowedActions) {

            if (checkPermission(action, requestedActionURI)) {
                result = true;
                break;
            }

        }
        return result;
    }

    private boolean checkPermission(String actionURIRegex,
        String actionURIRequest) {

        if (actionURIRegex == null)
            return false;

        actionURIRegex = actionURIRegex.toLowerCase().trim();

        // We want to match the regex till the end of string.
        actionURIRegex += "$";

        return actionURIRequest.matches(actionURIRegex);
    }

    private String getIdmAdminGroupName() {
        return config.getString("idm.AdminGroupName");
    }

    private String getIdmClientId() {
        return config.getString("idm.clientId");
    }

    private String getRackspaceCustomerId() {
        return config.getString("rackspace.customerId");
    }
}
