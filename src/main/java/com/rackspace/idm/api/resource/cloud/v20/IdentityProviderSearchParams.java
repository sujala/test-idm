package com.rackspace.idm.api.resource.cloud.v20;

import lombok.AllArgsConstructor;
import lombok.Getter;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

/**
 * Consolidates the various search params allowed for the list identity providers service.
 */
@Getter
@AllArgsConstructor
public class IdentityProviderSearchParams {
    /**
     * Filters the list by identity provider's name
     */
    @Nullable
    String name;

    /**
     * Filters the list by identity provider's issuer
     */
    @Nullable
    String issuer;

    /**
     * Filters the list by an approved domain ID.
     */
    @Nullable
    String approvedDomainId;

    /**
     * Filters the list by an approved tenant ID.
     */
    @Nullable
    String approvedTenantId;

    /**
     * Filters the list by identity provider's type .
     */
    @Nullable
    String idpType;

    /**
     * Filters the list by an email domain .
     */
    @Nullable
    String emailDomain;

    public IdentityProviderSearchParams() {
        this(null, null, null, null, null, null);
    }

    Map<String, String> getSearchParamsMap() throws IllegalAccessException {
        Map<String, String> params = new HashMap<>();
        for (Field f : this.getClass().getDeclaredFields()) {
            Object value = f.get(this);
            if (value != null && value instanceof String) {
                params.put(f.getName(), (String) value);
            }
        }
        return params;
    }
}
