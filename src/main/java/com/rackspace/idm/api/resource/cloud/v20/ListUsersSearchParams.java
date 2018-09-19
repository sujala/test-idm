package com.rackspace.idm.api.resource.cloud.v20;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * Consolidates the various search params allowed for the list users service.
 */
@Getter
@AllArgsConstructor
public class ListUsersSearchParams {

    /**
     * Filters the user across all domains with the specified username.
     */
    @Nullable
    String name;

    /**
     * Filters the list to those users which have the specified email.
     */
    @Nullable
    String email;

    /**
     * Filters the list to those users in the domain to which the specified tenant belongs.
     */
    @Nullable
    String tenantId;

    /**
     * Filters the list to those users in the specified domain.
     */
    @Nullable
    String domainId;

    /**
     * Filters the user admin in a domain.
     */
    @Nullable
    Boolean adminOnly;

    /**
     * Filters the list to those users with the specified user type.
     */
    @Nullable
    String userType;

    /**
     * Specifies the page of results to return
     */
    @Nullable
    private PaginationParams paginationRequest;

    public ListUsersSearchParams() {
        this(null, null, null, null, null, null, new PaginationParams());
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }
}
