package com.rackspace.idm.domain.service.impl;

import javax.ws.rs.core.UriInfo;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.ClientScopeAccess;
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.Entity;
import com.rackspace.idm.domain.entity.HasAccessToken;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.RackerScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.util.WadlTrie;

public class DefaultAuthorizationService implements AuthorizationService {

    private final ApplicationDao clientDao;
    private final Configuration config;
    private final ScopeAccessDao scopeAccessDao;
    private final TenantDao tenantDao;
    private final WadlTrie wadlTrie;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static String IDM_ADMIN_GROUP_DN = null;
    private static ClientRole CLOUD_ADMIN_ROLE = null;
    private static ClientRole IDM_SUPER_ADMIN_ROLE = null;

    public DefaultAuthorizationService(ScopeAccessDao scopeAccessDao,
        ApplicationDao clientDao, TenantDao tenantDao, WadlTrie wadlTrie,
        Configuration config) {
        this.scopeAccessDao = scopeAccessDao;
        this.clientDao = clientDao;
        this.tenantDao = tenantDao;
        this.wadlTrie = wadlTrie;
        this.config = config;
    }

    
    @Override
	public void authorize(String token, Entity object, String... authorizedRoles) 
    		throws ForbiddenException {

    	final ScopeAccess scopeAccess = scopeAccessDao.getScopeAccessByAccessToken(token.trim());
    	
    	// 1. if client has super admin role, grant access
    	// 2. if client is the entity being modified, grant access
    	// 3. if client has any of the authorized roles, grant access
    	
    	if (doesClientHaveSuperAdminRole(scopeAccess)) {
    		return;
    	}
    	
    	if (isClientTheEntityBeingAccessed(scopeAccess, object)) {
    		return;
    	}
    
    	//if (doesClientHaveAuthorizedRole(scopeAccess, ))
    	
    	//TODO: implement
	}
    
    private boolean doesClientHaveAuthorizedRoles(ScopeAccess scopeAccess, String... authorizedRoles) {
    	return false;
    }
    
