package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.util.WadlTrie;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

public class DefaultAuthorizationService implements AuthorizationService {

    private final ApplicationDao clientDao;
    private final Configuration config;
    private final ScopeAccessDao scopeAccessDao;
    private final TenantDao tenantDao;
    private final WadlTrie wadlTrie;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private ScopeAccessService scopeAccessService;

    private static String IDM_ADMIN_GROUP_DN = null;
    private static ClientRole CLOUD_ADMIN_ROLE = null;
    private static ClientRole CLOUD_SERVICE_ADMIN_ROLE = null;
    private static ClientRole CLOUD_USER_ROLE = null;
    private static ClientRole CLOUD_USER_ADMIN_ROLE = null;
    private static ClientRole IDM_SUPER_ADMIN_ROLE = null;
    private static ClientRole RACKER_ROLE = null ;

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

    	if(token == null){
            throw new IllegalArgumentException("Token cannot be null");
        }

        final ScopeAccess scopeAccess = scopeAccessDao.getScopeAccessByAccessToken(token.trim());
    	
    	// 1. if client has any of the authorized roles (by default super admin role), grant access
    	// 2. if client is the entity being modified, grant access
    	if (doesClientHaveAuthorizedRoles(scopeAccess, authorizedRoles)) {
    		return;
    	}
    	
    	if (isClientTheEntityBeingAccessed(scopeAccess, object)) {
    		return;
    	}
    
