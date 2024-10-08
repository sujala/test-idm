package com.rackspace.idm

class Constants {
    static def X_AUTH_TOKEN = "X-Auth-Token"
    static def POST = "POST"

    static def SERVICE_ADMIN_ID = "173190"
    static def SERVICE_ADMIN_USERNAME = "AuthQE"
    static def SERVICE_ADMIN_PASSWORD = "Auth1234"

    static def SERVICE_ADMIN_ID_2 = "173195"
    static def SERVICE_ADMIN_2_USERNAME = "AuthQE2"
    static def SERVICE_ADMIN_2_PASSWORD = "Auth1234"
    static def SERVICE_ADMIN_2_API_KEY = "1234567890"

    static def IDENTITY_ADMIN_USERNAME = "auth"
    static def IDENTITY_ADMIN_PASSWORD = "auth123"
    static def IDENTITY_ADMIN_API_KEY = "thisismykey"

    static def USER_WITH_NO_DOMAIN_USERNAME = "userWithNoDomain"
    static def TEST_USER_USER_ADMIN_USERNAME = "testUserAdmin_doNotDelete"

    static def DEFAULT_PASSWORD = "Password1"
    static def EASY_PASSWORD = "Pass"
    static def BLACKLISTED_PASSWORD = "blackListedPassword1"
    static def BLACKLISTED_PASSWORD_1 = "blackListedPassword11"
    static def DEFAULT_API_KEY = "Password1"

    static def SCOPE_SETUP_MFA = "SETUP-MFA"

    static def NAST_ROLE_ID = "5"
    static def MOSSO_ROLE_ID = "6"

    static def SERVICE_ADMIN_ROLE_ID = "4"
    static def IDENTITY_ADMIN_ROLE_ID = "1"
    static def USER_ADMIN_ROLE_ID = "3"
    static def USER_MANAGE_ROLE_ID = "7"
    static def USER_MANAGE_ROLE_NAME = "identity:user-manage"
    static def DEFAULT_USER_ROLE_ID = "2"
    static def DEFAULT_USER_ROLE_NAME = "identity:default"
    static def IDENTITY_INTERNAL_ROLE_ID = "17"
    static def IDENTITY_RS_TENANT_ADMIN_ROLE_ID = "18"
    static def IDENTITY_RS_DOMAIN_ADMIN_ROLE_ID = "19"
    static def IDENTITY_RS_ENDPOINT_ADMIN_ROLE_ID = "20"

    static def IDENTITY_PROVIDER_MANAGER_ROLE_ID = "8470c459cfa043ef989976a66ba4d74e"
    static def IDENTITY_PROVIDER_MANAGER_ROLE_NAME = "identity:identity-provider-manager"
    static def IDENTITY_PROVIDER_READ_ONLY_ROLE_ID = "33e5af5cbc1b45f897a09cf24fffcd74"
    static def PURGE_TRR_ROLE_ID = "6e567515bacb4ba18a04d99915198b55"
    static def IDENTITY_ENDPOINT_RULE_ADMIN_ROLE_ID = "67f155a58ae841d28a745a99200a583a"
    static def IDENTITY_TENANT_ACCESS_ROLE_ID = "51ce91246bfc4a5982f77b55fe786fa1"
    static def IDENTITY_TENANT_ACCESS_ROLE_NAME = "identity:tenant-access"
    static def IDENTITY_RCN_CLOUD_TENANT_ROLE_ID = "ee3ee1fb9be046ed969b93a1c45bbaca"
    static def IDENTITY_RCN_CLOUD_TENANT_ROLE_NAME = "identity:rcn-cloud"
    static def IDENTITY_RCN_FILES_TENANT_ROLE_ID = "aa981813224e4d56ab5eb2b02b62b303"
    static def IDENTITY_RCN_FILES_TENANT_ROLE_NAME = "identity:rcn-files"
    static def IDENTITY_RCN_MANAGED_HOSTING_TENANT_ROLE_ID = "c87fa82f7a594c40b028aa312fd20019"
    static def IDENTITY_RCN_MANAGED_HOSTING_TENANT_ROLE_NAME = "identity:rcn-managed_hosting"
    static def IDENTITY_RCN_ALL_TENANT_ROLE_ID = "1d7be4300e2141a5b49b66397ac8a145"
    static def IDENTITY_RCN_ALL_TENANT_ROLE_NAME = "identity:rcn-all"
    static def RCN_ADMIN_ROLE_NAME = "rcn:admin"
    static def RCN_ADMIN_ROLE_ID = "c5f683e2b554ee8b28b9a8b612954c4"
    static def IDENTITY_CHANGE_DOMAIN_ADMIN_ROLE_ID="bfa826160e644c4fa48fd2da3f039153"
    static def IDENTITY_CHANGE_DOMAIN_ADMIN_ROLE_NAME="identity:domain-admin-change"
    static def IDENTITY_SWITCH_DOMAIN_RCN_ROLE_ID="1f83e03681cc11e7a7e037b1affb8d28"
    static def IDENTITY_SWITCH_DOMAIN_RCN_ROLE_NAME="identity:domain-rcn-switch"
    static def IDENTITY_UPDATE_USERNAME_ROLE_ID="055871b85f8911e89da027ba45309430"

