package com.rackspace.idm.api.security;

import lombok.Getter;
import org.apache.commons.lang.StringUtils;

/**
 * The set of identity roles against which authorization decisions can be made.
 * These are distinct from the {@link com.rackspace.idm.domain.service.IdentityUserTypeEnum} roles which
 * classify a user as a specific type.
 *
 * NOTE: Any new roles added to this class will need to be created in the directory PRIOR to the deployment due
 *  to caching of roles. This will be fixed in https://jira.rax.io/browse/CID-114
 */
public enum IdentityRole {

    VALIDATE_TOKEN_GLOBAL("identity:validate-token-global")
    , GET_TOKEN_ENDPOINTS_GLOBAL("identity:get-token-endpoint-global")
    , GET_USER_ROLES_GLOBAL("identity:get-user-roles-global")
    , GET_USER_GROUPS_GLOBAL("identity:get-user-groups-global")
    , REPOSE_STANDARD("identity:repose-standard")
    , IDENTITY_PROVIDER_MANAGER("identity:identity-provider-manager")
    , IDENTITY_PROVIDER_READ_ONLY("identity:identity-provider-read-only")
    , IDENTITY_MFA_ADMIN("identity:mfa-admin")
    , IDENTITY_QUERY_PROPS("identity:query-props")
    , IDENTITY_UPGRADE_USER_TO_CLOUD("identity:upgrade-user-to-cloud")
    , IDENTITY_PURGE_TOKEN_REVOCATION_RECORDS("identity:purge-trr")
    , IDENTITY_ENDPOINT_RULE_ADMIN("identity:endpoint-rule-admin")
    , IDENTITY_PROPERTY_ADMIN("identity:property-admin")
    , IDENTITY_INTERNAL("identity:internal")
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
