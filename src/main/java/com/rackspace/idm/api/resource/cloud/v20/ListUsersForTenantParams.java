package com.rackspace.idm.api.resource.cloud.v20;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * Consolidates the various search params allowed for the list user groups service.
 */
@Getter
@AllArgsConstructor
public class ListUsersForTenantParams {
    /**
     * Filters the list to those users which effectively have the specified role on the tenant
     */
    @Nullable
    String roleId;

    /**
     * Filters the list to those users which effectively have the specified contactId on the tenant
     */
    @Nullable
    String contactId;

    /**
     * Specifies the page of results to return
     */
    @Nullable
    private PaginationParams paginationRequest;
}