    private boolean doesClientHaveSuperAdminRole(ScopeAccess scopeAccess) {
    	return this.tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, getIdmSuperAdminRole());
    }
    
	private boolean isClientTheEntityBeingAccessed(ScopeAccess scopeAccess, Entity entity) {
//		if (entity != null) {
//			if (scopeAccess instanceof ClientScopeAccess) {
//				ClientScopeAccess csa = (ClientScopeAccess) scopeAccess;
//				return csa.getClientId().equals(entity.getEntityId()) && entity.getEntityType() == Entity.APPLICATION;
//		    }
//			 
//			if (scopeAccess instanceof UserScopeAccess) {
//				UserScopeAccess usa = (UserScopeAccess) scopeAccess;
//				return usa.getUserId().equals(entity.getEntityId()) && entity.getEntityType() == Entity.USER;
//			}
//	
//			if (scopeAccess instanceof DelegatedClientScopeAccess) {
//				DelegatedClientScopeAccess dcsa = (DelegatedClientScopeAccess) scopeAccess;
//				return dcsa.getUserId().equals(entity.getEntityId()) && entity.getEntityType() == Entity.USER;
//			}
//		}
//		
		return false;
	}
    
    private boolean isClientThe(ScopeAccess scopeAccess, Entity entity) {
    	return false;
    }
    
    private ClientRole getIdmSuperAdminRole() {
    	if (IDM_SUPER_ADMIN_ROLE == null) {
    		IDM_SUPER_ADMIN_ROLE = this.clientDao.getClientRoleById(ClientRole.SUPER_ADMIN_ROLE);
    	}
    	
    	return IDM_SUPER_ADMIN_ROLE;
    }

	@Override
    public boolean authorizeCloudAdmin(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as cloud admin", scopeAccess);

        if (scopeAccess == null
            || ((HasAccessToken) scopeAccess)
                .isAccessTokenExpired(new DateTime())) {
            return false;
        }

        if (CLOUD_ADMIN_ROLE == null) {
            ClientRole role = this.clientDao
                .getClientRoleByClientIdAndRoleName(getCloudAuthClientId(),
                    getCloudAuthAdminRole());
            CLOUD_ADMIN_ROLE = role;
        }

        boolean authorized = this.tenantDao.doesScopeAccessHaveTenantRole(
            scopeAccess, CLOUD_ADMIN_ROLE);

        logger.debug("Authorized {} as cloud admin - {}", scopeAccess,
            authorized);
        return authorized;
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
            Permission permission = new Permission();
            permission.setClientId(getIdmClientId());
            permission.setCustomerId(getRackspaceCustomerId());
            permission.setPermissionId(permissionId);

            authorized = this.scopeAccessDao.doesAccessTokenHavePermission(
                scopeAccess, permission);
        }
        logger.debug("Authorized {} as client - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeUser(ScopeAccess scopeAccess, String customerId,
        String username) {
        logger.debug("Authorizing {} as user", scopeAccess);

        boolean authorized = false;

        if (scopeAccess instanceof UserScopeAccess) {
            UserScopeAccess usa = (UserScopeAccess) scopeAccess;
            authorized = usa.getUsername().equals(username)
                && usa.getUserRCN().equalsIgnoreCase(customerId);
        } else if (scopeAccess instanceof DelegatedClientScopeAccess) {
            DelegatedClientScopeAccess dcsa = (DelegatedClientScopeAccess) scopeAccess;
            authorized = dcsa.getUsername().equals(username)
                && dcsa.getUserRCN().equalsIgnoreCase(customerId);
        }

        logger.debug("Authorized {} as user - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeCustomerUser(ScopeAccess scopeAccess,
        String customerId) {
        logger.debug("Authorizing {} as customer user", scopeAccess);

        boolean authorized = false;

        if (scopeAccess instanceof UserScopeAccess) {
            UserScopeAccess usa = (UserScopeAccess) scopeAccess;
            authorized = usa.getUserRCN().equalsIgnoreCase(customerId);
        } else if (scopeAccess instanceof DelegatedClientScopeAccess) {
            DelegatedClientScopeAccess dcsa = (DelegatedClientScopeAccess) scopeAccess;
            authorized = dcsa.getUserRCN().equalsIgnoreCase(customerId);
        }

        logger.debug("Authorized {} as customer user - {}", scopeAccess,
            authorized);
        return authorized;
    }

    @Override
    public boolean authorizeAdmin(ScopeAccess scopeAccess, String customerId) {
        logger.debug("Authorizing {} as admin user", scopeAccess);
        if (!(scopeAccess instanceof UserScopeAccess || scopeAccess instanceof DelegatedClientScopeAccess)) {
            return false;
        }

        String username = null;
        String RCN = null;

        if (scopeAccess instanceof UserScopeAccess) {
            UserScopeAccess usa = (UserScopeAccess) scopeAccess;
            username = usa.getUsername();
            RCN = usa.getUserRCN();
        } else if (scopeAccess instanceof DelegatedClientScopeAccess) {
            DelegatedClientScopeAccess dcsa = (DelegatedClientScopeAccess) scopeAccess;
            username = dcsa.getUsername();
            RCN = dcsa.getUserRCN();
        }

        if (IDM_ADMIN_GROUP_DN == null) {
            ClientGroup group = this.clientDao.getClientGroup(
                getRackspaceCustomerId(), getIdmClientId(),
                getIdmAdminGroupName());
            IDM_ADMIN_GROUP_DN = group.getUniqueId();
        }

        boolean authorized = false;

        authorized = this.clientDao.isUserInClientGroup(username,
            IDM_ADMIN_GROUP_DN) && customerId.equalsIgnoreCase(RCN);
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
    public boolean authorizeAsRequestorOrOwner(ScopeAccess targetScopeAccess,
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
            isOwner = ((UserScopeAccess) requestingScopeAccess).getUsername()
                .equals(((UserScopeAccess) targetScopeAccess).getUsername());
        } else if (targetScopeAccess instanceof RackerScopeAccess) {
            isOwner = ((RackerScopeAccess) requestingScopeAccess).getRackerId()
                .equals(((RackerScopeAccess) targetScopeAccess).getRackerId());
        }

        logger.debug("Authorized as Requestor({}) or Owner({})", isRequestor,
            isOwner);
        return (isRequestor || isOwner);
    }

    @Override
    public void checkAuthAndHandleFailure(boolean authorized, ScopeAccess token) {
        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                ((HasAccessToken) token).getAccessTokenString());
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

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    private String getCloudAuthAdminRole() {
        return config.getString("cloudAuth.adminRole");
    }
}
