package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.ldap.sdk.controls.SubtreeDeleteRequestControl;
import com.unboundid.util.LDAPSDKUsageException;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class LdapRepository {

    // Definitions for LDAP Objectclasses
    public static final String OBJECTCLASS_BASEURL = "baseUrl";
    public static final String OBJECTCLASS_CLOUDGROUP = "rsGroup";
    public static final String OBJECTCLASS_CLIENTGROUP = "clientGroup";
    public static final String OBJECTCLASS_CLIENT_ROLE = "clientRole";
    public static final String OBJECTCLASS_CLIENTPERMISSION = "clientPermission";
    public static final String OBJECTCLASS_GROUPOFNAMES = "groupOfNames";
    public static final String OBJECTCLASS_ORGANIZATIONALUNIT = "organizationalUnit";
    public static final String OBJECTCLASS_RACKER = "rsRacker";
    public static final String OBJECTCLASS_RACKSPACEAPPLICATION = "rsApplication";
    public static final String OBJECTCLASS_RACKSPACEORGANIZATION = "rsOrganization";
    public static final String OBJECTCLASS_RACKSPACEPERSON = "rsPerson";
    public static final String OBJECTCLASS_RACKSPACETOKEN = "rsToken";
    public static final String OBJECTCLASS_SCOPEACCESS = "scopeAccess";
    public static final String OBJECTCLASS_USERSCOPEACCESS = "userScopeAccess";
    public static final String OBJECTCLASS_CLIENTSCOPEACCESS = "clientScopeAccess";
    public static final String OBJECTCLASS_IMPERSONATEDSCOPEACCESS = "impersonatedScopeAccess";
    public static final String OBJECTCLASS_PASSWORDRESETSCOPEACCESS = "passwordResetScopeAccess";
    public static final String OBJECTCLASS_RACKERSCOPEACCESS = "rackerScopeAccess";
    public static final String OBJECTCLASS_FEDERATEDUSERSCOPEACCESS = "federatedUserScopeAccess";
    public static final String OBJECTCLASS_RACKSPACE_CONTAINER = "rsContainer";
    public static final String OBJECTCLASS_TENANT = "tenant";
    public static final String OBJECTCLASS_TENANT_ROLE = "tenantRole";
    public static final String OBJECTCLASS_DOMAIN = "rsDomain";
    public static final String OBJECTCLASS_POLICY = "rsPolicy";
    public static final String OBJECTCLASS_CAPABILITY = "rsCapability";
    public static final String OBJECTCLASS_QUESTION = "rsQuestion";
    public static final String OBJECTCLASS_REGION = "rsCloudRegion";
    public static final String OBJECTCLASS_PATTERN = "rsPattern";
    public static final String OBJECTCLASS_PROPERTY = "rsProperty";
    public static final String OBJECTCLASS_EXTERNALPROVIDER = "externalProvider";

    public static final String OBJECTCLASS_NEXT_ID = "rsNextId";
    public static final String ATTR_ID = "rsId";

    protected static final String OBJECTCLASS_TOP = "top";

    protected static final String[] ATTR_GROUP_OBJECT_CLASS_VALUES = {OBJECTCLASS_TOP, OBJECTCLASS_CLOUDGROUP};
    protected static final String[] ATTR_POLICY_OBJECT_CLASS_VALUES = {OBJECTCLASS_TOP, OBJECTCLASS_POLICY};
    protected static final String[] ATTR_BASEURL_OBJECT_CLASS_VALUES = {OBJECTCLASS_TOP, OBJECTCLASS_BASEURL};
    protected static final String[] ATTR_CLIENT_GROUP_OBJECT_CLASS_VALUES = {OBJECTCLASS_TOP, OBJECTCLASS_GROUPOFNAMES, OBJECTCLASS_CLIENTGROUP};
    protected static final String[] ATTR_CLIENT_OBJECT_CLASS_VALUES = {OBJECTCLASS_TOP, OBJECTCLASS_RACKSPACEAPPLICATION};
    protected static final String[] ATTR_CUSTOMER_OBJECT_CLASS_VALUES = {OBJECTCLASS_TOP, OBJECTCLASS_RACKSPACEORGANIZATION};
    protected static final String[] ATTR_OBJECT_CLASS_OU_VALUES = {OBJECTCLASS_TOP, OBJECTCLASS_ORGANIZATIONALUNIT};
    protected static final String[] ATTR_PERMISSION_OBJECT_CLASS_VALUES = {OBJECTCLASS_TOP, OBJECTCLASS_CLIENTPERMISSION};
    protected static final String[] ATTR_RACKER_OBJECT_CLASS_VALUES = {OBJECTCLASS_TOP, OBJECTCLASS_RACKER};
    protected static final String[] ATTR_SCOPE_ACCESS_OBJECT_CLASS_VALUES = {OBJECTCLASS_TOP, OBJECTCLASS_SCOPEACCESS};
    protected static final String[] ATTR_TOKEN_OBJECT_CLASS_VALUES = {OBJECTCLASS_TOP, OBJECTCLASS_RACKSPACETOKEN};
    protected static final String[] ATTR_USER_OBJECT_CLASS_VALUES = {OBJECTCLASS_TOP, OBJECTCLASS_RACKSPACEPERSON};

    // Definitions for LDAP Attributes
    public static final String ATTR_ACCESS_TOKEN = "accessToken";
    public static final String ATTR_ACCESS_TOKEN_EXP = "accessTokenExp";
    public static final String ATTR_AUTH_CODE = "authCode";
    public static final String ATTR_AUTH_CODE_EXP = "authCodeExp";
    public static final String ATTR_ADMIN_URL = "rsAdminUrl";
    public static final String ATTR_BASEURL_ID = "baseUrlId";
    public static final String ATTR_V_ONE_DEFAULT = "vOneDefault";
    public static final String ATTR_BASEURL_TYPE = "baseUrlType";
    public static final String ATTR_BLOB = "blob";
    public static final String ATTR_C = "c";
    public static final String ATTR_CALLBACK_URL = "callbackUrl";
    public static final String ATTR_CLIENT_ID = "clientId";
    public static final String ATTR_CLIENT_RCN = "clientRCN";
    public static final String ATTR_CLIENT_SECRET = "userPassword";
    public static final String ATTR_CREATED_DATE = "createTimestamp";
    public static final String ATTR_DEF = "def";
    public static final String ATTR_PUBLIC_KEY = "publicKey";
    public static final String ATTR_DESCRIPTION = "description";
    public static final String ATTR_POLICYTYPE = "policyType";
    public static final String ATTR_DISPLAY_NAME = "rsDisplayName";
    public static final String ATTR_DOMAIN_ID = "rsDomainId";
    public static final String ATTR_ENABLED = "enabled";
    public static final String ATTR_ENDPOINT = "endpoint";
    public static final String ATTR_EXPIRATION = "expiration";
    public static final String ATTR_GIVEN_NAME = "rsGivenName";
    public static final String ATTR_GRANTED_BY_DEFAULT = "grantedByDefault";
    public static final String ATTR_GROUP_TYPE = "groupType";
    public static final String ATTR_GROUP_NAME = "name";
    public static final String ATTR_GROUP_ID = "rsGroupId";
    public static final String ATTR_IN_MIGRATION = "inMigration";
    public static final String ATTR_MIGRATION_DATE = "migrationDate";
    public static final String ATTR_INTERNAL_URL = "internalUrl";
    public static final String ATTR_LANG = "preferredLanguage";
    public static final String ATTR_MAIL = "mail";
    public static final String ATTR_MEMBER_OF = "rsGroupDN";
    public static final String ATTR_MIDDLE_NAME = "middleName";
    public static final String ATTR_MOSSO_ID = "rsMossoId";
    public static final String ATTR_NAME = "cn";
    public static final String ATTR_NAST_ID = "rsNastId";
    public static final String ATTR_NO_ATTRIBUTES = "NO_ATTRIBUTES";
    public static final String ATTR_O = "o";
    public static final String ATTR_URI = "uri";
    public static final String ATTR_OBJECT_CLASS = "objectClass";
    public static final String ATTR_OU = "ou";
    public static final String ATTR_OWNER = "owner";
    public static final String ATTR_PASSWORD = "userPassword";
    public static final String ATTR_CLEAR_PASSWORD = "clearPassword";
    public static final String ATTR_PASSWORD_SECRET_A = "secretAnswer";
    public static final String ATTR_PASSWORD_SECRET_Q = "secretQuestion";
    public static final String ATTR_PASSWORD_SECRET_Q_ID = "secretQuestionId";
    public static final String ATTR_PERMISSION = "permission";
    public static final String ATTR_PERMISSION_TYPE = "permissionType";
    public static final String ATTR_PUBLIC_URL = "publicUrl";
    public static final String ATTR_PWD_ACCOUNT_LOCKOUT_TIME = "dxPwdFailedTime";
    public static final String ATTR_RACKER_ID = "rackerId";
    public static final String ATTR_RACKSPACE_API_KEY = "rsApiKey";
    public static final String ATTR_RACKSPACE_CUSTOMER_NUMBER = "RCN";
    public static final String ATTR_RACKSPACE_PERSON_NUMBER = "RPN";
    public static final String ATTR_RACKSPACE_REGION = "rsRegion";
    public static final String ATTR_REFRESH_TOKEN = "refreshToken";
    public static final String ATTR_REFRESH_TOKEN_EXP = "refreshTokenExp";
    public static final String ATTR_REGION = "rsRegion";
    public static final String ATTR_RESOURCE_GROUP = "resourceGroup";
    public static final String ATTR_SECURE_ID = "rsSecureId";
    public static final String ATTR_SEE_ALSO = "seeAlso";
    public static final String ATTR_SERVICE = "service";
    public static final String ATTR_SN = "rsSn";
    public static final String ATTR_PASSWORD_ROTATION_ENABLED = "passwordRotationEnabled";
    public static final String ATTR_PASSWORD_ROTATION_DURATION = "passwordRotationDuration";
    public static final String ATTR_TENANT_ID = "tenantId";
    public static final String ATTR_TENANT_DISPLAY_NAME = "tenantDisplayName";
    public static final String ATTR_TIME_ZONE = "timeZone";
    public static final String ATTR_TITLE = "title";
    public static final String ATTR_TOKEN_SCOPE = "tokenScope";
    public static final String ATTR_TOKEN_OWNER = "tokenOwner";
    public static final String ATTR_TOKEN_REQUESTOR = "tokenRequestor";
    public static final String ATTR_UID = "uid";
    public static final String ATTR_USER_RCN = "userRCN";
    public static final String ATTR_USE_FOR_DEFAULT_REGION = "useForDefaultRegion";
    public static final String ATTR_UPDATED_DATE = "modifyTimestamp";
    public static final String ATTR_SOFT_DELETED_DATE = "softDeletedTimestamp";
    public static final String ATTR_PASSWORD_UPDATED_TIMESTAMP = "passwordUpdatedTimestamp";
    public static final String ATTR_PASSWORD_SELF_UPDATED = "passwordSelfUpdated";
    public static final String ATTR_VERSION_ID = "versionId";
    public static final String ATTR_VERSION_INFO = "versionInfo";
    public static final String ATTR_VERSION_LIST = "versionList";
    public static final String ATTR_GLOBAL = "rsGlobal";
    public static final String ATTR_OPENSTACK_TYPE = "openstackType";
    public static final String ATTR_IMPERSONATING_ID = "impersonatingId";
    public static final String ATTR_IMPERSONATING_USERNAME = "impersonatingUsername";
    public static final String ATTR_IMPERSONATING_TOKEN = "impersonatingToken";
    public static final String ATTR_POLICY_ID = "policyId";
    public static final String ATTR_ACTION = "rsAction";
    public static final String ATTR_URL = "rsUrl";
    public static final String ATTR_RESOURCES = "rsResources";
    public static final String ATTR_CLOUD = "rsCloud";
    public static final String ATTR_QUESTION = "question";
    public static final String ATTR_CAPABILITY_ID = "capabilityId";
    public static final String ATTR_RS_WEIGHT = "rsWeight";
    public static final String ATTR_RS_PROPAGATE = "rsPropagate";
    public static final String ATTR_RS_TYPE = "rsType";
    public static final String ATTR_VALUE = "value";
    public static final String ATTR_ENCRYPTION_SALT = "encryptionSalt";
    public static final String ATTR_ENCRYPTION_VERSION_ID = "encryptionVersionId";
    public static final String ATTR_IDP_NAME = "idpName";

    public static final String ATTR_TENANT_RS_ID = "tenantRsId";
    public static final String ATTR_ROLE_RS_ID = "roleRsId";
    public static final String ATTR_USER_RS_ID = "userRsId";

    public static final String ATTR_REGEX = "pattern";
    public static final String ATTR_ERRMSG = "errMsg";

    // Definitions for LDAP DNs
    protected static final String EXTERNAL_PROVIDERS_BASE_DN = "o=externalProviders,dc=rackspace,dc=com";
    protected static final String BASE_DN = "o=rackspace,dc=rackspace,dc=com";
    protected static final String SCOPE_ACCESS_BASE_DN = "dc=rackspace,dc=com";
    protected static final String BASEURL_BASE_DN = "ou=baseUrls,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String GROUP_BASE_DN = "ou=groups,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String TENANT_BASE_DN = "ou=tenants,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String DOMAIN_BASE_DN = "ou=domains,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String POLICY_BASE_DN = "ou=policies,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String QUESTION_BASE_DN = "ou=questions,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String CAPABILITY_BASE_DN = "ou=capabilities,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String REGION_BASE_DN = "ou=regions,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String CLOUD_ADMIN_BASE_DN = "ou=adminUsers,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String CUSTOMERS_BASE_DN = "ou=customers,o=rackspace,dc=rackspace,dc=com";
    protected static final String APPLICATIONS_BASE_DN = "ou=applications,o=rackspace,dc=rackspace,dc=com";
    protected static final String USERS_BASE_DN = "ou=users,o=rackspace,dc=rackspace,dc=com";
    protected static final String RACKERS_BASE_DN = "ou=rackers,o=rackspace,dc=rackspace,dc=com";
    protected static final String NEXT_IDS_BASE_DN = "ou=nextIds,o=rackspace,dc=rackspace,dc=com";
    protected static final String SOFT_DELETED_BASE_DN = "ou=softDeleted,o=rackspace,dc=rackspace,dc=com";
    protected static final String SOFT_DELETED_USERS_BASE_DN = "ou=users,ou=softDeleted,o=rackspace,dc=rackspace,dc=com";
    protected static final String SOFT_DELETED_POLICIES_BASE_DN = "ou=policies,ou=softDeleted,o=rackspace,dc=rackspace,dc=com";
    protected static final String SOFT_DELETED_CUSTOMERS_BASE_DN = "ou=customers,ou=softDeleted,o=rackspace,dc=rackspace,dc=com";
    protected static final String SOFT_DELETED_APPLICATIONS_BASE_DN = "ou=applications,ou=softDeleted,o=rackspace,dc=rackspace,dc=com";
    protected static final String PATTERN_BASE_DN = "ou=patterns,ou=configuration,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String PROPERTY_BASE_DN = "ou=properties,ou=configuration,ou=cloud,o=rackspace,dc=rackspace,dc=com";

    // Definitions for Contatiner Names
    protected static final String CONTAINER_ROLES = "ROLES";
    protected static final String CONTAINER_TOKENS = "TOKENS";
    protected static final String CONTAINER_APPLICATION_ROLES = "CLIENT ROLES";

    // Search Attributes
    protected static final String[] ATTR_DEFAULT_SEARCH_ATTRIBUTES = {"*"};
    protected static final String[] ATTR_GROUP_SEARCH_ATTRIBUTES = {ATTR_OBJECT_CLASS, ATTR_RACKSPACE_CUSTOMER_NUMBER, ATTR_CLIENT_ID, ATTR_GROUP_TYPE, ATTR_NAME};
    protected static final String[] ATTR_USER_SEARCH_ATTRIBUTES = {"*", ATTR_CREATED_DATE, ATTR_UPDATED_DATE, ATTR_PWD_ACCOUNT_LOCKOUT_TIME};
    protected static final String[] ATTR_TENANT_SEARCH_ATTRIBUTES = {"*", ATTR_CREATED_DATE, ATTR_UPDATED_DATE};
    protected static final String[] ATTR_DOMAIN_SEARCH_ATTRIBUTES = {"*"};
    protected static final String[] ATTR_POLICY_SEARCH_ATTRIBUTES = {"*"};
    protected static final String[] ATTR_CAPABILITY_SEARCH_ATTRIBUTES = {"*",ATTR_ACTION,ATTR_NAME};
    protected static final String[] ATTR_QUESTION_SEARCH_ATTRIBUTES = {"*"};
    protected static final String[] ATTR_REGION_SEARCH_ATTRIBUTES = {"*"};
    protected static final String[] ATTR_CLIENT_ROLE_SEARCH_ATTRIBUTES = {"*"};
    protected static final String[] ATTR_SCOPE_ACCESS_ATTRIBUTES = {"*", ATTR_CREATED_DATE};
    public static final String LDAP_SEARCH_ERROR = "LDAP Search error - {}";

    @Autowired
    protected LdapConnectionPools connPools;

    @Autowired
    protected Configuration config;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    protected LDAPInterface getAppInterface() {
        return connPools.getAppConnPoolInterface();
    }

    protected LDAPConnectionPool getBindConnPool() {
        return connPools.getBindConnPool();
    }

    protected Logger getLogger() {
        return logger;
    }

    protected void addEntry(String entryDn, Attribute[] attributes, Audit audit) {
        try {
            getAppInterface().add(entryDn, attributes);
        } catch (LDAPException e) {
            audit.fail();
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected void deleteEntryAndSubtree(String dn, Audit audit) {
        try {
            DeleteRequest deleteRequest = new DeleteRequest(dn);
            deleteRequest.addControl(new SubtreeDeleteRequestControl(true));
            LDAPInterface inter = getAppInterface();
            inter.delete(deleteRequest);
        } catch (LDAPException e) {
            audit.fail();
            getLogger().error(LDAP_SEARCH_ERROR, e.getMessage());
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected List<SearchResultEntry> getMultipleEntries(String baseDN, SearchScope scope, Filter searchFilter, String... attributes) {
        SearchResult searchResult;
        try {
            SearchRequest request = new SearchRequest(baseDN, scope, searchFilter, attributes);
            searchResult = getAppInterface().search(request);
        } catch (LDAPException ldapEx) {
            getLogger().error(LDAP_SEARCH_ERROR, ldapEx.getMessage());
            return new ArrayList<SearchResultEntry>();
        }

        return searchResult.getSearchEntries();
    }

    protected List<SearchResultEntry> getMultipleEntries(String baseDN, SearchScope scope, String sortAttribute, Filter searchFilter, String... attributes) {
        SearchResult searchResult;

        ServerSideSortRequestControl sortRequest = new ServerSideSortRequestControl(new SortKey(sortAttribute));

        try {
            SearchRequest request = new SearchRequest(baseDN, scope, searchFilter, attributes);
            request.setControls(new Control[]{sortRequest});
            searchResult = getAppInterface().search(request);
        } catch (LDAPException ldapEx) {
            getLogger().error(LDAP_SEARCH_ERROR, ldapEx.getMessage());
            return new ArrayList<SearchResultEntry>();
        }

        return searchResult.getSearchEntries();
    }

    protected SearchResult getMultipleEntries(SearchRequest searchRequest) {
        try {
            return getAppInterface().search(searchRequest);
        } catch (LDAPException ldapEx) {
            getLogger().error(LDAP_SEARCH_ERROR, ldapEx.getMessage());
            List<SearchResultEntry> results = new ArrayList<SearchResultEntry>();
            List<SearchResultReference> resultRefs = new ArrayList<SearchResultReference>();
            return new SearchResult(ldapEx.getResultCode().intValue(), ldapEx.getResultCode(),
                    ldapEx.getDiagnosticMessage(), null, ldapEx.getReferralURLs(), results, resultRefs, 0, 0, ldapEx.getResponseControls());
        }
    }

    protected SearchResultEntry getSingleEntry(String baseDN,
        SearchScope scope, Filter searchFilter, String... attributes) {
        SearchResultEntry entry = null;
        try {
            entry = getAppInterface().searchForEntry(baseDN, scope,
                searchFilter, attributes);
        } catch (LDAPSearchException ldapEx) {
            getLogger().error(LDAP_SEARCH_ERROR, ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        } catch (LDAPSDKUsageException ldapEx) {
            getLogger().error(LDAP_SEARCH_ERROR, ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        }

        return entry;
    }

    protected SearchResultEntry getEntryByDn(String entryDn) {
        SearchResultEntry entry = null;
        try {
            entry = getAppInterface().getEntry(entryDn);
        } catch (LDAPException ldapEx) {
            getLogger().error(LDAP_SEARCH_ERROR, ldapEx.getMessage());
            throw new IllegalStateException(ldapEx);
        }
        return entry;
    }

    protected void updateEntry(String entryDn, List<Modification> mods,
        Audit audit) {
        try {
            getAppInterface().modify(entryDn, mods);
        } catch (LDAPException ldapEx) {
            audit.fail();
            getLogger().error("Error updating entry {} - {}", entryDn, ldapEx);
            throw new IllegalStateException(ldapEx.getMessage(), ldapEx);
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

    protected String getRackspaceCustomerId() {
        return config.getString("rackspace.customerId");
    }

    protected void addContainer(String parentUniqueId, String name) {
        Audit audit = Audit.log("Adding ScopeAccess_Container").add();
        List<Attribute> atts = new ArrayList<Attribute>();
        atts.add(new Attribute(ATTR_OBJECT_CLASS,OBJECTCLASS_RACKSPACE_CONTAINER));
        atts.add(new Attribute(ATTR_NAME, name));
        Attribute[] attributes = atts.toArray(new Attribute[0]);
        String dn = new LdapDnBuilder(parentUniqueId).addAttribute(ATTR_NAME,name).build();
        this.addEntry(dn, attributes, audit);
        audit.succeed();
    }

    protected SearchResultEntry getContainer(String parentUniqueId, String name) {
        Filter filter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS,OBJECTCLASS_RACKSPACE_CONTAINER)
                .addEqualAttribute(ATTR_NAME, name).build();

        return this.getSingleEntry(parentUniqueId, SearchScope.ONE, filter);
    }
    
    protected static final String NEXT_USER_ID = "nextUserId";
    protected static final String NEXT_TENANT_ID = "nextTenantId";
    protected static final String NEXT_ROLE_ID = "nextRoleId";
    protected static final String NEXT_CLIENT_ID = "nextClientId";
    protected static final String NEXT_CUSTOMER_ID = "nextCustomerId";
    protected static final String NEXT_GROUP_ID = "nextGroupId";
    protected static final String NEXT_DOMAIN_ID = "nextDomainId";
    protected static final String NEXT_CAPABILITY_ID = "nextCapabilityId";
    protected static final String NEXT_QUESTION_ID = "nextQuestionId";
    protected static final String NEXT_POLICY_ID = "nextPolicyId";
    protected static final String NEXT_PATTERN_ID = "nextPatternId";

    protected boolean useUuidForRsId() {
        return config.getBoolean("rsid.uuid.enabled", false);
    }

    protected boolean useUuidForUserRsId() {
        return config.getBoolean("user.uuid.enabled", false);
    }

    protected String getUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    protected String getNextId(String type) {
        if (!type.equals(NEXT_USER_ID) && useUuidForRsId()) {
            return getUuid();
        }
        else if (type.equals(NEXT_USER_ID) && useUuidForUserRsId()) {
            return getUuid();
        }

        long nextId = 0;

        while(true) {
            Filter filter = new LdapSearchBuilder()
                    .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_NEXT_ID)
                    .addEqualAttribute(ATTR_NAME, type).build();

            SearchResultEntry entry = this.getSingleEntry(NEXT_IDS_BASE_DN, SearchScope.ONE, filter);

            nextId = entry.getAttributeValueAsLong(ATTR_ID);

            List<Modification> mods = new ArrayList<Modification>();
            mods.add(new Modification(ModificationType.DELETE,ATTR_ID, String.valueOf(nextId)));
            mods.add(new Modification(ModificationType.ADD,ATTR_ID, String.valueOf(nextId + 1)));

            try {
                getAppInterface().modify(entry.getDN(), mods);
                break;
            } catch (LDAPException ex) {
                if (ex.getResultCode() == ResultCode.NO_SUCH_ATTRIBUTE) {
                    getLogger().info("Could not user nextId, trying again...");
                    String errMsg = String.format("Message: %s - Exception Message %s", ex.getMessage(), ex.getExceptionMessage());
                    getLogger().error(errMsg, ex);
                    continue;
                }
                getLogger().error("Error getting next id of type {}", type, ex);
                throw new IllegalStateException(ex.getMessage(), ex);
            }
        }
        return String.valueOf(nextId);
    }

    static class QueryPair {
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
            this.baseDN = baseDN;
            this.queryPairs = new ArrayList<QueryPair>();
        }

        public LdapDnBuilder addAttribute(String attribute, String value) {
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

    public static class LdapSearchBuilder {
        private List<Filter> filters;

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

        public LdapSearchBuilder addPresenceAttribute(String attribute) {
            Filter filter = Filter.createPresenceFilter(attribute);
            filters.add(filter);
            return this;
        }

        public LdapSearchBuilder addOrAttributes(List<Filter> components) {
            Filter filter = Filter.createORFilter(components);
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
