package com.rackspace.idm.domain.service.impl;

import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.domain.dao.ScopeAccessObjectDao;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.RackerScopeAccessObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.UserScopeAccessObject;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ClientService;
import com.rackspace.idm.util.WadlTrie;

public class DefaultAuthorizationService implements AuthorizationService {

    
    private final ClientService clientService;
    private final Configuration config;
    private final ScopeAccessObjectDao scopeAccessDao;
    private final WadlTrie wadlTrie;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultAuthorizationService(ScopeAccessObjectDao scopeAccessDao,
        ClientService clientService, WadlTrie wadlTrie, Configuration config) {
        this.scopeAccessDao = scopeAccessDao;
        this.clientService = clientService;
        this.wadlTrie = wadlTrie;
        this.config = config;
    }

    @Override
    public boolean authorizeRacker(ScopeAccessObject scopeAccess) {
        logger.debug("Authorizing {} as racker", scopeAccess);
        boolean authorized = scopeAccess instanceof RackerScopeAccessObject;
        logger.debug("Authorized {} as racker - {}", authorized);
        return authorized;
    }

    @Override
    public boolean authorizeRackspaceClient(ScopeAccessObject scopeAccess) {
        if (!(scopeAccess instanceof ClientScopeAccessObject)) {
            return false;
        }
        return scopeAccess.getClientRCN().equalsIgnoreCase(
            this.getRackspaceCustomerId());
    }

    @Override
    public boolean authorizeClient(ScopeAccessObject scopeAccess,
        String verb, UriInfo uriInfo) {

        if (!(scopeAccess instanceof ClientScopeAccessObject)) {
            return false;
        }
        
        String permissionId = wadlTrie.getPermissionFor(verb, uriInfo).toString();

        Permission permission = new Permission();
        permission.setClientId(getIdmClientId());
        permission.setCustomerId(getRackspaceCustomerId());
        permission.setPermissionId(permissionId);

        return this.scopeAccessDao.doesAccessTokenHavePermission(
            ((ClientScopeAccessObject) scopeAccess).getAccessTokenString(),
            permission);
    }

    @Override
    public boolean authorizeUser(ScopeAccessObject scopeAccess,
        String customerId, String username) {

        if (!(scopeAccess instanceof UserScopeAccessObject)) {
            return false;
        }

        UserScopeAccessObject usa = (UserScopeAccessObject) scopeAccess;

        boolean authorized = usa.getUsername().equals(username)
            && usa.getUserRCN().equalsIgnoreCase(customerId);

        return authorized;
    }

    @Override
    public boolean authorizeCustomerUser(ScopeAccessObject scopeAccess,
        String customerId) {

        if (!(scopeAccess instanceof UserScopeAccessObject)) {
            return false;
        }

        UserScopeAccessObject usa = (UserScopeAccessObject) scopeAccess;

        boolean authorized = usa.getUserRCN().equalsIgnoreCase(customerId);

        return authorized;
    }

    @Override
    public boolean authorizeAdmin(ScopeAccessObject scopeAccess,
        String customerId) {

        if (!(scopeAccess instanceof UserScopeAccessObject)) {
            return false;
        }

        UserScopeAccessObject usa = (UserScopeAccessObject) scopeAccess;

        ClientGroup group = new ClientGroup();
        group.setClientId(getIdmClientId());
        group.setCustomerId(getRackspaceCustomerId());
        group.setName(getIdmAdminGroupName());

        boolean authorized = false;

        authorized = this.clientService.isUserMemberOfClientGroup(
            usa.getUsername(), group);

        return authorized;
    }

    @Override
    public boolean authorizeCustomerIdm(ScopeAccessObject scopeAccess) {
        if (!(scopeAccess instanceof ClientScopeAccessObject)) {
            return false;
        }

        boolean authorized = getIdmClientId().equalsIgnoreCase(
            scopeAccess.getClientId())
            && getRackspaceCustomerId().equalsIgnoreCase(
                scopeAccess.getClientRCN());

        return authorized;
    }

    @Override
    public boolean authorizeAsRequestorOrOwner(
        ScopeAccessObject targetScopeAccess,
        ScopeAccessObject requestingScopeAccess) {

        if (!(requestingScopeAccess instanceof ClientScopeAccessObject)
            || !(targetScopeAccess instanceof UserScopeAccessObject)) {
            return false;
        }

        boolean isRequestor = requestingScopeAccess.getClientId()
            .equalsIgnoreCase(targetScopeAccess.getClientId());

        boolean isOwner = false;

        if (targetScopeAccess instanceof UserScopeAccessObject) {
            isOwner = ((UserScopeAccessObject) requestingScopeAccess)
                .getUsername().equals(
                    ((UserScopeAccessObject) targetScopeAccess).getUsername());
        } else if (targetScopeAccess instanceof RackerScopeAccessObject) {
            isOwner = ((RackerScopeAccessObject) requestingScopeAccess)
                .getRackerId()
                .equals(
                    ((RackerScopeAccessObject) targetScopeAccess).getRackerId());
        }

        boolean authorized = isRequestor || isOwner;
        return authorized;
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
