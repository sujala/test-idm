package com.rackspace.idm;

import java.util.regex.Pattern;

/**
 * Add globally-used constants here.
 * 
 */
public final class GlobalConstants {
    /**
     * FIXME When we actually get an i-number for Rackspace, set it here.
     */
    public static final String RACKSPACE_INUMBER_PREFIX = "@!FFFF.FFFF.FFFF.FFFF";
    public static final String RACKSPACE_INUMBER = "@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE";
    public static final String RACKSPACE_CUSTOMER_ID = "RACKSPACE";
    public static final String NO_REPLY_EMAIL = "no-reply@idm.rackspace.com";
    public static final String API_NAMESPACE_LOCATION = "http://docs.rackspacecloud.com/idm/api/v1.0";
    public static final String RESTRICTED_CLIENT_ID = "1183ca858a25100bd8bbd68f5f82ebe2ec8dfa87";

    public static final String INUM_PREFIX = "inum=";
    // Match /users/username/password URI
//    public static final Pattern PASSWORD_URI_PATTERN = Pattern
//        .compile("^/(?i:users)/.+/(?i:password)$");

    public static final int LDAP_PAGING_DEFAULT_OFFSET = 0;
    public static final int LDAP_PAGING_DEFAULT_LIMIT = 25;
    public static final int LDAP_PAGING_MAX_LIMIT = 1000;

    public static final String ATTR_SOFT_DELETED = "softDeleted";

    public static final String DFW_DC = "DFW";
    public static final String LONDON_DC = "LON";

//    public static final String XML_NAMESPACE = "http://docs.rackspacecloud.com/idm/api/v1.0";
//    public static final String JSON_NAMESPACE = "idm";

//    public static final String SECRET_QUESTION = "secretQuestion";
//    public static final String SECRET_ANSWER = "secretAnswer";

    // Values to return for the version request
    public static final String DOC_URL = "http://172.17.16.85:8080/idm-docs/idm-devguide-20101111.pdf";
    public static final String WADL_URL = "http://172.17.16.85:8080/idm-docs/idm-api.wadl";
    public static final String VERSION = "1.0";
    public static final String VERSION_STATUS = "CURRENT";

    public static final String USER_PREFERRED_LANG_DEFAULT = "en_US";
    public static final String USER_TIME_ZONE_DEFAULT = "America/Chicago";
    
    public static final String IDM_ADMIN_ROLE_NAME = "Idm Admin";
}
