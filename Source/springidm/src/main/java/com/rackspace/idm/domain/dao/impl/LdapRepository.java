package com.rackspace.idm.domain.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.unboundid.ldap.sdk.LDAPConnectionPool;

public abstract class LdapRepository {
    protected static final DateTimeFormatter DATE_PARSER = DateTimeFormat
        .forPattern("yyyyMMddHHmmss.SSS'Z");

    // Definitions for LDAP Objectclasses
    protected static final String OBJECTCLASS_BASEURL = "baseUrl";
    protected static final String OBJECTCLASS_CLIENTGROUP = "clientGroup";
    protected static final String OBJECTCLASS_CLIENTPERMISSION = "clientPermission";
    protected static final String OBJECTCLASS_GROUPOFNAMES = "groupOfNames";
    protected static final String OBJECTCLASS_ORGANIZATIONALUNIT = "organizationalUnit";
    protected static final String OBJECTCLASS_RACKSPACEAPPLICATION = "rsApplication";
    protected static final String OBJECTCLASS_RACKSPACEGROUP = "rsGroup";
    protected static final String OBJECTCLASS_RACKSPACEORGANIZATION = "rsOrganization";
    protected static final String OBJECTCLASS_RACKSPACEPERSON = "rsPerson";
    protected static final String OBJECTCLASS_RACKSPACETOKEN = "rsToken";
    protected static final String OBJECTCLASS_TOP = "top";

    protected static final String[] ATTR_BASEURL_OBJECT_CLASS_VALUES = {
        OBJECTCLASS_TOP, OBJECTCLASS_BASEURL};
    protected static final String[] ATTR_CLIENT_GROUP_OBJECT_CLASS_VALUES = {
        OBJECTCLASS_TOP, OBJECTCLASS_GROUPOFNAMES, OBJECTCLASS_CLIENTGROUP};
    protected static final String[] ATTR_CLIENT_OBJECT_CLASS_VALUES = {
        OBJECTCLASS_TOP, OBJECTCLASS_RACKSPACEAPPLICATION};
    protected static final String[] ATTR_CUSTOMER_OBJECT_CLASS_VALUES = {
        OBJECTCLASS_TOP, OBJECTCLASS_RACKSPACEORGANIZATION};
    protected static final String[] ATTR_OBJECT_CLASS_OU_VALUES = {
        OBJECTCLASS_TOP, OBJECTCLASS_ORGANIZATIONALUNIT};
    protected static final String[] ATTR_PERMISSION_OBJECT_CLASS_VALUES = {
        OBJECTCLASS_TOP, OBJECTCLASS_CLIENTPERMISSION};
    protected static final String[] ATTR_ROLE_OBJECT_CLASS_VALUES = {
        OBJECTCLASS_TOP, OBJECTCLASS_GROUPOFNAMES, OBJECTCLASS_RACKSPACEGROUP};
    protected static final String[] ATTR_TOKEN_OBJECT_CLASS_VALUES = {
        OBJECTCLASS_TOP, OBJECTCLASS_RACKSPACETOKEN};
    protected static final String[] ATTR_USER_OBJECT_CLASS_VALUES = {
        OBJECTCLASS_TOP, OBJECTCLASS_RACKSPACEPERSON};

    // Definitions for LDAP Attributes
    public static final String ATTR_ADMIN_URL = "adminUrl";
    public static final String ATTR_BASEURL_ID = "baseUrlId";
    public static final String ATTR_BASEURL_TYPE = "baseUrlType";
    public static final String ATTR_BLOB = "blob";
    public static final String ATTR_C = "c";
    public static final String ATTR_CLIENT_ID = "clientId";
    public static final String ATTR_CLIENT_SECRET = "userPassword";
    public static final String ATTR_CREATED_DATE = "createTimestamp";
    public static final String ATTR_DEF = "def";
    public static final String ATTR_DISPLAY_NAME = "displayName";
    public static final String ATTR_ENDPOINT = "endpoint";
    public static final String ATTR_EXPIRATION = "expiration";
    public static final String ATTR_GIVEN_NAME = "givenName";
    public static final String ATTR_GROUP_TYPE = "groupType";
    public static final String ATTR_INAME = "iname";
    public static final String ATTR_INTERNAL_URL = "internalUrl";
    public static final String ATTR_INUM = "inum";
    public static final String ATTR_LANG = "preferredLanguage";
    public static final String ATTR_LOCKED = "locked";
    public static final String ATTR_MAIL = "mail";
    public static final String ATTR_MEMBER = "member";
    public static final String ATTR_MEMBER_OF = "isMemberOf";
    public static final String ATTR_MIDDLE_NAME = "middleName";
    public static final String ATTR_MOSSO_ID = "rsMossoId";
    public static final String ATTR_NAME = "cn";
    public static final String ATTR_NAST_ID = "rsNastId";
    public static final String ATTR_O = "o";
    public static final String ATTR_OBJECT_CLASS = "objectClass";
    public static final String ATTR_OU = "ou";
    public static final String ATTR_OWNER = "owner";
    public static final String ATTR_PASSWORD = "userPassword";
    public static final String ATTR_CLEAR_PASSWORD = "clearPassword";
    public static final String ATTR_PASSWORD_SECRET_A = "secretAnswer";
    public static final String ATTR_PASSWORD_SECRET_Q = "secretQuestion";
    public static final String ATTR_PERMISSION = "permission";
    public static final String ATTR_PERMISSION_TYPE = "permissionType";
    public static final String ATTR_PUBLIC_URL = "publicUrl";
    public static final String ATTR_PWD_ACCOUNT_LOCKOUT_TIME = "pwdAccountLockedTime";
    public static final String ATTR_RACKSPACE_API_KEY = "rsApiKey";
    public static final String ATTR_RACKSPACE_CUSTOMER_NUMBER = "RCN";
    public static final String ATTR_RACKSPACE_PERSON_NUMBER = "RPN";
    public static final String ATTR_RACKSPACE_REGION = "rsRegion";
    public static final String ATTR_REGION = "rsRegion";
    public static final String ATTR_SEE_ALSO = "seeAlso";
    public static final String ATTR_SERVICE = "service";
    public static final String ATTR_SN = "sn";
    public static final String ATTR_SOFT_DELETED = "softDeleted";
    public static final String ATTR_STATUS = "status";
    public static final String ATTR_TIME_ZONE = "timeZone";
    public static final String ATTR_TOKEN_OWNER = "tokenOwner";
    public static final String ATTR_TOKEN_REQUESTOR = "tokenRequestor";
    public static final String ATTR_UID = "uid";
    public static final String ATTR_UPDATED_DATE = "modifyTimestamp";
    public static final String ATTR_SOFT_DELETED_DATE = "softDeletedTimestamp";

