package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.ForbiddenException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
public class DefaultAuthorizationService implements AuthorizationService {

    public static final String NOT_AUTHORIZED_MSG = "Not Authorized";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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

    private static ClientRole idmSuperAdminRole = null;
    private static ClientRole cloudServiceAdminRole = null;
    private static ClientRole cloudIdentityAdminRole = null;
    private static ClientRole cloudUserAdminRole = null;
    private static ClientRole cloudUserRole = null;
    private static ClientRole cloudUserManagedRole = null ;
    private static ClientRole rackerRole = null ;

    @PostConstruct
    public void retrieveAccessControlRoles() {
        idmSuperAdminRole = applicationService.getClientRoleByClientIdAndRoleName(getIdmClientId(), getIdmSuperAdminRoleName());
        cloudServiceAdminRole = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthServiceAdminRole());
        cloudIdentityAdminRole = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthIdentityAdminRole());
        cloudUserAdminRole = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserAdminRole());
        cloudUserRole = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserRole());
        cloudUserManagedRole = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudUserManagedRole());
        rackerRole = applicationService.getClientRoleByClientIdAndRoleName(getIdmClientId(), "Racker");
    }

	@Override
    public boolean authorizeCloudServiceAdmin(ScopeAccess scopeAccess) {
        return authorizeRoleAccess(scopeAccess, Arrays.asList(cloudServiceAdminRole));
    }

    public boolean authorizeRacker(ScopeAccess scopeAccess){
        logger.debug("Authorizing {} as a Racker", scopeAccess);
        if (!(scopeAccess instanceof RackerScopeAccess)){
            return false;
        }
        boolean authorized = authorize(scopeAccess, Arrays.asList(rackerRole));
        logger.debug("Authorized {} as Racker - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeCloudIdentityAdmin(ScopeAccess scopeAccess) {
        return authorizeRoleAccess(scopeAccess, Arrays.asList(cloudIdentityAdminRole));
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
        return authorizeRoleAccess(scopeAccess, Arrays.asList(cloudUserAdminRole));
    }

    @Override
    public boolean authorizeUserManageRole(ScopeAccess scopeAccess) {
        return authorizeRoleAccess(scopeAccess, Arrays.asList(cloudUserManagedRole));
    }

    @Override
    public boolean authorizeCloudUser(ScopeAccess scopeAccess) {
        return authorizeRoleAccess(scopeAccess, Arrays.asList(cloudUserRole));
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
    public boolean authorizeIdmSuperAdmin(ScopeAccess scopeAccess) {
        logger.debug("Authorizing {} as idm super admin", scopeAccess);

        if (this.authorizeCustomerIdm(scopeAccess)) {
            return true;
        }

        boolean authorized = authorize(scopeAccess, Arrays.asList(idmSuperAdminRole));

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
        verifyRoleAccess(authScopeAccess, Arrays.asList(cloudServiceAdminRole));
    }

    @Override
    public void verifyRackerOrIdentityAdminAccess(ScopeAccess authScopeAccess) {
        verifyRoleAccess(authScopeAccess, Arrays.asList(rackerRole, cloudIdentityAdminRole));
    }

    @Override
    public void verifyIdentityAdminLevelAccess(ScopeAccess authScopeAccess) {
        verifyRoleAccess(authScopeAccess, Arrays.asList(cloudServiceAdminRole, cloudIdentityAdminRole));
    }

    @Override
    public void verifyUserAdminLevelAccess(ScopeAccess authScopeAccess) {
        verifyRoleAccess(authScopeAccess, Arrays.asList(cloudServiceAdminRole, cloudIdentityAdminRole, cloudUserAdminRole));
    }

    @Override
    public void verifyUserManagedLevelAccess(ScopeAccess authScopeAccess) {
        verifyRoleAccess(authScopeAccess, Arrays.asList(cloudServiceAdminRole, cloudIdentityAdminRole, cloudUserAdminRole, cloudUserManagedRole));
    }

    @Override
    public void verifyUserLevelAccess(ScopeAccess authScopeAccess) {
        verifyRoleAccess(authScopeAccess, Arrays.asList(cloudServiceAdminRole, cloudIdentityAdminRole, cloudUserAdminRole, cloudUserRole));
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
        if (authorizeRoleAccess(authScopeAccess, Arrays.asList(cloudServiceAdminRole, cloudIdentityAdminRole))) {
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

    private boolean authorize(ScopeAccess scopeAccess, List<ClientRole> clientRoles) {
        if (scopeAccessService.isScopeAccessExpired(scopeAccess)) {
            return false;
        }

        BaseUser user = userService.getUserByScopeAccess(scopeAccess);
        List<TenantRole> tenantRoles = tenantService.getTenantRolesForUser(user);

        HashSet<String> clientRoleIds = new HashSet<String>();
        for (ClientRole role : clientRoles) {
            clientRoleIds.add(role.getId());
        }

        for (TenantRole tenantRole : tenantRoles) {
            if (clientRoleIds.contains(tenantRole.getRoleRsId())) {
                return true;
            }
        }

        return false;
    }

    private void verifyRoleAccess(ScopeAccess scopeAccess, List<ClientRole> clientRoles) {
        if (!authorizeRoleAccess(scopeAccess, clientRoles)) {
            String errMsg = NOT_AUTHORIZED_MSG;
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    private boolean authorizeRoleAccess(ScopeAccess scopeAccess, List<ClientRole> clientRoles) {
        String rolesString = getRoleString(clientRoles);

        logger.debug("Authorizing {} as {}", scopeAccess, rolesString);
        boolean authorized = authorize(scopeAccess, clientRoles);
        logger.debug(String.format("Authorized %s as %s - %s", scopeAccess, rolesString, authorized));
        return authorized;
    }

    private String getRoleString(List<ClientRole> clientRoles) {
        List<String> roles = new ArrayList<String>();
        for (ClientRole clientRole : clientRoles) {
            roles.add(clientRole.getName());
        }
        return StringUtils.join(roles, " ");
    }

    public void setScopeAccessService(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    public static ClientRole getCloudServiceAdminRole() {
        return cloudServiceAdminRole;
    }

    public static void setCloudServiceAdminRole(ClientRole cloudServiceAdminRole) {
        DefaultAuthorizationService.cloudServiceAdminRole = cloudServiceAdminRole;
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

    private String getCloudUserManagedRole() {
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
