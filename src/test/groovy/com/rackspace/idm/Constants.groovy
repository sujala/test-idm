package com.rackspace.idm

class Constants {
    static def SERVICE_ADMIN_USERNAME = "AuthQE"
    static def SERVICE_ADMIN_PASSWORD = "Auth1234"

    static def SERVICE_ADMIN_2_USERNAME = "AuthQE2"
    static def SERVICE_ADMIN_2_PASSWORD = "Auth1234"

    static def IDENTITY_ADMIN_USERNAME = "auth"
    static def IDENTITY_ADMIN_PASSWORD = "auth123"
    static def DEFAULT_PASSWORD = "Password1"
    static def DEFAULT_API_KEY = "Password1"


    static def NAST_ROLE_ID = "5"
    static def MOSSO_ROLE_ID = "6"
    static def USER_MANAGE_ROLE_ID = "7"

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
}
