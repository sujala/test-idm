package com.rackspace.idm.domain.service.impl;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.dao.IdentityUserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.modules.endpointassignment.entity.Rule;
import com.rackspace.idm.modules.endpointassignment.service.RuleService;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import com.rackspace.idm.multifactor.service.MultiFactorService;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.*;

@Component
public class DefaultIdentityUserService implements IdentityUserService {

    @Autowired
    private IdentityUserDao identityUserRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private RuleService ruleService;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private IdentityProviderDao identityProviderDao;

    @Autowired
    private MultiFactorService multiFactorService;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Logger deleteUserLogger = LoggerFactory.getLogger(GlobalConstants.DELETE_USER_LOG_NAME);
    private static final String DELETE_USER_FORMAT = "DELETED username={},domainId={},roles={}";
    private static final String DELETE_FEDERATED_USER_FORMAT = "DELETED username={},federatedUri={},domainId={}";

    @Override
    public EndUser getEndUserById(String userId) {
        return identityUserRepository.getEndUserById(userId);
    }

    @Override
    public EndUser checkAndGetEndUserById(String userId) {
        EndUser user = getEndUserById(userId);

        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return user;
    }

    @Override
    public User getProvisionedUserById(String userId) {
        return identityUserRepository.getProvisionedUserById(userId);
    }


    @Override
    public User getProvisionedUserByIdWithPwdHis(String userId) {
        return identityUserRepository.getProvisionedUserByIdWithPwdHis(userId);
    }

    public FederatedUser getFederatedUserByUsernameAndIdentityProviderId(String username, String providerId) {
        return identityUserRepository.getFederatedUserByUsernameAndIdpId(username, providerId);
    }

    @Override
    public FederatedUser checkAndGetFederatedUserByUsernameAndIdentityProviderName(String username, String providerId) {
        FederatedUser user = getFederatedUserByUsernameAndIdentityProviderId(username, providerId);

        if (user == null) {
            String errMsg = String.format("Federated user %s not found for IDP with id %s", username, providerId);
            throw new NotFoundException(errMsg);
        }

        return user;
    }

    @Override
    public FederatedUser checkAndGetFederatedUserByUsernameAndIdentityProviderUri(String username, String idpUri) {
        IdentityProvider idp = identityProviderDao.getIdentityProviderByUri(idpUri);

        FederatedUser user = null;
        if(idp != null) {
            user = getFederatedUserByUsernameAndIdentityProviderId(username, idp.getProviderId());
        }

        if (user == null) {
            String errMsg = String.format("Federated user %s not found for IDP with URI %s", username, idpUri);
            throw new NotFoundException(errMsg);
        }

        return user;
    }

    @Override
    public FederatedUser getFederatedUserById(String userId) {
        return identityUserRepository.getFederatedUserById(userId);
    }

    @Override
    public Iterable<FederatedUser> getFederatedUsersByIdentityProviderId(String idpId) {
        return identityUserRepository.getFederatedUsersByIdentityProviderId(idpId);
    }

    @Override
    public Iterable<FederatedUser> getFederatedUsersNotInApprovedDomainIdsByIdentityProviderId(List<String> approvedDomainIds, String idpId) {
        return identityUserRepository.getFederatedUsersNotInApprovedDomainIdsByIdentityProviderId(approvedDomainIds, idpId);
    }

    @Override
    public int getFederatedUsersByDomainIdAndIdentityProviderNameCount(String domainId, String idpName) {
        return identityUserRepository.getFederatedUsersByDomainIdAndIdentityProviderIdCount(domainId, idpName);
    }

    @Override
    public int getUnexpiredFederatedUsersByDomainIdAndIdentityProviderNameCount(String domainId, String idpName) {
        return identityUserRepository.getUnexpiredFederatedUsersByDomainIdAndIdentityProviderIdCount(domainId, idpName);
    }

    @Override
    public Iterable<EndUser> getEndUsersByDomainId(String domainId) {
        return identityUserRepository.getEndUsersByDomainId(domainId);
    }

    @Override
    public Iterable<User> getProvisionedUsersByDomainId(String domainId) {
        return userService.getUsersWithDomain(domainId);
    }

    public Iterable<EndUser> getEndUsersByDomainIdAndEnabledFlag(String domainId, boolean enabled) {
        return identityUserRepository.getEndUsersByDomainIdAndEnabledFlag(domainId, enabled);
    }

    @Override
    public PaginatorContext<EndUser> getEndUsersByDomainIdPaged(String domainId, int offset, int limit) {
        return identityUserRepository.getEndUsersByDomainIdPaged(domainId, offset, limit);
    }

