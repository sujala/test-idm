package com.rackspace.idm.api.resource.cloud.v20;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nullable;

/**
 * Consolidates the various search params allowed for the list effective roles for user service.
 */
@Getter
@AllArgsConstructor
public class ListEffectiveRolesForUserParams {

    /**
     * Filters the list of effective roles for user by tenantId.
     */
    @Nullable
    String onTenantId;
}
