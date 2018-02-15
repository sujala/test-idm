package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationAgreement;

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
     * Retrieves the specified delegation agreement (DA). The caller must be the principal of the DA to retrieve it.
     *
     * @param authToken
     * @param agreementId
     * @return
     */
    Response getAgreement(String authToken, String agreementId);

    /**
     * Deletes the specified delegation agreement (DA). The caller must be the principal of the DA to delete it.
     *
     * @param authToken
     * @param agreementId
     * @return
     */
    Response deleteAgreement(String authToken, String agreementId);
}
