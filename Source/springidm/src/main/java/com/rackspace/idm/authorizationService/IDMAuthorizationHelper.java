package com.rackspace.idm.authorizationService;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.XACMLRequestCreationException;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.RoleService;

public class IDMAuthorizationHelper {

    private OAuthService oauthService;
    private AuthorizationService authorizationService;
    private RoleService roleService;
    private ClientService clientService;
    private Logger logger;

    public IDMAuthorizationHelper() {
    }

    public IDMAuthorizationHelper(OAuthService oauthService,
        AuthorizationService authorizationService, RoleService roleService,
        ClientService clientService, Logger logger) {
        this.oauthService = oauthService;
        this.authorizationService = authorizationService;
        this.roleService = roleService;
        this.clientService = clientService;
        this.logger = logger;
    }

    public boolean checkAdminAuthorization(String authHeader,
        String userCompanyId, String methodName) throws ForbiddenException {

        String subjectUsername = oauthService
            .getUsernameFromAuthHeaderToken(authHeader);

        return checkAdminAuthorizationForUser(subjectUsername, userCompanyId,
            methodName);
    }

    public boolean checkAdminAuthorizationForUser(String subjectUsername,
        String userCompanyId, String methodName) {

        List<String> resourceCompany = new Vector<String>();
        resourceCompany.add(userCompanyId);

        Entity subject;
        try {
            subject = getSubjectWithRoleInformation(subjectUsername);
        } catch (IllegalArgumentException ex) {
            // String errorMsg = String.format(
            // "Invalid username parameter. Username: %s", subjectUsername);
            // throw new NotAuthorizedException(errorMsg);
            return false;
        }
        Entity resource = new Entity(AuthorizationConstants.RESOURCE);
        resource.addAttribute(AuthorizationConstants.RESOURCE_COMPANY_ID,
            AuthorizationConstants.TYPE_STRING, resourceCompany);

        Entity action = new Entity(AuthorizationConstants.ACTION);
        action.addAttribute(AuthorizationConstants.ACTION_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, methodName);

        return doAuthorization(subject, resource, action);
    }

    public boolean checkCompanyAuthorization(String authHeader,
        String userCompanyId, String methodName) {

        String subjectCompanyId = oauthService
            .getCustomerIdFromAuthHeaderToken(authHeader);

        if (subjectCompanyId == null) {
            return false;
        }

        Entity subject = new Entity(AuthorizationConstants.SUBJECT);
        subject.addAttribute(AuthorizationConstants.SUBJECT_COMPANY_ID,
            AuthorizationConstants.TYPE_STRING, subjectCompanyId);

        Entity resource = new Entity(AuthorizationConstants.RESOURCE);
        resource.addAttribute(AuthorizationConstants.RESOURCE_COMPANY_ID,
            AuthorizationConstants.TYPE_STRING, userCompanyId);

        Entity action = new Entity(AuthorizationConstants.ACTION);
        action.addAttribute(AuthorizationConstants.ACTION_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, methodName);

        return doAuthorization(subject, resource, action);
    }

    public boolean checkPermission(List<Permission> permissionList,
        String methodName, String lookupPath) {

        String requestedActionURI = methodName + " " + lookupPath;
        requestedActionURI = requestedActionURI.toLowerCase();

        boolean result = false;

        if (permissionList == null) {
            logger.debug("Empty Permission List.");
            return false;
        }

        for (Permission perm : permissionList) {

            String allowedActionURI = perm.getValue();

            result = result
                || doAuthorization(allowedActionURI, requestedActionURI);

        }
        return result;
    }

    public boolean checkPermission(String authHeader, String httpMethodName,
        String requestURI) {

        String clientId = oauthService
            .getClientIdFromAuthHeaderToken(authHeader);

        Client client = clientService.getById(clientId);

        if (client == null) {
            return false;
        }

        List<Permission> permissionList = client.getPermissions();

        return checkPermission(permissionList, httpMethodName, requestURI);
    }

