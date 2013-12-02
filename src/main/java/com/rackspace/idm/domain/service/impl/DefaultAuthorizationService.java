package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class DefaultAuthorizationService implements AuthorizationService {
    public static final String NOT_AUTHORIZED_MSG = "Not Authorized";
    private static final Logger logger = LoggerFactory.getLogger(DefaultAuthorizationService.class);

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private Configuration config;
    @Autowired
    private ScopeAccessService scopeAccessService;
    @Autowired
    private TenantService tenantService;
    @Autowired
    private UserService userService;

    private ClientRole idmSuperAdminRole = null;
    private ClientRole cloudServiceAdminRole = null;
    private ClientRole cloudIdentityAdminRole = null;
    private ClientRole cloudUserAdminRole = null;
    private ClientRole cloudUserRole = null;
    private ClientRole cloudUserManagedRole = null ;
    private ClientRole rackerRole = null ;

    @PostConstruct
    public void retrieveAccessControlRoles() {
        idmSuperAdminRole = applicationService.getClientRoleByClientIdAndRoleName(getIdmClientId(), getIdmSuperAdminRoleName());
        cloudServiceAdminRole = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthServiceAdminRole());
        cloudIdentityAdminRole = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthIdentityAdminRole());
        cloudUserAdminRole = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserAdminRole());
        cloudUserRole = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserRole());
        cloudUserManagedRole = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserManagedRole());
        rackerRole = applicationService.getClientRoleByClientIdAndRoleName(getIdmClientId(), "Racker");
    }

	@Override
    public boolean authorizeCloudServiceAdmin(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as cloud admin", scopeAccess);
        boolean authorized = authorize(scopeAccess, cloudServiceAdminRole);
        logger.debug("Authorized {} as cloud admin - {}", scopeAccess, authorized);
        return authorized;
    }

    public boolean authorizeRacker(ScopeAccess scopeAccess){
        logger.debug("Authorizing {} as a Racker", scopeAccess);
        if (!(scopeAccess instanceof RackerScopeAccess)){
            return false;
        }
        boolean authorized = authorize(scopeAccess, rackerRole);
        logger.debug("Authorized {} as Racker - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeCloudIdentityAdmin(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as cloud defaultApplication admin", scopeAccess);
        boolean authorized = authorize(scopeAccess, cloudIdentityAdminRole);
        logger.debug("Authorized {} as cloud defaultApplication admin - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeIdmSuperAdminOrRackspaceClient(ScopeAccess scopeAccess) {
        boolean isRackspaceClient = authorizeRackspaceClient(scopeAccess);
        boolean isIdmSuperAdmin = false;
        //verify if caller is a rackspace client, idm client or super admin
        if(!isRackspaceClient){
            isIdmSuperAdmin = authorizeIdmSuperAdmin(scopeAccess);
        }

        if(!isRackspaceClient && ! isIdmSuperAdmin) {
            throw new ForbiddenException("Access denied");
        }
        return true;
    }
 
    @Override
    public boolean authorizeCloudUserAdmin(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as cloud user admin", scopeAccess);
        boolean authorized = authorize(scopeAccess, cloudUserAdminRole);
        logger.debug("Authorized {} as cloud user admin - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeUserManageRole(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as cloud managed role", scopeAccess);
        boolean authorized = authorize(scopeAccess, cloudUserManagedRole);
        logger.debug("Authorized {} as cloud user managed role - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeCloudUser(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as cloud user ", scopeAccess);
        boolean authorized = authorize(scopeAccess, cloudUserRole);
        logger.debug("Authorized {} as cloud user - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean hasDefaultUserRole(User user) {
        if (user == null) {
            return false;
        }
        return tenantService.doesUserContainTenantRole(user, cloudUserRole.getId());
    }

    @Override
    public boolean hasUserAdminRole(User user) {
        if (user == null) {
            return false;
        }
        return tenantService.doesUserContainTenantRole(user, cloudUserAdminRole.getId());
    }

    @Override
    public boolean hasUserManageRole(User user) {
        if (user == null) {
            return false;
        }
        return tenantService.doesUserContainTenantRole(user, cloudUserManagedRole.getId());
    }

    @Override
    public boolean hasIdentityAdminRole(User user) {
        if (user == null) {
            return false;
        }
        return tenantService.doesUserContainTenantRole(user, cloudIdentityAdminRole.getId());
    }

    @Override
    public boolean hasServiceAdminRole(User user) {
        if (user == null) {
            return false;
        }
        return tenantService.doesUserContainTenantRole(user, cloudServiceAdminRole.getId());
    }

    @Override
    public boolean hasSameDomain(User caller, User retrievedUser) {
        return caller.getDomainId() != null && caller.getDomainId().equals(retrievedUser.getDomainId());
    }

    @Override
    public boolean isSelf(User requester, User requestedUser) {
        if(requester.getUsername() != null && requester.getUsername().equals(requestedUser.getUsername())){
            return true;
        }

        if(requester.getUniqueId() != null && requester.getUniqueId().equals(requestedUser.getUniqueId())){
            return true;
        }

        return false;
    }

    private boolean hasNullvalues(String... values){
        for(String value : values){
            if(value == null){
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean authorizeIdmSuperAdmin(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as idm super admin", scopeAccess);

        if (this.authorizeCustomerIdm(scopeAccess)) {
            return true;
        }

        boolean authorized = authorize(scopeAccess, idmSuperAdminRole);

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
    public void verifyUserManagedLevelAccess(ScopeAccess authScopeAccess) {
        if (!authorizeCloudServiceAdmin(authScopeAccess) && !authorizeCloudIdentityAdmin(authScopeAccess)
                && !authorizeCloudUserAdmin(authScopeAccess) && !authorizeUserManageRole(authScopeAccess)) {
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
                    token.getAccessTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    private boolean authorize(ScopeAccess scopeAccess, ClientRole clientRole) {
        if (scopeAccessService.isScopeAccessExpired(scopeAccess)) {
            return false;
        }

        if (scopeAccess instanceof FederatedToken) {
            //federated scope accesses has role / tenant information stored at the token level
            FederatedToken token = (FederatedToken)scopeAccess;
            return tenantService.doesFederatedTokenContainTenantRole(token, clientRole.getId());
        }

        BaseUser user = userService.getUserByScopeAccess(scopeAccess);

        return tenantService.doesUserContainTenantRole(user, clientRole.getId());
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public ClientRole getCloudServiceAdminRole() {
        return cloudServiceAdminRole;
    }

    public void setCloudServiceAdminRole(ClientRole cloudServiceAdminRole) {
        this.cloudServiceAdminRole = cloudServiceAdminRole;
    }

    public ClientRole getRackerRole() {
        return rackerRole;
    }

    public  void setRackerRole(ClientRole rackerRole) {
        this.rackerRole = rackerRole;
    }

    public  ClientRole getCloudIdentityAdminRole() {
        return cloudIdentityAdminRole;
    }

    public  void setCloudIdentityAdminRole(ClientRole cloudIdentityAdminRole) {
        this.cloudIdentityAdminRole = cloudIdentityAdminRole;
    }

    public  ClientRole getCloudUserAdminRole() {
        return cloudUserAdminRole;
    }

    public  void setCloudUserAdminRole(ClientRole cloudUserAdminRole) {
        this.cloudUserAdminRole = cloudUserAdminRole;
    }

    public  ClientRole getCloudUserRole() {
        return cloudUserRole;
    }

    public  void setCloudUserRole(ClientRole cloudUserRole) {
        this.cloudUserRole = cloudUserRole;
    }

    public  ClientRole getIdmSuperAdminRole() {
        return idmSuperAdminRole;
    }

    public  void setIdmSuperAdminRole(ClientRole idmSuperAdminRole) {
        this.idmSuperAdminRole = idmSuperAdminRole;
    }

    public ClientRole getCloudUserManagedRole() {
        return cloudUserManagedRole;
    }

    public void setCloudUserManagedRole(ClientRole cloudUserManagedRole) {
        this.cloudUserManagedRole = cloudUserManagedRole;
    }

    public void setTenantService(TenantService tenantService) {
        this.tenantService = tenantService;
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

    private String getIdmSuperAdminRoleName() {
        return config.getString("idm.superAdminRole");
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

    private String getCloudAuthUserManagedRole() {
        return config.getString("cloudAuth.userManagedRole");
    }

	public void setConfig(Configuration config) {
		this.config = config;
	}

    public void setApplicationService(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    public void setUserService(UserService userService) {
        this.userService = userService;
    }
}
