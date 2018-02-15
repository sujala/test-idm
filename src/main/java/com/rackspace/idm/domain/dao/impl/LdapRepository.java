package com.rackspace.idm.domain.dao.impl;

import com.google.common.collect.ObjectArrays;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.controls.ServerSideSortRequestControl;
import com.unboundid.ldap.sdk.controls.SortKey;
import com.unboundid.ldap.sdk.controls.SubtreeDeleteRequestControl;
import com.unboundid.util.LDAPSDKUsageException;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public abstract class LdapRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapRepository.class);

    // Definitions for LDAP Objectclasses
    public static final String OBJECTCLASS_BASEURL = "baseUrl";
    public static final String OBJECTCLASS_CLOUDGROUP = "rsGroup";
    public static final String OBJECTCLASS_CLIENT_ROLE = "clientRole";
    public static final String OBJECTCLASS_RACKER = "rsRacker";
    public static final String OBJECTCLASS_RACKSPACEAPPLICATION = "rsApplication";
    public static final String OBJECTCLASS_RACKSPACEPERSON = "rsPerson";
    public static final String OBJECTCLASS_RACKSPACE_FEDERATED_PERSON = "rsFederatedPerson";
    public static final String OBJECTCLASS_SCOPEACCESS = "scopeAccess";
    public static final String OBJECTCLASS_USERSCOPEACCESS = "userScopeAccess";
    public static final String OBJECTCLASS_CLIENTSCOPEACCESS = "clientScopeAccess";
    public static final String OBJECTCLASS_IMPERSONATEDSCOPEACCESS = "impersonatedScopeAccess";
    public static final String OBJECTCLASS_PASSWORDRESETSCOPEACCESS = "passwordResetScopeAccess";
    public static final String OBJECTCLASS_RACKERSCOPEACCESS = "rackerScopeAccess";
    public static final String OBJECTCLASS_FEDERATEDUSERSCOPEACCESS = "rsFederatedUserScopeAccess";
    public static final String OBJECTCLASS_RACKSPACE_CONTAINER = "rsContainer";
    public static final String OBJECTCLASS_ORGANIZATIONAL_UNIT = "organizationalUnit";
    public static final String OBJECTCLASS_TENANT = "tenant";
    public static final String OBJECTCLASS_TENANT_TYPE = "rsTenantTypeObject";
    public static final String OBJECTCLASS_TENANT_ROLE = "tenantRole";
    public static final String OBJECTCLASS_DOMAIN = "rsDomain";
    public static final String OBJECTCLASS_PASSWORD_POLICY = "rsPasswordPolicy";
    public static final String OBJECTCLASS_POLICY = "rsPolicy";
    public static final String OBJECTCLASS_CAPABILITY = "rsCapability";
    public static final String OBJECTCLASS_QUESTION = "rsQuestion";
    public static final String OBJECTCLASS_REGION = "rsCloudRegion";
    public static final String OBJECTCLASS_PATTERN = "rsPattern";
    public static final String OBJECTCLASS_PROPERTY = "rsProperty";
    public static final String OBJECTCLASS_EXTERNALPROVIDER = "rsExternalProvider";
    public static final String OBJECTCLASS_MULTIFACTOR_MOBILE_PHONE = "rsMultiFactorMobilePhone";
    public static final String OBJECTCLASS_TOKEN_REVOCATION_RECORD = "rsTokenRevocationRecord";
    public static final String OBJECTCLASS_INET_ORG_PERSON = "inetOrgPerson";
    public static final String OBJECTCLASS_DELEGATION_AGREEMENT = "rsDelegationAgreement";

    public static final String ATTR_MULTIFACTOR_DOMAIN_ENFORCEMENT_LEVEL = "rsDomainMultiFactorEnforcementLevel";
    public static final String ATTR_MULTIFACTOR_USER_ENFORCEMENT_LEVEL = "rsUserMultiFactorEnforcementLevel";

    public static final String ATTR_TOKEN_FORMAT = "rsTokenFormat";
    public static final String ATTR_MULTIFACTOR_TYPE = "rsMultiFactorType";

    public static final String OBJECTCLASS_NEXT_ID = "rsNextId";

    public static final String OBJECTCLASS_OTP_DEVICE = "rsOTPDevice";
    public static final String ATTR_OTP_NAME = "rsOTPName";
    public static final String ATTR_OTP_KEY = "rsOTPKey";

    public static final String OBJECTCLASS_BYPASS_DEVICE = "rsBypassDevice";
    public static final String ATTR_BYPASS_CODE = "rsBypassCode";
    public static final String ATTR_ITERATION_COUNT = "rsIterationCount";

    public static final String OBJECTCLASS_KEY_DESCRIPTOR = "rsKeyDescriptor";
    public static final String OBJECTCLASS_KEY_METADATA = "rsKeyMetadata";
    public static final String OBJECTCLASS_API_NODE_SIGNOFF = "rsApiNodeSignOff";
    public static final String KEY_DISTRIBUTION_OU = "keydistribution";
    public static final String ATTR_KEY_DATA = "rsKeyData";
    public static final String ATTR_KEY_VERSION = "rsKeyVersion";
    public static final String ATTR_KEY_CREATED = "rsKeyCreated";
    public static final String ATTR_LOADED_DATE = "rsLoadedDate";
    public static final String ATTR_KEY_METADATA_ID = "rsKeyMetadataId";
    public static final String OBJECTCLASS_IDENTITY_PROPERTY = "rsIdentityProperty";

    public static final String ATTR_ID = "rsId";

    // Definitions for LDAP Attributes
    public static final String ATTR_ACCESS_TOKEN = "accessToken";
    public static final String ATTR_ACCESS_TOKEN_EXP = "accessTokenExp";
    public static final String ATTR_AUTH_CODE = "authCode";
    public static final String ATTR_AUTH_CODE_EXP = "authCodeExp";
    public static final String ATTR_ADMIN_URL = "rsAdminUrl";
    public static final String ATTR_BASEURL_ID = "baseUrlId";
    public static final String ATTR_V_ONE_DEFAULT = "vOneDefault";
    public static final String ATTR_TYPE = "rsType";
    public static final String ATTR_BASEURL_TYPE = "baseUrlType";
    public static final String ATTR_BLOB = "blob";
    public static final String ATTR_C = "c";
    public static final String ATTR_CALLBACK_URL = "callbackUrl";
    public static final String ATTR_CLIENT_ID = "clientId";
    public static final String ATTR_CLIENT_RCN = "clientRCN";
    public static final String ATTR_CLIENT_SECRET = "userPassword";
    public static final String ATTR_CREATED_DATE = "createTimestamp";
    public static final String ATTR_DEF = "def";
    public static final String ATTR_PUBLIC_KEY = "nisPublicKey";
    public static final String ATTR_USER_CERTIFICATES = "userCertificate;binary";
    public static final String ATTR_TARGET_USER_SOURCE = "rsTargetUserSource";
    public static final String ATTR_APPROVED_DOMAIN_GROUP = "rsApprovedDomainGroup";
    public static final String ATTR_APPROVED_DOMAIN_IDS = "rsApprovedDomainIds";
    public static final String ATTR_EMAIL_DOMAINS = "rsEmailDomains";
    public static final String ATTR_AUTHENTICATION_URL = "rsAuthenticationUrl";
    public static final String ATTR_FEDERATED_USER_EXPIRED_TIMESTAMP = "rsFederatedUserExpiredTimestamp";
    public static final String ATTR_ASSIGNMENT = "rsAssignment";

    public static final String ATTR_DESCRIPTION = "description";
    public static final String ATTR_POLICYTYPE = "policyType";
    public static final String ATTR_DISPLAY_NAME = "rsDisplayName";
    public static final String ATTR_DOMAIN_ID = "rsDomainId";
    public static final String ATTR_ENABLED = "enabled";
    public static final String ATTR_GIVEN_NAME = "rsGivenName";
    public static final String ATTR_GROUP_NAME = "name";
    public static final String ATTR_GROUP_ID = "rsGroupId";
    public static final String ATTR_USER_GROUP_DNS = "rsUserGroupDNs";
    public static final String ATTR_INTERNAL_URL = "internalUrl";
    public static final String ATTR_LANG = "preferredLanguage";
    public static final String ATTR_LABELED_URI = "labeledUri";
    public static final String ATTR_MAIL = "mail";
    public static final String ATTR_MIDDLE_NAME = "middleName";
    public static final String ATTR_MOSSO_ID = "rsMossoId";
    public static final String ATTR_NAME = "cn";
    public static final String ATTR_NAST_ID = "rsNastId";
    public static final String ATTR_NO_ATTRIBUTES = "NO_ATTRIBUTES";
    public static final String ATTR_URI = "labeledUri";
    public static final String ATTR_OBJECT_CLASS = "objectClass";
    public static final String ATTR_OU = "ou";
    public static final String ATTR_PASSWORD = "userPassword";
    public static final String ATTR_CLEAR_PASSWORD = "clearPassword";
    public static final String ATTR_PASSWORD_SECRET_A = "secretAnswer";
    public static final String ATTR_PASSWORD_SECRET_Q = "secretQuestion";
    public static final String ATTR_PASSWORD_SECRET_Q_ID = "secretQuestionId";
    public static final String ATTR_PUBLIC_URL = "publicUrl";
    public static final String ATTR_PWD_ACCOUNT_LOCKOUT_TIME = "dxPwdFailedTime";
    public static final String ATTR_PWD_FAILED_ATTEMPTS = "dxPwdFailedAttempts";
    public static final String ATTR_RACKER_ID = "rackerId";
    public static final String ATTR_RACKSPACE_API_KEY = "rsApiKey";
    public static final String ATTR_IDENTITY_PROVIDER_ID = "rsIdentityProviderId";

    /**
     * @deprecated
     */
    public static final String ATTR_RACKSPACE_CUSTOMER_NUMBER = "RCN";

    public static final String ATTR_RS_RACKSPACE_CUSTOMER_NUMBER = "rsRackspaceCustomerNumber";
    public static final String ATTR_RACKSPACE_PERSON_NUMBER = "RPN";
    public static final String ATTR_REFRESH_TOKEN = "refreshToken";
    public static final String ATTR_REFRESH_TOKEN_EXP = "refreshTokenExp";
    public static final String ATTR_REGION = "rsRegion";
    public static final String ATTR_SCOPE = "rsScope";
    public static final String ATTR_SECURE_ID = "rsSecureId";
    public static final String ATTR_SERVICE = "service";
    public static final String ATTR_SESSION_INACTIVITY_TIMEOUT = "rsSessionInactivityTimeout";
    public static final String ATTR_PASSWORD_POLICY = "rsPasswordPolicy";
    public static final String ATTR_PASSWORD_HISTORY = "rsPasswordHistory";
    public static final String ATTR_SN = "rsSn";
    public static final String ATTR_TENANT_DISPLAY_NAME = "tenantDisplayName";
    public static final String ATTR_TIME_ZONE = "timeZone";
    public static final String ATTR_TITLE = "title";
    public static final String ATTR_TOKEN_SCOPE = "tokenScope";
    public static final String ATTR_UID = "uid";
    public static final String ATTR_USE_FOR_DEFAULT_REGION = "useForDefaultRegion";
    public static final String ATTR_UPDATED_DATE = "modifyTimestamp";
    public static final String ATTR_PASSWORD_UPDATED_TIMESTAMP = "passwordUpdatedTimestamp";
    public static final String ATTR_PASSWORD_SELF_UPDATED = "passwordSelfUpdated";
    public static final String ATTR_VERSION_ID = "versionId";
    public static final String ATTR_VERSION_INFO = "versionInfo";
    public static final String ATTR_VERSION_LIST = "versionList";
    public static final String ATTR_GLOBAL = "rsGlobal";
    public static final String ATTR_OPENSTACK_TYPE = "openstackType";
    public static final String ATTR_IDP_POLICY = "rsIdpPolicy";
    public static final String ATTR_IDP_POLICY_FORMAT = "rsIdpPolicyFormat";
    public static final String ATTR_IDP_METADATA = "rsIdpMetadata";
    public static final String ATTR_IMPERSONATING_USERNAME = "impersonatingUsername";
    public static final String ATTR_IMPERSONATING_RS_ID = "rsImpersonatingRsId";
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
    public static final String ATTR_RS_TENANT_TYPE = "rsTenantType";
    public static final String ATTR_VALUE = "value";
    public static final String ATTR_ENCRYPTION_SALT = "encryptionSalt";
    public static final String ATTR_ENCRYPTION_VERSION_ID = "encryptionVersionId";
    public static final String ATTR_IDP_NAME = "rsIdpName";
    public static final String ATTR_TENANT_ALIAS = "rsTenantAlias";
    public static final String ATTR_CONTACT_ID = "rsContactId";
    public static final String ATTR_PHONE_PIN = "phonePin";

    public static final String ATTR_TENANT_RS_ID = "tenantRsId";
    public static final String ATTR_ROLE_RS_ID = "roleRsId";
    public static final String ATTR_USER_RS_ID = "userRsId";

    public static final String ATTR_REGEX = "pattern";
    public static final String ATTR_ERRMSG = "errMsg";

    // Identity Property attributes
    public static final String ATTR_PROPERTY_VALUE = "rsPropertyValue";
    public static final String ATTR_PROPERTY_VALUE_TYPE = "rsPropertyValueType";
    public static final String ATTR_PROPERTY_VERSION = "rsIdmVersion";
    public static final String ATTR_PROPERTY_SEARCHABLE = "rsSearchable";
    public static final String ATTR_PROPERTY_RELOADABLE = "rsReloadable";

    //multifactor attributes
    public static final String ATTR_TELEPHONE_NUMBER = "telephoneNumber";
    public static final String ATTR_EXTERNAL_MULTIFACTOR_PHONE_ID = "rsExternalMultiFactorPhoneId";
    public static final String ATTR_EXTERNAL_MULTIFACTOR_USER_ID = "rsExternalMultiFactorUserId";
    public static final String ATTR_MULTIFACTOR_MOBILE_PHONE_RSID = "rsMultiFactorMobilePhoneRsId";
    public static final String ATTR_MULTIFACTOR_DEVICE_PIN = "rsMultiFactorDevicePin";
    public static final String ATTR_MULTIFACTOR_DEVICE_PIN_EXPIRATION = "rsMultiFactorDevicePinExpiration";
    public static final String ATTR_MULTIFACTOR_DEVICE_VERIFIED = "rsMultiFactorDeviceVerified";
    public static final String ATTR_MULTI_FACTOR_ENABLED = "rsMultiFactorEnabled";
    public static final String ATTR_MULTI_FACTOR_STATE = "rsMultiFactorState";
    public static final String ATTR_MULTI_FACTOR_FAILED_ATTEMPT_COUNT = "rsMultiFactorFailedAttemptCount";
    public static final String ATTR_MULTI_FACTOR_LAST_FAILED_TIMESTAMP = "rsMultiFactorLastFailedTimestamp";
    public static final String ATTR_MEMBER = "member";
    public static final String ATTR_COMMON_NAME = "cn";

    // Keystone V3 compatibility attributes
    public static final String ATTR_INTERNAL_URL_ID = "rsInternalUrlId";
    public static final String ATTR_PUBLIC_URL_ID = "rsPublicUrlId";
    public static final String ATTR_ADMIN_URL_ID = "rsAdminUrlId";

    // Delegation Agreement attributes
    public static final String ATTR_RS_PRINCIPAL_DN = "rsPrincipalDN";
    public static final String ATTR_RS_DELEGATE_DNS = "rsDelegateDNs";

    // Definitions for LDAP DNs
    public static final String EXTERNAL_PROVIDERS_BASE_DN = "o=externalProviders,dc=rackspace,dc=com";
    protected static final String EXTERNAL_PROVIDERS_USER_CONTAINER_NAME = "users";

    protected static final String BASE_DN = "o=rackspace,dc=rackspace,dc=com";
    protected static final String SCOPE_ACCESS_BASE_DN = "dc=rackspace,dc=com";
    protected static final String IDENTITY_USER_BASE_DN = "dc=rackspace,dc=com";
    public static final String BASEURL_BASE_DN = "ou=baseUrls,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String GROUP_BASE_DN = "ou=groups,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String TENANT_BASE_DN = "ou=tenants,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String TENANT_TYPE_BASE_DN = "ou=tenantTypes,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    public static final String DOMAIN_BASE_DN = "ou=domains,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String POLICY_BASE_DN = "ou=policies,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String QUESTION_BASE_DN = "ou=questions,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String REGION_BASE_DN = "ou=regions,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String APPLICATIONS_BASE_DN = "ou=applications,o=rackspace,dc=rackspace,dc=com";
    public static final String USERS_BASE_DN = "ou=users,o=rackspace,dc=rackspace,dc=com";
    protected static final String RACKERS_BASE_DN = "ou=rackers,o=rackspace,dc=rackspace,dc=com";
    protected static final String FEDERATED_RACKERS_BASE_DN = EXTERNAL_PROVIDERS_BASE_DN;
    protected static final String NEXT_IDS_BASE_DN = "ou=nextIds,o=rackspace,dc=rackspace,dc=com";
    protected static final String PATTERN_BASE_DN = "ou=patterns,ou=configuration,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String PROPERTY_BASE_DN = "ou=properties,ou=configuration,ou=cloud,o=rackspace,dc=rackspace,dc=com";
    protected static final String MULTIFACTOR_MOBILE_PHONE_BASE_DN = "ou=mobilePhones,ou=multiFactorDevices,o=rackspace,dc=rackspace,dc=com";
    protected static final String TOKEN_REVOCATION_BASE_DN = "ou=TRRs,o=tokens,dc=rackspace,dc=com";
    protected static final String IDENTITY_PROPERTIES_BASE_DN = "ou=properties,ou=cloud,o=configuration,dc=rackspace,dc=com";
    public static final String DELEGATION_AGREEMENT_BASE_DN = "ou=delegationAgreements,ou=cloud,o=rackspace,dc=rackspace,dc=com";

    // Definitions for Contatiner Names
    protected static final String CONTAINER_ROLES = "ROLES";
    protected static final String CONTAINER_TOKENS = "TOKENS";
    protected static final String CONTAINER_USERS = "USERS";
    protected static final String CONTAINER_APPLICATION_ROLES = "CLIENT ROLES";
    protected static final String CONTAINER_OTP_DEVICES = "OTPDEVICES";
    protected static final String CONTAINER_BYPASS_CODES = "BYPASSCODES";

    // Search Attributes
    protected static final String[] ATTR_USER_SEARCH_ATTRIBUTES_NO_PWD_HIS = {
            ATTR_OBJECT_CLASS, ATTR_ID, ATTR_UID, ATTR_MAIL, ATTR_PASSWORD_UPDATED_TIMESTAMP, ATTR_PASSWORD_SELF_UPDATED, ATTR_PASSWORD_SECRET_Q,
            ATTR_PASSWORD_SECRET_A, ATTR_PASSWORD_SECRET_Q_ID, ATTR_PASSWORD, ATTR_DISPLAY_NAME, ATTR_RACKSPACE_API_KEY,
            ATTR_REGION, ATTR_NAST_ID, ATTR_MOSSO_ID, ATTR_CREATED_DATE, ATTR_UPDATED_DATE, ATTR_PWD_ACCOUNT_LOCKOUT_TIME, ATTR_PWD_FAILED_ATTEMPTS,
            ATTR_ENABLED, ATTR_DOMAIN_ID, ATTR_ENCRYPTION_VERSION_ID, ATTR_ENCRYPTION_SALT, ATTR_GROUP_ID,
            ATTR_MULTIFACTOR_MOBILE_PHONE_RSID, ATTR_MULTIFACTOR_DEVICE_PIN, ATTR_MULTIFACTOR_DEVICE_PIN_EXPIRATION,
            ATTR_MULTIFACTOR_DEVICE_VERIFIED, ATTR_MULTI_FACTOR_ENABLED, ATTR_EXTERNAL_MULTIFACTOR_USER_ID,
            ATTR_MULTIFACTOR_USER_ENFORCEMENT_LEVEL, ATTR_TOKEN_FORMAT, ATTR_MULTIFACTOR_TYPE, ATTR_CONTACT_ID,
            ATTR_MULTI_FACTOR_LAST_FAILED_TIMESTAMP, ATTR_MULTI_FACTOR_FAILED_ATTEMPT_COUNT, ATTR_USER_GROUP_DNS, ATTR_PHONE_PIN };
    protected static final String[] ATTR_PROV_FED_USER_SEARCH_ATTRIBUTES_NO_PWD_HIS = ObjectArrays.concat(ATTR_USER_SEARCH_ATTRIBUTES_NO_PWD_HIS, new String[] { ATTR_URI, ATTR_FEDERATED_USER_EXPIRED_TIMESTAMP}, String.class);
    protected static final String[] ATTR_USER_SEARCH_ATTRIBUTES = {"*", ATTR_CREATED_DATE, ATTR_UPDATED_DATE, ATTR_PWD_ACCOUNT_LOCKOUT_TIME, ATTR_PWD_FAILED_ATTEMPTS};

    protected static final String[] ATTR_DEFAULT_SEARCH_ATTRIBUTES = {"*"};
    protected static final String[] ATTR_KEY_SEARCH_ATTRIBUTES = {"*", ATTR_CREATED_DATE};
    protected static final String[] ATTR_REGION_SEARCH_ATTRIBUTES = {"*"};
    protected static final String[] ATTR_CLIENT_ROLE_SEARCH_ATTRIBUTES = {"*"};
    protected static final String[] ATTR_SCOPE_ACCESS_ATTRIBUTES = {"*", ATTR_CREATED_DATE};
    protected static final String[] ATTR_IDENTITY_PROVIDER_ATTRIBUTES = {ATTR_OU, ATTR_NAME, ATTR_URI, ATTR_DESCRIPTION,
                                                                         ATTR_USER_CERTIFICATES, ATTR_TARGET_USER_SOURCE,
                                                                         ATTR_APPROVED_DOMAIN_GROUP, ATTR_APPROVED_DOMAIN_IDS,
                                                                         ATTR_AUTHENTICATION_URL, ATTR_IDP_POLICY, ATTR_ENABLED,
                                                                         ATTR_IDP_POLICY_FORMAT, ATTR_EMAIL_DOMAINS};
    public static final String LDAP_SEARCH_ERROR = "LDAP Search error - {}";

    //LDAP Change Event
    public static final String OBJECTCLASS_CHANGE_EVENT = "rsChangeEvent";
    public static final String ATTR_CHANGE_OCCURRED_DATE = "rsChangeOccurredDate";
    public static final String ATTR_CHANGE_TYPE = "rsChangeType";
    public static final String ATTR_ENTITY_TYPE = "rsEntityType";
    public static final String ATTR_ENTITY_ID = "rsEntityId";
    public static final String ATTR_HOST_NAME = "rsHostName";
    public static final String CHANGE_EVENT_BASE_DN = "ou=changeevent,dc=rackspace,dc=com";

    @Autowired
    protected LdapConnectionPools connPools;

    @Autowired
    protected Configuration config;

    @Autowired
    protected ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    protected IdentityConfig identityConfig;

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
        if (identityConfig.getReloadableConfig().useSubtreeDeleteControlForSubtreeDeletion()) {
            deleteEntryAndSubtreeUsingSubtreeDeleteControl(dn, audit);
        }
        else {
            deleteEntryAndSubtreeUsingRecursion(dn, audit);
        }
    }

    protected void deleteEntryAndSubtreeUsingSubtreeDeleteControl(String dn, Audit audit) {
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

    protected void deleteEntryAndSubtreeUsingRecursion(String dn, Audit audit) {
        try {

            SearchResult searchResult = getAppInterface().search(dn, SearchScope.ONE,
                    "(objectClass=*)", ATTR_NO_ATTRIBUTES);

            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                deleteEntryAndSubtree(entry.getDN(), audit);
            }

            getAppInterface().delete(dn);
        } catch (LDAPException e) {
            audit.fail();
            getLogger().error(LDAP_SEARCH_ERROR, e.getMessage());
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    protected boolean shouldEmitEventForDN(String dn) {
        if (dn.endsWith(CHANGE_EVENT_BASE_DN) || dn.contains("cn=" + CONTAINER_TOKENS) || dn.contains("ou=" + KEY_DISTRIBUTION_OU)) {
            //don't record tokens or change events
            return false;
        }
        return true;
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

    protected SearchResult search(String baseDN, SearchScope scope, Filter searchFilter, String... attributes) {
        SearchResult entry;
        try {
            entry = getAppInterface().search(baseDN, scope,
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

    protected int getLdapPagingOffsetDefault() {
        return config.getInt("ldap.paging.offset.default");
    }

    protected int getLdapPagingLimitDefault() {
        return config.getInt("ldap.paging.limit.default");
    }

    protected String getRackspaceInumPrefix() {
        return config.getString("rackspace.inum.prefix");
    }

    protected static final String NEXT_ROLE_ID = "nextRoleId";
    protected static final String NEXT_GROUP_ID = "nextGroupId";
    protected static final String NEXT_DOMAIN_ID = "nextDomainId";
    protected static final String NEXT_CAPABILITY_ID = "nextCapabilityId";
    protected static final String NEXT_QUESTION_ID = "nextQuestionId";
    protected static final String NEXT_POLICY_ID = "nextPolicyId";
    protected static final String NEXT_PATTERN_ID = "nextPatternId";

    protected boolean useUuidForRsId() {
        return false;
    }

    protected String getUuid() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    protected String getNextId(String type) {
        if (useUuidForRsId()) {
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

        public LdapSearchBuilder addNotEqualAttribute(String attribute,
            String value) {
            Filter filter = Filter.createNOTFilter(Filter.createEqualityFilter(attribute, value));
            filters.add(filter);
            return this;
        }

        public LdapSearchBuilder addEqualAttribute(String attribute,
            byte[] value) {
            Filter filter = Filter.createEqualityFilter(attribute, value);
            filters.add(filter);
            return this;
        }

        public LdapSearchBuilder addLessOrEqualAttribute(String attribute,
                                                            String value) {
            Filter filter = Filter.createLessOrEqualFilter(attribute, value);
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

        public LdapSearchBuilder addSubStringAttribute(String attribute,
                                                       String subInitial, String[] subAny, String subFinal) {
            Filter filter = Filter.createSubstringFilter(attribute, subInitial, subAny, subFinal);
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
