package com.rackspace.idm.modules.usergroups.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.impl.DefaultFederatedIdentityService;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.modules.usergroups.Constants;
import com.rackspace.idm.modules.usergroups.api.resource.UserGroupRoleSearchParams;
import com.rackspace.idm.modules.usergroups.dao.UserGroupDao;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import com.rackspace.idm.modules.usergroups.exception.FailedGrantRoleAssignmentsException;
import com.rackspace.idm.validation.Validator20;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import java.util.*;

import static com.rackspace.idm.modules.usergroups.Constants.*;
import static com.rackspace.idm.validation.Validator20.MAX_LENGTH_255;
import static com.rackspace.idm.validation.Validator20.MAX_LENGTH_64;

@Component
public class DefaultUserGroupService implements UserGroupService {
    private static final Logger log = LoggerFactory.getLogger(DefaultFederatedIdentityService.class);

    public static final String GROUP_NOT_FOUND_ERROR_MESSAGE = "Group '%s' not found";

    @Autowired
    private TenantRoleDao tenantRoleDao;

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private UserGroupDao userGroupDao;

    @Autowired
    private Validator20 validator20;

    @Autowired
    private IdentityConfig identityConfig;

    @Override
    public UserGroup addGroup(UserGroup group) {
        Validate.notNull(group);
        Validate.notEmpty(group.getDomainId());

        Assert.isTrue(StringUtils.isNotBlank(group.getDomainId()));

        // Verify group requirements
        validateUserGroupForCreateAndUpdate(group);

        if (getGroupByNameForDomain(group.getName(), group.getDomainId()) != null) {
            throw new DuplicateException("Group already exists with this name in this domain");
        }

        // Validate there is room to create this group in the domain
        int numGroupsInDomain = userGroupDao.countGroupsInDomain(group.getDomainId());
        if (numGroupsInDomain >= identityConfig.getReloadableConfig().getMaxUsersGroupsPerDomain()) {
            throw new ForbiddenException(Constants.ERROR_CODE_USER_GROUPS_MAX_THRESHOLD_REACHED_MSG, Constants.ERROR_CODE_USER_GROUPS_MAX_THRESHOLD_REACHED);
        }

        userGroupDao.addGroup(group);

        return group;
    }

    public void validateUserGroupForCreateAndUpdate(UserGroup userGroup) {
        validator20.validateStringNotNullWithMaxLength("name", userGroup.getName(), MAX_LENGTH_64);
        validator20.validateStringMaxLength("description", userGroup.getDescription(), MAX_LENGTH_255);
    }

    @Override
    public UserGroup updateGroup(UserGroup group) {
        throw new NotImplementedException("This method has not yet been implemented");
    }

    @Override
    public void deleteGroup(UserGroup group) {
        userGroupDao.deleteGroup(group);
    }

    @Override
    public UserGroup getGroupById(String groupId) {
        Validate.notEmpty(groupId);

        return userGroupDao.getGroupById(groupId);
    }

    @Override
    public UserGroup checkAndGetGroupById(String groupId) {
        Validate.notEmpty(groupId);

        UserGroup group = getGroupById(groupId);
        if (group == null) {
            throw new NotFoundException(String.format(GROUP_NOT_FOUND_ERROR_MESSAGE, groupId));
        }
        return group;
    }

    @Override
    public UserGroup getGroupByIdForDomain(String groupId, String domainId) {
        Validate.notEmpty(domainId);
        Validate.notEmpty(groupId);

        UserGroup group = getGroupById(groupId);
        if (group != null && !domainId.equalsIgnoreCase(group.getDomainId())) {
            return null; // If group exists, but doesn't belong to domain, pretend it doesn't exist
        }
        return group;
    }

    @Override
    public UserGroup checkAndGetGroupByIdForDomain(String groupId, String domainId) {
        Validate.notEmpty(groupId);
        Validate.notEmpty(domainId);

        UserGroup group = getGroupByIdForDomain(groupId, domainId);
        if (group == null) {
            /*
             While technically the group may exist, just not in the specified domain, want the error message to be
             the same in both cases.
             */
            throw new NotFoundException(String.format(GROUP_NOT_FOUND_ERROR_MESSAGE, groupId));
        }
        return group;
    }

    @Override
    public UserGroup getGroupByNameForDomain(String groupName, String domainId) {
        Validate.notEmpty(groupName);
        Validate.notEmpty(domainId);

        return userGroupDao.getGroupByNameForDomain(groupName, domainId);
    }

