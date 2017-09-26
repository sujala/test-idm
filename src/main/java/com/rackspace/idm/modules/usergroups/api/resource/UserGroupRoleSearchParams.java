package com.rackspace.idm.modules.usergroups.api.resource;

import com.rackspace.idm.api.resource.cloud.v20.PaginationParams;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang.Validate;

import javax.annotation.Nullable;

/**
 * Consolidates the various search params allowed for the list user group roles service.
 */
@Getter
public class UserGroupRoleSearchParams {
    /**
     * What results to return
     */
    private PaginationParams paginationRequest;

    public UserGroupRoleSearchParams(PaginationParams paginationRequest) {
        Validate.notNull(paginationRequest);

        this.paginationRequest = paginationRequest;
    }

    public UserGroupRoleSearchParams() {
        this(new PaginationParams());
    }
}
