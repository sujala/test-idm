package com.rackspace.idm.api.security;

import lombok.Getter;
import org.apache.commons.lang.StringUtils;

/**
 * The set of identity roles against which authorization decisions can be made.
 * These are distinct from the {@link com.rackspace.idm.domain.service.IdentityUserTypeEnum} roles which
 * classify a user as a specific type.
 */
public enum IdentityRole {

    VALIDATE_TOKEN_GLOBAL("identity:validate-token-global")
    , GET_TOKEN_ENDPOINTS_GLOBAL("identity:get-token-endpoint-global")
    , GET_USER_ROLES_GLOBAL("identity:get-user-roles-global")
    , GET_USER_GROUPS_GLOBAL("identity:get-user-groups-global")
    , REPOSE_STANDARD("identity:repose-standard")
    , IDENTITY_CACHE_ADMIN("identity:rs-cache-admin")
    , IDENTITY_PROVIDER_MANAGER("identity:identity-provider-manager")
    , IDENTITY_PROVIDER_READ_ONLY("identity:identity-provider-read-only")
    , IDENTITY_MFA_ADMIN("identity:mfa-admin")
    , IDENTITY_QUERY_PROPS("identity:query-props")
    , IDENTITY_PURGE_TOKEN_REVOCATION_RECORDS("identity:purge-trr")
    , IDENTITY_ENDPOINT_RULE_ADMIN("identity:endpoint-rule-admin")
    , IDENTITY_PROPERTY_ADMIN("identity:property-admin")
    , IDENTITY_INTERNAL("identity:internal")
    , RCN_ADMIN("rcn:admin")
    , IDENTITY_DOMAIN_ADMIN_CHANGE("identity:domain-admin-change")
    , DOMAIN_RCN_SWITCH("identity:domain-rcn-switch")
    , IDENTITY_ANALYZE_TOKEN("identity:analyze-token")
    , IDENTITY_TENANT_ACCESS("identity:tenant-access")
    , IDENTITY_PHONE_PIN_ADMIN("identity:phone-pin-admin")
    , IDENTITY_MIGRATE_DOMAIN_ADMIN("identity:migrate-domain-admin")
    , IDENTITY_UPDATE_USERNAME("identity:allow-update-user-username")
    , IDENTITY_RS_TENANT_ADMIN("identity:rs-tenant-admin")
    , IDENTITY_RS_DOMAIN_ADMIN("identity:rs-domain-admin")
    , IDENTITY_RS_ENDPOINT_ADMIN("identity:rs-endpoint-admin")
    , IDENTITY_V20_LIST_USERS_GLOBAL("identity:v2_0_list_users_global")
    ;

    @Getter
    private String roleName;

    private IdentityRole(String roleName) {
        this.roleName = roleName;
    }

    public static IdentityRole fromRoleName(String roleName) {
        if (StringUtils.isBlank(roleName)) {
            return null;
        }

        for (IdentityRole identityRole : values()) {
            if (identityRole.roleName.equalsIgnoreCase(roleName)) {
                return identityRole;
            }
        }
        return null;
    }
}