    // Definitions for LDAP DNs
    protected static final String BASE_DN = "o=rackspace,dc=rackspace,dc=com";
    protected static final String BASEURL_BASE_DN = "ou=BaseUrls,dc=rackspace,dc=com";
    protected static final String TOKEN_BASE_DN = "ou=Tokens,dc=rackspace,dc=com";
    
    // Definitions for OU names
    protected static final String OU_GROUPS_NAME = "groups";
    protected static final String OU_PEOPLE_NAME = "people";
    protected static final String OU_APPLICATIONS_NAME = "applications";
    protected static final String OU_PERMISSIONS_NAME = "permissions";

    private LdapConnectionPools conn;
    private Configuration config;
    
    final private Logger logger = LoggerFactory.getLogger(this.getClass());   

    protected LdapRepository(LdapConnectionPools conn, Configuration config) {
        this.conn = conn;
        this.config = config;
    }

    protected LDAPConnectionPool getAppConnPool() {
        return conn.getAppConnPool();
    }

    protected LDAPConnectionPool getBindConnPool() {
        return conn.getBindConnPool();
    }

    protected Logger getLogger() {
        return logger;
    }

    protected int getLdapPagingOffsetDefault() {
        return config.getInt("ldap.paging.offset.default");
    }

    protected int getLdapPagingLimitDefault() {
        return config.getInt("ldap.paging.limit.default");
    }

    protected int getLdapPagingLimitMax() {
        return config.getInt("ldap.paging.limit.max");
    }

    protected int getLdapPasswordFailureLockoutMin() {
        return config.getInt("ldap.password.failure.lockout.min");
    }

    protected String getRackspaceInumPrefix() {
        return config.getString("rackspace.inum.prefix");
    }

    private static class QueryPair {
        private String comparer;
        private String attribute;
        private String value;

        public QueryPair(String attribute, String comparer, String value) {
            if (StringUtils.isBlank(attribute)) {
                throw new IllegalArgumentException("attribute cannot be empty");
            }
            if (StringUtils.isBlank(comparer)) {
                throw new IllegalArgumentException("comparer cannot be empty");
            }
            if (StringUtils.isBlank(value)) {
                throw new IllegalArgumentException("value cannot be empty");
            }
            this.comparer = comparer;
            this.attribute = attribute;
            this.value = value;
        }

        public String getQueryString() {
            return String.format("(%s)", attribute + comparer + value);
        }

        public String getDnString() {
            return attribute + comparer + value;
        }
    }

    protected static class LdapDnBuilder {
        private List<QueryPair> queryPairs;
        private String baseDN;

        public LdapDnBuilder(String baseDN) {
            if (StringUtils.isBlank(baseDN)) {
                throw new IllegalArgumentException("baseDN cannot be empty");
            }
            this.baseDN = baseDN;
            this.queryPairs = new ArrayList<QueryPair>();
        }

        public LdapDnBuilder addAttriubte(String attribute, String value) {
            queryPairs.add(new QueryPair(attribute, "=", value));
            return this;
        }

        public String build() {
            StringBuilder builder = new StringBuilder();

            for (QueryPair pair : queryPairs) {
                builder.append(pair.getDnString());
                builder.append(",");
            }

            String searchString = builder.toString() + baseDN;

            return searchString;
        }
    }

    protected static class LdapSearchBuilder {
        private List<QueryPair> queryPairs;

        public LdapSearchBuilder() {
            queryPairs = new ArrayList<QueryPair>();
        }

        public LdapSearchBuilder addEqualAttribute(String attribute,
            String value) {
            queryPairs.add(new QueryPair(attribute, "=", value));
            return this;
        }

        public LdapSearchBuilder addGreaterOrEqualAttribute(String attribute,
            String value) {
            queryPairs.add(new QueryPair(attribute, ">=", value));
            return this;
        }

        public String build() {
            if (queryPairs.size() == 0) {
                return "";
            }
            
            StringBuilder builder = new StringBuilder();

            for (QueryPair pair : queryPairs) {
                builder.append(pair.getQueryString());
            }

            String searchString = builder.toString();

            if (queryPairs.size() > 0) {
                searchString = String.format("(&%s)", searchString);
            }

            return searchString;
        }
    }
    
   
}
