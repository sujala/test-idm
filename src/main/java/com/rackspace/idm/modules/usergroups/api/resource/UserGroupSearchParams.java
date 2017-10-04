package com.rackspace.idm.modules.usergroups.api.resource;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * Consolidates the various search params allowed for the list user groups service.
 */
@Getter
@AllArgsConstructor
public class UserGroupSearchParams {
    /**
     * Filters the list by group name
     */
    @Nullable
    String name;

    @Nullable
    String userId;

    boolean hasSearchParams(){
        return name != null || userId != null;
    }
}
