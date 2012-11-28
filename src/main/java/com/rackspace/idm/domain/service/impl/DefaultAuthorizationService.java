package com.rackspace.idm.domain.service.impl;

import org.springframework.stereotype.Component;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultAuthorizationService implements AuthorizationService {

    public static final String NOT_AUTHORIZED_MSG = "Not authorized.";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private ApplicationDao clientDao;
    @Autowired
    private Configuration config;
    @Autowired
    private ScopeAccessDao scopeAccessDao;
    @Autowired
    private TenantDao tenantDao;
    @Autowired
    private ScopeAccessService scopeAccessService;
    @Autowired
    private TenantService tenantService;

    private static String idmAdminGroupDn = null;
    private static ClientRole cloudAdminRole = null;
    private static ClientRole cloudIdentityAdminRole = null;
    private static ClientRole cloudServiceAdminRole = null;
    private static ClientRole cloudUserRole = null;
    private static ClientRole cloudUserAdminRole = null;
    private static ClientRole idmSuperAdminRole = null;
    private static ClientRole rackerRole = null ;
    
    @Override
	public void authorize(String token, Entity object, String... authorizedRoles) {

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
    public boolean authorizeCloudServiceAdmin(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as cloud admin", scopeAccess);

        if (scopeAccess == null || ((HasAccessToken) scopeAccess).isAccessTokenExpired(new DateTime())) {
            return false;
        }

        if (cloudAdminRole == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthServiceAdminRole());
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
    public boolean authorizeCloudIdentityAdmin(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as cloud defaultApplication admin", scopeAccess);

        if (scopeAccess == null || ((HasAccessToken) scopeAccess).isAccessTokenExpired(new DateTime())) {
            return false;
        }

        if (cloudIdentityAdminRole == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthIdentityAdminRole());
            cloudIdentityAdminRole = role;
        }

        boolean authorized = this.tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, cloudIdentityAdminRole);

        logger.debug("Authorized {} as cloud defaultApplication admin - {}", scopeAccess, authorized);
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

    //This method does not check if the scope access has an access token.
    //This method checks if the scope access has the cloud default user role.
    @Override
    public boolean hasServiceAdminRole(ScopeAccess scopeAccess) {
        if (scopeAccess == null) {
            return false;
        }
        if (cloudServiceAdminRole == null) {
            ClientRole role = clientDao.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthServiceAdminRole());
            cloudServiceAdminRole = role;
        }
        return tenantDao.doesScopeAccessHaveTenantRole(scopeAccess, cloudServiceAdminRole);
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
    public void verifyServiceAdminLevelAccess(ScopeAccess authScopeAccess) {
        if (!authorizeCloudServiceAdmin(authScopeAccess)) {
            String errMsg = NOT_AUTHORIZED_MSG;
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    @Override
    public void verifyRackerOrIdentityAdminAccess(ScopeAccess authScopeAccess) {
        if (!authorizeRacker(authScopeAccess) && !authorizeCloudIdentityAdmin(authScopeAccess)) {
            String errMsg = NOT_AUTHORIZED_MSG;
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    @Override
    public void verifyIdentityAdminLevelAccess(ScopeAccess authScopeAccess) {
        if (!authorizeCloudServiceAdmin(authScopeAccess) && !authorizeCloudIdentityAdmin(authScopeAccess)) {
            String errMsg = NOT_AUTHORIZED_MSG;
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    @Override
    public void verifyUserAdminLevelAccess(ScopeAccess authScopeAccess) {
        if (!authorizeCloudServiceAdmin(authScopeAccess) && !authorizeCloudIdentityAdmin(authScopeAccess)
                && !authorizeCloudUserAdmin(authScopeAccess)) {
            String errMsg = NOT_AUTHORIZED_MSG;
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    @Override
    public void verifyUserLevelAccess(ScopeAccess authScopeAccess) {
        if (!authorizeCloudServiceAdmin(authScopeAccess) && !authorizeCloudIdentityAdmin(authScopeAccess)
                && !authorizeCloudUserAdmin(authScopeAccess) && !authorizeCloudUser(authScopeAccess)) {

            String errMsg = NOT_AUTHORIZED_MSG;
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    @Override
    public void verifySelf(User requester, User requestedUser) {
        if (!(requester.getUsername().equals(requestedUser.getUsername()) && (requester.getUniqueId().equals(requestedUser.getUniqueId())))) {
            String errMsg = NOT_AUTHORIZED_MSG;
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    @Override
    public void verifyTokenHasTenantAccess(String tenantId, ScopeAccess authScopeAccess) {
        if (authorizeCloudServiceAdmin(authScopeAccess) || authorizeCloudIdentityAdmin(authScopeAccess)) {
            return;
        }

        List<Tenant> adminTenants = tenantService.getTenantsForScopeAccessByTenantRoles(authScopeAccess);

        for (Tenant tenant : adminTenants) {
            if (tenant.getTenantId().equals(tenantId)) {
                return;
            }
        }

        String errMsg = NOT_AUTHORIZED_MSG;
        logger.warn(errMsg);
        throw new ForbiddenException(errMsg);
    }

    @Override
    public void verifyDomain(User caller, User retrievedUser) {
        if (!caller.getId().equals(retrievedUser.getId())) {
            if (caller.getDomainId() == null || !caller.getDomainId().equals(retrievedUser.getDomainId())) {
                throw new ForbiddenException(NOT_AUTHORIZED_MSG);
            }
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

    public static void setCloudAdminRole(ClientRole cloudAdminRole) {
        DefaultAuthorizationService.cloudAdminRole = cloudAdminRole;
    }

    public static ClientRole getRackerRole() {
        return rackerRole;
    }

    public static void setRackerRole(ClientRole rackerRole) {
        DefaultAuthorizationService.rackerRole = rackerRole;
    }

    public static ClientRole getCloudIdentityAdminRole() {
        return cloudIdentityAdminRole;
    }

    public static void setCloudIdentityAdminRole(ClientRole cloudIdentityAdminRole) {
        DefaultAuthorizationService.cloudIdentityAdminRole = cloudIdentityAdminRole;
    }

    public static ClientRole getCloudUserAdminRole() {
        return cloudUserAdminRole;
    }

    public static void setCloudUserAdminRole(ClientRole cloudUserAdminRole) {
        DefaultAuthorizationService.cloudUserAdminRole = cloudUserAdminRole;
    }

    public static ClientRole getCloudUserRole() {
        return cloudUserRole;
    }

    public static void setCloudUserRole(ClientRole cloudUserRole) {
        DefaultAuthorizationService.cloudUserRole = cloudUserRole;
    }

    public static ClientRole getIdmSuperAdminRole() {
        return idmSuperAdminRole;
    }

    public static void setIdmSuperAdminRole(ClientRole idmSuperAdminRole) {
        DefaultAuthorizationService.idmSuperAdminRole = idmSuperAdminRole;
    }

    public static String getIdmAdminGroupDn() {
        return idmAdminGroupDn;
    }

    public static void setIdmAdminGroupDn(String idmAdminGroupDn) {
        DefaultAuthorizationService.idmAdminGroupDn = idmAdminGroupDn;
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
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

    private String getCloudAuthServiceAdminRole() {
        return config.getString("cloudAuth.serviceAdminRole");
    }

    private String getCloudAuthIdentityAdminRole() {
        return config.getString("cloudAuth.adminRole");
    }

    private String getCloudAuthUserAdminRole() {
        return config.getString("cloudAuth.userAdminRole");
    }

    private String getCloudAuthUserRole() {
        return config.getString("cloudAuth.userRole");
    }

	public void setScopeAccessDao(ScopeAccessDao accessDao) {
		this.scopeAccessDao = accessDao;
	}

	public void setApplicationDao(ApplicationDao applicationDao) {
		this.clientDao = applicationDao;
	}

	public void setTenantDao(TenantDao tenantDao) {
		this.tenantDao = tenantDao;
	}

	public void setConfig(Configuration config) {
		this.config = config;
	}
}
