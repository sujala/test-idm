package com.rackspace.idm.dao;

import org.slf4j.Logger;

import com.unboundid.ldap.sdk.LDAPConnectionPool;

public abstract class LdapRepository {
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
