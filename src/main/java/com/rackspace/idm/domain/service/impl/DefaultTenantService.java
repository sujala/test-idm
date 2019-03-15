package com.rackspace.idm.domain.service.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Types;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.FeedsUserStatusEnum;
import com.rackspace.idm.api.resource.cloud.v20.PaginationParams;
import com.rackspace.idm.api.security.IdentityRole;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.api.security.ImmutableTenantRole;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.domain.service.rolecalculator.UserRoleLookupService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ClientConflictException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.modules.usergroups.api.resource.UserGroupAuthorizationService;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import com.rackspace.idm.modules.usergroups.service.UserGroupService;
import com.rackspace.idm.util.RoleUtil;
import com.rackspace.idm.validation.PrecedenceValidator;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.*;
import java.util.stream.Collectors;

import static com.rackspace.idm.GlobalConstants.IAM_IMPLICIT_ROLE_PREFIX;
import static com.rackspace.idm.GlobalConstants.MANAGED_HOSTING_TENANT_PREFIX;

@Component
public class DefaultTenantService implements TenantService {

    public static final String GETTING_TENANT_ROLES = "Getting Tenant Roles";
    public static final String GOT_TENANT_ROLES = "Got {} Tenant Roles";

    public static final int TENANT_TYPE_SEARCH_LIMIT = 999;

    @Autowired
    private Configuration config;

    @Autowired
    private DomainService domainService;

    @Autowired
    UserGroupService userGroupService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private TenantDao tenantDao;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private UserService userService;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private TenantRoleDao tenantRoleDao;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private IdentityConfig identityConfig;

    @Lazy
    @Autowired
    private AtomHopperClient atomHopperClient;

    @Autowired
    private TenantTypeService tenantTypeService;

    @Autowired
    private UserGroupAuthorizationService userGroupAuthorizationService;

    @Autowired
    private FederatedUserDao federatedUserDao;

    @Autowired
    private DelegationService delegationService;

    @Autowired
    private TenantTypeWhitelistFilter tenantTypeWhitelistFilter;

    // Not using component w/ Autowiring because want just a dumb a utility class
    private RoleUtil roleUtil = new RoleUtil();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void addTenant(Tenant tenant) {
        logger.info("Adding Tenant {}", tenant);
        if(tenant == null){
            throw new IllegalArgumentException("Tenant cannot be null");
        }
        Tenant exists = this.tenantDao.getTenant(tenant.getName());
        if (exists != null) {
            String errMsg = String.format("Tenant with name %s already exists", tenant.getName());
            logger.warn(errMsg);
            throw new DuplicateException(errMsg);
        }
        this.tenantDao.addTenant(tenant);
        logger.info("Added Tenant {}", tenant);
    }

    @Override
    public void deleteTenant(Tenant tenant) {
        logger.info("Deleting Tenant {}", tenant.getTenantId());

        String defaultTenant =  identityConfig.getReloadableConfig().getIdentityRoleDefaultTenant();
        if (tenant.getTenantId().equalsIgnoreCase(defaultTenant)) {
            String msg = String.format("Deleting '%s' tenant is not allowed.", defaultTenant);
            throw new BadRequestException(msg);
        }

        // Delete all tenant roles for this tenant
        for (TenantRole role : this.tenantRoleDao.getAllTenantRolesForTenant(tenant.getTenantId())) {
            if (role.getTenantIds().size() == 1) {
                this.tenantRoleDao.deleteTenantRole(role);
            } else {
                role.getTenantIds().remove(tenant.getTenantId());
                this.tenantRoleDao.updateTenantRole(role);
            }
        }

        // Delete the tenantId off of the associated domain
        if(!StringUtils.isBlank(tenant.getDomainId())) {
            Domain domain = domainService.getDomain(tenant.getDomainId());
            if(domain != null) {
                List<String> tenantIds = new ArrayList<String>(Arrays.asList(getTenantIdsForDomain(domain)));
                tenantIds.remove(tenant.getTenantId());
                domain.setTenantIds(tenantIds.toArray(new String[0]));
                domainService.updateDomain(domain);
            }
        }

        this.tenantDao.deleteTenant(tenant.getTenantId());

        logger.info("Added Tenant {}", tenant.getTenantId());
    }

    @Override
    public Tenant getTenant(String tenantId) {
        logger.info("Getting Tenant {}", tenantId);
        Tenant tenant = this.tenantDao.getTenant(tenantId);
        logger.info("Got Tenant {}", tenant);
        return tenant;
    }

