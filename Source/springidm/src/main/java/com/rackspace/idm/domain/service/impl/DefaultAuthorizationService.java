package com.rackspace.idm.domain.service.impl;

import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientScopeAccess;
import com.rackspace.idm.domain.entity.PermissionEntity;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.entity.hasAccessToken;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.util.WadlTrie;

public class DefaultAuthorizationService implements AuthorizationService {

    private final ClientDao clientDao;
    private final Configuration config;
    private final ScopeAccessDao scopeAccessDao;
    private final WadlTrie wadlTrie;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static String IDM_ADMIN_GROUP_DN = null;

    public DefaultAuthorizationService(ScopeAccessDao scopeAccessDao,
        ClientDao clientDao, WadlTrie wadlTrie, Configuration config) {
        this.scopeAccessDao = scopeAccessDao;
        this.clientDao = clientDao;
        this.wadlTrie = wadlTrie;
        this.config = config;
    }

    @Override
    public boolean authorizeRacker(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as racker", scopeAccess);
        boolean authorized = scopeAccess instanceof RackerScopeAccess;
        logger.debug("Authorized {} as racker - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeRackspaceClient(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as rackspace client", scopeAccess);
        if (!(scopeAccess instanceof ClientScopeAccess)) {
            return false;
        }
        boolean authorized = scopeAccess.getClientRCN().equalsIgnoreCase(
            this.getRackspaceCustomerId());
        logger.debug("Authorized {} as rackspace client - {}", scopeAccess,
            authorized);
        return authorized;
    }

    @Override
    public boolean authorizeClient(ScopeAccess scopeAccess, String verb,
        UriInfo uriInfo) {
        logger.debug("Authorizing {} as client", scopeAccess);
        if (!(scopeAccess instanceof ClientScopeAccess)) {
            return false;
        }

        Object o = wadlTrie.getPermissionFor(verb, uriInfo);
        String permissionId = o == null ? null : o.toString();

        boolean authorized = false;

        if (!StringUtils.isBlank(permissionId)) {
            PermissionEntity permission = new PermissionEntity();
            permission.setClientId(getIdmClientId());
            permission.setCustomerId(getRackspaceCustomerId());
            permission.setPermissionId(permissionId);

            authorized = this.scopeAccessDao.doesAccessTokenHavePermission(
                ((ClientScopeAccess) scopeAccess).getAccessTokenString(),
                permission);
        }
        logger.debug("Authorized {} as client - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeUser(ScopeAccess scopeAccess,
        String customerId, String username) {
        logger.debug("Authorizing {} as user", scopeAccess);
        if (!(scopeAccess instanceof UserScopeAccess)) {
            return false;
        }

        UserScopeAccess usa = (UserScopeAccess) scopeAccess;

        boolean authorized = usa.getUsername().equals(username)
            && usa.getUserRCN().equalsIgnoreCase(customerId);
        logger.debug("Authorized {} as user - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeCustomerUser(ScopeAccess scopeAccess,
        String customerId) {
        logger.debug("Authorizing {} as customer user", scopeAccess);
        if (!(scopeAccess instanceof UserScopeAccess)) {
            return false;
        }

        UserScopeAccess usa = (UserScopeAccess) scopeAccess;

        boolean authorized = usa.getUserRCN().equalsIgnoreCase(customerId);
        logger.debug("Authorized {} as customer user - {}", scopeAccess,
            authorized);
        return authorized;
    }

    @Override
    public boolean authorizeAdmin(ScopeAccess scopeAccess,
        String customerId) {
        logger.debug("Authorizing {} as admin user", scopeAccess);
        if (!(scopeAccess instanceof UserScopeAccess)) {
            return false;
        }

        UserScopeAccess usa = (UserScopeAccess) scopeAccess;

        if (IDM_ADMIN_GROUP_DN == null) {
            ClientGroup group = this.clientDao.getClientGroup(
                getRackspaceCustomerId(), getIdmClientId(),
                getIdmAdminGroupName());
            IDM_ADMIN_GROUP_DN = group.getUniqueId();
        }

        boolean authorized = false;

        authorized = this.clientDao.isUserInClientGroup(usa.getUsername(),
            IDM_ADMIN_GROUP_DN)
            && customerId.equalsIgnoreCase(usa.getUserRCN());
        logger.debug("Authorized {} as admin user - {}", scopeAccess,
            authorized);
        return authorized;
    }

    @Override
    public boolean authorizeCustomerIdm(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as Idm", scopeAccess);
        if (!(scopeAccess instanceof ClientScopeAccess)) {
            return false;
        }

        boolean authorized = getIdmClientId().equalsIgnoreCase(
            scopeAccess.getClientId())
            && getRackspaceCustomerId().equalsIgnoreCase(
                scopeAccess.getClientRCN());
        logger.debug("Authorized {} as Idm - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeAsRequestorOrOwner(
        ScopeAccess targetScopeAccess,
        ScopeAccess requestingScopeAccess) {
        logger.debug("Authorizing as Requestor or Owner");

        boolean isRequestor = requestingScopeAccess instanceof ClientScopeAccess
            && requestingScopeAccess.getClientId().equalsIgnoreCase(
                targetScopeAccess.getClientId());

        boolean isOwner = false;

        if (targetScopeAccess instanceof ClientScopeAccess) {
            isOwner = requestingScopeAccess.getClientId().equals(
                targetScopeAccess.getClientId());
        } else if (targetScopeAccess instanceof UserScopeAccess) {
            isOwner = ((UserScopeAccess) requestingScopeAccess)
                .getUsername().equals(
                    ((UserScopeAccess) targetScopeAccess).getUsername());
        } else if (targetScopeAccess instanceof RackerScopeAccess) {
            isOwner = ((RackerScopeAccess) requestingScopeAccess)
                .getRackerId()
                .equals(
                    ((RackerScopeAccess) targetScopeAccess).getRackerId());
        }
        
        logger.debug("Authorized as Requestor({}) or Owner({})", isRequestor,
            isOwner);
        return (isRequestor || isOwner);
    }

    @Override
    public void checkAuthAndHandleFailure(boolean authorized,
        ScopeAccess token) {
        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                ((hasAccessToken) token).getAccessTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
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
