package com.rackspace.idm;

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
    public static final String AUTHENTICATED_BY_PASSWORD = "PASSWORD";
    public static final String AUTHENTICATED_BY_RSAKEY = "RSAKEY";
    public static final String AUTHENTICATED_BY_APIKEY = "APIKEY";
    public static final String AUTHENTICATED_BY_PASSCODE = "PASSCODE";
    public static final String AUTHENTICATED_BY_FEDERATION= "FEDERATED";

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
}