    	throw new ForbiddenException("Token " + token + " is not allowed to execute the specified capability.");
	}
    
    boolean doesClientHaveAuthorizedRoles(ScopeAccess scopeAccess, String... authorizedRoles) {
    	List<String> allAuthorizedRoles = createRoleList(authorizedRoles);
    	for (String authorizedRole : allAuthorizedRoles) {
    		ClientRole clientRole = this.clientDao.getClientRoleById(authorizedRole);
    		if (this.tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, clientRole)) {
    			return true;
    		}
    	}
    	
    	return false;
    }
    
    List<String> createRoleList(String... authorizedRoles) {
    	List<String> allAuthorizedRoles = new ArrayList<String>();
    	allAuthorizedRoles.add(ClientRole.SUPER_ADMIN_ROLE);
    	
    	if (authorizedRoles != null) {
    		for (String authorizedRole : authorizedRoles) {
    			allAuthorizedRoles.add(authorizedRole);
    		}
    	}
    	
    	return allAuthorizedRoles;
    }
    
	boolean isClientTheEntityBeingAccessed(ScopeAccess scopeAccess, Entity entity) {
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

	@Override
    public boolean authorizeCloudIdentityAdmin(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as cloud admin", scopeAccess);

        if (scopeAccess == null || ((HasAccessToken) scopeAccess).isAccessTokenExpired(new DateTime())) {
            return false;
        }

        if (CLOUD_ADMIN_ROLE == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthIdentityAdminRole());
            CLOUD_ADMIN_ROLE = role;
        }

        boolean authorized = this.tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, CLOUD_ADMIN_ROLE);

        logger.debug("Authorized {} as cloud admin - {}", scopeAccess, authorized);
        return authorized;
    }

    public boolean authorizeRacker(ScopeAccess scopeAccess){
        logger.debug("Authorizing {} as a Racker", scopeAccess);
        if (!(scopeAccess instanceof RackerScopeAccess)){
            return false;
        }
        if (scopeAccess == null || ((HasAccessToken) scopeAccess).isAccessTokenExpired(new DateTime())) {
            return false;
        }

        if (RACKER_ROLE == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(config.getString("idm.clientId"), "Racker");
            RACKER_ROLE = role;
        }

        boolean authorized = this.tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, RACKER_ROLE);

        logger.debug("Authorized {} as Racker - {}", scopeAccess, authorized);
        return authorized;
    }


    @Override
    public boolean authorizeCloudServiceAdmin(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as cloud service admin", scopeAccess);

        if (scopeAccess == null || ((HasAccessToken) scopeAccess).isAccessTokenExpired(new DateTime())) {
            return false;
        }

        if (CLOUD_SERVICE_ADMIN_ROLE == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthServiceAdminRole());
            CLOUD_SERVICE_ADMIN_ROLE = role;
        }

        boolean authorized = this.tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, CLOUD_SERVICE_ADMIN_ROLE);

        logger.debug("Authorized {} as cloud service admin - {}", scopeAccess, authorized);
        return authorized;
    }


    @Override
    public void authorizeIdmSuperAdminOrRackspaceClient(ScopeAccess scopeAccess) {
        boolean isRackspaceClient = authorizeRackspaceClient(scopeAccess);
        boolean isIdmSuperAdmin = false;
        //verify if caller is a rackspace client, idm client or super admin
        if(!isRackspaceClient){
            isIdmSuperAdmin = authorizeIdmSuperAdmin(scopeAccess);
        }

        if(!isRackspaceClient && ! isIdmSuperAdmin) {
            throw new ForbiddenException("Access denied");
        }
    }
 
    @Override
    public boolean authorizeCloudUserAdmin(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as cloud user admin", scopeAccess);

        if (scopeAccess == null || ((HasAccessToken) scopeAccess).isAccessTokenExpired(new DateTime())) {
            return false;
        }

        if (CLOUD_USER_ADMIN_ROLE == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserAdminRole());
            CLOUD_USER_ADMIN_ROLE = role;
        }

        boolean authorized = this.tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, CLOUD_USER_ADMIN_ROLE);

        logger.debug("Authorized {} as cloud user admin - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeCloudUser(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as cloud user ", scopeAccess);

        if (scopeAccess == null || ((HasAccessToken) scopeAccess).isAccessTokenExpired(new DateTime())) {
            return false;
        }

        if (CLOUD_USER_ROLE == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserRole());
            CLOUD_USER_ROLE = role;
        }

        boolean authorized = tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, CLOUD_USER_ROLE);

        logger.debug("Authorized {} as cloud user - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeIdmSuperAdmin(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as idm super admin", scopeAccess);

        if(this.authorizeCustomerIdm(scopeAccess)){
            return true;
        }

        if (scopeAccess == null || ((HasAccessToken) scopeAccess).isAccessTokenExpired(new DateTime())) {
            return false;
        }

        if (IDM_SUPER_ADMIN_ROLE == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(getIdmClientId(), getIdmSuperAdminRole());
            IDM_SUPER_ADMIN_ROLE = role;
        }

        boolean authorized = this.tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, IDM_SUPER_ADMIN_ROLE);

        logger.debug("Authorized {} as idm super admin - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeRackspaceClient(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as rackspace client", scopeAccess);
        if (!(scopeAccess instanceof ClientScopeAccess)) {
            return false;
        }
        boolean authorized = scopeAccess.getClientRCN().equalsIgnoreCase(this.getRackspaceCustomerId());
        logger.debug("Authorized {} as rackspace client - {}", scopeAccess, authorized);
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
            authorized = this.scopeAccessDao.doesAccessTokenHavePermission(scopeAccess, permission);
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
    public boolean authorizeCustomerUser(ScopeAccess scopeAccess, String customerId) {
        logger.debug("Authorizing {} as customer user", scopeAccess);

        boolean authorized = false;

        if (scopeAccess instanceof UserScopeAccess) {
            UserScopeAccess usa = (UserScopeAccess) scopeAccess;
            authorized = usa.getUserRCN().equalsIgnoreCase(customerId);
        } else if (scopeAccess instanceof DelegatedClientScopeAccess) {
            DelegatedClientScopeAccess dcsa = (DelegatedClientScopeAccess) scopeAccess;
            authorized = dcsa.getUserRCN().equalsIgnoreCase(customerId);
        }

        logger.debug("Authorized {} as customer user - {}", scopeAccess,  authorized);
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
            ClientGroup group = clientDao.getClientGroup(getRackspaceCustomerId(), getIdmClientId(), getIdmAdminGroupName());
            IDM_ADMIN_GROUP_DN = group.getUniqueId();
        }

        boolean authorized = false;
        authorized = clientDao.isUserInClientGroup(username, IDM_ADMIN_GROUP_DN) && customerId.equalsIgnoreCase(RCN);
        logger.debug("Authorized {} as admin user - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeCustomerIdm(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as Idm", scopeAccess);
        if (!(scopeAccess instanceof ClientScopeAccess)) {
            return false;
        }

        boolean authorized = getIdmClientId().equalsIgnoreCase(scopeAccess.getClientId())
                && getRackspaceCustomerId().equalsIgnoreCase(scopeAccess.getClientRCN());
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

    public void verifyIdmSuperAdminAccess(String authHeader) {
        if(!this.authorizeIdmSuperAdmin(scopeAccessService.getScopeAccessByAccessToken(authHeader))){
            throw new ForbiddenException("Access denied");
        }
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

    public static ClientRole getCLOUD_ADMIN_ROLE() {
        return CLOUD_ADMIN_ROLE;
    }

    public static void setCLOUD_ADMIN_ROLE(ClientRole CLOUD_ADMIN_ROLE) {
        DefaultAuthorizationService.CLOUD_ADMIN_ROLE = CLOUD_ADMIN_ROLE;
    }

    public static ClientRole getRACKER_ROLE() {
        return RACKER_ROLE;
    }

    public static void setRACKER_ROLE(ClientRole RACKER_ROLE) {
        DefaultAuthorizationService.RACKER_ROLE = RACKER_ROLE;
    }

    public static ClientRole getCLOUD_SERVICE_ADMIN_ROLE() {
        return CLOUD_SERVICE_ADMIN_ROLE;
    }

    public static void setCLOUD_SERVICE_ADMIN_ROLE(ClientRole CLOUD_SERVICE_ADMIN_ROLE) {
        DefaultAuthorizationService.CLOUD_SERVICE_ADMIN_ROLE = CLOUD_SERVICE_ADMIN_ROLE;
    }

    public static ClientRole getCLOUD_USER_ADMIN_ROLE() {
        return CLOUD_USER_ADMIN_ROLE;
    }

    public static void setCLOUD_USER_ADMIN_ROLE(ClientRole CLOUD_USER_ADMIN_ROLE) {
        DefaultAuthorizationService.CLOUD_USER_ADMIN_ROLE = CLOUD_USER_ADMIN_ROLE;
    }

    public static ClientRole getCLOUD_USER_ROLE() {
        return CLOUD_USER_ROLE;
    }

    public static void setCLOUD_USER_ROLE(ClientRole CLOUD_USER_ROLE) {
        DefaultAuthorizationService.CLOUD_USER_ROLE = CLOUD_USER_ROLE;
    }

    public static ClientRole getIDM_SUPER_ADMIN_ROLE() {
        return IDM_SUPER_ADMIN_ROLE;
    }

    public static void setIDM_SUPER_ADMIN_ROLE(ClientRole IDM_SUPER_ADMIN_ROLE) {
        DefaultAuthorizationService.IDM_SUPER_ADMIN_ROLE = IDM_SUPER_ADMIN_ROLE;
    }

    public static String getIDM_ADMIN_GROUP_DN() {
        return IDM_ADMIN_GROUP_DN;
    }

    public static void setIDM_ADMIN_GROUP_DN(String IDM_ADMIN_GROUP_DN) {
        DefaultAuthorizationService.IDM_ADMIN_GROUP_DN = IDM_ADMIN_GROUP_DN;
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

    private String getCloudAuthIdentityAdminRole() {
        return config.getString("cloudAuth.adminRole");
    }

    private String getCloudAuthServiceAdminRole() {
        return config.getString("cloudAuth.serviceAdminRole");
    }

    private String getCloudAuthUserAdminRole() {
        return config.getString("cloudAuth.userAdminRole");
    }

    private String getCloudAuthUserRole() {
        return config.getString("cloudAuth.userRole");
    }

    private String getIdmSuperAdminRole() {
        return config.getString("idm.superAdminRole");
    }
}