    static def IDENTITY_ANALYZE_TOKEN_ROLE_ID = "2b4d276b677d47dd85d254168ed452d2"
    static def IDENTITY_PHONE_PIN_ADMIN_ROLE_ID = "fda6705a7ba640598f3eacd9884d8a0e"
    static def IDENTITY_MIGRATE_DOMAIN_ADMIN = "b5e1ace62bb511e8b4670ed5f89f718b"

    static def DEFAULT_GROUP = "Default"
    static def DEFAULT_GROUP_ID = "0"
    static def RAX_STATUS_RESTRICTED_GROUP_NAME = "rax_status_restricted"
    static def RAX_STATUS_RESTRICTED_GROUP_ID = "1"

    static def DEFAULT_SECRET_ANWSER = "home"
    static def DEFAULT_SECRET_QUESTION_ID = "1"
    static def DEFAULT_RAX_KSQA_SECRET_QUESTION = "question"
    static def DEFAULT_RAX_KSQA_SECRET_ANWSER = "anwser"

    static String RACKER = "test.racker"
    static String RACKER_PASSWORD = "password"

    static String RACKER_IMPERSONATE = "test.impersonate"
    static String RACKER_IMPERSONATE_PASSWORD = "password"

    static String RACKER_NOGROUP = "test.nogroup"
    static String RACKER_NOGROUP_PASSWORD = "password"

    /**
     * This "username" is the uid associated with multiple entries in LDAP
     */
    static String RACKER_DUP_UID = "dup.uid"
    static String RACKER_DUP_UID_PASSWORD = "password"

    static def IDENTITY_CLIENT_ID = "bde1268ebabeeabb70a0e702a4626977c331d5c4"
    static def CLIENT_ID = "18e7a7032733486cd32f472d7bd58f709ac0d221"
    static def CLIENT_SECRET = "Password1"

    static def MOSSO_V1_DEF_US = ["15", "120"]
    static def NAST_V1_DEF_US = ["103", "111"]

    static def MOSSO_V1_DEF_UK = ["1015"]
    static def NAST_V1_DEF_UK = ["1008"]

    static def MOSSO_ENDPOINT_TEMPLATE_ID = "1026"
    static def MOSSO_ENDPOINT_TEMPLATE_PUBLIC_URL = "https://dfw.servers.api.rackspacecloud.com/v2"
    static def NAST_ENDPOINT_TEMPLATE_ID = "1003"
    static def NAST_ENDPOINT_TEMPLATE_PUBLIC_URL = "https://storage.stg.swift.racklabs.com/v1"

    static def NAST_TENANT_PREFIX = "MossoCloudFS_"

