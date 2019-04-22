package com.rackspace.idm;

import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum;

import javax.ws.rs.core.MediaType;

/**
 * Add globally-used constants here.
 * 
 */
public final class GlobalConstants {

    private GlobalConstants() {}

    public static final String NO_REPLY_EMAIL = "no-reply@idm.rackspace.com";
    public static final String API_NAMESPACE_LOCATION = "http://docs.rackspacecloud.com/idm/api/v1.0";

    public static final String USER_PREFERRED_LANG_DEFAULT = "en_US";
    public static final String USER_TIME_ZONE_DEFAULT = "America/Chicago";

    public static final String RACKSPACE_DOMAIN = "RACKSPACE";

    /**
     * @deprecated - Use associated {@link AuthenticatedByMethodEnum.PASSWORD}
     */
    @Deprecated
    public static final String AUTHENTICATED_BY_PASSWORD = AuthenticatedByMethodEnum.PASSWORD.getValue();

    /**
     * @deprecated - Use associated {@link AuthenticatedByMethodEnum.RSAKEY}
     */
    @Deprecated
    public static final String AUTHENTICATED_BY_RSAKEY = AuthenticatedByMethodEnum.RSAKEY.getValue();

    /**
     * @deprecated - Use associated {@link AuthenticatedByMethodEnum.APIKEY}
     */
    @Deprecated
    public static final String AUTHENTICATED_BY_APIKEY = AuthenticatedByMethodEnum.APIKEY.getValue();

    /**
     * @deprecated - Use associated {@link AuthenticatedByMethodEnum.PASSCODE}
     */
    @Deprecated
    public static final String AUTHENTICATED_BY_PASSCODE = AuthenticatedByMethodEnum.PASSCODE.getValue();

    /**
     * @deprecated - Use associated {@link AuthenticatedByMethodEnum.FEDERATION}
     */
    @Deprecated
    public static final String AUTHENTICATED_BY_FEDERATION= AuthenticatedByMethodEnum.FEDERATION.getValue();

    /**
     * @deprecated - Use associated {@link AuthenticatedByMethodEnum.IMPERSONATION}
     */
    @Deprecated
    public static final String AUTHENTICATED_BY_IMPERSONATION = AuthenticatedByMethodEnum.IMPERSONATION.getValue();

    public static final String TENANT_ALIAS_PATTERN = "{tenant}";

    public static final String MULTIFACTOR_CONSISTENCY_LOG_NAME = "multifactorConsistencyLogger";
    public static final String MIGRATION_CHANGE_EVENT_LOG_NAME = "migrationChangeEventLogger";

    public static final String MOSSO = "MOSSO";
    public static final String NAST = "NAST";

    public static final String SETUP_MFA_SCOPE = "SETUP-MFA";

    public static final String USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT = "DEFAULT";
    public static final String USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL = "OPTIONAL";
    public static final String USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED = "REQUIRED";

    public static final String DOMAIN_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL = "OPTIONAL";
    public static final String DOMAIN_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED = "REQUIRED";

    public static final String ALL_TENANTS_DISABLED_ERROR_MESSAGE = "Forbidden: not allowed for suspended account";

    public static final String RESTRICT_UNVERIFIED_USER_MESSAGE = "Operation not permitted on an unverified user.";

    public static final String IDENTITY_ROLE_PREFIX = "identity:";

    public static final String ROLE_NAME_RACKER = "Racker";

    public static final String ERROR_MSG_NO_TENANTS_BELONG_TO_DOMAIN = "No tenants belong to this domain.";

