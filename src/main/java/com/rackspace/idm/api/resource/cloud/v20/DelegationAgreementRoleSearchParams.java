package com.rackspace.idm.api.resource.cloud.v20;

import lombok.Getter;
import org.apache.commons.lang.Validate;

/**
 * Consolidates the various search params allowed for the list agreement roles service.
 */
@Getter
public class DelegationAgreementRoleSearchParams {
    /**
     * What results to return
     */
    private PaginationParams paginationRequest;

    public DelegationAgreementRoleSearchParams(PaginationParams paginationRequest) {
        Validate.notNull(paginationRequest);

        this.paginationRequest = paginationRequest;
    }

    public DelegationAgreementRoleSearchParams() {
        this(new PaginationParams());
    }
}