    @Override
    public Tenant checkAndGetTenant(String tenantId) {
        Tenant tenant = getTenant(tenantId);

        if (tenant == null) {
            String errMsg = String.format("Tenant with id/name: '%s' was not found.", tenantId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return tenant;
    }

    @Override
    public Tenant getTenantByName(String name) {
        logger.info("Getting Tenant {}", name);
        Tenant tenant = this.tenantDao.getTenantByName(name);
        logger.info("Got Tenant {}", tenant);
        return tenant;
    }

    @Override
    public Iterable<Tenant> getTenants() {
        logger.info("Getting Tenants");
        return this.tenantDao.getTenants();
    }

    @Override
    public void updateTenant(Tenant tenant) {
        logger.info("Updating Tenant {}", tenant);
        String defaultTenant =  identityConfig.getReloadableConfig().getIdentityRoleDefaultTenant();
        if (tenant.getTenantId().equalsIgnoreCase(defaultTenant)) {
            String msg = String.format("Updating '%s' tenant is not allowed.", defaultTenant);
            throw new BadRequestException(msg);
        }
        this.tenantDao.updateTenant(tenant);
        logger.info("Updated Tenant {}", tenant);
    }

    @Override
    public TenantRole getTenantRoleForUserById(BaseUser user, String roleId) {
        return tenantRoleDao.getTenantRoleForUser(user, roleId);
    }

    @Override
    public boolean doesUserContainTenantRole(BaseUser user, String roleId) {
        if (user instanceof Racker) {
            //rackers are assumed to have the "RACKER" role
            return identityConfig.getStaticConfig().getRackerRoleId().equals(roleId);

        } else if (user instanceof ProvisionedUserDelegate) {
            ProvisionedUserDelegate provisionedUserDelegate = (ProvisionedUserDelegate)user;
            SourcedRoleAssignments sourcedRoleAssignments = getSourcedRoleAssignmentsForUser(provisionedUserDelegate);
            List<TenantRole> tenantRoles = sourcedRoleAssignments.asTenantRoles();

            for (TenantRole currentTenantRole : tenantRoles) {
                if (currentTenantRole.getRoleRsId().equals(roleId)) {
                    return true;
                }
            }
        } else if (user instanceof EndUser){
            EndUser eu = (EndUser) user;
            TenantRole tenantRole = tenantRoleDao.getTenantRoleForUser(eu, roleId);
            return tenantRole != null;
        }
        return false;
    }

    @Override
    public TenantRole checkAndGetTenantRoleForUserById(EndUser user, String roleId) {
        TenantRole tenantRole = getTenantRoleForUserById(user, roleId);
        if(tenantRole == null) {
            String errMsg = String.format("Tenant Role %s not found for user %s", roleId, user.getId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return tenantRole;
    }

    @Override
    public List<Tenant> getTenantsForUserByTenantRoles(EndUser user) {
        if (user == null) {
            throw new IllegalStateException();
        }

        logger.info("Getting Tenants for Parent");

        SourcedRoleAssignments sourcedRoleAssignments = getSourcedRoleAssignmentsForUser(user);
        SourcedRoleAssignmentsLegacyAdapter legacyAdapter = new SourcedRoleAssignmentsLegacyAdapter(sourcedRoleAssignments);
        List<TenantRole> tenantRoles = legacyAdapter.getStandardTenantRoles();

        List<Tenant> tenants = getTenants(tenantRoles, new TenantEnabledPredicate());

        logger.info("Got {} tenants", tenants.size());
        return tenants;
    }

    @Override
    public List<Tenant> getTenantsForUserByTenantRolesApplyRcnRoles(EndUser user) {
        if (user == null) {
            throw new IllegalStateException();
        }

        logger.info("Getting Tenants (apply_rcn_roles=true");

        SourcedRoleAssignments sourcedRoleAssignments = getSourcedRoleAssignmentsForUser(user);
        List<TenantRole> tenantRoles = sourcedRoleAssignments.asTenantRolesExcludeNoTenants();


        List<Tenant> tenants = getTenants(tenantRoles, new TenantEnabledPredicate());

        logger.info("Got {} tenants", tenants.size());
        return tenants;
    }

    /**
     * Returns a list of effective tenant roles the user has assigned. This includes all roles the user explicitly has
     * assigned on tenants and, if enabled, the automatically assigned "access" role to all tenants within the user's
     * domain, and those due to group membership.
     *
     * The returned roles are NOT populated with "role details" such as name, propagation status, etc.
     *
     * @param user
     * @return
     */
    private List<TenantRole> getEffectiveTenantRolesForUser(BaseUser user) {
        logger.debug("Getting effective tenant roles for user");
        List<TenantRole> userTenantRoles = new ArrayList<>();

        if (user instanceof Racker) {
            /*
             Rackers only have one "tenant role" which is the racker role. All other roles are from eDir which are not
             returned by this service as callers expect all roles returned to exist within Identity LDAP
             */
            TenantRole rackerRole = getEphemeralRackerTenantRole();
            if (rackerRole != null) {
                userTenantRoles.add(rackerRole);
            }
        } else if (user instanceof EndUser) {
            List<TenantRole> rolesToMerge = new ArrayList<>();

            // Get the list of tenant roles explicitly assigned to the user
            List<TenantRole> tenantRoles = Lists.newArrayList(this.tenantRoleDao.getTenantRolesForUser(user));

            /*
             If the user has user groups associated with it, only include group membership roles if user groups are enabled
             for that domain. Checking whether a domain is
             enabled could require a directory call depending on how the properties are configured. If the user doesn't
             belong to any groups, there is no point in even checking whether the domain is enabled for groups as
             regardless the user's list of roles won't be affected. This is why the collection check occurs first, to
             shortcircuit the statement and prevent the userGroupAuthorizationService from even running.
             */
            Set<String> groupIds = ((EndUser) user).getUserGroupIds();
            if (CollectionUtils.isNotEmpty(groupIds)
                    && userGroupAuthorizationService.areUserGroupsEnabledForDomain(user.getDomainId())) {
                for (String groupId : groupIds) {
                    List<TenantRole> groupRoles = Collections.emptyList();
                    try {
                        groupRoles = userGroupService.getRoleAssignmentsOnGroup(groupId);
                    } catch (NotFoundException ex) {
                        /*
                         Log the data referential integrity issue, but eat the exception as we don't want to fail if
                         the linked user group does not exist.
                         */
                        logger.warn(String.format("User '%s' is assigned group '%s', but the group does not exist.", user.getId(), groupId));
                    }
                    rolesToMerge.addAll(groupRoles);
                }
            }

            List<TenantRole> tempTenantRoles = new ArrayList<>();
            tempTenantRoles.addAll(tenantRoles);
            tempTenantRoles.addAll(rolesToMerge);
            IdentityUserTypeEnum userType = authorizationService.getIdentityTypeRoleAsEnum(tempTenantRoles);

            // Create the identity:tenant-access role if required
            TenantRole implicitRole = createTenantRoleForAutoAssignment(user, userType);
            if (implicitRole != null) {
                rolesToMerge.add(implicitRole);
            }

            userTenantRoles.addAll(roleUtil.mergeTenantRoleSets(tenantRoles, rolesToMerge));

        }
        return userTenantRoles;
    }

    /**
     * Determines the tenants within the domain's RCN and prepares them for matching against RCN roles. If the domain
     * does not have an RCN, returns just the domain's tenants.
     *
     * @param domain
     * @return
     */
    private List<Tenant> calculateRcnTenantsForRoleMatching(Domain domain) {
        List<Tenant> rcnTenants = new ArrayList<>();

        List<Domain> domainsToApply = new ArrayList<>();
        if (StringUtils.isNotBlank(domain.getRackspaceCustomerNumber())) {
            Iterable<Domain> domains = domainService.findDomainsWithRcn(domain.getRackspaceCustomerNumber());
            domainsToApply.addAll(IteratorUtils.toList(domains.iterator()));
        } else {
            // Always search over user's domain even if RCN attribute is blank
            domainsToApply.add(domain);
        }

        // Retrieve all the tenant types to perform matching
        Set<String> types = getTenantTypes();

        // Retrieve all the tenants within these domains
        for (Domain myDomain : domainsToApply) {
            // Retrieve all the tenants in the domains
            String[] tenantIds = getTenantIdsForDomain(myDomain);
            if (tenantIds != null) {
                for (String tenantId : tenantIds) {
                    Tenant tenant = getTenant(tenantId);
                    if (tenant != null) {
                        setInferredTenantTypeOnTenantIfNecessary(tenant, types);
                        rcnTenants.add(tenant);
                    }
                }
            }
        }
        return rcnTenants;
    }

    /**
     * Update the provided tenant's tenant types as appropriate based on infer rules. No-Op if tenant already has
     * one or more tenant types.
     *
     * @param tenant
     * @param existingTenantTypes
     */
    private void setInferredTenantTypeOnTenantIfNecessary(Tenant tenant, Set<String> existingTenantTypes) {
        if (identityConfig.getReloadableConfig().inferTenantTypeForTenant()
                && CollectionUtils.isEmpty(tenant.getTypes())) {
            String inferredTenantType = inferTenantTypeForTenantId(tenant.getTenantId(), existingTenantTypes);
            if (StringUtils.isNotBlank(inferredTenantType)) {
                tenant.getTypes().add(inferredTenantType);
            }
        }
    }

    /**
     * Retrieves, from the backend, the tenant types that exist within Identity
     *
     * @return
     */
    private Set<String> getTenantTypes() {
        PaginatorContext<TenantType> tenantTypes = tenantTypeService.listTenantTypes(0, TENANT_TYPE_SEARCH_LIMIT);
        List<TenantType> typeEntities = tenantTypes.getValueList();

        return typeEntities.stream().map(TenantType::getName).collect(Collectors.toSet());
    }

    @Override
    public String inferTenantTypeForTenantId(String tenantId) {
        return inferTenantTypeForTenantId(tenantId, getTenantTypes());
    }

    private String inferTenantTypeForTenantId(String tenantId, Set<String> existingTenantTypes) {
        if (existingTenantTypes.contains(GlobalConstants.TENANT_TYPE_CLOUD)) {
            try {
                Integer.parseInt(tenantId);
                return GlobalConstants.TENANT_TYPE_CLOUD;
            } catch (NumberFormatException ex) {
                // eat. We're just validating whether is a parsable int
            }
        }

        if (tenantId.startsWith(identityConfig.getStaticConfig().getNastTenantPrefix())
                && existingTenantTypes.contains(GlobalConstants.TENANT_TYPE_FILES)) {
            return GlobalConstants.TENANT_TYPE_FILES;
        }

        if (tenantId.startsWith(MANAGED_HOSTING_TENANT_PREFIX)
                && existingTenantTypes.contains(GlobalConstants.TENANT_TYPE_MANAGED_HOSTING)) {
            return GlobalConstants.TENANT_TYPE_MANAGED_HOSTING;
        }

        /*
         If prefix (up till first ':') matches an existing tenant type, use that as tenant type. TenantId have at least
         2 parts for inferring to work so 'asf:' would not attempt to infer 'asf' as a tenant type. However, it would
         for 'asf:a'
          */
        String tenantPrefix = parseTenantPrefixFromTenantId(tenantId);
        if (StringUtils.isNotEmpty(tenantPrefix) && existingTenantTypes.contains(tenantPrefix)) {
            return tenantPrefix;
        }

        return null;
    }

    /**
     * Given a tenant ID, parse out the tenant prefix. The tenant prefix is anything in the tenant ID up to but excluding
     * the first ':'. If the tenant ID does not contain a ':', the tenant ID does not have a prefix.
     *
     * @param tenantId
     * @return
     */
    private String parseTenantPrefixFromTenantId(String tenantId) {
        String[] idComponents = tenantId.split(":");
        return idComponents.length >= 2 ? idComponents[0] : null;
    }

    /**
     * Create a dynamic tenant role based on auto-assignment logic for the specified user and user's domain.
     *
     * The autoassigned role must be a non-propagating "identity:" product role. If not an identity
     * role, performance will be bad since it's not cached. If a propagating role, it will end up getting
     * errantly explicitly assigned to newly created subusers and federated users as roles are copied from
     * user-admins.
     *
     * @param user
     * @return
     */
    private TenantRole createTenantRoleForAutoAssignment(BaseUser user, IdentityUserTypeEnum userType) {
        TenantRole implicitRole = null;

        // If enabled, auto-assign access role to all tenants within user's domain
        if (StringUtils.isNotBlank(user.getDomainId())
                && !user.getDomainId().equalsIgnoreCase(identityConfig.getReloadableConfig().getTenantDefaultDomainId())) {
                /*
                 There's a 2 way linkage between Domains and Tenants. The link from domains to tenants needs to be retired,
                 but all existing code is really based on using the domain's list of tenants as the source of truth for
                 tenants in the domain. As part of v3 keystone reconciliation we updated tenants to point to the domain.
                 All tenants were updated to point to the appropriate domain, but there were various subsequent defects found
                 where the pointer may not have been updated. Until we switch all code to use the tenant linkage as the source
                 of truth, we need to still use the domain list.
                 */
            Domain domain = domainService.getDomain(user.getDomainId());
            if (domain != null) {
                String[] tenantIds = getTenantIdsForDomain(domain);
                if (ArrayUtils.isNotEmpty(tenantIds)) {
                    // Load the auto-assigned role from cache
                    ImmutableClientRole autoAssignedRole = getAutoAssignedRole();
                    List<String> tenantIdsToGetAutoAssignRole = new ArrayList<>(Arrays.asList(tenantIds));

                    if (autoAssignedRole != null && CollectionUtils.isNotEmpty(tenantIdsToGetAutoAssignRole)) {
                        // Add the auto-assigned role for all tenants in domain.
                        implicitRole = new TenantRole();
                        implicitRole.setClientId(autoAssignedRole.getClientId());
                        implicitRole.setRoleRsId(autoAssignedRole.getId());
                        implicitRole.setUserId(user.getId());
                        implicitRole.getTenantIds().addAll(tenantIdsToGetAutoAssignRole);
                    }
                }
            }
        }
        return implicitRole;
    }

    /**
     * Retrieve the auto-assigned 'identity:tenant-access' role based on configuration. The role name is configurable,
     * but is set to 'identity:tenant-access' in dev, staging, and production.
     **
     * @return
     */
    private ImmutableClientRole getAutoAssignedRole() {
        ImmutableClientRole autoAssignedRole = this.applicationService.getCachedClientRoleByName(IdentityRole.IDENTITY_TENANT_ACCESS.getRoleName());

        if (autoAssignedRole == null) {
            logger.warn(String.format("The auto-assign role '%s' is invalid. Not found in identity role cache.", IdentityRole.IDENTITY_TENANT_ACCESS.getRoleName()));
        } else if (BooleanUtils.isTrue(autoAssignedRole.getPropagate())) {
            logger.warn(String.format("The auto-assign role '%s' is invalid. Propagating roles are not allowed.", IdentityRole.IDENTITY_TENANT_ACCESS.getRoleName()));
            autoAssignedRole = null; // Null out role as role is not valid
        }
        return autoAssignedRole;
    }

    /**
     * Retrieves all the roles "effectively" assigned to the user. The resultant tenant roles are further fully populated
     * with all the client role details such as role
     * name, propagation, description, etc that are stored in the "client role" rather than that "tenant role".
     *
     * The client role details are loaded from back end client role w/o use of the client role cache.
     *
     * The calculation of which roles the user has includes:
     * <ul>
     * <li>Explicitly assigned to user</li>
     * <li>Assigned via user group membership</li>
     * <li>Implicitly assigned identity:tenant-access role</li>
     * </ul>
     *
     * @param user
     * @return
     */
    private List<TenantRole> getEffectiveTenantRolesForUserFullyPopulated(BaseUser user) {
        // Get the full set of tenant roles for the user
        List<TenantRole> tenantRoles = getEffectiveTenantRolesForUser(user);

        // Update all the tenant roles from that stored in the client role
        List<TenantRole> populatedTenantRoles = getRoleDetails(tenantRoles);
        return populatedTenantRoles;
    }

    @Override
    public boolean allTenantsDisabledForUser(EndUser user) {
        if (user == null) {
            throw new IllegalStateException();
        }

        List<Tenant> tenants = getTenants(this.tenantRoleDao.getTenantRolesForUser(user), new TenantNoOpPredicate());

        return !tenants.isEmpty() && CollectionUtils.find(tenants, new TenantEnabledPredicate()) == null;
    }

    /**
     * Loads all tenants for the given list of tenant roles. Also filters tenants
     * based on the given tenantPredicate. The tenant is included in the response if
     * tenantPredicate.evaluate() returns true
     *
     * Does apply RCN logic to any tenant role for an RCN role. The resultant tenants are solely limited to those
     * tenants explicitly listed in the tenant role.
     *
     * @param tenantRoles
     * @param tenantPredicate
     * @return
     */
    private List<Tenant> getTenants(Iterable<TenantRole> tenantRoles, Predicate<Tenant> tenantPredicate) {
        List<Tenant> tenants = new ArrayList<Tenant>();
        List<String> tenantIds = new ArrayList<String>();
        for (TenantRole role : tenantRoles) {
            if (role.getTenantIds() != null && role.getTenantIds().size() > 0) {
                for (String tenantId : role.getTenantIds()) {
                    if (!tenantIds.contains(tenantId)) {
                        Tenant tenant = this.getTenant(tenantId);
                        if (tenant != null && tenantPredicate.evaluate(tenant)) {
                            tenants.add(tenant);
                            tenantIds.add(tenantId);
                        }
                    }
                }
            }
        }

        return tenants;
    }

    @Override
    public void deleteGlobalRole(TenantRole role) {
        logger.info("Deleting Global Role {}", role);
        this.tenantRoleDao.deleteTenantRole(role);
        logger.info("Deleted Global Role {}", role);
    }

    @Override
    public void deleteRbacRolesForUser(EndUser user) {
        logger.info("Deleting Product Roles for {}", user);
        Iterable<TenantRole> tenantRoles = tenantRoleDao.getTenantRolesForUser(user);

        boolean sendFeedEvent = false;
        try {
            for (TenantRole role : tenantRoles) {
                if (role != null) {
                    // TODO: The client role is retrieved in method deleteTenantRoleForUser. This causes extract calls
                    // to ldap to retrieve the same client role and should be fixed.
                    ClientRole cRole = this.applicationService.getClientRoleById(role.getRoleRsId());
                    if (cRole != null && cRole.getRsWeight() == RoleLevelEnum.LEVEL_1000.getLevelAsInt()) {
                        deleteTenantRoleForUser(user, role, false);
                        sendFeedEvent = true;
                    }
                }
            }
        } finally {
            // Send user feed event if at least one tenant role was deleted from the user.
            if (sendFeedEvent) {
                atomHopperClient.asyncPost(user, FeedsUserStatusEnum.ROLE, MDC.get(Audit.GUUID));
            }
        }

        logger.info("Deleted Product Roles for {}", user);
    }

    @Override
    public void addTenantRoleToUser(BaseUser user, TenantRole role, boolean sendEventFeedForTargetUser) {
        if (user == null || StringUtils.isBlank(user.getUniqueId()) || role == null) {
            throw new IllegalArgumentException(
                    "User cannot be null and must have uniqueID; role cannot be null");
        }
        role.setUserId(user.getId());

        validateTenantRole(role);

        tenantRoleDao.addTenantRoleToUser(user, role);

        if (sendEventFeedForTargetUser && user instanceof EndUser) {
            atomHopperClient.asyncPost((EndUser) user, FeedsUserStatusEnum.ROLE, MDC.get(Audit.GUUID));
        }

        if(user instanceof User){
            // TODO: Method validateTenantRole already retrieves the client role. This causes a duplicate request
            // to retrieve the same role and should be fixed.
            ClientRole cRole = this.applicationService.getClientRoleByClientIdAndRoleName(role.getClientId(), role.getName());

            if (isUserAdmin((User) user) && cRole.getPropagate()) {
                //add the role to all sub-users
                for (User subUser : userService.getSubUsers((User) user)) {
                    try {
                        role.setUniqueId(null);
                        tenantRoleDao.addTenantRoleToUser(subUser, role);
                        atomHopperClient.asyncPost(subUser, FeedsUserStatusEnum.ROLE, MDC.get(Audit.GUUID));
                    } catch (ClientConflictException ex) {
                        String msg = String.format("User %s already has tenantRole %s", subUser.getId(), role.getName());
                        logger.warn(msg);
                    }
                }

                //add role to all federated users
                for(FederatedUser subUser : federatedUserDao.getUsersByDomainId(user.getDomainId())) {
                    try {
                        role.setUniqueId(null);
                        tenantRoleDao.addTenantRoleToUser(subUser, role);
                        atomHopperClient.asyncPost(subUser, FeedsUserStatusEnum.ROLE, MDC.get(Audit.GUUID));
                    } catch (ClientConflictException ex) {
                        String msg = String.format("Federated user %s already has tenantRole %s", subUser.getId(), role.getName());
                        logger.warn(msg);
                    }
                }
            }

        }
        logger.info("Adding tenantRole {} to user {}", role, user);
    }

    private boolean isUserAdmin(User user) {
        return hasRole(user, IdentityUserTypeEnum.USER_ADMIN.getRoleName());
    }

    /**
     * Whether or not the specified user is explicitly assigned a role with the given name. This does not account for
     * roles the user may receive based on group membership or via identity:tenant-access logic.
     * @param user
     * @param roleName
     * @return
     */
    private boolean hasRole(User user, String roleName) {
        for (TenantRole role : tenantRoleDao.getTenantRolesForUser(user)) {
            ClientRole cRole = applicationService.getClientRoleById(role.getRoleRsId());
            if (cRole.getName().equals(roleName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addTenantRolesToUser(BaseUser user, List<TenantRole> tenantRoles) {
        boolean sendFeedEvent = false;
        try {
            for (TenantRole tenantRole : tenantRoles) {
                addTenantRoleToUser(user, tenantRole, false);
                sendFeedEvent = true;
            }
        } finally {
            // Send user feed event if at least one tenant role was added to the user.
            if (user instanceof EndUser && sendFeedEvent) {
                atomHopperClient.asyncPost((EndUser) user, FeedsUserStatusEnum.ROLE, MDC.get(Audit.GUUID));
            }
        }

        logger.info("Added tenantRoles {} to user {}", tenantRoles, user);
    }

    @Override
    public void deleteTenantRoleForUser(EndUser endUser, TenantRole role, boolean sendEventFeedForTargetUser) {
        if (endUser == null || role == null) {
            throw new IllegalArgumentException();
        }

        ClientRole cRole = applicationService.getClientRoleById(role.getRoleRsId());
        if (cRole == null) {
            String errMsg = String.format("ClientRole %s not found", role.getName());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        tenantRoleDao.deleteTenantRoleForUser(endUser, role);

        if (sendEventFeedForTargetUser) {
            atomHopperClient.asyncPost(endUser, FeedsUserStatusEnum.ROLE, MDC.get(Audit.GUUID));
        }

        if (endUser instanceof User) {
            //this only applies for users, not federatedusers for now...
            User user = (User) endUser;

            if (isUserAdmin(user) && cRole.getPropagate()) {
                //remove propagating roles from sub-users
                for (User subUser : userService.getSubUsers(user)) {
                    try {
                        role.setUniqueId(null);
                        tenantRoleDao.deleteTenantRoleForUser(subUser, role);
                        atomHopperClient.asyncPost(subUser, FeedsUserStatusEnum.ROLE, MDC.get(Audit.GUUID));
                    } catch (NotFoundException ex) {
                        String msg = String.format("User %s does not have tenantRole %s", subUser.getId(), role.getName());
                        logger.warn(msg);
                    }
                }

                //remove propagating roles from federated users
                for(FederatedUser subUser : federatedUserDao.getUsersByDomainId(user.getDomainId())) {
                    try {
                        role.setUniqueId(null);
                        tenantRoleDao.deleteTenantRoleForUser(subUser, role);
                        atomHopperClient.asyncPost(subUser, FeedsUserStatusEnum.ROLE, MDC.get(Audit.GUUID));
                    } catch (NotFoundException ex) {
                        String msg = String.format("Federated user %s does not have tenantRole %s", user.getId(), role.getName());
                        logger.warn(msg);
                    }
                }
            }
        }
    }

    @Override
    public void deleteTenantOnRoleForUser(EndUser endUser, TenantRole role, Tenant tenant) {
        Assert.notNull(endUser);
        Assert.notNull(role);
        Assert.notNull(tenant);

        ClientRole cRole = applicationService.getClientRoleById(role.getRoleRsId());
        if (cRole == null) {
            String errMsg = String.format("ClientRole %s not found", role.getName());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        deleteTenantFromTenantRole(role, tenant.getTenantId());

        if (endUser instanceof User) {
            //this only applies for users, not federatedusers for now...
            User user = (User) endUser;

            atomHopperClient.asyncPost(user, FeedsUserStatusEnum.ROLE, MDC.get(Audit.GUUID));

            if (isUserAdmin(user) && cRole.getPropagate()) {
                //remove propagating roles from sub-users
                for (User subUser : userService.getSubUsers(user)) {
                    try {
                        TenantRole subUserTenantRole = tenantRoleDao.getTenantRoleForUser(subUser, role.getRoleRsId());
                        if (subUserTenantRole != null) {
                            deleteTenantFromTenantRole(subUserTenantRole, tenant.getTenantId());
                            atomHopperClient.asyncPost(subUser, FeedsUserStatusEnum.ROLE, MDC.get(Audit.GUUID));
                        }
                    } catch (NotFoundException ex) {
                        String msg = String.format("User %s does not have tenantRole %s", subUser.getId(), role.getName());
                        logger.warn(msg);
                    }
                }

                //remove propagating roles from federated users
                for(FederatedUser subUser : federatedUserDao.getUsersByDomainId(user.getDomainId())) {
                    try {
                        TenantRole subUserTenantRole = tenantRoleDao.getTenantRoleForUser(subUser, role.getRoleRsId());
                        deleteTenantFromTenantRole(subUserTenantRole, tenant.getTenantId());
                        atomHopperClient.asyncPost(subUser, FeedsUserStatusEnum.ROLE, MDC.get(Audit.GUUID));
                    } catch (NotFoundException ex) {
                        String msg = String.format("Federated user %s does not have tenantRole %s", user.getId(), role.getName());
                        logger.warn(msg);
                    }
                }
            }
        }
    }

    @Override
    public void deleteTenantFromTenantRole(TenantRole role, String tenantId) {
        boolean roleContainedTenant = role.getTenantIds().remove(tenantId);

        if (roleContainedTenant) {
            if(role.getTenantIds().size() == 0) {
                tenantRoleDao.deleteTenantRole(role);
            } else {
                tenantRoleDao.updateTenantRole(role);
            }
        }
    }

    @Override
    public List<TenantRole> getGlobalRolesForUser(BaseUser user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        logger.debug("Getting Global Roles for user {}", user.getUniqueId());
        Iterable<TenantRole> tenantRoles = getTenantRolesForUserNoDetail(user);
        return getGlobalRoles(tenantRoles, false);
    }

    @Override
    public List<TenantRole> getEffectiveGlobalRolesForUserIncludeRcnRoles(BaseUser user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        logger.debug("Getting Global Roles for user {}", user.getUniqueId());
        Iterable<TenantRole> tenantRoles = getEffectiveTenantRolesForUser(user);
        return getGlobalRoles(tenantRoles, false);
    }

    @Override
    public List<TenantRole> getEffectiveGlobalRolesForUserExcludeRcnRoles(BaseUser user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        logger.debug("Getting Global Roles (apply_rcn_roles=true) for user {}", user.getUniqueId());

        Iterable<TenantRole> tenantRoles = getEffectiveTenantRolesForUser(user);
        return getGlobalRoles(tenantRoles, true);
    }

    @Override
    public List<TenantRole> getEphemeralRackerTenantRoles(String rackerId) {
        List<TenantRole> rackerTenantRoles = new ArrayList<TenantRole>();

        rackerTenantRoles.add(getEphemeralRackerTenantRole());
        List<String> rackerIamGroups = userService.getRackerIamRoles(rackerId);
        if (CollectionUtils.isNotEmpty(rackerIamGroups)) {
            for (String r : rackerIamGroups) {
                TenantRole t = new TenantRole();
                t.setName(r);
                rackerTenantRoles.add(t);
            }
        }
        return rackerTenantRoles;
    }

    private TenantRole getEphemeralRackerTenantRole() {
        ImmutableClientRole rackerClientRole = applicationService.getCachedClientRoleById(identityConfig.getStaticConfig().getRackerRoleId());

        TenantRole rackerTenantRole = new TenantRole();
        rackerTenantRole.setRoleRsId(rackerClientRole.getId());
        rackerTenantRole.setClientId(rackerClientRole.getClientId());
        rackerTenantRole.setName(rackerClientRole.getName());

        return rackerTenantRole;
    }

    @Override
    public List<Tenant> getTenantsForEndpoint(String endpointId) {
        List<Tenant> tenantList = new ArrayList<>();
        for (Tenant tenant : tenantDao.getTenantsByBaseUrlId(endpointId)){
            tenantList.add(tenant);
        }

        return tenantList;
    }

    @Override
    public List<TenantRole> getGlobalRbacRolesForUser(EndUser user) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "User cannot be null.");
        }
        logger.debug("Getting Global Rbac Roles for user {}", user.getUniqueId());

        List<TenantRole> allRbacRoles = getRbacRolesForUser(user);

        List<TenantRole> globalRbacRoles = new ArrayList<TenantRole>();
        for (TenantRole role : allRbacRoles) {
            if (role.getTenantIds() == null || role.getTenantIds().size() == 0) {
                globalRbacRoles.add(role);
            }
        }
        return globalRbacRoles;
    }

    @Override
    public List<TenantRole> getRbacRolesForUser(EndUser user) {
        if (user == null) {
            throw new IllegalArgumentException(
                    "User cannot be null.");
        }
        logger.debug("Getting Rbac Roles for user {}", user.getUniqueId());
        Iterable<TenantRole> roles = this.tenantRoleDao.getTenantRolesForUser(user);

        List<TenantRole> globalRbacRoles = new ArrayList<TenantRole>();
        for (TenantRole role : roles) {
            if (role != null) {
                //TODO: Caching option?
                ClientRole cRole = this.applicationService.getClientRoleById(role
                        .getRoleRsId());
                if (cRole.getRsWeight() == PrecedenceValidator.RBAC_ROLES_WEIGHT) {
                    role.setName(cRole.getName());
                    role.setDescription(cRole.getDescription());
                    globalRbacRoles.add(role);
                }
            }
        }
        return globalRbacRoles;
    }

    @Override
    public List<TenantRole> getExplicitlyAssignedTenantRolesForUserPerformant(EndUser user) {
        Assert.notNull(user);

        logger.debug("Getting Rbac Roles for user {}", user.getUniqueId());
        Iterable<TenantRole> roles = this.tenantRoleDao.getTenantRolesForUser(user);

        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        for (TenantRole role : roles) {
            if (role != null) {
                ImmutableClientRole cRole = this.applicationService.getCachedClientRoleById(role.getRoleRsId());
                role.setName(cRole.getName());
                role.setDescription(cRole.getDescription());
                tenantRoles.add(role);
            }
        }

        return tenantRoles;
    }

    @Override
    public List<TenantRole> getEffectiveGlobalRolesForUserIncludeRcnRoles(BaseUser user, String applicationId) {
        logger.debug("Getting Global Roles (apply_rcn_roles=false)");
        List<TenantRole> allRoles = getEffectiveGlobalRolesForUserIncludeRcnRoles(user);
        return CollectionUtils.select(allRoles, new RoleForClientPredicate(applicationId), new ArrayList<TenantRole>());
    }

    @Override
    public List<TenantRole> getEffectiveGlobalRolesForUserExcludeRcnRoles(EndUser user, String applicationId) {
        logger.debug("Getting Global Roles (apply_rcn_roles=true)");
        List<TenantRole> allRoles = getEffectiveGlobalRolesForUserExcludeRcnRoles(user);
        return CollectionUtils.select(allRoles, new RoleForClientPredicate(applicationId), new ArrayList<TenantRole>());
    }

    @Override
    public List<TenantRole> getEffectiveTenantRolesForUserOnTenant(EndUser user, Tenant tenant) {
        if (tenant == null) {
            throw new IllegalArgumentException(
                    "Tenant cannot be null.");
        }

        logger.debug(GETTING_TENANT_ROLES);

        // Original code didn't return fully populated roles (e.g. role names) so continue just returning minimal info
        SourcedRoleAssignments sourcedRoleAssignments = getSourcedRoleAssignmentsForUser(user);
        SourcedRoleAssignmentsLegacyAdapter legacyAdapter = new SourcedRoleAssignmentsLegacyAdapter(sourcedRoleAssignments);
        List<TenantRole> allRoles = legacyAdapter.getTenantAssignedTenantRoles();

        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        for (TenantRole role : allRoles) {
            if (role.getTenantIds().contains(tenant.getTenantId())) {
                TenantRole newRole = new TenantRole();
                newRole.setClientId(role.getClientId());
                newRole.setRoleRsId(role.getRoleRsId());
                newRole.setName(role.getName());
                newRole.getTenantIds().add(tenant.getTenantId());
                tenantRoles.add(newRole);
            }
        }
        logger.debug(GOT_TENANT_ROLES, tenantRoles.size());
        return tenantRoles;
    }

    @Override
    public List<TenantRole> getEffectiveTenantRolesForUserOnTenantApplyRcnRoles(EndUser user, Tenant tenant) {
        if (tenant == null) {
            throw new IllegalArgumentException(
                    "Tenant cannot be null.");
        }

        logger.debug(GETTING_TENANT_ROLES);

        SourcedRoleAssignments sourcedRoleAssignments = getSourcedRoleAssignmentsForUser(user);
        List<TenantRole> allRoles = sourcedRoleAssignments.asTenantRolesExcludeNoTenants();

        // Original code didn't return fully populated roles (e.g. role names), so don't move over.
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        for (TenantRole role : allRoles) {
            if (role.getTenantIds().contains(tenant.getTenantId())) {
                TenantRole newRole = new TenantRole();
                newRole.setClientId(role.getClientId());
                newRole.setRoleRsId(role.getRoleRsId());
                newRole.setName(role.getName());
                newRole.getTenantIds().add(tenant.getTenantId());
                tenantRoles.add(newRole);
            }
        }
        logger.debug(GOT_TENANT_ROLES, tenantRoles.size());
        return tenantRoles;
    }

    @Override
    public List<TenantRole> getTenantRolesForUser(BaseUser user) {
        return getEffectiveTenantRolesForUserFullyPopulated(user);
    }

    @Override
    public Iterable<TenantRole> getTenantRolesForUserWithId(User user, Collection<String> roleIds) {
        return tenantRoleDao.getTenantRolesForUserWithId(user, roleIds);
    }


    @Override
    public int countTenantsWithTypeInDomain(String tenantType, String domainId) {
        if (StringUtils.isBlank(tenantType) || StringUtils.isBlank(domainId)) {
            throw new IllegalArgumentException("A valid tenant type and domain ID must be provided.");
        }

        return tenantDao.countTenantsWithTypeInDomain(tenantType, domainId);
    }

    @Override
    public List<TenantRole> getTenantRolesForUserPerformant(BaseUser user) {
        // Get the full set of tenant roles for the user
        List<TenantRole> tenantRoles = getEffectiveTenantRolesForUser(user);

        // Update all the tenant roles from that stored in the client role
        List<TenantRole> populatedTenantRoles = new ArrayList<>();
        for (TenantRole tenantRole : tenantRoles) {
            if (tenantRole != null) {
                ImmutableClientRole cRole = this.applicationService.getCachedClientRoleById(tenantRole.getRoleRsId());
                if (cRole != null) {
                    tenantRole.setName(cRole.getName());
                    tenantRole.setDescription(cRole.getDescription());
                    tenantRole.setRoleType(cRole.getRoleType());
                    populatedTenantRoles.add(tenantRole);
                } else {
                    logger.error(String.format("User w/ id '%s' is assigned a tenant role that is associated with " +
                            "the non-existent client role '%s'", user.getId(), tenantRole.getRoleRsId()));
                }
            }
        }
        return populatedTenantRoles;
    }

    @Override
    public Iterable<TenantRole> getTenantRolesForUserNoDetail(BaseUser user) {
        logger.debug(GETTING_TENANT_ROLES);

        Iterable<TenantRole> result = Collections.EMPTY_LIST;
        if (user instanceof Racker) {
            result = Arrays.asList(getEphemeralRackerTenantRole());
        } else if (user instanceof EndUser) {
            EndUser eu = (EndUser) user;
            result = tenantRoleDao.getTenantRolesForUser(eu);
        }
        return result;
    }

    /**
     * Populates the provides tenant roles w/ details from the client role including name, description, and role type.
     *
     * @param roles
     * @return
     */
    private List<TenantRole> getRoleDetails(Iterable<TenantRole> roles) {
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        for (TenantRole role : roles) {
            if (role != null) {
                ClientRole cRole = this.applicationService.getClientRoleById(role.getRoleRsId());
                //TODO: If the cRole returns null, this will cause NPE. Should just not populate role details
                role.setName(cRole.getName());
                role.setDescription(cRole.getDescription());
                role.setRoleType(cRole.getRoleType());
                tenantRoles.add(role);
            }
        }
        return tenantRoles;
    }

    @Override
    public List<TenantRole> getTenantRolesForTenant(String tenantId) {

        List<TenantRole> roles = new ArrayList<TenantRole>();

        HashMap<String,ClientRole> clientRolesMap = new HashMap<String, ClientRole>();
        for (TenantRole role : this.tenantRoleDao.getAllTenantRolesForTenant(tenantId)){
            if(!clientRolesMap.containsKey(role.getRoleRsId())){
                ClientRole cRole = this.applicationService.getClientRoleById(role.getRoleRsId());
                if(cRole != null){
                    clientRolesMap.put(role.getRoleRsId(),cRole);
                }
            }
            ClientRole clientRole = clientRolesMap.get(role.getRoleRsId());
            if (clientRole != null) {
                role.setDescription(clientRole.getDescription());
                role.setName(clientRole.getName());
            }
            roles.add(role);
        }

        return roles;
    }

    @Override
    public List<Tenant> getTenantsFromNameList(String[] tenants){
        List<Tenant> tenantList = new ArrayList<Tenant>();
        if(tenants == null) {
            return tenantList;
        }
        for (String tenantId : tenants) {
            Tenant tenant = this.tenantDao.getTenant(tenantId);
            if (tenant != null) {
                tenantList.add(tenant);
            }
        }
        return tenantList;
    }

    /**
     * Iterates over the specified set of roles and returns the subset that represent a globally assigned role. For
     * purposes of this method, it will consider an RCN role as globally assigned if applyRcnRoles is set to false. When
     * true, RCN roles will not be returned.
     *
     * For those roles returned, the name and description attribute are populated (and types for RCN roles).
     *
     * Note - this method modifies the provided roles to populate it with information from the client role for those
     * returned.
     *
     * @param roles
     * @param applyRcnRoles
     * @return
     */
    private List<TenantRole> getGlobalRoles(Iterable<TenantRole> roles, boolean applyRcnRoles) {
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        for (TenantRole role : roles) {
            if (role != null
                    && (role.getTenantIds() == null || role.getTenantIds().size() == 0)) {
                ClientRole cRole = this.applicationService.getClientRoleById(role.getRoleRsId());
                if (cRole == null) {
                    logger.error(String.format("Entity w/ id '%s' is assigned a tenant role that is associated with the non-existent client role '%s'. Skipping assignment.", role.getIdOfEntityAssignedRole(), role.getRoleRsId()));
                    continue;
                }
                if (cRole.getRoleType() == RoleTypeEnum.RCN) {
                    // If 'apply_rcn_roles=true', RCN roles assigned to the user are NOT returned.
                    if (applyRcnRoles) {
                        continue;
                    }
                    role.setRoleType(RoleTypeEnum.RCN);
                    Types types = new Types();
                    types.getType().addAll(cRole.getTenantTypes());
                    role.setTypes(types);
                }

                role.setName(cRole.getName());
                role.setDescription(cRole.getDescription());
                role.setRoleType(cRole.getRoleType());

                globalRoles.add(role);
            }
        }
        return globalRoles;
    }

    /**
     * Iterates over the specified set of roles and returns the subset that represent a tenant assigned role. For purposes
     * of this method RCN roles are always considered globally assigned, and will therefore never be returned.
     *
     * For those roles returned, the name and description attribute are populated.
     *
     * Note - this method creates new instances of matching tenant roles. It does NOT modify any provided role
     *
     * @param roles
     * @return
     */
    List<TenantRole> getTenantOnlyRoles(Iterable<TenantRole> roles) {
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        for (TenantRole role : roles) {
            // we only want to include roles on a tenant, and not global roles
            if (role.getTenantIds() != null && role.getTenantIds().size() > 0) {
                TenantRole newRole = new TenantRole();
                newRole.setClientId(role.getClientId());
                newRole.setRoleRsId(role.getRoleRsId());
                newRole.setName(role.getName());
                newRole.setDescription(role.getDescription());
                newRole.setTenantIds(role.getTenantIds());
                tenantRoles.add(newRole);
            }
        }

        return tenantRoles;
    }

    /**
     * Whether or not the specified tenant is eligible for auto role assignment based on the domain feature flag,
     * tenant type exclusion feature flag, and tenant characteristics.
     *
     * @param tenant
     * @return
     */
    private boolean isAutoAssignmentOfRoleEnabledForTenantDomain(Tenant tenant) {
        return tenant != null && StringUtils.isNotBlank(tenant.getDomainId())
                && !tenant.getDomainId().equalsIgnoreCase(identityConfig.getReloadableConfig().getTenantDefaultDomainId());
    }

    /**
     * Returns the list of enabled provisioned users which should receive the auto assigned role. Checks for eligibility for
     * auto-assignment first, and returns an empty list if not eligible.
     *
     * Only enabled users are eligible.
     *
     * TODO: Update to retrieve Fed users
     *
     * @param tenant
     * @return
     */
    private List<User> getEnabledUsersForAutoRoleAssignmentOnTenant(Tenant tenant) {
        List<User> users = Collections.EMPTY_LIST;

        if (isAutoAssignmentOfRoleEnabledForTenantDomain(tenant)) {
            users = new ArrayList<>();
            Iterable<User> domainUsers = userService.getUsersWithDomainAndEnabledFlag(tenant.getDomainId(), true);

            for (User domainUser : domainUsers) {
                users.add(domainUser);
            }
        }

        return users;
    }

    /**
     * Sort the list of users based on the user's id.
     *
     * @param users
     * @return
     */
    private List<User> sortUsersById(List<User> users) {
        Ordering<User> byId = Ordering.natural().onResultOf(
                new Function<User, Comparable>() {
                    public String apply(User user) {
                        return StringUtils.lowerCase(user.getId());
                    }
                });
        List<User> orderedUsers = byId.sortedCopy(users);
        return orderedUsers;
    }

    /**
     * Returns a list of users with access to the specified tenant. Optionally limited to only those with the specified role.
     *
     * TODO: This service is a performance concern as it needs to load up every user to then sort and return a page since
     * it needs to account for users outside of domain w/ access to the tenant
     *
     * @param tenant
     * @param limitingRoleId
     * @return
     */
    @Override
    public PaginatorContext<User> getEnabledUsersForTenantWithRole(Tenant tenant, String limitingRoleId, PaginationParams paginationParams) {
        Validate.notNull(tenant);

        List<User> finalUsers = new ArrayList<>();
        ImmutableClientRole limitByRole = null;
        if (StringUtils.isNotBlank(limitingRoleId)) {
            // Unknown roleIds will be ignored
            limitByRole = applicationService.getCachedClientRoleById(limitingRoleId);
        }

        /*
        List of user Ids already considered for inclusion. Not necessarily are included (e.g. if disabled).
         */
        Set<String> processedUserIds = new HashSet<>();

        /*
         If not limiting by role or the specified role to search is the implicit assignment role then include all the
         domain users.
        */
        List<User> autoAssignedUsers = Collections.emptyList();
        if (limitByRole == null || IdentityRole.IDENTITY_TENANT_ACCESS.getRoleName().equalsIgnoreCase(limitByRole.getName())) {
            autoAssignedUsers = getEnabledUsersForAutoRoleAssignmentOnTenant(tenant);
        }
        for (User autoAssignedUser : autoAssignedUsers) {
            processedUserIds.add(autoAssignedUser.getId());
            finalUsers.add(autoAssignedUser);
        }

        // Get list of users/groups that are assigned this role on tenant. This could include users in other domains
        Set<String> userIdsToProcess = new HashSet<>();
        Set<String> groupIdsToLookup = new HashSet<>();
        Iterable<TenantRole> tenantRoleIterable = null;

        if (limitByRole == null) {
            tenantRoleIterable = tenantRoleDao.getAllTenantRolesForTenant(tenant.getTenantId());
        } else {
            tenantRoleIterable = tenantRoleDao.getAllTenantRolesForTenantAndRole(tenant.getTenantId(), limitByRole.getId());
        }
        for (TenantRole role : tenantRoleIterable) {
            if (role.isUserAssignedRole()) {
                String userId = role.getIdOfEntityAssignedRole();
                if (StringUtils.isNotBlank(userId) && !processedUserIds.contains(userId)) {
                    userIdsToProcess.add(userId);
                }
            } else if (role.isUserGroupAssignedRole()) {
                String groupId = role.getIdOfEntityAssignedRole();
                if (StringUtils.isNotBlank(groupId)) {
                    groupIdsToLookup.add(groupId);
                }
            }
        }

        // Retrieve all enabled provisioned users that are members of the group assigned the role.
        for (String groupId : groupIdsToLookup) {
            UserGroup group = userGroupService.getGroupById(groupId);
            /*
             A group should always exist, given the groupIds are determined by looking at a TenantRoles uniqueId unless
             there's exceptional circumstances (e.g. group deleted between time tenant role read and group looked up).
             The check is trivial though so might as well make it.
              */
            if (group != null) {
                Iterable<EndUser> endUsers = identityUserService.getEndUsersInUserGroup(group);
                for (EndUser endUser : endUsers) {
                    if (endUser instanceof User && !endUser.isDisabled() && !processedUserIds.contains(endUser.getId())) {
                        finalUsers.add((User)endUser);
                    }
                    /* Since we retrieved the user we are effectively processing the user regardless of whether we actually
                     add the user. Remove the id from the to process list (if there) as well as add it to the processed list (if
                     needed).
                     */
                    userIdsToProcess.remove(endUser.getId());
                    processedUserIds.add(endUser.getId());
                }
            }
        }

        // Loop through remaining users that need to be looked up
        if (CollectionUtils.isNotEmpty(userIdsToProcess)) {
            for (String userId : userIdsToProcess) {
                //TODO: The existing use of the userService here limits this service to provisioned users only. Should include federated
                User user = this.userService.getUserById(userId);
                if (user != null && user.getEnabled()) {
                    finalUsers.add(user);
                }
            }
        }

        // Sort result & retrieve appropriate page
        finalUsers = sortUsersById(finalUsers);
        PaginatorContext<User> pageContext = new PaginatorContext<User>();
        pageContext.update(finalUsers, paginationParams.getEffectiveMarker(), paginationParams.getEffectiveLimit());

        return pageContext;
    }

    @Override
    public List<User> getEnabledUsersWithContactIdForTenant(String tenantId, String contactId) {
        Assert.notNull(tenantId);
        Assert.notNull(contactId);

        List<User> users = new ArrayList<>() ;
        for (User user : userService.getEnabledUsersByContactId(contactId)) {
            List<TenantRole> tenantRoles = getEffectiveTenantRolesForUser(user);
            if (isTenantIdContainedInTenantRoles(tenantId, tenantRoles)) {
                users.add(user);
            }
        }

        return users;
    }

    @Override
    public boolean isTenantIdContainedInTenantRoles(String tenantId, List<TenantRole> roles){
        boolean truth = false;

        if (roles != null) {
            for (TenantRole role : roles) {
                if (role.getTenantIds().contains(tenantId)) {
                    truth = true;
                }
            }
        }

        return truth;
    }

    @Override
    public List<Tenant> getTenantsByDomainId(String domainId) {
        List<Tenant> tenantList = new ArrayList<Tenant>();
        if (identityConfig.getReloadableConfig().isOnlyUseTenantDomainPointersEnabled()) {
            tenantDao.getTenantsByDomainId(domainId).forEach(tenantList::add);
            if (tenantList.size() == 0) {
                throw new NotFoundException(GlobalConstants.ERROR_MSG_NO_TENANTS_BELONG_TO_DOMAIN);
            }
        } else {
            //TODO: This should probably return an empty list as opposed to throwing an exception
            Domain domain = domainService.getDomain(domainId);
            String[] tenantIds = domain.getTenantIds();
            if (tenantIds == null || tenantIds.length == 0) {
                throw new NotFoundException(GlobalConstants.ERROR_MSG_NO_TENANTS_BELONG_TO_DOMAIN);
            }
            for (String tenantId : tenantIds) {
                Tenant tenant = getTenant(tenantId);
                if (tenant != null) {
                    tenantList.add(tenant);
                }
            }
        }
        return tenantList;
    }

    @Override
    public List<Tenant> getTenantsByDomainId(String domainId, boolean enabled) {
        List<Tenant> tenants = getTenantsByDomainId(domainId);
        tenants.removeIf(tenant -> !tenant.getEnabled().equals(enabled));
        return tenants;
    }

    @Override
    public String[] getTenantIdsForDomain(Domain domain) {
        List<String> tenantList = new ArrayList<String>();

        if (domain != null) {
            if (identityConfig.getReloadableConfig().isOnlyUseTenantDomainPointersEnabled()) {
                tenantDao.getTenantsByDomainId(domain.getDomainId()).forEach(t -> tenantList.add(t.getTenantId()));
            } else {
                return domain.getTenantIds();
            }
        }

        return tenantList.toArray(new String[0]);
    }

    @Override
    public Iterable<TenantRole> getTenantRolesForClientRole(ClientRole role) {
        return tenantRoleDao.getAllTenantRolesForClientRole(role);
    }

    @Override
    public void deleteTenantRole(TenantRole role) {
        tenantRoleDao.deleteTenantRole(role);
    }

    @Override
    public Iterable<TenantRole> getTenantRolesForUserById(EndUser user, List<ClientRole> rolesForFilter) {
        return tenantRoleDao.getTenantRoleForUser(user, rolesForFilter);
    }

    @Override
    public List<String> getIdsForUsersWithTenantRole(String roleId, int sizeLimit) {
        return tenantRoleDao.getIdsForUsersWithTenantRole(roleId, sizeLimit);
    }

    @Override
    public void addUserIdToTenantRole(TenantRole tenantRole) {
        String userId = tenantRoleDao.getUserIdForParent(tenantRole);
        if (userId != null) {
            tenantRole.setUserId(userId);
            tenantRoleDao.updateTenantRole(tenantRole);
        }
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }

    @Override
    public void setTenantDao(TenantDao tenantDao) {
        this.tenantDao = tenantDao;
    }

    @Override
    public void setTenantRoleDao(TenantRoleDao tenantRoleDao) {
        this.tenantRoleDao = tenantRoleDao;
    }

    private void validateTenantRole(TenantRole role) {
        Application owner = this.applicationService.getById(role.getClientId());
        if (owner == null) {
            String errMsg = String.format("Client %s not found", role.getClientId());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        ClientRole cRole = this.applicationService.getClientRoleByClientIdAndRoleName(role.getClientId(), role.getName());
        if (cRole == null) {
            String errMsg = String.format("ClientRole %s not found", role.getName());
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
    }

    @Override
    public String getMossoIdFromTenantRoles(List<TenantRole> roles) {

        Assert.notNull(roles);

        for (TenantRole role : roles) {
            if (role.getName().equals("compute:default") && !role.getTenantIds().isEmpty()) {
                return role.getTenantIds().iterator().next();
            }
        }

        for (TenantRole tenantRole : roles) {
            for (String tenantId : tenantRole.getTenantIds()) {
                if (tenantId.matches("\\d+")) {
                    return tenantId;
                }
            }
        }
        return null;
    }

    /**
     * A predicate that will return true if the given tenant is enabled
     */
    private static class TenantEnabledPredicate implements Predicate<Tenant> {
        @Override
        public boolean evaluate(Tenant tenant) {
            return tenant != null && tenant.getEnabled();
        }
    }

    /**
     * A No-Op predicate that will always return true
     */
    private static class TenantNoOpPredicate implements Predicate<Tenant> {
        @Override
        public boolean evaluate(Tenant tenant) {
            return true;
        }
    }

    private static class RoleForClientPredicate implements  Predicate<TenantRole> {
        private String clientId;

        public RoleForClientPredicate(String clientId) {
            Validate.notEmpty(clientId);
            this.clientId = clientId;
        }

        @Override
        public boolean evaluate(TenantRole tenantRole) {
            return clientId.equalsIgnoreCase(tenantRole.getClientId());
        }
    }


    /**
     * This returns only the role assignments the user has within the Identity system. It does not include roles the
     * racker has in AD.
     *
     * @param racker
     * @return
     */
    public SourcedRoleAssignments getSourcedRoleAssignmentsForRacker(Racker racker) {
        Validate.notNull(racker);
        Validate.notNull(racker.getRackerId());
        Validate.notNull(racker.getUsername());

        RackerSourcedRoleAssignmentsBuilder rackerSourcedRoleAssignmentsBuilder = RackerSourcedRoleAssignmentsBuilder.rackerBuilder(racker);

        /*
         Rackers only have one "tenant role" which is the racker role. All other roles are from eDir which are not
         returned by this service as callers expect all roles returned to exist within Identity LDAP
         */
        ImmutableClientRole rackerCr = applicationService.getCachedClientRoleById(identityConfig.getStaticConfig().getRackerRoleId());
        if (rackerCr != null) {
            rackerSourcedRoleAssignmentsBuilder.addIdentitySystemSourcedAssignment(rackerCr);
        }

        // Add the implicit roles associated with the racker's IAM groups
        List<String> rackerAdGroups = userService.getRackerIamRoles(racker.getRackerId());
        if (CollectionUtils.isNotEmpty(rackerAdGroups)) {
            for (String adGroup : rackerAdGroups) {
                String roleNameForImplicit = IAM_IMPLICIT_ROLE_PREFIX + adGroup; // prefix group name to avoid name collisions between IAM group names and Identity roles
                List<ImmutableClientRole> implicitIdentityManagedRoles = authorizationService.getImplicitRolesForRole(roleNameForImplicit);
                for (ImmutableClientRole implicitIdentityManagedRole : implicitIdentityManagedRoles) {
                    rackerSourcedRoleAssignmentsBuilder.addImplicitAssignment(roleNameForImplicit, implicitIdentityManagedRole);
                }
            }
        }
        return rackerSourcedRoleAssignmentsBuilder.build();
    }

    /**
     * Returns a list of effective roles the user has assigned. This includes all roles the user explicitly has
     * assigned on tenants, if enabled, the automatically assigned "access" role to all tenants within the user's
     * domain, and those due to group membership.
     *
     * @param user
     * @throws NotFoundException is user's domain is not found
     * @return
     */
    public SourcedRoleAssignments getSourcedRoleAssignmentsForUser(EndUser user) {
        logger.debug("Getting effective tenant roles for user");
        Validate.notNull(user);
        Validate.notNull(user.getId());

        // Code smell. Need to replace this with factory or some other manner to avoid all these 'instanceof'
        UserRoleLookupService userRoleLookupService = null;
        if (user instanceof ProvisionedUserDelegate) {
            userRoleLookupService = new DelegateUserRoleLookupService((ProvisionedUserDelegate) user);
        } else {
            userRoleLookupService = new CachedUserRoleLookupService(user);
        }

        EndUserDenormalizedSourcedRoleAssignmentsBuilder sourcedRoleAssignmentsBuilder = EndUserDenormalizedSourcedRoleAssignmentsBuilder.endUserBuilder(userRoleLookupService);
        SourcedRoleAssignments finalAssignments = sourcedRoleAssignmentsBuilder.build();

        if (identityConfig.getReloadableConfig().isTenantRoleWhitelistVisibilityFilterEnabled()) {
            finalAssignments = tenantTypeWhitelistFilter.apply(finalAssignments);
        }

        return finalAssignments;
    }

    /**
     * A wrapping of external functionality required for user role calculations. Caches the lookups locally to guarantee
     * common results for each lookup.
     *
     * Part of the purpose of this class is to reuse existing, but not public, methods already
     * provided by the DefaultTenantService. Eventually would like to migrate retrieval of a Users roles to this
     * single service - at which point we could move the appropriate logic into it and externalize it as a RoleAssignment
     * builder. Until then, we reuse the existing methods.
     */
    @NotThreadSafe
    private class CachedUserRoleLookupService implements UserRoleLookupService {
        private List<Tenant> domainRcnTenants = null;
        private Map<String, ImmutableClientRole> immutableClientRoleMap = new HashMap<>();

        private List<TenantRole> userSourcedRoles = null;
        private Map<String, List<TenantRole>> groupSourcedRoles = null;
        private Map<String, List<TenantRole>> systemSourcedRoles = null;

        @Getter
        private EndUser user;

        @Getter
        private Domain userDomain;

        @Getter
        private String[] tenantIds;

        public CachedUserRoleLookupService(EndUser user) {
            this.user = user;
            if (StringUtils.isNotBlank(user.getDomainId())) {
                this.userDomain = domainService.getDomain(user.getDomainId());
                this.tenantIds = tenantService.getTenantIdsForDomain(this.userDomain);
            }
        }

        /**
         * Returns the roles associated with the user
         * @return
         */
        @Override
        public List<TenantRole> getUserSourcedRoles() {
            if (userSourcedRoles == null) {
                Iterable<TenantRole> itUserSourceTenantRoles = tenantRoleDao.getTenantRolesForUser(user);
                userSourcedRoles = IteratorUtils.toList(itUserSourceTenantRoles.iterator());
            }
            return userSourcedRoles;
        }

        @Override
        public Map<String, List<TenantRole>> getGroupSourcedRoles() {
            /*
             If the user has user groups associated with it, only include group membership roles if user groups are enabled
             for that domain. Checking whether a domain is
             enabled could require a directory call depending on how the properties are configured. If the user doesn't
             belong to any groups, there is no point in even checking whether the domain is enabled for groups as
             regardless the user's list of roles won't be affected. This is why the collection check occurs first, to
             shortcircuit the statement and prevent the userGroupAuthorizationService from even running.
             */
            if (groupSourcedRoles == null) {
                Map<String, List<TenantRole>> groupRoleMap = new HashMap<>();

                Set<String> groupIds = user.getUserGroupIds();
                if (CollectionUtils.isNotEmpty(groupIds)
                        && userGroupAuthorizationService.areUserGroupsEnabledForDomain(user.getDomainId())) {
                    for (String groupId : groupIds) {
                        List<TenantRole> groupRoles = Collections.emptyList();
                        try {
                            groupRoles = userGroupService.getRoleAssignmentsOnGroup(groupId);
                        } catch (NotFoundException ex) {
                        /*
                         Log the data referential integrity issue, but eat the exception as we don't want to fail if
                         the linked user group does not exist.
                         */
                            logger.warn(String.format("User '%s' is assigned group '%s', but the group does not exist.", user.getId(), groupId));
                        }
                        groupRoleMap.put(groupId, groupRoles);
                    }
                }
                groupSourcedRoles = groupRoleMap;
            }
            return groupSourcedRoles;
        }

        @Override
        public Map<String, List<TenantRole>> getSystemSourcedRoles() {
            if (systemSourcedRoles == null) {
                Map<String, List<TenantRole>> systemRoleMap = new HashMap<>();

                // Assign the identity:tenant-access role if required
                TenantRole systemAssignedRole = createTenantRoleForAutoAssignment(user, authorizationService.getIdentityTypeRoleAsEnum(user));
                if (systemAssignedRole != null) {
                    systemRoleMap.put(SYSTEM_SOURCE_IDENTITY, Arrays.asList(systemAssignedRole));
                }
                systemSourcedRoles = systemRoleMap;
            }
            return systemSourcedRoles;
        }

        @Override
        public Map<TenantRole, RoleAssignmentSource> getOtherSourcedRoles() {
            return Collections.emptyMap();
        }

        @Override
        public List<Tenant> calculateRcnTenants() {
            if (domainRcnTenants == null) {
                domainRcnTenants = calculateRcnTenantsForRoleMatching(userDomain);
            }
            return domainRcnTenants;
        }

        /**
         * Locally caches the roles. While uses a cached version of the role lookup service, we want to guarantee the
         * exact same role for each instance of this service - which is meant to be short lived. This prevents any
         * potential changes from affecting calculations that look up the same role at various times.
         * @param id
         * @return
         */
        @Override
        public ImmutableClientRole getImmutableClientRole(String id) {
            ImmutableClientRole immutableClientRole = immutableClientRoleMap.get(id);
            if (immutableClientRole == null) {
                immutableClientRole = applicationService.getCachedClientRoleById(id);
                immutableClientRoleMap.put(id, immutableClientRole);
            }
            return immutableClientRole;
        }
    }

    @NotThreadSafe
    private class DelegateUserRoleLookupService implements UserRoleLookupService {
        private List<Tenant> domainRcnTenants = null;
        private Map<String, ImmutableClientRole> immutableClientRoleMap = new HashMap<>();

        private List<TenantRole> userSourcedRoles = null;
        private Map<String, List<TenantRole>> groupSourcedRoles = null;
        private Map<String, List<TenantRole>> systemSourcedRoles = null;

        private ProvisionedUserDelegate provisionedUserDelegate;

        @Getter
        private Domain userDomain;

        @Getter
        private String[] tenantIds;

        public DelegateUserRoleLookupService(ProvisionedUserDelegate provisionedUserDelegate) {
            this.provisionedUserDelegate = provisionedUserDelegate;
            if (StringUtils.isNotBlank(provisionedUserDelegate.getDomainId())) {
                this.userDomain = domainService.getDomain(provisionedUserDelegate.getDomainId());
                this.tenantIds = tenantService.getTenantIdsForDomain(this.userDomain);
            }
        }

        @Override
        public EndUser getUser() {
            return provisionedUserDelegate;
        }

        @Override
        public List<Tenant> calculateRcnTenants() {
            if (domainRcnTenants == null) {
                domainRcnTenants = calculateRcnTenantsForRoleMatching(userDomain);
            }
            return domainRcnTenants;
        }

        /**
         * Locally caches the roles. While uses a cached version of the role lookup service, we want to guarantee the
         * exact same role for each instance of this service - which is meant to be short lived. This prevents any
         * potential changes from affecting calculations that look up the same role at various times.
         * @param id
         * @return
         */
        @Override
        public ImmutableClientRole getImmutableClientRole(String id) {
            ImmutableClientRole immutableClientRole = immutableClientRoleMap.get(id);
            if (immutableClientRole == null) {
                immutableClientRole = applicationService.getCachedClientRoleById(id);
                immutableClientRoleMap.put(id, immutableClientRole);
            }
            return immutableClientRole;
        }

        @Override
        public List<TenantRole> getUserSourcedRoles() {
            // User sourced roles are just those based on domain defaults for consistency with provisioned users
            if (userSourcedRoles == null) {
                userSourcedRoles = new ArrayList<>(provisionedUserDelegate.getDefaultDomainRoles().size());
                for (ImmutableTenantRole immutableTenantRole : provisionedUserDelegate.getDefaultDomainRoles()) {
                    userSourcedRoles.add(immutableTenantRole.asTenantRole());
                }
            }
            return userSourcedRoles;
        }

        @Override
        public Map<String, List<TenantRole>> getGroupSourcedRoles() {
            return Collections.emptyMap(); // Not supported yet
        }

        @Override
        public Map<String, List<TenantRole>> getSystemSourcedRoles() {
            if (systemSourcedRoles == null) {
                Map<String, List<TenantRole>> systemRoleMap = new HashMap<>();

                // Assign the identity:tenant-access role if required
                TenantRole systemAssignedRole = createTenantRoleForAutoAssignment(provisionedUserDelegate, authorizationService.getIdentityTypeRoleAsEnum(provisionedUserDelegate));
                if (systemAssignedRole != null) {
                    systemRoleMap.put(SYSTEM_SOURCE_IDENTITY, Arrays.asList(systemAssignedRole));
                }
                systemSourcedRoles = systemRoleMap;
            }
            return systemSourcedRoles;
        }

        @Override
        public Map<TenantRole, RoleAssignmentSource> getOtherSourcedRoles() {
            return getDelegationAgreementSourcedRoles();
        }

        // TODO: Need to refactor these searches into a single call that retrieves all sources
        private Map<TenantRole, RoleAssignmentSource> getDelegationAgreementSourcedRoles() {
            Map<TenantRole, RoleAssignmentSource> daSourcedRoles = new LinkedHashMap<>();

            Iterable<TenantRole> tenantRoles = delegationService.getAllRoleAssignmentsOnDelegationAgreement(provisionedUserDelegate.getDelegationAgreement());

            if (tenantRoles != null) {
                for (TenantRole tenantRole : tenantRoles) {
                    RoleAssignmentSource source = new RoleAssignmentSource(RoleAssignmentSourceType.DA, provisionedUserDelegate.getDelegationAgreement().getId(), null, tenantRole.getTenantIds());
                    daSourcedRoles.put(tenantRole, source);
                }
            }

            return daSourcedRoles;
        }
    }
}
