package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.DelegationDelegate;
import com.rackspace.idm.domain.entity.DelegationPrincipal;
import com.rackspace.idm.domain.entity.Domain;
import lombok.Getter;

import java.util.Set;

/**
 * The various search params allowed for the list delegation agreements for user service. Either a delegate reference
 * or principal reference must be provided.
 */
@Getter
public class FindDelegationAgreementParams {
    /**
     * If looking up agreements by delegate, the delegate for which to search. When the delegate is for a user, the search
     * will include both agreements on which the user is explicitly listed as the delegate and those for which the user
     * is a delegate based on user group membership.
     */
    private DelegationDelegate delegate;

    /**
     * If looking up agreements by principal, the principal for which to search. When the principal is for a user, the search
     * will include both agreements on which the user is explicitly listed as the principal and those for which the user
     * is a principal based on user group membership.
     */
    private DelegationPrincipal principal;

    /**
     * If looking up agreements by principal domains.
     */
    private Set<Domain> principalDomains;

    public FindDelegationAgreementParams(DelegationDelegate delegate, DelegationPrincipal principal, Set<Domain> principalDomains) {
        if (delegate == null && principal == null) {
            throw new IllegalArgumentException("A delegate or principal must be supplied");
        }
        this.delegate = delegate;
        this.principal = principal;
        this.principalDomains = principalDomains;
    }
}