    @Override
    public List<TenantRole> getRoleAssignmentsOnGroup(String userGroupId) {
        Assert.notNull(userGroupId);

        UserGroup group = getGroupById(userGroupId);

        List<TenantRole> assignedRoles = Collections.emptyList();
        if (group != null) {
            Iterable<TenantRole> rolesIt = tenantRoleDao.getRoleAssignmentsOnGroup(group);
            assignedRoles = IteratorUtils.toList(rolesIt.iterator());
            return assignedRoles;
        }
        return assignedRoles;
    }

    @Override
    public PaginatorContext<TenantRole> getRoleAssignmentsOnGroup(UserGroup userGroup, UserGroupRoleSearchParams userGroupRoleSearchParams) {
        Validate.notNull(userGroup);
        Validate.notNull(userGroupRoleSearchParams);
        Validate.notNull(userGroupRoleSearchParams.getPaginationRequest());

        return tenantRoleDao.getRoleAssignmentsOnGroup(userGroup, userGroupRoleSearchParams);
    }

    @Override
    public List<TenantRole> replaceRoleAssignmentsOnGroup(UserGroup userGroup, RoleAssignments roleAssignments) {
        Validate.notNull(userGroup);
        Validate.notNull(userGroup.getUniqueId());
        Validate.notNull(roleAssignments);

        if (roleAssignments.getTenantAssignments() == null || CollectionUtils.isEmpty(roleAssignments.getTenantAssignments().getTenantAssignment())) {
            return Collections.emptyList();
        }

        return replaceTenantAssignmentsOnGroup(userGroup, roleAssignments.getTenantAssignments().getTenantAssignment());
    }

    private List<TenantRole> replaceTenantAssignmentsOnGroup(UserGroup userGroup, List<TenantAssignment> tenantAssignments) {
        Validate.notNull(userGroup);
        Validate.notNull(userGroup.getUniqueId());
        Validate.notNull(tenantAssignments);

        // Iterate over all the roles to verify they're all valid before assigning any
        AssignmentCache assignmentCache = verifyRolesForAssignmentWithCache(userGroup, tenantAssignments);

        // Iterate over the assignments and create/modify the tenant role
        List<TenantRole> tenantRoles = new ArrayList<>(assignmentCache.roleCache.size());
        try {
            TenantRole changedRole;
            for (TenantAssignment tenantAssignment : tenantAssignments) {
                // See if existing assignment
                TenantRole existingAssignment = tenantRoleDao.getRoleAssignmentOnGroup(userGroup, tenantAssignment.getOnRole());

                if (existingAssignment == null) {
                    ClientRole role = assignmentCache.roleCache.get(tenantAssignment.getOnRole());

                    TenantRole tenantRole = new TenantRole();
                    tenantRole.setRoleRsId(tenantAssignment.getOnRole());
                    tenantRole.setClientId(role.getClientId());
                    tenantRole.setName(role.getName());
                    tenantRole.setDescription(role.getDescription());
                    tenantRole.setRoleType(role.getRoleType());

                    if (!isDomainAssignment(tenantAssignment)) {
                        tenantRole.setTenantIds(new HashSet<>(tenantAssignment.getForTenants()));
                    }
                    tenantRoleDao.addRoleAssignmentOnGroup(userGroup, tenantRole);
                    changedRole = tenantRole;
                } else {
                    existingAssignment.setTenantIds(new HashSet<>(tenantAssignment.getForTenants()));
                    tenantRoleDao.updateRoleAssignmentOnGroup(userGroup, existingAssignment);
                    changedRole = existingAssignment;
                }
                tenantRoles.add(changedRole);
            }
        } catch (Exception e) {
            log.error("Encountered error saving a tenant role on a group", e);
            throw new FailedGrantRoleAssignmentsException("Error assigning role(s)", tenantRoles, e);
        }

        return tenantRoles;
    }

    /**
     * Dual-purpose method - verify the role assignments while cacheing the lookups for use in the future.
     *
     * @param userGroup
     * @param tenantAssignments
     * @return
     */
    private AssignmentCache verifyRolesForAssignmentWithCache(UserGroup userGroup, List<TenantAssignment> tenantAssignments) {
        AssignmentCache cache = new AssignmentCache();

        // First validate no roles are duplicated
        Set<String> roles = new HashSet();
        for (TenantAssignment tenantAssignment : tenantAssignments) {
            if (roles.contains(tenantAssignment.getOnRole())) {
                throw new BadRequestException(Constants.ERROR_CODE_USER_GROUPS_DUP_ROLE_ASSIGNMENT_MSG, Constants.ERROR_CODE_USER_GROUPS_DUP_ROLE_ASSIGNMENT);
            }
            roles.add(tenantAssignment.getOnRole());
        }

        // Perform static analysis of each assignment
        for (TenantAssignment tenantAssignment : tenantAssignments) {
            verifyAssignmentToGroupFormat(userGroup, tenantAssignment);
        }

        // Perform in-depth analysis to verify requested backend data exists appropriately
        for (TenantAssignment tenantAssignment : tenantAssignments) {
            verifyAssignmentToGroupBackendWithCache(userGroup, tenantAssignment, cache);
        }

        return cache;
    }

