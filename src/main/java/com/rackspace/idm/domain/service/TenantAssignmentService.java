package com.rackspace.idm.domain.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.exception.FailedGrantRoleAssignmentsException;

import java.util.List;

public interface TenantAssignmentService {

    /**
     * Assign the specified roles to the entity. The entity must extend calls UniqueId. The entities supplied to this
     * method (ex. user, userGroup) are meant to have ROLES container. If ROLES container does not exist it will be
     * created.
     *
     * Validation is performed on all roles prior to persisting any assignment to reduce the likelihood of failure.
     * If any assignment is deemed invalid during the initial validation, none will be saved. If an error is
     * encountered during saving, processing assignments will stop.
     *
     * @param entity
     * @param domainId
     * @param tenantAssignments
     * @param allowedRoleAccess
     *
     * @throws IllegalArgumentException if entity, entity.getUniqueId(), tenantAssignments, or allowedRoleAccess is null
     * @throws com.rackspace.idm.exception.BadRequestException If same role is repeated multiple times or assignment contains
     * invalid tenant set such as not specifying any, or containing '*' AND a set of tenants
     * @throws com.rackspace.idm.exception.NotFoundException If role or tenant is not found
     * @throws com.rackspace.idm.exception.ForbiddenException If role can not be assigned to the entity as specified
     *
     * @throws FailedGrantRoleAssignmentsException If error encountered
     * persisting the assignments post-validation
     * @return the tenant roles saved
     */
    List<TenantRole> replaceTenantAssignmentsOnEntityInDomain(UniqueId entity, String domainId, List<TenantAssignment> tenantAssignments, Integer allowedRoleAccess);
}
