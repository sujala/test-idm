package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.oauth.OAuthService;

public class DefaultAuthorizationService implements AuthorizationService {

    private OAuthService oauthService;
    private ClientService clientService;
    private Logger logger;

    public DefaultAuthorizationService(OAuthService oauthService,
        ClientService clientService, Logger logger) {
        this.oauthService = oauthService;
        this.clientService = clientService;
        this.logger = logger;
    }

    public boolean authorizeRacker(String authHeader) {
        AccessToken token = oauthService.getTokenFromAuthHeader(authHeader);
        return token.getIsTrusted();
    }

    public boolean authorizeRackspaceClient(String authHeader) {
        AccessToken token = oauthService.getTokenFromAuthHeader(authHeader);
        return token.isClientToken()
            && token.getTokenClient().getCustomerId()
                .equals(GlobalConstants.RACKSPACE_CUSTOMER_ID);
    }

    public boolean authorizeClient(String authHeader, String verb, String uri) {
        AccessToken token = oauthService.getTokenFromAuthHeader(authHeader);

        if (!token.hasClientPermissions()) {
            return false;
        }

        List<String> allowedActions = getAllowedMethodsFromPermissions(token
            .getTokenClient().getPermissions());

        return checkPermissions(allowedActions, verb, uri);
    }

    public boolean authorizeUser(String authHeader, String customerId,
        String username) {

        AccessToken token = oauthService.getTokenFromAuthHeader(authHeader);

        if (token.isClientToken()) {
            return false;
        }

        boolean authorized = token.getTokenUser().getUsername()
            .equals(username)
            && token.getTokenUser().getCustomerId().equals(customerId);

        return authorized;
    }

    public boolean authorizeCustomerUser(String authHeader, String customerId) {

        AccessToken token = oauthService.getTokenFromAuthHeader(authHeader);

        if (token.isClientToken()) {
            return false;
        }

        boolean authorized = token.getTokenUser().getCustomerId() == customerId;

        return authorized;
    }

    public boolean authorizeAdmin(String authHeader, String customerId) {
        AccessToken token = oauthService.getTokenFromAuthHeader(authHeader);

        if (!token.hasUserRoles()
            || !token.getTokenUser().getCustomerId().equals(customerId)) {
            return false;
        }

        boolean authorized = false;

        for (Role r : token.getTokenUser().getRoles()) {
            if (r.getName().toLowerCase()
                .equals(GlobalConstants.IDM_ADMIN_ROLE_NAME.toLowerCase())) {
                authorized = true;
            }
        }

        return authorized;
    }

    private List<String> getAllowedMethodsFromPermissions(
        List<Permission> permissions) {

        if (permissions == null || permissions.size() < 1) {
            return null;
        }

        List<String> uris = new ArrayList<String>();

        for (Permission perm : permissions) {
            // TODO: Refactor for pulling from cache first
            Permission p = clientService
                .getDefinedPermissionByClientIdAndPermissionId(
                    perm.getClientId(), perm.getPermissionId());
            if (p != null) {
                uris.add(p.getValue());
            }
        }

        return uris;

    }

    private boolean checkPermissions(List<String> allowedActions, String verb,
        String uri) {

        String requestedActionURI = verb + " " + uri;
        requestedActionURI = requestedActionURI.toLowerCase();

        boolean result = false;

        if (allowedActions == null) {
            logger.debug("Empty Permission List.");
            return false;
        }

        for (String action : allowedActions) {

            result = result || checkPermission(action, requestedActionURI);

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
}
