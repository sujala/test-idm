package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotAuthorizedException;
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
    @Autowired
    private DomainService domainService;

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
    public boolean authorizeCloudServiceAdmin(AuthorizationContext context) {
        return authorizeRoleAccess(context, Arrays.asList(cloudServiceAdminRole));
    }

    @Override
    public AuthorizationContext getAuthorizationContext(ScopeAccess scopeAccess) {
        AuthorizationContext context = new AuthorizationContext();
        context.setScopeAccess(scopeAccess);
        context.setRoles(new HashSet<String>());

        if (scopeAccessService.isScopeAccessExpired(scopeAccess)) {
            return context;
        }

        Iterable<TenantRole> tenantRoles = new ArrayList<TenantRole>();

        if (scopeAccess instanceof FederatedToken) {
            //federated scope accesses has role / tenant information stored at the token level
            FederatedToken token = (FederatedToken)scopeAccess;
            tenantRoles = tenantService.getTenantRolesForFederatedTokenNoDetail(token);
        } else {
            BaseUser user = userService.getUserByScopeAccess(scopeAccess, false);
            context.setUser(user);
            tenantRoles = tenantService.getTenantRolesForUserNoDetail(user);
            if(user.getDomainId() != null){
                context.setDomain(domainService.getDomain(user.getDomainId()));
            }
        }

        for (TenantRole tenantRole : tenantRoles) {
            context.getRoles().add(tenantRole.getRoleRsId());
        }

        return context;
    }

    @Override
    public AuthorizationContext getAuthorizationContext(User user) {
        AuthorizationContext context = new AuthorizationContext();
        context.setRoles(new HashSet<String>());

        List<TenantRole> tenantRoles = tenantService.getTenantRolesForUser(user);

        for (TenantRole tenantRole : tenantRoles) {
            context.getRoles().add(tenantRole.getRoleRsId());
        }

        return context;
    }

    public boolean authorizeRacker(AuthorizationContext context){
        logger.debug("Authorizing {} as a Racker", context.getScopeAccess());
        if (!(context.getScopeAccess() instanceof RackerScopeAccess)){
            return false;
        }
        boolean authorized = authorize(context, Arrays.asList(rackerRole));
        logger.debug("Authorized {} as Racker - {}", context.getScopeAccess(), authorized);
        return authorized;
    }

    @Override
    public boolean authorizeCloudIdentityAdmin(AuthorizationContext context) {
        return authorizeRoleAccess(context, Arrays.asList(cloudIdentityAdminRole));
    }

    @Override
    public boolean authorizeIdmSuperAdminOrRackspaceClient(AuthorizationContext context) {
        boolean isRackspaceClient = authorizeRackspaceClient(context);
        boolean isIdmSuperAdmin = false;
        //verify if caller is a rackspace client, idm client or super admin
        if(!isRackspaceClient){
            isIdmSuperAdmin = authorizeIdmSuperAdmin(context);
        }

        if(!isRackspaceClient && ! isIdmSuperAdmin) {
            throw new ForbiddenException("Access denied");
        }
        return true;
    }

    @Override
    public boolean authorizeCloudUserAdmin(AuthorizationContext context) {
        return authorizeUserAccess(context) &&
               authorizeDomainAccess(context) &&
               authorizeRoleAccess(context, Arrays.asList(cloudUserAdminRole));
    }

    @Override
    public boolean authorizeUserManageRole(AuthorizationContext context) {
        return authorizeUserAccess(context) &&
               authorizeDomainAccess(context) &&
               authorizeRoleAccess(context, Arrays.asList(cloudUserManagedRole));
    }

    @Override
    public boolean authorizeCloudUser(AuthorizationContext context) {
        return authorizeUserAccess(context) &&
               authorizeDomainAccess(context) &&
               authorizeRoleAccess(context, Arrays.asList(cloudUserRole));
    }

    @Override
    public boolean hasDefaultUserRole(AuthorizationContext context) {
        if (context == null) {
            return false;
        }
        return containsRole(context, Arrays.asList(cloudUserRole));
    }

    @Override
    public boolean hasUserAdminRole(AuthorizationContext context) {
        if (context == null) {
            return false;
        }
        return containsRole(context, Arrays.asList(cloudUserAdminRole));
    }

    @Override
    public boolean hasUserManageRole(AuthorizationContext context) {
        if (context == null) {
            return false;
        }
        return containsRole(context, Arrays.asList(cloudUserManagedRole));
    }

    @Override
    public boolean hasIdentityAdminRole(AuthorizationContext context) {
        if (context == null) {
            return false;
        }
        return containsRole(context, Arrays.asList(cloudIdentityAdminRole));
    }

    @Override
    public boolean hasServiceAdminRole(AuthorizationContext context) {
        if (context == null) {
            return false;
        }
        return containsRole(context, Arrays.asList(cloudServiceAdminRole));
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
    public boolean authorizeIdmSuperAdmin(AuthorizationContext context) {
        ScopeAccess scopeAccess = context.getScopeAccess();
        logger.debug("Authorizing {} as idm super admin", scopeAccess);

        if (this.authorizeCustomerIdm(context)) {
            return true;
        }

        boolean authorized = authorize(context, Arrays.asList(idmSuperAdminRole));

        logger.debug("Authorized {} as idm super admin - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeRackspaceClient(AuthorizationContext context) {
        ScopeAccess scopeAccess = context.getScopeAccess();
        logger.debug("Authorizing {} as rackspace client", scopeAccess);
        if (!(scopeAccess instanceof ClientScopeAccess)) {
            return false;
        }
        boolean authorized = scopeAccess.getClientRCN().equalsIgnoreCase(this.getRackspaceCustomerId());
        logger.debug("Authorized {} as rackspace client - {}", scopeAccess, authorized);
        return authorized;
    }

    @Override
    public boolean authorizeCustomerIdm(AuthorizationContext context) {
        ScopeAccess scopeAccess = context.getScopeAccess();
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
        if(!this.authorizeIdmSuperAdmin(getAuthorizationContext(scopeAccessService.getScopeAccessByAccessToken(authHeader)))){
            throw new ForbiddenException("Access denied");
        }
    }

    @Override
    public void verifyServiceAdminLevelAccess(AuthorizationContext context) {
        verifyUserAccess(context);
        verifyRoleAccess(context, Arrays.asList(cloudServiceAdminRole));
    }

    @Override
    public void verifyRackerOrIdentityAdminAccess(AuthorizationContext context) {
        verifyUserAccess(context);
        verifyRoleAccess(context, Arrays.asList(rackerRole, cloudIdentityAdminRole));
    }

    @Override
    public void verifyIdentityAdminLevelAccess(AuthorizationContext context) {
        verifyUserAccess(context);
        verifyRoleAccess(context, Arrays.asList(cloudServiceAdminRole, cloudIdentityAdminRole));
    }

    @Override
    public void verifyUserAdminLevelAccess(AuthorizationContext context) {
        verifyUserAccess(context);
        verifyDomainAccess(context);
        verifyRoleAccess(context, Arrays.asList(cloudServiceAdminRole, cloudIdentityAdminRole, cloudUserAdminRole));
    }

    @Override
    public void verifyUserManagedLevelAccess(AuthorizationContext context) {
        verifyUserAccess(context);
        verifyDomainAccess(context);
        verifyRoleAccess(context, Arrays.asList(cloudServiceAdminRole, cloudIdentityAdminRole, cloudUserAdminRole, cloudUserManagedRole));
    }

    @Override
    public void verifyUserLevelAccess(AuthorizationContext context) {
        verifyUserAccess(context);
        verifyDomainAccess(context);
        verifyRoleAccess(context, Arrays.asList(cloudServiceAdminRole, cloudIdentityAdminRole, cloudUserAdminRole, cloudUserRole));
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
        if (authorizeRoleAccess(getAuthorizationContext(authScopeAccess), Arrays.asList(cloudServiceAdminRole, cloudIdentityAdminRole))) {
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
    public void checkAuthAndHandleFailure(boolean authorized, AuthorizationContext context) {
        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call",
                    context.getScopeAccess().getAccessTokenString());
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    private boolean authorize(AuthorizationContext context, List<ClientRole> clientRoles) {
        if (scopeAccessService.isScopeAccessExpired(context.getScopeAccess())) {
            return false;
        }

        return containsRole(context, clientRoles);
    }

    private boolean containsRole(AuthorizationContext context, List<ClientRole> clientRoles) {
        HashSet<String> clientRoleIds = new HashSet<String>();
        for (ClientRole role : clientRoles) {
            clientRoleIds.add(role.getId());
        }

        for (String tenantRoleId : context.getRoles()) {
            if (clientRoleIds.contains(tenantRoleId)) {
                return true;
            }
        }

        return false;
    }

    private void verifyRoleAccess(AuthorizationContext context, List<ClientRole> clientRoles) {
        if (!authorizeRoleAccess(context, clientRoles)) {
            String errMsg = NOT_AUTHORIZED_MSG;
            logger.warn(errMsg);
            throw new ForbiddenException(errMsg);
        }
    }

    private boolean authorizeRoleAccess(AuthorizationContext context, List<ClientRole> clientRoles) {
        String rolesString = getRoleString(clientRoles);

        logger.debug("Authorizing {} as {}", context.getScopeAccess(), rolesString);
        boolean authorized = authorize(context, clientRoles);
        logger.debug(String.format("Authorized %s as %s - %s", context.getScopeAccess(), rolesString, authorized));
        return authorized;
    }

    private void verifyDomainAccess(AuthorizationContext context) {
        Domain domain = context.getDomain();
        if(domain != null && !domain.getEnabled()) {
            String errMsg = NOT_AUTHORIZED_MSG;
            logger.warn(errMsg);
            throw new NotAuthorizedException(errMsg);
        }
    }

    private boolean authorizeDomainAccess(AuthorizationContext context) {
        Domain domain = context.getDomain();
        return domain == null || domain.getEnabled();

    }

    private void verifyUserAccess(AuthorizationContext context) {
        BaseUser user = context.getUser();
        if( user != null && user.isDisabled() ) {
            String errMsg = NOT_AUTHORIZED_MSG;
            logger.warn(errMsg);
            throw new NotAuthorizedException(errMsg);
        }
    }

    private boolean authorizeUserAccess(AuthorizationContext context) {
        BaseUser user = context.getUser();
        return user == null || !user.isDisabled();

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
