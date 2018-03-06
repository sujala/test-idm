package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.FailedGrantRoleAssignmentsException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.modules.usergroups.Constants;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

import static com.rackspace.idm.ErrorCodes.*;

@Component
public class DefaultTenantAssignmentService implements TenantAssignmentService {

    private static final Logger log = LoggerFactory.getLogger(DefaultTenantAssignmentService.class);

    public static final String ALL_TENANTS_IN_DOMAIN_WILDCARD = "*";

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private TenantRoleDao tenantRoleDao;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private AuthorizationService authorizationService;

    @Override
    public List<TenantRole> replaceTenantAssignmentsOnUser(User user, List<TenantAssignment> tenantAssignments, Integer allowedRoleAccess) {
        Validate.notNull(user);
        Validate.notNull(user.getUniqueId());
        Validate.notNull(tenantAssignments);
        Validate.notNull(allowedRoleAccess);

        verifyTenantAssignments(tenantAssignments);

        // Iterate over all the roles to verify they're all valid before assigning any
        AssignmentCache assignmentCache = verifyTenantAssignmentsWithCacheForUser(user, tenantAssignments, allowedRoleAccess);

        // Iterate over the assignments and create/modify the tenant role
        List<TenantRole> tenantRoles = new ArrayList<>(assignmentCache.roleCache.size());
        try {
            TenantRole changedRole;
            for (TenantAssignment tenantAssignment : tenantAssignments) {
                // See if existing assignment
                TenantRole existingAssignment = tenantRoleDao.getTenantRoleForUser(user, tenantAssignment.getOnRole());

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
                    tenantRoleDao.addTenantRoleToUser(user, tenantRole);
                    changedRole = tenantRole;
                } else {
                    if (isDomainAssignment(tenantAssignment)) {
                        existingAssignment.setTenantIds(new HashSet<String>());
                    } else {
                        existingAssignment.setTenantIds(new HashSet<>(tenantAssignment.getForTenants()));
                    }
                    tenantRoleDao.updateTenantRole(existingAssignment);
                    changedRole = existingAssignment;
                }
                tenantRoles.add(changedRole);
            }
        } catch (Exception e) {
            log.error("Encountered error saving a tenant role on a user", e);
            throw new FailedGrantRoleAssignmentsException("Error assigning role(s)", tenantRoles, e);
        }

