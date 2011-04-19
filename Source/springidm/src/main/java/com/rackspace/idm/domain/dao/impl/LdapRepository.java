package com.rackspace.idm.domain.dao.impl;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.audit.Audit;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;

public abstract class LdapRepository {

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
    protected static final String OBJECTCLASS_SCOPEACCESS = "scopeAccess";
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
    protected static final String[] ATTR_SCOPE_ACCESS_OBJECT_CLASS_VALUES = {
        OBJECTCLASS_TOP, OBJECTCLASS_SCOPEACCESS};
    protected static final String[] ATTR_TOKEN_OBJECT_CLASS_VALUES = {
        OBJECTCLASS_TOP, OBJECTCLASS_RACKSPACETOKEN};
    protected static final String[] ATTR_USER_OBJECT_CLASS_VALUES = {
        OBJECTCLASS_TOP, OBJECTCLASS_RACKSPACEPERSON};

    // Definitions for LDAP Attributes
    public static final String ATTR_ACCESS_TOKEN = "accessToken";
    public static final String ATTR_ACCESS_TOKEN_EXP = "accessTokenExp";
    public static final String ATTR_ADMIN_URL = "rsAdminUrl";
    public static final String ATTR_BASEURL_ID = "baseUrlId";
    public static final String ATTR_BASEURL_TYPE = "baseUrlType";
    public static final String ATTR_BLOB = "blob";
    public static final String ATTR_C = "c";
    public static final String ATTR_CLIENT_ID = "clientId";
    public static final String ATTR_CLIENT_RCN = "clientRCN";
    public static final String ATTR_CLIENT_SECRET = "userPassword";
    public static final String ATTR_CREATED_DATE = "createTimestamp";
    public static final String ATTR_DEF = "def";
    public static final String ATTR_DISPLAY_NAME = "rsDisplayName";
    public static final String ATTR_ENABLED = "enabled";
    public static final String ATTR_ENDPOINT = "endpoint";
    public static final String ATTR_EXPIRATION = "expiration";
    public static final String ATTR_GIVEN_NAME = "rsGivenName";
    public static final String ATTR_GRANTED_BY_DEFAULT = "grantedByDefault";
    public static final String ATTR_GROUP_TYPE = "groupType";
    public static final String ATTR_INAME = "iname";
    public static final String ATTR_INTERNAL_URL = "internalUrl";
    public static final String ATTR_INUM = "inum";
    public static final String ATTR_LANG = "preferredLanguage";
    public static final String ATTR_LOCKED = "locked";
    public static final String ATTR_MAIL = "rsMail";
    public static final String ATTR_MEMBER = "member";
    public static final String ATTR_MEMBER_OF = "memberOf";
    public static final String ATTR_MIDDLE_NAME = "middleName";
    public static final String ATTR_MOSSO_ID = "rsMossoId";
    public static final String ATTR_NAME = "cn";
    public static final String ATTR_NAST_ID = "rsNastId";
    public static final String ATTR_NO_ATTRIBUTES = "NO_ATTRIBUTES";
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
    public static final String ATTR_PWD_ACCOUNT_LOCKOUT_TIME = "dxPwdFailedTime";
    public static final String ATTR_RACKSPACE_API_KEY = "rsApiKey";
    public static final String ATTR_RACKSPACE_CUSTOMER_NUMBER = "RCN";
    public static final String ATTR_RACKSPACE_PERSON_NUMBER = "RPN";
    public static final String ATTR_RACKSPACE_REGION = "rsRegion";
    public static final String ATTR_REFRESH_TOKEN = "refreshToken";
    public static final String ATTR_REFRESH_TOKEN_EXP = "refreshTokenExp";
    public static final String ATTR_REGION = "rsRegion";
    public static final String ATTR_RESOURCE_GROUP = "resourceGroup";
    public static final String ATTR_SEE_ALSO = "seeAlso";
    public static final String ATTR_SERVICE = "service";
    public static final String ATTR_SN = "rsSn";
    public static final String ATTR_SOFT_DELETED = "softDeleted";
    public static final String ATTR_PASSWORD_ROTATION_ENABLED = "passwordRotationEnabled";
    public static final String ATTR_PASSWORD_ROTATION_DURATION = "passwordRotationDuration";
    public static final String ATTR_STATUS = "status";
    public static final String ATTR_TIME_ZONE = "timeZone";
    public static final String ATTR_TITLE = "title";
    public static final String ATTR_TOKEN_SCOPE = "tokenScope";
    public static final String ATTR_TOKEN_OWNER = "tokenOwner";
    public static final String ATTR_TOKEN_REQUESTOR = "tokenRequestor";
    public static final String ATTR_UID = "uid";
    public static final String ATTR_USER_RCN = "userRCN";
    public static final String ATTR_UPDATED_DATE = "modifyTimestamp";
    public static final String ATTR_SOFT_DELETED_DATE = "softDeletedTimestamp";
    public static final String ATTR_PASSWORD_UPDATED_TIMESTAMP = "passwordUpdatedTimestamp";
    public static final String ATTR_PASSWORD_SELF_UPDATED = "passwordSelfUpdated";

    // Definitions for LDAP DNs
    protected static final String BASE_DN = "o=rackspace,dc=rackspace,dc=com";
    protected static final String BASEURL_BASE_DN = "ou=BaseUrls,dc=rackspace,dc=com";
    protected static final String TOKEN_BASE_DN = "ou=Tokens,dc=rackspace,dc=com";

    // Definitions for OU names
    protected static final String OU_GROUPS_NAME = "groups";
    protected static final String OU_PEOPLE_NAME = "people";
    protected static final String OU_APPLICATIONS_NAME = "applications";
    protected static final String OU_PERMISSIONS_NAME = "permissions";
    
