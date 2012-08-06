package com.rackspace.idm.domain.dao.impl;

import com.unboundid.ldap.sdk.LDAPConnectionPool;

/**
 * Wraps two connection pools to reduce insanity.
 * 
 */
public class LdapConnectionPools {
    public static final String APP_CONN_POOL_NAME = "application";
    public static final String BIND_CONN_POOL_NAME = "bind";

    private LDAPConnectionPool appConnPool;
    private LDAPConnectionPool bindConnPool;

    public LdapConnectionPools(LDAPConnectionPool applicationConnectionPool,
        LDAPConnectionPool bindConnectionPool) {
        if (applicationConnectionPool == null || bindConnectionPool == null) {
            throw new IllegalArgumentException("One of the parameters is null");
        }

        // Reference check -- make sure they are separate objects!
        if (applicationConnectionPool.equals(bindConnectionPool)) {
            throw new IllegalArgumentException(
                "Don't pass in the same connection pool instance!!!");
        }

        appConnPool = applicationConnectionPool;
        appConnPool.setConnectionPoolName(APP_CONN_POOL_NAME);
        bindConnPool = bindConnectionPool;
        bindConnPool.setConnectionPoolName(BIND_CONN_POOL_NAME);
    }

    public LDAPConnectionPool getAppConnPool() {
        return appConnPool;
    }

    public LDAPConnectionPool getBindConnPool() {
        return bindConnPool;
    }

    public void close() {
        if (appConnPool != null) {
            appConnPool.close();
        }

        if (bindConnPool != null) {
            bindConnPool.close();
        }
    }

    public void setAppConnPool(LDAPConnectionPool appConnPool) {
        this.appConnPool = appConnPool;
    }

    public void setBindConnPool(LDAPConnectionPool bindConnPool) {
        this.bindConnPool = bindConnPool;
    }
}