    @Override
    public PaginatorContext<EndUser> getEnabledEndUsersPaged(int offset, int limit) {
        return identityUserRepository.getEnabledEndUsersPaged(offset, limit);
    }

    @Override
    public Iterable<Group> getGroupsForEndUser(String userId) {
        return identityUserRepository.getGroupsForEndUser(userId);
    }

    @Override
    public EndUser checkAndGetUserById(String userId) {
        EndUser user = getEndUserById(userId);

        if (user == null) {
            String errMsg = String.format("User %s not found", userId);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        return user;
    }

    /**
     * Adds the group to the end user. After adding will send atom hopper event
     * @param groupId
     * @param endUserId
     *
     */
    @Override
    public void addGroupToEndUser(String groupId, String endUserId) {
        EndUser user = getEndUserById(endUserId);
        if (user != null && !user.getRsGroupId().contains(groupId)) {
            logger.debug("Adding groupId {} to user {}", groupId, endUserId);
            user.getRsGroupId().add(groupId);
            identityUserRepository.updateIdentityUser(user);
            logger.debug("Added groupId {} to user {}", groupId, endUserId);
        }
    }

    /**
     * Removes the group from the end user. After removing will send atom hopper event
     * @param groupId
     * @param endUserId
     *
     */
    @Override
    public void removeGroupFromEndUser(String groupId, String endUserId) {
        EndUser user = getEndUserById(endUserId);
        if (user != null && user.getRsGroupId().contains(groupId)) {
            logger.debug("Removing groupId {} from user {}", groupId, endUserId);
            user.getRsGroupId().remove(groupId);
            identityUserRepository.updateIdentityUser(user);
            logger.debug("Removed groupId {} from user {}", groupId, endUserId);
        }
    }

    @Override
    public Iterable<EndUser> getEnabledEndUsersByGroupId(String groupId) {
        logger.debug("Getting All Users: {} - {}", groupId);
        return identityUserRepository.getEnabledEndUsersByGroupId(groupId);
    }

    @Override
    public void deleteUser(BaseUser user) {
        if (user instanceof Racker) {
            //rackers are not persistent, so don't bother trying to delete
            return;
        }

        if (user instanceof FederatedUser) {
            deleteUserLogger.warn(DELETE_FEDERATED_USER_FORMAT,
                    new Object[] {user.getUsername(), ((FederatedUser)user).getFederatedIdpUri(), user.getDomainId()});
        } else if (user instanceof User){
            if (StringUtils.isNotBlank(((User)user).getExternalMultiFactorUserId())) {
                multiFactorService.removeMultiFactorForUser(user.getId());
            }

            List<TenantRole> roles = this.tenantService.getTenantRolesForUser(user);
            deleteUserLogger.warn(DELETE_USER_FORMAT,
                    new Object[] {user.getUsername(), user.getDomainId(), roles.toString()});
        }

        identityUserRepository.deleteIdentityUser(user);
    }

    @Override
    public void addGroupToUser(User baseUser, UserGroup group) {
        userService.addGroupToUser(baseUser, group);
    }

    @Override
    public void removeGroupFromUser(User baseUser, UserGroup group) {
        userService.removeGroupFromUser(baseUser, group);
    }

    @Override
    public int getUsersWithinRegionCount(String regionName) {
        return identityUserRepository.getUsersWithinRegionCount(regionName);
    }

    @Override
    public ServiceCatalogInfo getServiceCatalogInfo(BaseUser baseUser) {
        return getServiceCatalogInfoInternal(baseUser, false);
    }

    @Override
    public ServiceCatalogInfo getServiceCatalogInfoApplyRcnRoles(BaseUser baseUser) {
        return getServiceCatalogInfoInternal(baseUser, true);
    }


    private ServiceCatalogInfo getServiceCatalogInfoInternal(BaseUser baseUser, boolean applyRcnRoles) {
        if (baseUser == null || !(baseUser instanceof EndUser)) {
            /*
             Ideally this should probably throw an error rather than return an empty catalog for null users, but legacy
             seems to be empty catalog so maintain that for now. Only "EndUsers" (not Rackers) have service catalogs
             */
            return new ServiceCatalogInfo(Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, null);
        }

        EndUser user = (EndUser) baseUser;

        // Get the tenantRoles for the user
        List<TenantRole> tenantRoles;
        IdentityUserTypeEnum userTypeEnum;
        if (applyRcnRoles) {
            tenantRoles = tenantService.getTenantRolesForUserApplyRcnRoles(baseUser);

            // Determine the user type
            userTypeEnum = authorizationService.getIdentityTypeRoleAsEnum(tenantRoles);
            if (userTypeEnum == null) {
                /*
                 Chance of this happening if user exists in a domain without any tenants. Calling logic depends on user
                 type being included so fall back to looking up from scratch. This should only occur in rare circumstances
                 so additional impact should be minimal.
                  */
                logger.warn(String.format("Authenticating userId '%s' resulted in no user type role when applying RCN roles. User likely has no tenants", baseUser.getId()));
                userTypeEnum = authorizationService.getIdentityTypeRoleAsEnum(baseUser);
            }
        } else {
            tenantRoles = tenantService.getTenantRolesForUserPerformant(baseUser);
            userTypeEnum = authorizationService.getIdentityTypeRoleAsEnum(tenantRoles);
        }

        // Translate the tenantRoles to all the info necessary to determine endpoints
        List<TenantEndpointMeta> tenantMetas = generateTenantEndpointMetaForUser(user, tenantRoles);

        final Set<OpenstackEndpoint> endpoints = new HashSet<>();
        for (TenantEndpointMeta tenantMeta : tenantMetas) {
            final OpenstackEndpoint endpoint = endpointService.calculateOpenStackEndpointForTenantMeta(tenantMeta);
            if (endpoint != null && endpoint.getBaseUrls().size() > 0) {
                endpoints.add(endpoint);
            }
        }

        List<Tenant> tenants = Lists.transform(tenantMetas, new Function<TenantEndpointMeta, Tenant>() {
            @Nullable
            @Override
            public Tenant apply(@Nullable TenantEndpointMeta input) {
                return input.getTenant();
            }
        });

        return new ServiceCatalogInfo(tenantRoles,  tenants, new ArrayList<>(endpoints), userTypeEnum);
    }

    /**
     * Retries all the tenants
     * @param roles
     * @return
     */
    private List<TenantEndpointMeta> generateTenantEndpointMetaForUser(final EndUser user, Collection<TenantRole> roles) {
        List<TenantEndpointMeta> tenantEndpointMetas = new ArrayList<>();

        // Step 1: Map the tenantIds to roles
        Map<String, List<TenantRole>> tenantIdsToRolesMap = new HashMap<>();
        if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(roles)) {
            for (TenantRole role : roles) {
                if (org.apache.commons.collections4.CollectionUtils.isNotEmpty(role.getTenantIds())) {
                    // Add the role to each tenant for which it is assigned
                    for (String tenantId : role.getTenantIds()) {
                        List<TenantRole> roleMap = tenantIdsToRolesMap.get(tenantId);
                        if (roleMap == null) {
                            roleMap = new ArrayList<TenantRole>();
                            tenantIdsToRolesMap.put(tenantId, roleMap);
                        }
                        roleMap.add(role);
                    }
                }
            }
        }

        // Step 2: Lookup all rules if enabled
        List<Rule> rules = Collections.EMPTY_LIST;
        if (identityConfig.getReloadableConfig().includeEndpointsBasedOnRules()) {
            rules = Collections.unmodifiableList(ruleService.findAllEndpointAssignmentRules());
        }

        for (String tenantId : tenantIdsToRolesMap.keySet()) {
            // Step 3: Look up the tenant
            final Tenant tenant = tenantService.getTenant(tenantId);

            if (tenant != null) {
                // Step 4: Get rules that are applicable for this tenant/user
                List<Rule> tenantRules = new ArrayList<>();
                if (CollectionUtils.isNotEmpty(rules)) {
                    CollectionUtils.select(rules, new org.apache.commons.collections4.Predicate<Rule>() {
                                @Override
                                public boolean evaluate(Rule rule) {
                                    return rule != null && rule.matches(user, tenant);
                                }
                            }, tenantRules);
                }

                // Step 5: Create tenant meta
                TenantEndpointMeta meta = new TenantEndpointMeta(user, tenant, tenantIdsToRolesMap.get(tenant.getTenantId()), tenantRules);
                tenantEndpointMetas.add(meta);
                if (logger.isTraceEnabled()) {
                    logger.trace("Calculated TenantEndpointMeta: " + meta.getLogString());
                }
            } else {
                /*
                  If tenant doesn't exist, log error, but ignore. User's service catalog just won't contain anything
                  for this tenant
                 */
                logger.error(String.format("User with id '%s' is assigned a role on a tenant with id '%s', but the tenant does not exist", user.getId(), tenantId));
            }
        }

        return tenantEndpointMetas;
    }

}
