package com.rackspace.idm.domain.service.impl;

import com.google.common.base.Function;
import com.google.common.collect.Ordering;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Types;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants;
import com.rackspace.idm.api.security.ImmutableClientRole;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.FederatedUserDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ClientConflictException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.modules.usergroups.service.UserGroupService;
import com.rackspace.idm.util.RoleUtil;
import com.rackspace.idm.validation.PrecedenceValidator;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.*;
import java.util.stream.Collectors;

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
    FederatedUserDao federatedUserDao;

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
                this.tenantRoleDao.deleteTenantRole(role, tenant.getTenantId());
            } else {
                role.getTenantIds().remove(tenant.getTenantId());
                this.tenantRoleDao.updateTenantRole(role, tenant.getTenantId());
            }
        }

        // Delete the tenantId off of the associated domain
        if(!StringUtils.isBlank(tenant.getDomainId())) {
            Domain domain = domainService.getDomain(tenant.getDomainId());
            if(domain != null) {
                List<String> tenantIds = new ArrayList<String>(Arrays.asList(domain.getTenantIds()));
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
    public List<Tenant> getTenantsForUserByTenantRoles(BaseUser user) {
        if (user == null) {
            throw new IllegalStateException();
        }

        logger.info("Getting Tenants for Parent");

        Iterable<TenantRole> tenantRoles = getEffectiveTenantRolesForUser(user);
        List<Tenant> tenants = getTenants(tenantRoles, new TenantEnabledPredicate());

        logger.info("Got {} tenants", tenants.size());
        return tenants;
    }

    @Override
    public List<Tenant> getTenantsForUserByTenantRolesApplyRcnRoles(BaseUser user) {
        if (user == null) {
            throw new IllegalStateException();
        }

        logger.info("Getting Tenants (apply_rcn_roles=true");

        Iterable<TenantRole> tenantRoles = getEffectiveTenantRolesForUserApplyRcnRoles(user);
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
            Iterable<TenantRole> itTenantRoles = this.tenantRoleDao.getTenantRolesForUser(user);

            // Create the identity:tenant-access role if required
            TenantRole implicitRole = createTenantRoleForAutoAssignment(user);
            if (implicitRole != null) {
                rolesToMerge.add(implicitRole);
            }

            /*
             Add the roles the user has due to group membership.
             */
            if (identityConfig.getReloadableConfig().applyGroupMembershipForEffectiveRoleCalculation()) {
                Set<String> groupIds = ((EndUser) user).getUserGroupIds();
                for (String groupId : groupIds) {
                    List<TenantRole> groupRoles = Collections.emptyList();
                    try {
                        groupRoles = userGroupService.getRoleAssignmentsOnGroup(groupId);
                    } catch (NotFoundException ex) {
                        logger.warn(String.format("User '%s' is assigned group '%s', but the group does not exist.", user.getId(), groupId));
                    }
                    rolesToMerge.addAll(groupRoles);
                }
            }
            userTenantRoles.addAll(roleUtil.mergeTenantRoleSets(itTenantRoles, rolesToMerge));
        }
        return userTenantRoles;
    }



    /**
     * Get the effective tenant roles for a user and apply RCN logic rules. The application of RCN rules does the following:
     * <ol>
     *     <li>All non-RCN global roles are assigned to all tenants within the user's domain</li>
     *     <li><All RCN assigned roles are matched against all tenants in all domains within the user's RCN to determine on
     * which tenants the user will gain that role./li>
     * </ol>
     *
     * Note - this is NOT efficient when a user has an RCN role as it will cause multiple lookups of the same domain as
     * well as a subset of the tenants within the RCN. This may need to be
     * optimized prior to RCN roles being in widespread use.
     *
     * @param user
     * @return
     */
    private List<TenantRole> getEffectiveTenantRolesForUserApplyRcnRoles(BaseUser user) {
        List<TenantRole> initialRoles = getTenantRolesForUserPerformant(user); //returns full populated roles

        // We'll always need the user's domain to apply the RCN logic so retrieve it from the getgo
        String domainId = user.getDomainId();
        Domain domain = domainService.getDomain(domainId);
        if (domain == null) {
            /*
             Only possible w/ data referential integrity issues. Just return initial roles since without a domain can't
             apply any RCN logic
             */
            return initialRoles;
        }

        // Determine all RCN Roles the user has assigned
        Map<ImmutableClientRole, TenantRole> rcnRolesAssigned = new LinkedHashMap<>();
        List<TenantRole> nonRcnRolesAssigned = new ArrayList<TenantRole>();
        for (TenantRole initialRole : initialRoles) {
            ImmutableClientRole clientRole = applicationService.getCachedClientRoleById(initialRole.getRoleRsId());
            if (clientRole != null) {
                if (clientRole.getRoleType() == RoleTypeEnum.RCN) {
                    rcnRolesAssigned.put(clientRole, initialRole);
                } else {
                    nonRcnRolesAssigned.add(initialRole);
                }
            }
        }

        // If the user has an RCN role assigned we'll need to perform tenant type matching so look up all tenants within RCN
        List<Tenant> rcnTenants = new ArrayList<>();
        if (MapUtils.isNotEmpty(rcnRolesAssigned)) {
            rcnTenants = calculateRcnTenantsForRoleMatching(domain);
        }

        // Loop over each RCN role assigned to user and apply across all tenants
        for (Map.Entry<ImmutableClientRole, TenantRole> rcnEntry : rcnRolesAssigned.entrySet()) {
            ImmutableClientRole clientRole = rcnEntry.getKey();
            TenantRole tenantRole = rcnEntry.getValue();

            Set<String> roleTenantTypes = clientRole.getTenantTypes();

            for (Tenant rcnTenant : rcnTenants) {
                Set<String> tenantTenantTypes = rcnTenant.getTypes();
                // If role matches tenant, add tenant to tenantRole tenant list
                if (roleTenantTypes.contains("*") 
                        || CollectionUtils.isNotEmpty(CollectionUtils.intersection(roleTenantTypes, tenantTenantTypes))) {
                    tenantRole.getTenantIds().add(rcnTenant.getTenantId());
                }
            }
        }

        // Loop over non-RCN global roles assigned to user and apply to all domain tenants
        String[] domainTenants = domain.getTenantIds();
        if (!ArrayUtils.isEmpty(domainTenants)) {
            for (TenantRole tenantRole : nonRcnRolesAssigned) {
                // Only apply to globally assigned roles
                if (CollectionUtils.isEmpty(tenantRole.getTenantIds())) {
                    for (String tenantId : domainTenants) {
                        tenantRole.getTenantIds().add(tenantId);
                    }
                }
            }
        }

        /*
         Only "keep" roles which were assigned a tenant. This adds a potential edge case for users that belong to a
         domain that does not have any tenant. In this case, the user will not have an identity user classification role
         as those are always globally assigned and are never RCN roles. This is as designed and systems affected by this
          should submit a new query with apply_rcn_roles=false to retried the "un-tenantized" list of rcn roles.
         */

        List<TenantRole> finalRoles = new ArrayList<TenantRole>();
        for (TenantRole tenantRole : rcnRolesAssigned.values()) {
            if (CollectionUtils.isNotEmpty(tenantRole.getTenantIds())) {
                finalRoles.add(tenantRole);
            }
        }
        for (TenantRole tenantRole : nonRcnRolesAssigned) {
            if (CollectionUtils.isNotEmpty(tenantRole.getTenantIds())) {
                finalRoles.add(tenantRole);
            }
        }

        return finalRoles;
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
            String[] tenantIds = myDomain.getTenantIds();
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
        String[] prefixes = tenantId.split(":");
        if (prefixes.length >= 2 && existingTenantTypes.contains(prefixes[0])) {
            return prefixes[0];
        }

        return null;
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
    private TenantRole createTenantRoleForAutoAssignment(BaseUser user) {
        TenantRole implicitRole = null;

        // If enabled, auto-assign access role to all tenants within user's domain
        if (StringUtils.isNotBlank(user.getDomainId())
                && identityConfig.getReloadableConfig().isAutomaticallyAssignUserRoleOnDomainTenantsEnabled()
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
                String[] tenantIds = domain.getTenantIds();
                if (ArrayUtils.isNotEmpty(tenantIds)) {
                    // Load the auto-assigned role from cache
                    ImmutableClientRole autoAssignedRole = getAutoAssignedRole();

                    if (autoAssignedRole != null) {
                        // Add the autoassigned role for all tenants in domain.
                        implicitRole = new TenantRole();
                        implicitRole.setClientId(autoAssignedRole.getClientId());
                        implicitRole.setRoleRsId(autoAssignedRole.getId());
                        implicitRole.setUserId(user.getId());
                        Collections.addAll(implicitRole.getTenantIds(), tenantIds);
                    }
                }
            }
        }
        return implicitRole;
    }

    /**
     * Retrieve the auto-assigned 'identity:tenant-access' role based on configuration. The role name is configurable,
     * but is set to 'identity:tenant-access' in dev, staging, and production.
     *
     * TODO: Eliminate the configuration and hardcode to identity:tenant-access role name.
     *
     * @return
     */
    private ImmutableClientRole getAutoAssignedRole() {
        ImmutableClientRole autoAssignedRole = null;

        if (identityConfig.getReloadableConfig().isAutomaticallyAssignUserRoleOnDomainTenantsEnabled()) {
            if(identityConfig.getReloadableConfig().getCacheRolesWithoutApplicationRestartFlag()) {
                autoAssignedRole = this.applicationService.getCachedClientRoleByName(identityConfig.getReloadableConfig()
                        .getAutomaticallyAssignUserRoleOnDomainTenantsRoleName());
            } else {
                autoAssignedRole = authorizationService.getCachedIdentityRoleByName(identityConfig.getReloadableConfig()
                        .getAutomaticallyAssignUserRoleOnDomainTenantsRoleName());
            }
            if (autoAssignedRole == null) {
                logger.warn(String.format("The auto-assign role '%s' is invalid. Not found in identity role cache.", autoAssignedRole.getName()));
            } else if (BooleanUtils.isTrue(autoAssignedRole.getPropagate())) {
                logger.warn(String.format("The auto-assign role '%s' is invalid. Propagating roles are not allowed.", autoAssignedRole.getName()));
                autoAssignedRole = null; // Null out role as role is not valid
            }
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

        for (TenantRole role : tenantRoles) {
            if (role != null) {
                ClientRole cRole = this.applicationService.getClientRoleById(role.getRoleRsId());
                if (cRole != null && cRole.getRsWeight() == RoleLevelEnum.LEVEL_1000.getLevelAsInt()) {
                    deleteTenantRoleForUser(user, role);
                }
            }
        }

        logger.info("Deleted Product Roles for {}", user);
    }

    @Override
    public void addTenantRoleToUser(BaseUser user, TenantRole role) {
        if (user == null || StringUtils.isBlank(user.getUniqueId()) || role == null) {
            throw new IllegalArgumentException(
                    "User cannot be null and must have uniqueID; role cannot be null");
        }
        role.setUserId(user.getId());

        validateTenantRole(role);

        tenantRoleDao.addTenantRoleToUser(user, role);

        if(user instanceof User){
            ClientRole cRole = this.applicationService.getClientRoleByClientIdAndRoleName(role.getClientId(), role.getName());
            atomHopperClient.asyncPost((User) user, AtomHopperConstants.ROLE);
            if (isUserAdmin((User) user) && cRole.getPropagate()) {
                //add the role to all sub-users
                for (User subUser : userService.getSubUsers((User) user)) {
                    try {
                        role.setUniqueId(null);
                        tenantRoleDao.addTenantRoleToUser(subUser, role);
                        atomHopperClient.asyncPost(subUser, AtomHopperConstants.ROLE);
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
        String roleName = config.getString("cloudAuth.userAdminRole");
        return hasRole(user, roleName);
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
    public void addCallerTenantRolesToUser(User caller, User user) {
        List<TenantRole> tenantRoles = this.getTenantRolesForUser(caller);
        for (TenantRole tenantRole : tenantRoles) {
            if (!tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.adminRole"))
                    && !tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.serviceAdminRole"))
                    && !tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.userAdminRole"))
                    && !tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.userRole"))
                    && !tenantRole.getName().equalsIgnoreCase(config.getString("cloudAuth.userManagedRole"))
                    && tenantRole.getPropagate()
                    ) {
                TenantRole role = new TenantRole();
                role.setClientId(tenantRole.getClientId());
                role.setDescription(tenantRole.getDescription());
                role.setName(tenantRole.getName());
                role.setRoleRsId(tenantRole.getRoleRsId());
                role.setTenantIds(tenantRole.getTenantIds());
                role.setUserId(user.getId());
                this.addTenantRoleToUser(user, role);
            }
        }
    }

    @Override
    public void addTenantRolesToUser(BaseUser user, List<TenantRole> tenantRoles) {
        for (TenantRole tenantRole : tenantRoles) {
            addTenantRoleToUser(user, tenantRole);
        }

        logger.info("Added tenantRoles {} to user {}", tenantRoles, user);
    }

    @Override
    public void deleteTenantRoleForUser(EndUser endUser, TenantRole role) {
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

        if (endUser instanceof User) {
            //this only applies for users, not federatedusers for now...
            User user = (User) endUser;
            atomHopperClient.asyncPost((User) user, AtomHopperConstants.ROLE);

            if (isUserAdmin(user) && cRole.getPropagate()) {
                //remove propagating roles from sub-users
                for (User subUser : userService.getSubUsers(user)) {
                    try {
                        role.setUniqueId(null);
                        tenantRoleDao.deleteTenantRoleForUser(subUser, role);
                        atomHopperClient.asyncPost(subUser, AtomHopperConstants.ROLE);
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

        deleteTenantFromTenantRole(role, tenant);

        if (endUser instanceof User) {
            //this only applies for users, not federatedusers for now...
            User user = (User) endUser;
            atomHopperClient.asyncPost(user, AtomHopperConstants.ROLE);

            if (isUserAdmin(user) && cRole.getPropagate()) {
                //remove propagating roles from sub-users
                for (User subUser : userService.getSubUsers(user)) {
                    try {
                        TenantRole subUserTenantRole = tenantRoleDao.getTenantRoleForUser(subUser, role.getRoleRsId());
                        if (subUserTenantRole != null) {
                            deleteTenantFromTenantRole(subUserTenantRole, tenant);
                            atomHopperClient.asyncPost(subUser, AtomHopperConstants.ROLE);
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
                        deleteTenantFromTenantRole(subUserTenantRole, tenant);
                    } catch (NotFoundException ex) {
                        String msg = String.format("Federated user %s does not have tenantRole %s", user.getId(), role.getName());
                        logger.warn(msg);
                    }
                }

            }
        }

    }

    private void deleteTenantFromTenantRole(TenantRole role, Tenant tenant) {
        role.getTenantIds().remove(tenant.getTenantId());
        if(role.getTenantIds().size() == 0) {
            tenantRoleDao.deleteTenantRole(role, tenant.getTenantId());
        } else {
            tenantRoleDao.updateTenantRole(role, tenant.getTenantId());
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
    public List<TenantRole> getGlobalRolesForUserApplyRcnRoles(BaseUser user) {
        if (user == null) {
            throw new IllegalArgumentException("User cannot be null.");
        }
        logger.debug("Getting Global Roles (apply_rcn_roles=true) for user {}", user.getUniqueId());

        Iterable<TenantRole> tenantRoles = getTenantRolesForUserNoDetail(user);
        return getGlobalRoles(tenantRoles, true);
    }

    @Override
    public List<TenantRole> getEphemeralRackerTenantRoles(String rackerId) {
        List<TenantRole> rackerTenantRoles = new ArrayList<TenantRole>();

        rackerTenantRoles.add(getEphemeralRackerTenantRole());
        List<String> rackerEdirGroups = userService.getRackerEDirRoles(rackerId);
        if (CollectionUtils.isNotEmpty(rackerEdirGroups)) {
            for (String r : rackerEdirGroups) {
                TenantRole t = new TenantRole();
                t.setName(r);
                rackerTenantRoles.add(t);
            }
        }
        return rackerTenantRoles;
    }

    @Override
    public TenantRole getEphemeralRackerTenantRole() {
        ImmutableClientRole rackerClientRole = null;
        if (identityConfig.getReloadableConfig().getCacheRolesWithoutApplicationRestartFlag()) {
            rackerClientRole = applicationService.getCachedClientRoleById(identityConfig.getStaticConfig().getRackerRoleId());
        } else {
            rackerClientRole = authorizationService.getCachedIdentityRoleById(identityConfig.getStaticConfig().getRackerRoleId());
        }
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
    public List<TenantRole> getGlobalRolesForUser(EndUser user, String applicationId) {
        logger.debug("Getting Global Roles");
        Iterable<TenantRole> roles = this.tenantRoleDao.getTenantRolesForUser(user, applicationId);
        return getGlobalRoles(roles, false);
    }

    @Override
    public List<TenantRole> getGlobalRolesForUserApplyRcnRoles(EndUser user, String applicationId) {
        logger.debug("Getting Global Roles (apply_rcn_roles=true)");
        Iterable<TenantRole> roles = this.tenantRoleDao.getTenantRolesForUser(user, applicationId);
        return getGlobalRoles(roles, true);
    }

    @Override
    public List<TenantRole> getEffectiveTenantRolesForUserOnTenant(EndUser user, Tenant tenant) {
        if (tenant == null) {
            throw new IllegalArgumentException(
                    "Tenant cannot be null.");
        }

        logger.debug(GETTING_TENANT_ROLES);

        // Original code didn't return fully populated roles (e.g. role names) so continue just returning minimal info
        List<TenantRole> allRoles = getEffectiveTenantRolesForUser(user);

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

        // Original code didn't return fully populated roles (e.g. role names) so continue just returning minimal info
        List<TenantRole> allRoles = getEffectiveTenantRolesForUserApplyRcnRoles(user);

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
    public List<TenantRole> getTenantRolesForUserApplyRcnRoles(BaseUser user) {
        return getEffectiveTenantRolesForUserApplyRcnRoles(user);
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
    public PaginatorContext<User> getPaginatedEffectiveEnabledUsersForTenant(String tenantId, int offset, int limit) {
        logger.debug("Getting Users for Tenant {}", tenantId);
        List<User> users = getEffectiveEnabledUsersForTenant(tenantId);

        // Sort the users by Id to provide for paging
        List<User> orderedUsers = sortUsersById(users);

        logger.debug("Got {} Users for Tenant {}", orderedUsers.size(), tenantId);
        PaginatorContext<User> pageContext = new PaginatorContext<User>();
        pageContext.update(orderedUsers, offset, limit);
        return pageContext;
    }

    private List<User> getEffectiveEnabledUsersForTenant(String tenantId) {
        List<User> users = new ArrayList<User>();
        Set<String> userIds = new HashSet<String>();

        Tenant tenant = getTenant(tenantId);

        // Retrieve explicitly assigned roles on tenant
        for (TenantRole role : this.tenantRoleDao.getAllTenantRolesForTenant(tenantId)) {
            // TODO: This is inefficient. Uses userId on role for comparison, then uses logic to get userId based on DN
            if (!userIds.contains(role.getUserId())) {
                String userId = tenantRoleDao.getUserIdForParent(role);
                if(!StringUtils.isBlank(userId)){
                    userIds.add(userId);
                }
            }
        }

        // Retrieve all the enabled users on the tenant's domain, if appropriate. Guaranteed to return a non-null list
        List<User> domainUsersOnTenant = getUsersForAutoRoleAssignmentOnTenant(tenant);
        /*
          Iterate through the auto assigned users, removing the userId from the list of users with a role explicitly
          assigned on the tenant to avoid looking up user needlessly
        */
        for (User domainUser : domainUsersOnTenant) {
            users.add(domainUser);
            userIds.remove(domainUser.getId());
        }

        // Look up any users from explicit assignment list that weren't already looked up due to auto assigned role
        for (String userId : userIds) {
            User user = this.userService.getUserById(userId);
            if (user != null && user.getEnabled()) {
                users.add(user);
            }
        }

        return users;
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
    List<TenantRole> getGlobalRoles(Iterable<TenantRole> roles, boolean applyRcnRoles) {
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        for (TenantRole role : roles) {
            if (role != null
                    && (role.getTenantIds() == null || role.getTenantIds().size() == 0)) {
                ClientRole cRole = this.applicationService.getClientRoleById(role.getRoleRsId());
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
     * Whether or not the specified tenant is eligible for auto role assignment based on feature flag and tenant
     * characteristics.
     *
     * @param tenant
     * @return
     */
    private boolean isAutoAssignmentOfRoleEnabledForTenantDomain(Tenant tenant) {
        return tenant != null && StringUtils.isNotBlank(tenant.getDomainId())
                && identityConfig.getReloadableConfig().isAutomaticallyAssignUserRoleOnDomainTenantsEnabled()
                && !tenant.getDomainId().equalsIgnoreCase(identityConfig.getReloadableConfig().getTenantDefaultDomainId());
    }

    /**
     * Returns the list of provisioned users which should receive the auto assigned role. Checks for eligibility for
     * auto-assignment first, and returns an empty list if not eligible.
     *
     * Only enabled users are eligible.
     *
     * TODO: Update to retrieve Fed users
     *
     * @param tenant
     * @return
     */
    private List<User> getUsersForAutoRoleAssignmentOnTenant(Tenant tenant) {
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

    @Override
    public PaginatorContext<User> getEnabledUsersWithEffectiveTenantRole(Tenant tenant, ClientRole cRole, int offset, int limit) {
        List<User> users = new ArrayList<User>();

        /*
         If automatic assignment of role is enabled AND the specified role to search is the implicit assignment role
         then include all the domain users.
        */
        String autoRoleName = identityConfig.getReloadableConfig().getAutomaticallyAssignUserRoleOnDomainTenantsRoleName();
        if (StringUtils.equalsIgnoreCase(autoRoleName, cRole.getName())) {
            users.addAll(getUsersForAutoRoleAssignmentOnTenant(tenant));
        }

        // Get list of users that are explicitly assigned this role on tenant. This could include users in other domains
        Set<String> userIds = new HashSet<String>();
        for (TenantRole role : this.tenantRoleDao.getAllTenantRolesForTenantAndRole(tenant.getTenantId(), cRole.getId())) {
            userIds.add(role.getUserId());
        }

        // Loop through domain users, removing from list of explicit to avoid looking up twice, and look up remaining
        if (CollectionUtils.isNotEmpty(userIds)) {
            for (User user : users) {
                userIds.remove(user.getId());
            }
            for (String userId : userIds) {
                User user = this.userService.getUserById(userId);
                if (user != null && user.getEnabled()) {
                    users.add(user);
                }
            }
        }

        // Sort the users by Id to provide for paging
        List<User> orderedUsers = sortUsersById(users);

        logger.debug("Got {} Users for Tenant {}", orderedUsers.size(),
                tenant.getTenantId());
        PaginatorContext<User> pageContext = new PaginatorContext<User>();
        pageContext.update(orderedUsers, offset, limit);
        return pageContext;
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
        //TODO: This should probably return an empty list as opposed to throwing an exception
        Domain domain = domainService.getDomain(domainId);
        if(domain.getTenantIds() == null || domain.getTenantIds().length == 0) {
            throw new NotFoundException(GlobalConstants.ERROR_MSG_NO_TENANTS_BELONG_TO_DOMAIN);
        }
        List<Tenant> tenantList = new ArrayList<Tenant>();
        for (String tenantId : domain.getTenantIds()){
            Tenant tenant = getTenant(tenantId);
            if(tenant != null){
                tenantList.add(tenant);
            }

        }
        return tenantList;
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
    public int getCountOfTenantRolesByRoleIdForProvisionedUsers(String roleId) {
        return tenantRoleDao.getCountOfTenantRolesByRoleIdForProvisionedUsers(roleId);
    }

    @Override
    public int getCountOfTenantRolesByRoleIdForFederatedUsers(String roleId) {
        return tenantRoleDao.getCountOfTenantRolesByRoleIdForFederatedUsers(roleId);
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

}
