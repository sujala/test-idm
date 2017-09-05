package com.rackspace.idm.modules.usergroups.api.resource;

import com.rackspace.idm.api.resource.cloud.v20.PaginationParams;
import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * Consolidates the various search params allowed for the list user group members service.
 */
@Getter
@AllArgsConstructor
public class UserSearchCriteria {
    /**
     * What results to return
     */
    @Nullable
    private PaginationParams paginationRequest;
}
