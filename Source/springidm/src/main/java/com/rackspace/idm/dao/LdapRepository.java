package com.rackspace.idm.dao;

import org.slf4j.Logger;

import com.unboundid.ldap.sdk.LDAPConnectionPool;

public abstract class LdapRepository {
    
    // Definitions for LDAP Objectclasses
    protected static final String[] ATTR_BASEURL_OBJECT_CLASS_VALUES = {"top", "baseUrl"};
    protected static final String[] ATTR_CLIENT_GROUP_OBJECT_CLASS_VALUES = {
        "top", "clientGroup"};
    protected static final String[] ATTR_CLIENT_OBJECT_CLASS_VALUES = {"top",
    "rackspaceApplication"};
    protected static final String[] ATTR_CUSTOMER_OBJECT_CLASS_VALUES = {"top",
    "rackspaceOrganization"};
    protected static final String[] ATTR_OBJECT_CLASS_OU_VALUES = {"top",
    "organizationalUnit"};
    protected static final String[] ATTR_PERMISSION_OBJECT_CLASS_VALUES = {
        "top", "clientPermission"};
    protected static final String[] ATTR_ROLE_OBJECT_CLASS_VALUES = {"top",
    "rackspaceGroup"};
    protected static final String[] ATTR_TOKEN_OBJECT_CLASS_VALUES = {"top",
    "rackspaceToken"};
    protected static final String[] ATTR_USER_OBJECT_CLASS_VALUES = {"top",
    "rackspacePerson"};
    
    // Definitions for LDAP Attributes
    protected static final String ATTR_ADMIN_URL = "adminUrl";
    protected static final String ATTR_BASEURL_ID = "baseUrlId";
    protected static final String ATTR_BASEURL_TYPE = "baseUrlType";
    protected static final String ATTR_BLOB = "blob";
    protected static final String ATTR_C = "c";
    protected static final String ATTR_CLIENT_ID = "rackspaceApiKey";
    protected static final String ATTR_CLIENT_SECRET = "userPassword";
    protected static final String ATTR_CREATED_DATE = "createTimestamp";
    protected static final String ATTR_DEF = "def";
    protected static final String ATTR_DISPLAY_NAME = "displayName";
    protected static final String ATTR_ENDPOINT = "endpoint";
    protected static final String ATTR_EXPIRATION = "expiration";
    protected static final String ATTR_GIVEN_NAME = "givenName";
    protected static final String ATTR_GROUP_TYPE = "groupType";
    protected static final String ATTR_INAME = "iname";
    protected static final String ATTR_INTERNAL_URL = "internalUrl";
    protected static final String ATTR_INUM = "inum";
    protected static final String ATTR_LANG = "preferredLanguage";
    protected static final String ATTR_LOCKED = "locked";
    protected static final String ATTR_MAIL = "mail";
    protected static final String ATTR_MEMBER = "member";
    protected static final String ATTR_MEMBER_OF = "isMemberOf";
    protected static final String ATTR_MIDDLE_NAME = "middleName";
    protected static final String ATTR_MOSSO_ID = "rsMossoId";
    protected static final String ATTR_NAME = "cn";
    protected static final String ATTR_NAST_ID = "rsNastId";
    protected static final String ATTR_O = "o";
    protected static final String ATTR_OBJECT_CLASS = "objectClass";
    protected static final String ATTR_OU = "ou";
    protected static final String ATTR_OWNER = "owner";
    protected static final String ATTR_PASSWORD = "userPassword";
    protected static final String ATTR_PASSWORD_SECRET_A = "secretAnswer";
    protected static final String ATTR_PASSWORD_SECRET_Q = "secretQuestion";
    protected static final String ATTR_PERMISSION = "permission";
    protected static final String ATTR_PERMISSION_TYPE = "permissionType";
    protected static final String ATTR_PUBLIC_URL = "publicUrl";
    protected static final String ATTR_PWD_ACCOUNT_LOCKOUT_TIME = "pwdAccountLockedTime";
    protected static final String ATTR_RACKSPACE_API_KEY = "rackspaceApiKey";
    protected static final String ATTR_RACKSPACE_CUSTOMER_NUMBER = "rackspaceCustomerNumber";
    protected static final String ATTR_RACKSPACE_PERSON_NUMBER = "rackspacePersonNumber";
    protected static final String ATTR_RACKSPACE_REGION = "rackspaceRegion";
    protected static final String ATTR_REGION = "rackspaceRegion";
    protected static final String ATTR_SEE_ALSO = "seeAlso";
    protected static final String ATTR_SERVICE = "service";
    protected static final String ATTR_SN = "sn";
    protected static final String ATTR_SOFT_DELETED = "softDeleted";
    protected static final String ATTR_STATUS = "status";
    protected static final String ATTR_TIME_ZONE = "timeZone";
    protected static final String ATTR_TOKEN_OWNER = "tokenOwner";
    protected static final String ATTR_TOKEN_REQUESTOR = "tokenRequestor";
    protected static final String ATTR_UID = "uid";
    protected static final String ATTR_UPDATED_DATE = "modifyTimestamp";
    
    // Definitions for LDAP DNs
    protected static final String BASE_DN = "o=rackspace,dc=rackspace,dc=com";
    protected static final String BASEURL_BASE_DN = "ou=BaseUrls,dc=rackspace,dc=com";
    protected static final String TOKEN_BASE_DN = "ou=Tokens,dc=rackspace,dc=com";

    private LdapConnectionPools conn;
    private Logger logger;

    protected LdapRepository(LdapConnectionPools conn, Logger logger) {
        this.conn = conn;
        this.logger = logger;
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
}