    /**
     * Performs static analysis of the assignment w/o querying backend.
     * @param group
     * @param tenantAssignment
     */
    private void verifyAssignmentToGroupFormat(UserGroup group, TenantAssignment tenantAssignment) {
        boolean isDomainAssignment = isDomainAssignment(tenantAssignment);

        if (org.apache.commons.collections4.CollectionUtils.isEmpty(tenantAssignment.getForTenants())) {
            throw new BadRequestException(ERROR_CODE_ROLE_ASSIGNMENT_MISSING_FOR_TENANTS_MSG, ERROR_CODE_USER_GROUPS_MISSING_REQUIRED_ATTRIBUTE);
        }

        if (isDomainAssignment) {
            if (tenantAssignment.getForTenants().size() > 1) {
                throw new BadRequestException(ERROR_CODE_ROLE_ASSIGNMENT_INVALID_FOR_TENANTS_MSG, ERROR_CODE_USER_GROUPS_INVALID_ATTRIBUTE);
            }
        }
    }

        /**
         * Verifies the assignment from backend perspective, including that  that the specified roles and tenants exist,
         * etc. Populates the cache with retrieved roles/tenants.
         *
         * @param group
         * @param tenantAssignment
         * @param assignmentCache
         */
    private void verifyAssignmentToGroupBackendWithCache(UserGroup group, TenantAssignment tenantAssignment, AssignmentCache assignmentCache) {
        boolean isDomainAssignment = isDomainAssignment(tenantAssignment);

        String roleId = tenantAssignment.getOnRole();

        // For assignment, don't use cache to ensure using the absolute latest
        ClientRole role = applicationService.getClientRoleById(roleId);

        if (role == null) {
            throw new NotFoundException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_NONEXISTANT_ROLE_MSG_PATTERN, roleId), ERROR_CODE_USER_GROUPS_INVALID_ATTRIBUTE);
        } else if (isDomainAssignment && role.getAssignmentTypeAsEnum() == RoleAssignmentEnum.TENANT) {
            throw new ForbiddenException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_TENANT_ASSIGNMENT_ONLY_MSG_PATTERN, roleId), ERROR_CODE_USER_GROUPS_INVALID_ATTRIBUTE);
        } else if (!isDomainAssignment && role.getAssignmentTypeAsEnum() == RoleAssignmentEnum.GLOBAL) {
            throw new ForbiddenException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_GLOBAL_ROLE_ASSIGNMENT_ONLY_MSG_PATTERN, roleId), ERROR_CODE_USER_GROUPS_INVALID_ATTRIBUTE);
        } else if (role.getRsWeight() != Constants.USER_GROUP_ALLOWED_ROLE_WEIGHT) {
            throw new ForbiddenException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, roleId), ERROR_CODE_USER_GROUPS_INVALID_ATTRIBUTE);
        }

        // Add role to cache
        assignmentCache.roleCache.put(role.getId(), role);

        // Verify tenants
        if (!isDomainAssignment) {
            for (String tenantId : tenantAssignment.getForTenants()) {
                if (!assignmentCache.tenantCache.containsKey(tenantId)) {
                    Tenant foundTenant = tenantService.getTenant(tenantId);
                    if (foundTenant == null) {
                        throw new NotFoundException(String.format(Constants.ERROR_CODE_ROLE_ASSIGNMENT_NONEXISTANT_TENANT_MSG_PATTERN, roleId), ERROR_CODE_USER_GROUPS_INVALID_ATTRIBUTE);
                    } else if (!foundTenant.getDomainId().equalsIgnoreCase(group.getDomainId())) {
                        throw new ForbiddenException(String.format(Constants.ERROR_CODE_ROLE_ASSIGNMENT_WRONG_DOMAIN_TENANT_MSG_PATTERN, roleId), ERROR_CODE_USER_GROUPS_INVALID_ATTRIBUTE);
                    }
                    assignmentCache.tenantCache.put(tenantId, foundTenant);
                }
            }
        }
    }

    private class AssignmentCache {
        Map<String, Tenant> tenantCache = new HashMap<>();
        Map<String, ClientRole> roleCache = new HashMap<>();
    }

    private boolean isDomainAssignment(TenantAssignment tenantAssignment) {
        return tenantAssignment.getForTenants().contains(ALL_TENANT_IN_DOMAIN_WILDCARD);
    }

    public Iterable<UserGroup> getGroupsForDomain(String domainId) {
        Validate.notEmpty(domainId);

        return userGroupDao.getGroupsForDomain(domainId);
    }
}
