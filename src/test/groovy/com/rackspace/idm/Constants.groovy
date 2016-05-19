package com.rackspace.idm

class Constants {
    static def X_AUTH_TOKEN = "X-Auth-Token"
    static def POST = "POST"

    static def SERVICE_ADMIN_ID = "173190"
    static def SERVICE_ADMIN_USERNAME = "AuthQE"
    static def SERVICE_ADMIN_PASSWORD = "Auth1234"

    static def SERVICE_ADMIN_2_USERNAME = "AuthQE2"
    static def SERVICE_ADMIN_2_PASSWORD = "Auth1234"
    static def SERVICE_ADMIN_2_API_KEY = "1234567890"

    static def IDENTITY_ADMIN_USERNAME = "auth"
    static def IDENTITY_ADMIN_PASSWORD = "auth123"
    static def IDENTITY_ADMIN_API_KEY = "thisismykey"

    static def DEFAULT_PASSWORD = "Password1"
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

    static def IDENTITY_PROVIDER_MANAGER_ROLE_ID = "8470c459cfa043ef989976a66ba4d74e"
    static def IDENTITY_PROVIDER_MANAGER_ROLE_NAME = "identity:identity-provider-manager"
    static def IDENTITY_PROVIDER_READ_ONLY_ROLE_ID = "33e5af5cbc1b45f897a09cf24fffcd74"
    static def UPGRADE_USER_TO_CLOUD_ROLE_ID = "1920567cf12511e5b888bf8c67992003"
    static def UPGRADE_USER_TO_CLOUD_ROLE_NAME = "identity:upgrade-user-to-cloud"
    static def UPGRADE_USER_ELIGIBILITY_ROLE_ID = "a66e717ef5f011e5aa224f755bc52f48"
    static def UPGRADE_USER_ELIGIBILITY_ROLE_NAME = "identity:eligible-for-upgrade"
    static def PURGE_TRR_ROLE_ID = "6e567515bacb4ba18a04d99915198b55"

    static def DEFAULT_GROUP = "Default"

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

    static def CLIENT_ID = "18e7a7032733486cd32f472d7bd58f709ac0d221"
    static def CLIENT_SECRET = "Password1"

    static def MOSSO_V1_DEF = ["15","120"]
    static def NAST_V1_DEF = ["103","111"]

    static def NAST_TENANT_PREFIX="MossoCloudFS_"

    static def IDENTITY_USER_ADMIN_ROLE = "identity:user-admin"
    static def DEFAULT_OBJECT_STORE_ROLE = "object-store:default"
    static def DEFAULT_COMPUTE_ROLE = "compute:default"

    static def DEFAULT_IDP_NAME = "test"
    static def DEFAULT_IDP_URI = "http://test.rackspace.com"
    static def DEFAULT_IDP_PRIVATE_KEY = "saml.pkcs8"
    static def DEFAULT_IDP_PUBLIC_KEY = "saml.crt"
    static def IDP_2_NAME = "identityqe"
    static def IDP_2_URI = "http://identityqe.rackspace.com"
    static def IDP_2_PUBLIC_KEY = "saml-qe-idp.crt"
    static def IDP_2_PRIVATE_KEY = "saml-qe-idp.pkcs8"
    static def DEFAULT_FED_EMAIL = "federated@rackspace.com"

    static def RACKER_IDP_NAME = "rackertest"
    static def RACKER_IDP_URI = "http://racker.rackspace.com"
    static def RACKER_IDP_PRIVATE_KEY = "saml.pkcs8"
    static def RACKER_IDP_PUBLIC_KEY = "saml.crt"
    static def RACKER_IDP_PUBLIC_KEY_2 = "saml-qe-idp.crt"
    static def RACKER_IDP_PRIVATE_KEY_2 = "saml-qe-idp.pkcs8"

    public static String MFA_DEFAULT_PIN = "1234"
    public static String MFA_DEFAULT_USER_PROVIDER_ID = "USER123"
    public static String MFA_DEFAULT_PHONE_PROVIDER_ID = "PHONE123"
    public static String MFA_DEFAULT_BYPASS_CODE_1 = "BYPASS1"
    public static String MFA_DEFAULT_BYPASS_CODE_2 = "BYPASS2"
    public static String MFA_DEFAULT_BYPASS_CODE_3 = "BYPASS3"

    public static String IDENTITY_SERVICE_ID = "bde1268ebabeeabb70a0e702a4626977c331d5c4"

    public static final String TEST_KEYS_LOCATION = "/keys";

    public static final int TEST_MOCK_FEEDS_PORT = 8887
    public static final String TEST_MOCK_FEEDS_PATH = "/namespace/feed"
    public static final String TEST_MOCK_FEEDS_URL = "http://localhost:" + TEST_MOCK_FEEDS_PORT + TEST_MOCK_FEEDS_PATH
}