    public static final String ERROR_MSG_DELETE_DEFAULT_DOMAIN = "Can not delete default domain for tenants";
    public static final String ERROR_MSG_DELETE_ENABLED_DOMAIN = "Can not delete enabled domains";
    public static final String ERROR_MSG_DELETE_DOMAIN_WITH_TENANTS = "Cannot delete domains which contain tenants";
    public static final String ERROR_MSG_DELETE_DOMAIN_WITH_USERS = "Cannot delete Domains which contain users";
    public static final String ERROR_MSG_SERVICE_NOT_FOUND = "Service not found";
    public static final String NOT_AUTHORIZED_MSG = "Not Authorized";
    public static final String ERROR_MSG_CANNOT_UPDATE_REGION_CLOUD = "Cannot update a region's cloud";
    public static final String ERROR_MSG_USER_S_NOT_FOUND = "User %s not found";

    public static final String DELETE_USER_LOG_NAME = "userDelete";
    public static final String FORBIDDEN_DUE_TO_RESTRICTED_TOKEN = "The scope of this token does not allow access to this resource";

    public static final String X_TENANT_ID = "X-Tenant-Id";
    public static final String X_USER_NAME = "X-User-Name";
    public static final String X_PASSWORD_EXPIRATION = "X-Password-Expiration";
    public static final String X_REQUEST_ID = "X-Request-Id";
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String X_AUTH_TOKEN = "X-Auth-Token";

    public static final String HEADER_IDENTITY_API_VERSION = "Identity-API-Version";
    public static final String FEDERATION_API_V1_0 = "1.0";
    public static final String FEDERATION_API_V2_0 = "2.0";

    public static final String COMPUTE_DEFAULT_ROLE = "compute:default";
    public static final String FILES_DEFAULT_ROLE = "object-store:default";
    public static final String MANAGED_HOSTING_DEFAULT_ROLE = "dedicated:default";

    public static final String  TENANT_TYPE_RCN = "rcn";
    public static final String  TENANT_TYPE_CLOUD = "cloud";
    public static final String  TENANT_TYPE_FILES = "files";
    public static final String  TENANT_TYPE_MANAGED_HOSTING = "managed_hosting";

    public static final String  MANAGED_HOSTING_TENANT_PREFIX = "hybrid:";

    public static final String RAX_STATUS_RESTRICTED_GROUP_NAME = "rax_status_restricted";

    public static final String TEXT_YAML = "text/yaml";
    public static final MediaType TEXT_YAML_TYPE = new MediaType("text", "yaml");

    public static final String V2_VALIDATE_TOKEN = "v2.0 Validate token";
    public static final String V2_GET_USER_BY_ID = "v2.0 Get user by id";
    public static final String V2_LIST_EFFECTIVE_ROLES_FOR_USER = "v2.0 List effective roles for user";
    public static final String V2_LIST_TOKEN_ENDPOINTS = "v2.0 List token endpoints";
    public static final String V2_AUTHENTICATE = "v2.0 Authenticate";
    public static final String V2_LIST_USER_LEGACY_GROUPS = "v2.0 List user legacy groups";

    public static final String ALL_RCNS_WILDCARD = "*";
    public static final Integer PHONE_PIN_SIZE = 6;

    /**
     * This prefix is used in front of IAM groups when defining the implicit roles for that group. For example,
     * if the IAM group is <i>group</i>, then the property defining the implicit roles would be <i>implicit.roles.iam\:group</i>.
     * The <b>\</b> before the colon is required.
     *
     */
    public static final String IAM_IMPLICIT_ROLE_PREFIX = "iam:";

    public static final String CLOUD_REGION_UK = "UK";
    public static final String CLOUD_REGION_US = "US";

    public static final String DOMAIN_TYPE_RACKSPACE_CLOUD_US = "RACKSPACE_CLOUD_US";
    public static final String DOMAIN_TYPE_RACKSPACE_CLOUD_UK = "RACKSPACE_CLOUD_UK";
    public static final String DOMAIN_TYPE_DEDICATED = "DEDICATED";
    public static final String DOMAIN_TYPE_DATAPIPE = "DATAPIPE";
    public static final String DOMAIN_TYPE_UNKNOWN = "UNKNOWN";

    public static final String DOMAIN_PREFIX_DEDICATED = "dedicated:";
    public static final String DOMAIN_PREFIX_DATAPIPE = "dp:";
}