    static def TENANT_TYPE_CLOUD = 'cloud'
    static def TENANT_TYPE_FILES = 'files'
    static def TENANT_TYPE_MANAGED_HOSTING = 'managed_hosting'
    static def TENANT_TYPE_FAWS = 'faws'
    static def TENANT_TYPE_RCN = 'rcn'
    static def TENANT_TYPE_PROTECTED_PREFIX = 'protectedprefix'
    static def TENANT_TYPE_WHITELIST_TEST = 'wl_tenant_type'

    static def IDENTITY_USER_ADMIN_ROLE = "identity:user-admin"
    static def DEFAULT_OBJECT_STORE_ROLE = "object-store:default"
    static def DEFAULT_OBJECT_STORE_ROLE_ID = "5"
    static def DEFAULT_COMPUTE_ROLE = "compute:default"
    static def DEFAULT_COMPUTE_ROLE_ID = "6"


    static def DEFAULT_COMPUTE_APPLICATION_NAME = 'cloudServers'

    static def DEFAULT_FED_EMAIL = "federated@rackspace.com"
    static def DEFAULT_SAML_EXP_SECS = 1 * 60 * 60 //1 hour

    /* ************************************************************************************
    V1 IDPs. These IDPs are configured, by default, in the repose configuration to be Fed v1 API consumers. This only
    matters to Repose
    ************************************************************************************ */
    static def DEFAULT_IDP_ID = "test"
    static def DEFAULT_IDP_URI = "http://test.rackspace.com"
    static def DEFAULT_IDP_PRIVATE_KEY = "saml.pkcs8"
    static def DEFAULT_IDP_PUBLIC_KEY = "saml.crt"

    static def IDP_2_ID = "identityqe"
    static def IDP_2_URI = "http://identityqe.rackspace.com"
    static def IDP_2_PUBLIC_KEY = "saml-qe-idp.crt"
    static def IDP_2_PRIVATE_KEY = "saml-qe-idp.pkcs8"

    static def RACKER_IDP_ID = "rackertest"
    static def RACKER_IDP_URI = "http://racker.rackspace.com"
    static def RACKER_IDP_PRIVATE_KEY = "saml.pkcs8"
    static def RACKER_IDP_PUBLIC_KEY = "saml.crt"
    static def RACKER_IDP_PUBLIC_KEY_2 = "saml-qe-idp.crt"
    static def RACKER_IDP_PRIVATE_KEY_2 = "saml-qe-idp.pkcs8"
    /* *********************************************************************************** */

    /* ************************************************************************************
    V2 IDPs. These IDPs are configured, by default, in the repose configuration to be Fed v2 API consumers. This only
    matters to Repose. For simplicity, the domain/racker IDPs are configured w/ the same keys
    ************************************************************************************ */
    static String IDP_V2_DOMAIN_ID = "38471f9c27064940ba8154f4b2217269"
    static String IDP_V2_DOMAIN_URI = "http://v2domain1.rackspace.com"
    static String IDP_V2_DOMAIN_PRIVATE_KEY = "saml.pkcs8"
    static String IDP_V2_DOMAIN_PUBLIC_KEY = "saml.crt"

    static String IDP_V2_RACKER_ID = "8b78bf44f88342278a3c75951bf22262"
    static String IDP_V2_RACKER_URI = "http://v2racker1.rackspace.com"
    static String IDP_V2_RACKER_PRIVATE_KEY = "saml.pkcs8"
    static String IDP_V2_RACKER_PUBLIC_KEY = "saml.crt"

    static String DEFAULT_BROKER_IDP_ID = "8f2502e8e41c49869b0de0b2b205c0df"
    static String DEFAULT_BROKER_IDP_URI = "http://broker.rackspace.com"
    static String DEFAULT_BROKER_IDP_PRIVATE_KEY = "fed-broker.pkcs8"
    static String DEFAULT_BROKER_IDP_PUBLIC_KEY = "fed-broker.crt"
    /* *********************************************************************************** */


