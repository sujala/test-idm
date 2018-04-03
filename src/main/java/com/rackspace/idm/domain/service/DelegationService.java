package com.rackspace.idm.domain.service;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;
import com.rackspace.idm.api.resource.cloud.v20.DelegateReference;
import com.rackspace.idm.api.resource.cloud.v20.DelegationAgreementRoleSearchParams;
import com.rackspace.idm.domain.entity.DelegationAgreement;
import com.rackspace.idm.domain.entity.DelegationDelegate;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.exception.FailedGrantRoleAssignmentsException;

import java.util.List;

public interface DelegationService {

    /**
     * Adds a new delegation agreement (DA). Service assumes the caller has verified the DA is valid. The id of the passed
     * in delegation agreement will be updated with the unique id when saving. Any existing value in the field will
     * be overwritten.
     *
     * The returned DA may, or may not, be the same DA instance as passed in.
     *
     * @param delegationAgreement
     * @return
     */
    DelegationAgreement addDelegationAgreement(DelegationAgreement delegationAgreement);

    /**
     * Deletes the specified delegation agreement
     *
     * @param delegationAgreement
     */
    void deleteDelegationAgreement(DelegationAgreement delegationAgreement);

    /**
     * Retrieves the delegation agreement with the specified Id
     *
     * @param delegationAgreementId
     * @return
     */
    DelegationAgreement getDelegationAgreementById(String delegationAgreementId);

    /**
     * Retrieve the specified role on the delegation agreement (DA), or null if the role does not exist on the DA.
     *
     * @param delegationAgreement
     * @param roleId
     * @return
     */
    TenantRole getRoleAssignmentOnDelegationAgreement(DelegationAgreement delegationAgreement, String roleId);

    /**
     * Retrieve the set of roles assignments on the delegation agreement that match the specified criteria. If no roles match,
     * a context with an empty list of results will be returned.
     *
     * @param delegationAgreement
     * @param searchParams
     *
     * @throws IllegalArgumentException if delegationAgreement, or searchParams is null
     *
     * @return
     */
    PaginatorContext<TenantRole> getRoleAssignmentsOnDelegationAgreement(DelegationAgreement delegationAgreement, DelegationAgreementRoleSearchParams searchParams);

    /**
     * Assign the specified roles to a delegation agreement (DA). Validation is performed on all roles prior to persisting
     * any assignment to reduce the likelihood of failure. If any assignment is deemed invalid during the initial
     * validation, none will be saved. If an error is encountered during saving, processing assignments will stop.
     *
     * @param delegationAgreement
     * @param roleAssignments
     *
     * @throws IllegalArgumentException if delegationAgreement, delegationAgreement.getUniqueId(), or tenantAssignments is null
     * @throws com.rackspace.idm.exception.BadRequestException If same role is repeated multiple times or assignment contains
     * invalid tenant set such as not specifying any, or containing '*' AND a set of tenants
     * @throws com.rackspace.idm.exception.NotFoundException If role or tenant is not found
     * @throws com.rackspace.idm.exception.ForbiddenException If role can not be assigned to the DA as specified
     *
     * @throws FailedGrantRoleAssignmentsException If error encountered
     * persisting the assignments post-validation
     * @return the tenant roles saved
     */
    List<TenantRole> replaceRoleAssignmentsOnDelegationAgreement(DelegationAgreement delegationAgreement, RoleAssignments roleAssignments);

    /**
     * Removes the specified role from the delegation agreement (DA) regardless of whether it is assigned as a domain or
     * tenant role.
     *
     * @param delegationAgreement
     * @param roleId
     * @throws IllegalArgumentException if DA, DA's uniqueId, or roleId is null
     * @throws com.rackspace.idm.exception.NotFoundException If role is not assigned to DA
     */
    void revokeRoleAssignmentOnDelegationAgreement(DelegationAgreement delegationAgreement, String roleId);

    /**
     * Given a reference to a delegate, look up the delegate.
     *
     * @param delegateReference
     * @return
     */
    DelegationDelegate getDelegateByReference(DelegateReference delegateReference);

    /**
     * Adds the delegate to the delegation agreement. This service does not validate that the delegate is appropriate
     * for the delegation agreement. Such checks need to be made prior to calling this service.
     *
     * The passed in delegation agreement will be modified to reflect the change and used to update the backend. As
     * such, no changes should be made to the passed in DA by the caller.
     *
     * @param delegationAgreement
     * @param delegate
     */
    void addDelegate(DelegationAgreement delegationAgreement, DelegationDelegate delegate);

    /**
     * Deletes the specified delegate from the delegation agreement if it exists. Returns true if the delegate existed
     * on the DA, false otherwise.
     *
     * The passed in delegation agreement will be modified to reflect the change and used to update the backend. As
     * such, no changes should be made to the passed in DA by the caller.
     *
     * @param delegationAgreement
     * @param delegateReference
     */
    boolean deleteDelegate(DelegationAgreement delegationAgreement, DelegateReference delegateReference);
}
