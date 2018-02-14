package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import com.rackspace.idm.domain.service.TenantAssignmentService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.FailedGrantRoleAssignmentsException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotFoundException;
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

    @Autowired
    private ApplicationService applicationService;

    @Autowired
    private TenantRoleDao tenantRoleDao;

    @Autowired
    private TenantService tenantService;

    @Override
    public List<TenantRole> replaceTenantAssignmentsOnEntityInDomain(UniqueId entity, String domainId, List<TenantAssignment> tenantAssignments, Integer allowedRoleAccess) {
        Validate.notNull(entity);
        Validate.notNull(entity.getUniqueId());
        Validate.notNull(tenantAssignments);
        Validate.notNull(allowedRoleAccess);

        // Iterate over all the roles to verify they're all valid before assigning any
        AssignmentCache assignmentCache = verifyRolesForAssignmentWithCache(domainId, tenantAssignments, allowedRoleAccess);

        // Iterate over the assignments and create/modify the tenant role
        List<TenantRole> tenantRoles = new ArrayList<>(assignmentCache.roleCache.size());
        try {
            TenantRole changedRole;
            for (TenantAssignment tenantAssignment : tenantAssignments) {
                // See if existing assignment
                TenantRole existingAssignment = tenantRoleDao.getRoleAssignmentOnEntity(entity, tenantAssignment.getOnRole());

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
                    tenantRoleDao.addRoleAssignmentOnEntity(entity, tenantRole);
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
            log.error("Encountered error saving a tenant role on a entity", e);
            throw new FailedGrantRoleAssignmentsException("Error assigning role(s)", tenantRoles, e);
        }

        return tenantRoles;
    }

    /**
     * Dual-purpose method - verify the role assignments while caching the lookups for use in the future.
     *
     * @param domainId
     * @param tenantAssignments
     * @param allowedRoleWeight
     * @return
     */
    private AssignmentCache verifyRolesForAssignmentWithCache(String domainId, List<TenantAssignment> tenantAssignments, Integer allowedRoleWeight) {
        AssignmentCache cache = new AssignmentCache();

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

        // Perform in-depth analysis to verify requested backend data exists appropriately
        for (TenantAssignment tenantAssignment : tenantAssignments) {
            verifyAssignmentToDomainBackendWithCache(domainId, tenantAssignment, cache, allowedRoleWeight);
        }

        return cache;
    }

    /**
     * Performs static analysis of the assignment w/o querying backend.
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

    /**
     * Verifies the assignment from backend perspective, including that the specified roles and tenants exist,
     * etc. Role assignments must also be accessible. Populates the cache with retrieved roles/tenants.
     *
     * @param domainId
     * @param tenantAssignment
     * @param assignmentCache
     * @param allowedRoleWeight
     */
    private void verifyAssignmentToDomainBackendWithCache(String domainId, TenantAssignment tenantAssignment, AssignmentCache assignmentCache, Integer allowedRoleWeight) {
        boolean isDomainAssignment = isDomainAssignment(tenantAssignment);

        String roleId = tenantAssignment.getOnRole();

        // For assignment, don't use cache to ensure using the absolute latest
        ClientRole role = applicationService.getClientRoleById(roleId);

        if (role == null) {
            throw new NotFoundException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_NONEXISTANT_ROLE_MSG_PATTERN, roleId), ERROR_CODE_INVALID_ATTRIBUTE);
        } else if (isDomainAssignment && role.getAssignmentTypeAsEnum() == RoleAssignmentEnum.TENANT) {
            throw new ForbiddenException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_TENANT_ASSIGNMENT_ONLY_MSG_PATTERN, roleId), ERROR_CODE_INVALID_ATTRIBUTE);
        } else if (!isDomainAssignment && role.getAssignmentTypeAsEnum() == RoleAssignmentEnum.GLOBAL) {
            throw new ForbiddenException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_GLOBAL_ROLE_ASSIGNMENT_ONLY_MSG_PATTERN, roleId), ERROR_CODE_INVALID_ATTRIBUTE);
        } else if (IdentityUserTypeEnum.fromRoleName(role.getName()) != null || role.getRsWeight() < allowedRoleWeight) {
            // Do not allow assigning identity user type roles or roles not accessible to caller.
            throw new ForbiddenException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_FORBIDDEN_ASSIGNMENT_MSG_PATTERN, roleId), ERROR_CODE_INVALID_ATTRIBUTE);
        }

        // Add role to cache
        assignmentCache.roleCache.put(role.getId(), role);

        // Verify tenants
        if (!isDomainAssignment) {
            for (String tenantId : tenantAssignment.getForTenants()) {
                if (!assignmentCache.tenantCache.containsKey(tenantId)) {
                    Tenant foundTenant = tenantService.getTenant(tenantId);
                    if (foundTenant == null) {
                        throw new NotFoundException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_NONEXISTANT_TENANT_MSG_PATTERN, roleId), ERROR_CODE_INVALID_ATTRIBUTE);
                    } else if (!foundTenant.getDomainId().equalsIgnoreCase(domainId)) {
                        throw new ForbiddenException(String.format(ERROR_CODE_ROLE_ASSIGNMENT_WRONG_DOMAIN_TENANT_MSG_PATTERN, roleId, domainId), ERROR_CODE_INVALID_ATTRIBUTE);
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
}
