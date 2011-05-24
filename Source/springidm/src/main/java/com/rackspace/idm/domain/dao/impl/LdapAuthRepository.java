package com.rackspace.idm.domain.dao.impl;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.AuthDao;
import com.unboundid.ldap.sdk.BindResult;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;

public class LdapAuthRepository implements AuthDao {

    private static final String BASE_DN = "ou=users,o=rackspace";
    private final LDAPConnectionPool connPool;
    private final Configuration config;
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public LdapAuthRepository(LDAPConnectionPool connPool, Configuration config) {
        this.connPool = connPool;
        this.config = config;
    }

    
    public boolean authenticate(String userName, String password) {
        logger.debug("Authenticating racker {}", userName);
        BindResult result = null;

        Audit audit = Audit.authRacker(userName);

        String userDn = String.format("cn=%s,", userName) + getBaseDn();

        try {
            result = connPool.bind(userDn, password);
        } catch (LDAPException e) {
            if (ResultCode.INVALID_CREDENTIALS.equals(e.getResultCode())) {
                logger.info("Invalid login attempt by racker {}", userName);
                audit.fail("Incorrect Credentials");
                return false;
            }
            logger.error("Bind operation on username " + userName + " failed.",
                e);
            throw new IllegalStateException(e);
        }
        if (result == null) {
            audit.fail();
            throw new IllegalStateException("Could not get bind result.");
        }
        logger.debug(result.toString());

        audit.succeed();
        return ResultCode.SUCCESS.equals(result.getResultCode());
    }

    private String getBaseDn() {
        return config.getString("auth.ldap.base.dn", BASE_DN);
    }
}
