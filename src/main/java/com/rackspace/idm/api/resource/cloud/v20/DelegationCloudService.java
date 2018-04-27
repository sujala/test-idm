package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

public interface DelegationCloudService {

    /**
     * Adds the specified delegation agreement (DA). Currently, various temporary restrictions are in place on creating
     * DAs. These will change over the course of the next few iterations.
     *
     * Current Requirements
     * <ul>
     *     <li>Name: Must be specified and <= 32 characters</li>
     *     <li>description: Optional. If provided, must be <=255 characters</li>
     *     <li>PrincipalType/PrincipalId/Domain: The principal info should not be set. It will automatically be set to the caller of this service. The caller MUST
     *          exist within an RCN which is enabled for creating DAs, or, if the caller's domain is not in an RCN, the feature
     *          must be globally enabled for all RCNs (which implies the "empty" RCN)</li>
     *     <li>Delegate: A delegate must be specified. The delegate must be within the same RCN as the caller, or, if the caller is not
     *          within an RCN (and DAs are globally enabled), then the delegate must be within the same domain as the caller.
     *     <li>id: Should not be set. A unique value will be automatically set</li>
     *     <li>parentDelegationAgreementId: Should not be set. Any value is ignored.</li>
     * </ul>
     *
     * @param uriInfo
     * @param authToken
     * @param agreement
     * @return
     */
    Response addAgreement(UriInfo uriInfo, String authToken, DelegationAgreement agreement);

    /**
     * Update a delegation agreement (DA). Only name, description, and  allowSubAgreements fields are allowed to be
     * updated on a DA.
     *
     * On success returns:
     * <ol>
     *     <li>200 response</li>
     *     <li>The updated delegation agreement in the response body</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not an Identity Admin</li>
     *     <li>404 - If the DA does not exist</li>
     *     <li>400 - If the DA changes do not meet validation requirements
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param agreement
     * @return
     */
    Response updateAgreement(String authToken, DelegationAgreement agreement);

    /**
     * Retrieves the specified delegation agreement (DA). The caller must be the principal of the DA to retrieve it.
     *
     * @param authToken
     * @param agreementId
     * @return
     */
    Response getAgreement(String authToken, String agreementId);

    /**
     * Retrieves the delegation agreements (DA) associated with the caller with the specified relationship. Possible
     * values (case insensitive):
     * <ul>
     *     <li>delegate</li>
     *     <li>principal</li>
     * </ul>
     *
     * A null or blank value for relationship implies both. Any other value must result in a BadRequestException and
     * return a 400 response code.
     *
     * @param authToken
     * @param relationshipType
     * @return
     */
    Response listAgreements(String authToken, String relationshipType);

    /**
     * Deletes the specified delegation agreement (DA). The caller must be the principal of the DA to delete it.
     *
     * @param authToken
     * @param agreementId
     * @return
     */
    Response deleteAgreement(String authToken, String agreementId);

    /**
     * Adds a delegate to an existing delegation agreement.
     *
     * @param authToken
     * @param agreementId
     * @param delegateReference
     * @return
     */
    Response addDelegate(String authToken, String agreementId, DelegateReference delegateReference);

    /**
     * Deletes a delegate from an existing delegation agreement
     *
     * @param authToken
     * @param agreementId
     * @param delegateReference
     * @return
     */
    Response deleteDelegate(String authToken, String agreementId, DelegateReference delegateReference);

    /**
     * Lists the delegates of an existing delegation agreement
     *
     * @param authToken
     * @param agreementId
     * @return
     */
    Response listDelegates(String authToken, String agreementId);


    /**
     * Grant or update the role assignments on a delegation agreement (DA). If any role is currently assigned to the DA
     * it is replaced with the provided one. A given role can only appear once in the list of roles to assign. The same
     * constraints apply for each individual role assignment specified as if they were being assigned individually. The
     * entire request will be validated prior to assigning any roles.
     *
     * If the request is deemed valid, the assignments are iterated over to apply. If an error is encountered,
     * processing will stop on the current assignment, but no efforts will be made to rollback previously successful
     * assignments. Upon receiving an error the caller should verify the state of the user roles and take corrective
     * action as necessary.
     *
     * On success returns:
     * <ol>
     *     <li>A 200 response</li>
     *     <li>The response body is the final role assignments associated with the DA after applying the updates.</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not allowed to modify target DA</li>
     *     <li>404 - If the DA does not exist</li>
     *     <li>400 - If the request does not meet validation requirements.</li>
     *     <li>403 - If role can not be assigned to the DA</li>
     *     <li>403 - If adding an identity user type role</li>
     *     <li>500 - Catch all for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param agreementId
     * @param roleAssignments
     * @return
     */
    Response grantRolesToAgreement(String authToken, String agreementId, RoleAssignments roleAssignments);

    /**
     * Returns an array of RoleAssignments on the specified delegation agreement (DA).
     *
     * On success returns:
     * <ol>
     *     <li>A 200 response</li>
     *     <li>An array of RoleAssignment entries.</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not allowed to list role on DA.</li>
     *     <li>404 - If the DA does not exist</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param agreementId
     * @param searchParams
     * @return
     */
    Response listRoleAssignmentsOnAgreement(UriInfo uriInfo, String authToken, String agreementId, DelegationAgreementRoleSearchParams searchParams);

    /**
     * Remove the assignment of the specified role from the delegation agreement (DA). This service removes the entire
     * assignment regardless of whether it was assigned for all tenants or a subset of tenants with in a domain.
     *
     * On success returns:
     * <ol>
     *     <li>A 204 response</li>
     *     <li>No response body</li>
     * </ol>
     *
     * On failure will return appropriate v2 error responses:
     * <ol>
     *     <li>401 - If the supplied token is not a valid token or expired</li>
     *     <li>403 - If the caller is not allowed to remove role from DA</li>
     *     <li>404 - If the DA does not exist</li>
     *     <li>404 - If the role is not assigned to DA</li>
     *     <li>500 - Catchall for any other exception thrown by implementation</li>
     * </ol>
     *
     * @param authToken
     * @param agreementId
     * @param roleId
     * @return
     */
    Response revokeRoleFromAgreement(String authToken, String agreementId, String roleId);
}