    public static String MFA_DEFAULT_PIN = "1234"
    public static String MFA_DEFAULT_USER_PROVIDER_ID = "USER123"
    public static String MFA_DEFAULT_PHONE_PROVIDER_ID = "PHONE123"
    public static String MFA_DEFAULT_BYPASS_CODE_1 = "BYPASS1"
    public static String MFA_DEFAULT_BYPASS_CODE_2 = "BYPASS2"
    public static String MFA_DEFAULT_BYPASS_CODE_3 = "BYPASS3"

    public static String IDENTITY_SERVICE_ID = "bde1268ebabeeabb70a0e702a4626977c331d5c4"
    public static String SERVERS_SERVICE_ID = "a45b14e394a57e3fd4e45d59ff3693ead204998b"

    public static final String TEST_KEYS_LOCATION = "/keys";

    public static final int TEST_MOCK_FEEDS_PORT = 8887
    public static final String TEST_MOCK_FEEDS_PATH = "/namespace/feed"
    public static final String TEST_MOCK_FEEDS_URL = "http://localhost:" + TEST_MOCK_FEEDS_PORT + TEST_MOCK_FEEDS_PATH

    /**
     * A canned rbac (user manager assignable) role that is included in default ldif
     */
    public static final String ROLE_RBAC1_NAME = "rbacRole1"
    public static final String ROLE_RBAC1_ID = "22776"

    /**
     * A canned rbac (user manager assignable) role that is included in default ldif
     */
    public static final String ROLE_RBAC2_NAME = "rbacRole2"
    public static final String ROLE_RBAC2_ID = "122776"

    /* ******************************
    Repository property ids
     ******************************* */
    public static final String REPO_PROP_FEATURE_AUTHORIZATION_DOMAIN_DEFAULT_ID = "aswerfbmwersdf"
    public static final String REPO_PROP_FEATURE_AUTHORIZATION_DOMAIN_VERIFICATION_ID = "kjajkhweroiudfnwer"
    public static final String REPO_PROP_FEATURE_ENABLE_USE_DOMAIN_TYPE_ON_NEW_USER_CREATION_ID = "9931a88060ea1d2r5c1ec5bc873de26a3"
    public static final String REPO_PROP_FEATURE_ENABLE_USE_DOMAIN_TYPE_FOR_UPDATE_USER_ID = "4331a77060ea1d2r5a1ec5bc873de37b4"
    public static final String REPO_PROP_TOKEN_CACHE_CONFIG_ID = "a21d8b34f1e498995c5c3be192ed666"

    /**
     * Hierarchical roles for nested da
     */
    public static final String ADMIN_ROLE_ID="d54478e2df284130a77accb5c4f60ba8";
    public static final String OBSERVER_ROLE_ID="8ac541ae2e9d4ab3879e14bd9003fc01";
    public static final String BILLING_ADMIN_ROLE_ID="3fabab074eb04b56a6052b634aaf1610";
    public static final String BILLING_OBSERVER_ROLE_ID="4fb94f6f7fcb4085bec35ceaaebb4f17";
    public static final String TICKETING_ADMIN_ROLE_ID="2d48788b89b34a65b24bfc2df1690819";
    public static final String TICKETING_OBSERVER_ROLE_ID="318c6ae090504cf79cc13f051d700e57";

    public static final String RCN_ALLOWED_FOR_INVITE_USERS = 'RCN-TEST1'

    /**
     * Reusable error message
     */
    public static String ERROR_MSG_400_INVALID_JSON = "Invalid json request body"
    public static String ERROR_MSG_401_INVALID_TOKEN = "No valid token provided. Please use the 'X-Auth-Token' header with a valid token."
    public static String ERROR_MSG_403_FORBIDDEN = "Not Authorized"
    public static String ERROR_MSG_503_SERVICE_UNAVAILABE = "Service Not Found"

    public static final String PHONE_PIN_LOCKED_EMAIL_SUBJECT = "Rackspace Phone Pin Locked"
    public static final String PHONE_PIN_LOCKED_EMAIL_FROM = "no-reply@rackspace.com"
}
