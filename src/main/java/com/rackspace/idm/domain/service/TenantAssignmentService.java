package com.rackspace.idm.domain.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment;
import com.rackspace.idm.domain.entity.DelegationAgreement;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.exception.FailedGrantRoleAssignmentsException;
import com.rackspace.idm.modules.usergroups.entity.UserGroup;

import java.util.List;

public interface TenantAssignmentService {

    /**
     * Assign the specified roles to the user.
     *
     * Validation is performed on all roles prior to persisting any assignment to reduce the likelihood of failure.
     * If any assignment is deemed invalid during the initial validation, none will be saved. If an error is
     * encountered during saving, processing assignments will stop.
     *
     * @param user
     * @param tenantAssignments
     * @param allowedRoleAccess
     *
     * @throws IllegalArgumentException if user, user.getUniqueId(), tenantAssignments, or allowedRoleAccess is null
     * @throws com.rackspace.idm.exception.BadRequestException If same role is repeated multiple times or assignment contains
     * invalid tenant set such as not specifying any, or containing '*' AND a set of tenants
     * @throws com.rackspace.idm.exception.NotFoundException If role or tenant is not found
     * @throws com.rackspace.idm.exception.ForbiddenException If role can not be assigned to the entity as specified
     *
     * @throws FailedGrantRoleAssignmentsException If error encountered
     * persisting the assignments post-validation
     * @return the tenant roles saved
     */
    List<TenantRole> replaceTenantAssignmentsOnUser(User user, List<TenantAssignment> tenantAssignments, Integer allowedRoleAccess);

    /**
     * Assign the specified roles to the userGroup.
     *
     * Validation is performed on all roles prior to persisting any assignment to reduce the likelihood of failure.
     * If any assignment is deemed invalid during the initial validation, none will be saved. If an error is
     * encountered during saving, processing assignments will stop.
     *
     * @param userGroup
     * @param tenantAssignments
     *
     * @throws IllegalArgumentException if userGroup, userGroup.getUniqueId(), or tenantAssignments is null
     * @throws com.rackspace.idm.exception.BadRequestException If same role is repeated multiple times or assignment contains
     * invalid tenant set such as not specifying any, or containing '*' AND a set of tenants
     * @throws com.rackspace.idm.exception.NotFoundException If role or tenant is not found
     * @throws com.rackspace.idm.exception.ForbiddenException If role can not be assigned to the entity as specified
     *
     * @throws FailedGrantRoleAssignmentsException If error encountered
     * persisting the assignments post-validation
     * @return the tenant roles saved
     */
    List<TenantRole> replaceTenantAssignmentsOnUserGroup(UserGroup userGroup, List<TenantAssignment> tenantAssignments);

    /**
     * Assign the specified roles to a delegation agreement.
     *
     * Validation is performed on all roles prior to persisting any assignment to reduce the likelihood of failure.
     * If any assignment is deemed invalid during the initial validation, none will be saved. If an error is
     * encountered during saving, processing assignments will stop.
     *
     * @param delegationAgreement
     * @param tenantAssignments
     *
     * @throws IllegalArgumentException if delegationAgreement, or tenantAssignments is null
     * @throws com.rackspace.idm.exception.BadRequestException If same role is repeated multiple times or assignment contains
     * invalid tenant set such as not specifying any, or containing '*' AND a set of tenants
     * @throws com.rackspace.idm.exception.NotFoundException If role or tenant is not found
     * @throws com.rackspace.idm.exception.ForbiddenException If role can not be assigned to the entity as specified
     *
     * @throws FailedGrantRoleAssignmentsException If error encountered
     * persisting the assignments post-validation
     * @return the tenant roles saved
     */
    List<TenantRole> replaceTenantAssignmentsOnDelegationAgreement(DelegationAgreement delegationAgreement, List<TenantAssignment> tenantAssignments);
}