    public boolean checkRackspaceClientAuthorization(String authHeader,
        String methodName) {

        String subjectCompanyName = oauthService
            .getCustomerIdFromAuthHeaderToken(authHeader);

        if (subjectCompanyName == null) {
            return false;
        }

        Entity subject = new Entity(AuthorizationConstants.SUBJECT);
        subject.addAttribute(AuthorizationConstants.SUBJECT_COMPANY_ID,
            AuthorizationConstants.TYPE_STRING, subjectCompanyName);

        Entity resource = new Entity(AuthorizationConstants.RESOURCE);

        Entity action = new Entity(AuthorizationConstants.ACTION);
        action.addAttribute(AuthorizationConstants.ACTION_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, methodName);

        return doAuthorization(subject, resource, action);
    }

    public boolean checkRackspaceEmployeeAuthorization(String authHeader) {
        
        // TODO - this needs to check the token and make sure it has the "trusted" flag
        if (StringUtils.isEmpty(authHeader)) {
            return false;
        }
        
        AccessToken token = oauthService.getTokenFromAuthHeader(authHeader);
        
        if (token.getIsTrusted()) {
            return true;
        }
        
        return false;
    }

    public boolean checkUserAuthorization(String subjectUsername,
        String username, String methodName) {

        String resourceUsername = null;

        resourceUsername = username;

        Entity subject = new Entity(AuthorizationConstants.SUBJECT);
        subject.addAttribute(AuthorizationConstants.SUBJECT_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, subjectUsername);

        Entity resource = new Entity(AuthorizationConstants.RESOURCE);
        resource.addAttribute(AuthorizationConstants.RESOURCE_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, resourceUsername);

        Entity action = new Entity(AuthorizationConstants.ACTION);
        action.addAttribute(AuthorizationConstants.ACTION_ID_ATTRIBUTE,
            AuthorizationConstants.TYPE_STRING, methodName);

        return doAuthorization(subject, resource, action);
    }

    public boolean doAuthorization(Entity subject, Entity resource,
        Entity action) {

        List<Entity> entities = new Vector<Entity>();
        entities.add(subject);
        entities.add(resource);
        entities.add(action);

        AuthorizationRequest authRequest = null;

        try {
            authRequest = authorizationService.createRequest(entities);
        } catch (XACMLRequestCreationException xacmlExp) {

            return false;
        }

        if (authorizationService.doAuthorization(authRequest)) {
            return true;
        }
        return false;
    }

    public boolean doAuthorization(String actionURIRegex,
        String actionURIRequest) {

        if (actionURIRegex == null)
            return false;

        actionURIRegex = actionURIRegex.toLowerCase().trim();
        
        // We want to match the regex till the end of string.
        actionURIRegex += "$";

        return actionURIRequest.matches(actionURIRegex);
    }

    public void handleAuthorizationFailure() {
        String errorMsg = String.format("Authorization Failed");
        logger.error(errorMsg);
        throw new ForbiddenException(errorMsg);
    }

    private Entity getSubjectWithRoleInformation(String subjectUsername) {
        Entity subject = new Entity(AuthorizationConstants.SUBJECT);
        List<String> subjectRoles = new Vector<String>();
        List<String> subjectCompany = new Vector<String>();

        List<Role> roles = roleService.getRolesForUser(subjectUsername);

        if (roles != null) {
            for (Iterator<Role> en = roles.listIterator(); en.hasNext();) {
                Role role = en.next();
                String roleName = role.getName().toLowerCase();
                String customerId = role.getCustomerId().toLowerCase();
                subjectRoles.add(roleName);
                subjectCompany.add(customerId);
            }

            subject.addAttribute(AuthorizationConstants.SUBJECT_ROLE_ATTRIBUTE,
                AuthorizationConstants.TYPE_STRING, subjectRoles);
            subject.addAttribute(
                AuthorizationConstants.SUBJECT_ROLE_COMPANY_ID,
                AuthorizationConstants.TYPE_STRING, subjectCompany);
        }
        return subject;
    }
}
