package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class DefaultAuthorizationService implements AuthorizationService {

    private final ApplicationDao clientDao;
    private final Configuration config;
    private final ScopeAccessDao scopeAccessDao;
    private final TenantDao tenantDao;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());
    @Autowired
    private ScopeAccessService scopeAccessService;

    private static String idmAdminGroupDn = null;
    private static ClientRole cloudAdminRole = null;
    private static ClientRole cloudServiceAdminRole = null;
    private static ClientRole cloudUserRole = null;
    private static ClientRole cloudUserAdminRole = null;
    private static ClientRole idmSuperAdminRole = null;
    private static ClientRole rackerRole = null ;

    public DefaultAuthorizationService(ScopeAccessDao scopeAccessDao,
        ApplicationDao clientDao, TenantDao tenantDao,
        Configuration config) {
        this.scopeAccessDao = scopeAccessDao;
        this.clientDao = clientDao;
        this.tenantDao = tenantDao;
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

        if (cloudAdminRole == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthIdentityAdminRole());
            cloudAdminRole = role;
        }

        boolean authorized = this.tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, cloudAdminRole);

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

        if (rackerRole == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(config.getString("idm.clientId"), "Racker");
            rackerRole = role;
        }

        boolean authorized = this.tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, rackerRole);

        logger.debug("Authorized {} as Racker - {}", scopeAccess, authorized);
        return authorized;
    }


    @Override
    public boolean authorizeCloudServiceAdmin(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as cloud defaultApplicationService admin", scopeAccess);

        if (scopeAccess == null || ((HasAccessToken) scopeAccess).isAccessTokenExpired(new DateTime())) {
            return false;
        }

        if (cloudServiceAdminRole == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthServiceAdminRole());
            cloudServiceAdminRole = role;
        }

        boolean authorized = this.tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, cloudServiceAdminRole);

        logger.debug("Authorized {} as cloud defaultApplicationService admin - {}", scopeAccess, authorized);
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

        if (cloudUserAdminRole == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserAdminRole());
            cloudUserAdminRole = role;
        }

        boolean authorized = this.tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, cloudUserAdminRole);

        logger.debug("Authorized {} as cloud user admin - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeCloudUser(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as cloud user ", scopeAccess);

        if (scopeAccess == null || ((HasAccessToken) scopeAccess).isAccessTokenExpired(new DateTime())) {
            return false;
        }

        if (cloudUserRole == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserRole());
            cloudUserRole = role;
        }

        boolean authorized = tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, cloudUserRole);

        logger.debug("Authorized {} as cloud user - {}", scopeAccess, authorized);
        return authorized;
    }


    //This method does not check if the scope access has an access token.
    //This method checks if the scope access has the cloud default user role.
    @Override
    public boolean hasDefaultUserRole(ScopeAccess scopeAccess) {
        if (scopeAccess == null) {
            return false;
        }
        if (cloudUserRole == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserRole());
            cloudUserRole = role;
        }
        return tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, cloudUserRole);
    }

    //This method does not check if the scope access has an access token.
    //This method checks if the scope access has the cloud default user role.
    @Override
    public boolean hasUserAdminRole(ScopeAccess scopeAccess) {
        if (scopeAccess == null) {
            return false;
        }
        if (cloudUserAdminRole == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserAdminRole());
            cloudUserAdminRole = role;
        }
        return tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, cloudUserAdminRole);
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

        if (idmSuperAdminRole == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(getIdmClientId(), config.getString("idm.superAdminRole"));
            idmSuperAdminRole = role;
        }

        boolean authorized = this.tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, idmSuperAdminRole);

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

        if (idmAdminGroupDn == null) {
            ClientGroup group = clientDao.getClientGroup(getRackspaceCustomerId(), getIdmClientId(), getIdmAdminGroupName());
            idmAdminGroupDn = group.getUniqueId();
        }

        boolean authorized = false;
        authorized = clientDao.isUserInClientGroup(username, idmAdminGroupDn) && customerId.equalsIgnoreCase(RCN);
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

    public void verifyServiceAdminLevelAccess(ScopeAccess authScopeAccess) {
        if (!authorizeCloudIdentityAdmin(authScopeAccess) && !authorizeCloudServiceAdmin(authScopeAccess)) {
            String errMsg = "Not authorized.";
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
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

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public static ClientRole getCloudAdminRole() {
        return cloudAdminRole;
    }

    public static void setCloudAdminRole(ClientRole CLOUD_ADMIN_ROLE) {
        DefaultAuthorizationService.cloudAdminRole = CLOUD_ADMIN_ROLE;
    }

    public static ClientRole getRackerRole() {
        return rackerRole;
    }

    public static void setRackerRole(ClientRole RACKER_ROLE) {
        DefaultAuthorizationService.rackerRole = RACKER_ROLE;
    }

    public static ClientRole getCloudServiceAdminRole() {
        return cloudServiceAdminRole;
    }

    public static void setCloudServiceAdminRole(ClientRole CLOUD_SERVICE_ADMIN_ROLE) {
        DefaultAuthorizationService.cloudServiceAdminRole = CLOUD_SERVICE_ADMIN_ROLE;
    }

    public static ClientRole getCloudUserAdminRole() {
        return cloudUserAdminRole;
    }

    public static void setCloudUserAdminRole(ClientRole CLOUD_USER_ADMIN_ROLE) {
        DefaultAuthorizationService.cloudUserAdminRole = CLOUD_USER_ADMIN_ROLE;
    }

    public static ClientRole getCloudUserRole() {
        return cloudUserRole;
    }

    public static void setCloudUserRole(ClientRole CLOUD_USER_ROLE) {
        DefaultAuthorizationService.cloudUserRole = CLOUD_USER_ROLE;
    }

    public static ClientRole getIdmSuperAdminRole() {
        return idmSuperAdminRole;
    }

    public static void setIdmSuperAdminRole(ClientRole IDM_SUPER_ADMIN_ROLE) {
        DefaultAuthorizationService.idmSuperAdminRole = IDM_SUPER_ADMIN_ROLE;
    }

    public static String getIdmAdminGroupDn() {
        return idmAdminGroupDn;
    }

    public static void setIdmAdminGroupDn(String IDM_ADMIN_GROUP_DN) {
        DefaultAuthorizationService.idmAdminGroupDn = IDM_ADMIN_GROUP_DN;
    }

    private String getIdmAdminGroupName() {
        return config.getString("idm.AdminGroupName");
    }

    String getIdmClientId() {
        return config.getString("idm.clientId");
    }

    String getRackspaceCustomerId() {
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
}