        return tenantRoles;
    }

    @Override
    public List<TenantRole> replaceTenantAssignmentsOnUserGroup(UserGroup userGroup, List<TenantAssignment> tenantAssignments) {
        Validate.notNull(userGroup);
        Validate.notNull(userGroup.getUniqueId());
        Validate.notNull(tenantAssignments);

        verifyTenantAssignments(tenantAssignments);

        // Iterate over all the roles to verify they're all valid before assigning any
        AssignmentCache assignmentCache = verifyTenantAssignmentsWithCacheForUserGroup(userGroup, tenantAssignments);

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
                    if (isDomainAssignment(tenantAssignment)) {
                        existingAssignment.setTenantIds(new HashSet<String>());
                    } else {
                        existingAssignment.setTenantIds(new HashSet<>(tenantAssignment.getForTenants()));
                    }
                    tenantRoleDao.updateTenantRole(existingAssignment);
                    changedRole = existingAssignment;
                }
                tenantRoles.add(changedRole);
            }
        } catch (Exception e) {
            log.error("Encountered error saving a tenant role on a userGroup", e);
            throw new FailedGrantRoleAssignmentsException("Error assigning role(s)", tenantRoles, e);
        }

        return tenantRoles;
    }

    /**
     * Verify the tenant assignments.
     *
     * @param tenantAssignments
     * @return
     */
    private void verifyTenantAssignments(List<TenantAssignment> tenantAssignments) {
        // First validate no roles are duplicated
        Set<String> roles = new HashSet();
        for (TenantAssignment tenantAssignment : tenantAssignments) {
            if (roles.contains(tenantAssignment.getOnRole())) {
                throw new BadRequestException(ERROR_CODE_DUP_ROLE_ASSIGNMENT_MSG, ERROR_CODE_DUP_ROLE_ASSIGNMENT);
            }
            roles.add(tenantAssignment.getOnRole());
        }

        // Perform static analysis of each assignment
        for (TenantAssignment tenantAssignment : tenantAssignments) {
            verifyAssignmentFormat(tenantAssignment);
        }
    }

    /**
     * Verify the role assignments for user while caching the lookups for use in the future.
     *
     * @param user
     * @param tenantAssignments
     * @param allowedRoleAccess
     * @return
     */
    private AssignmentCache verifyTenantAssignmentsWithCacheForUser(User user, List<TenantAssignment> tenantAssignments, Integer allowedRoleAccess) {
        AssignmentCache cache = new AssignmentCache();

        // Perform in-depth analysis to verify requested backend data exists appropriately
        for (TenantAssignment tenantAssignment : tenantAssignments) {
            verifyTenantAssignmentWithCache(tenantAssignment, user.getDomainId(), cache);

            // Validate role being added to user
            String roleId = tenantAssignment.getOnRole();
            ClientRole role = cache.roleCache.get(roleId);

            if (role.getRsWeight() < allowedRoleAccess) {
                throw new ForbiddenException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, roleId), ERROR_CODE_INVALID_ATTRIBUTE);
            }

            // Identity user type roles can not be assigned to users via assignments, except for the
            // "identity:user-manage" role.
            if (IdentityUserTypeEnum.isIdentityUserTypeRoleName(role.getName())) {
                if (IdentityUserTypeEnum.fromRoleName(role.getName()) != IdentityUserTypeEnum.USER_MANAGER) {
                    throw new ForbiddenException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, roleId), ERROR_CODE_INVALID_ATTRIBUTE);
                }
                // Verify that target user has the "identity:default" role.
                if(!authorizationService.hasDefaultUserRole(user)) {
                    throw new ForbiddenException(ERROR_CODE_USER_MANAGE_ON_NON_DEFAULT_USER_MSG, ERROR_CODE_USER_MANAGE_ON_NON_DEFAULT_USER);
                }
                // Verify that the "identity:user-manage" role is assigned globally.
                if (!isDomainAssignment(tenantAssignment)) {
                    throw new ForbiddenException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_GLOBAL_ROLE_ASSIGNMENT_ONLY_MSG_PATTERN, tenantAssignment.getOnRole()), ERROR_CODE_INVALID_ATTRIBUTE);
                }
            }
        }

        return cache;
    }

    /**
     * Verify the role assignments for userGroups while caching the lookups for use in the future.
     *
     * @param userGroup
     * @param tenantAssignments
     * @return
     */
    private AssignmentCache verifyTenantAssignmentsWithCacheForUserGroup(UserGroup userGroup, List<TenantAssignment> tenantAssignments) {
        AssignmentCache cache = new AssignmentCache();

        // Perform in-depth analysis to verify requested backend data exists appropriately
        for (TenantAssignment tenantAssignment : tenantAssignments) {
            verifyTenantAssignmentWithCache(tenantAssignment, userGroup.getDomainId(), cache);

            // Validate role being added to userGroup
            String roleId = tenantAssignment.getOnRole();
            if (cache.roleCache.get(roleId).getRsWeight() != Constants.USER_GROUP_ALLOWED_ROLE_WEIGHT) {
                throw new ForbiddenException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, roleId), ERROR_CODE_INVALID_ATTRIBUTE);
            }
        }

        return cache;
    }

    /**
     * Dual-purpose method - verify the tenant assignment while caching the lookups for future use.
     *
     * @param tenantAssignment
     * @param domainId
     * @param cache
     * @return
     */
    private void verifyTenantAssignmentWithCache(TenantAssignment tenantAssignment, String domainId, AssignmentCache cache) {
        String roleId = tenantAssignment.getOnRole();
        ClientRole role = applicationService.getClientRoleById(roleId);

        // Validate role being added
        boolean isDomainAssignment = isDomainAssignment(tenantAssignment);
        if (role == null) {
            throw new NotFoundException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_NONEXISTANT_ROLE_MSG_PATTERN, roleId), ERROR_CODE_INVALID_ATTRIBUTE);
        } else if (isDomainAssignment && role.getAssignmentTypeAsEnum() == RoleAssignmentEnum.TENANT) {
            throw new ForbiddenException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_TENANT_ASSIGNMENT_ONLY_MSG_PATTERN, roleId), ERROR_CODE_INVALID_ATTRIBUTE);
        } else if (!isDomainAssignment && role.getAssignmentTypeAsEnum() == RoleAssignmentEnum.GLOBAL) {
            throw new ForbiddenException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_GLOBAL_ROLE_ASSIGNMENT_ONLY_MSG_PATTERN, roleId), ERROR_CODE_INVALID_ATTRIBUTE);
        }

        // Add role to cache
        cache.roleCache.put(role.getId(), role);

        // Verify tenants
        if (!isDomainAssignment) {
            for (String tenantId : tenantAssignment.getForTenants()) {
                if (!cache.tenantCache.containsKey(tenantId)) {
                    Tenant foundTenant = tenantService.getTenant(tenantId);
                    if (foundTenant == null) {
                        throw new NotFoundException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_NONEXISTANT_TENANT_MSG_PATTERN, tenantAssignment.getOnRole()), ERROR_CODE_INVALID_ATTRIBUTE);
                    } else if (!foundTenant.getDomainId().equalsIgnoreCase(domainId)) {
                        throw new ForbiddenException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_WRONG_DOMAIN_TENANT_MSG_PATTERN, tenantAssignment.getOnRole(), domainId), ERROR_CODE_INVALID_ATTRIBUTE);
                    }
                    cache.tenantCache.put(tenantId, foundTenant);
                }
            }
        }
    }

    /**
     * Performs static analysis of the assignment w/o querying backend.
     *
     * @param tenantAssignment
     */
    private void verifyAssignmentFormat(TenantAssignment tenantAssignment) {
        boolean isDomainAssignment = isDomainAssignment(tenantAssignment);

        if (org.apache.commons.collections4.CollectionUtils.isEmpty(tenantAssignment.getForTenants())) {
            throw new BadRequestException(ERROR_CODE_ROLE_ASSIGNMENT_MISSING_FOR_TENANTS_MSG, ERROR_CODE_REQUIRED_ATTRIBUTE);
        }

        // Iterate over all the tenantIds supplied to ensure they are not blank
        for (String tenantId : tenantAssignment.getForTenants()) {
            if (StringUtils.isBlank(tenantId)) {
                throw new BadRequestException(ERROR_CODE_ROLE_ASSIGNMENT_INVALID_FOR_TENANTS_MSG, ERROR_CODE_INVALID_ATTRIBUTE);
            }
        }

        if (isDomainAssignment) {
            if (tenantAssignment.getForTenants().size() > 1) {
                throw new BadRequestException(ERROR_CODE_ROLE_ASSIGNMENT_INVALID_FOR_TENANTS_MSG, ERROR_CODE_INVALID_ATTRIBUTE);
            }
        }
    }

    private class AssignmentCache {
        Map<String, Tenant> tenantCache = new HashMap<>();
        Map<String, ClientRole> roleCache = new HashMap<>();
    }

    private boolean isDomainAssignment(TenantAssignment tenantAssignment) {
        return tenantAssignment.getForTenants().contains(ALL_TENANTS_IN_DOMAIN_WILDCARD);
    }
}