    //Search Attributes
    protected static final String[] ATTR_GROUP_SEARCH_ATTRIBUTES = {
        ATTR_OBJECT_CLASS, ATTR_RACKSPACE_CUSTOMER_NUMBER, ATTR_CLIENT_ID, ATTR_GROUP_TYPE,
        ATTR_NAME };
    protected static final String[] ATTR_USER_SEARCH_ATTRIBUTES = {"*",
        ATTR_CREATED_DATE, ATTR_UPDATED_DATE, ATTR_PWD_ACCOUNT_LOCKOUT_TIME};

    
    

    private final LdapConnectionPools connPools;
    private final Configuration config;

    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    protected LdapRepository(LdapConnectionPools conn, Configuration config) {
        this.connPools = conn;
        this.config = config;
    }

    protected LDAPConnectionPool getAppConnPool() {
        return connPools.getAppConnPool();
    }

    protected LDAPConnection getAppPoolConnection(Audit audit) {
        LDAPConnection conn = null;

        try {
            conn = getAppConnPool().getConnection();
        } catch (LDAPException e) {
            audit.fail(e.getMessage());
            getLogger().error("Error getting connection to LDAP {}", e);
        }

        return conn;
    }

    protected LDAPConnectionPool getBindConnPool() {
        return connPools.getBindConnPool();
    }

    protected Logger getLogger() {
        return logger;
    }

    protected void addEntry(String entryDn, Attribute[] attributes, Audit audit) {
        LDAPConnection conn = getAppPoolConnection(audit);
        addEntry(conn, entryDn, attributes, audit);
        getAppConnPool().releaseConnection(conn);
    }

    protected void addEntry(LDAPConnection conn, String entryDn,
        Attribute[] attributes, Audit audit) {
        try {
            conn.add(entryDn, attributes);
        } catch (LDAPException ldapEx) {
            audit.fail();
            getLogger().error("Error adding entry {} - {}", entryDn, ldapEx);
            throw new IllegalStateException(ldapEx);
        }
    }

    protected void deleteEntryAndSubtree(String dn, Audit audit) {
        LDAPConnection conn = getAppPoolConnection(audit);
        this.deleteEntryAndSubtree(conn, dn, audit);
        getAppConnPool().releaseConnection(conn);
    }
    
    protected List<SearchResultEntry> getMultipleEntries(String baseDN, SearchScope scope, Filter searchFilter, String sortAttribute, String... attributes) {
        SearchResult searchResult = null;
        
        ServerSideSortRequestControl sortRequest = new ServerSideSortRequestControl(
            new SortKey(sortAttribute));
        
        try {

            SearchRequest request = new SearchRequest(baseDN, scope,
                searchFilter, attributes);

            request.setControls(new Control[]{sortRequest});
            searchResult = getAppConnPool().search(request);

        } catch (LDAPException ldapEx) {
            getLogger().error("LDAP Search error - {}", ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        }
        
        return searchResult.getSearchEntries();
    }
    
    protected SearchResultEntry getSingleEntry(String baseDN, SearchScope scope, Filter searchFilter, String... attributes) {
        SearchResultEntry entry = null;
        try {
            entry = getAppConnPool().searchForEntry(baseDN, scope,
                searchFilter, attributes);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("LDAP Search error - {}", ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        }

        return entry;
    }

    protected void updateEntry(String entryDn, List<Modification> mods,
        Audit audit) {
        LDAPConnection conn = getAppPoolConnection(audit);
        updateEntry(conn, entryDn, mods, audit);
        getAppConnPool().releaseConnection(conn);
    }

    protected void updateEntry(LDAPConnection conn, String entryDn,
        List<Modification> mods, Audit audit) {
        try {
            conn.modify(entryDn, mods);
        } catch (LDAPException ldapEx) {
            audit.fail();
            getLogger().error("Error updating entry {} - {}", entryDn, ldapEx);
            throw new IllegalStateException(ldapEx);
        }
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

    private void deleteEntryAndSubtree(LDAPConnection conn, String dn,
        Audit audit) {

        try {

            Filter filter = Filter.createEqualityFilter(ATTR_OBJECT_CLASS,
                "top");
            SearchResult searchResult = conn.search(dn, SearchScope.ONE,
                filter, ATTR_NO_ATTRIBUTES);

            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                deleteEntryAndSubtree(conn, entry.getDN(), audit);
            }

            conn.delete(dn);

        } catch (LDAPException e) {
            getLogger().error("LDAP Search error - {}", e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    private static class QueryPair {
        private final String comparer;
        private final String attribute;
        private final String value;

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

        public String getDnString() {
            return attribute + comparer + value;
        }
    }

    protected static class LdapDnBuilder {
        private final List<QueryPair> queryPairs;
        private final String baseDN;

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
        List<Filter> filters;

        public LdapSearchBuilder() {
            filters = new ArrayList<Filter>();
        }

        public LdapSearchBuilder addEqualAttribute(String attribute,
            String value) {
            Filter filter = Filter.createEqualityFilter(attribute, value);
            filters.add(filter);
            return this;
        }

        public LdapSearchBuilder addEqualAttribute(String attribute,
            byte[] value) {
            Filter filter = Filter.createEqualityFilter(attribute, value);
            filters.add(filter);
            return this;
        }

        public LdapSearchBuilder addGreaterOrEqualAttribute(String attribute,
            String value) {
            Filter filter = Filter.createGreaterOrEqualFilter(attribute, value);
            filters.add(filter);
            return this;
        }

        public Filter build() {
            if (filters.isEmpty()) {
                return Filter.createEqualityFilter(ATTR_OBJECT_CLASS, "*");
            }

            if (filters.size() == 1) {
                return filters.get(0);
            }

            return Filter.createANDFilter(filters);
        }
    }

}
