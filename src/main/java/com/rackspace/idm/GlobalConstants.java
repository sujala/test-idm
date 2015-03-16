package com.rackspace.idm;

import com.rackspace.idm.domain.entity.AuthenticatedByMethodEnum;

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
    public static final String MOSSO = "MOSSO";
    public static final String NAST = "NAST";

    public static final String SETUP_MFA_SCOPE = "SETUP-MFA";

    public static final String USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT = "DEFAULT";
    public static final String USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL = "OPTIONAL";
    public static final String USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED = "REQUIRED";

    public static final String DOMAIN_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL = "OPTIONAL";
    public static final String DOMAIN_MULTI_FACTOR_ENFORCEMENT_LEVEL_REQUIRED = "REQUIRED";

    public static final String ALL_TENANTS_DISABLED_ERROR_MESSAGE = "Forbidden: not allowed for suspended account";

    public static final String IDENTITY_ROLE_PREFIX = "identity:";

    public static final String ROLE_NAME_RACKER = "Racker";
}
