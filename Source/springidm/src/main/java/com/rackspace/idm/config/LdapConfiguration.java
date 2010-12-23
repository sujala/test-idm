package com.rackspace.idm.config;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import com.rackspace.idm.dao.LdapConnectionPools;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;

/**
 * @author john.eo <br/>
 *         Automatic Spring configuration for LDAP to be consumed by Spring.
 */
@org.springframework.context.annotation.Configuration
public class LdapConfiguration {
    private static final int SERVER_PORT = 389;
    private static final int SERVER_POOL_SIZE_INIT = 1;
    private static final int SERVER_POOL_SIZE_MAX = 100;
    private static final String CONNECT_ERROR_STRING = "Could not connect/bind to the LDAP server instance. Make sure that the LDAP server is available and that the bind credential is correct.";

    @Autowired
    private Configuration config;
    private Logger logger;

    public LdapConfiguration() {
        logger = LoggerFactory.getLogger(LdapConfiguration.class);
    }

    /**
     * Use for unit testing.
     * 
     * @param config
     * @param logger
     */
    public LdapConfiguration(Configuration config, Logger logger) {
        this.config = config;
        this.logger = logger;
    }

    /**
     * Convenience method to create LDAPConnectionPool instance.
     * 
     * @return
     */
    private LDAPConnectionPool connection() {
        String address = config.getString("ldap.server.address");
        int port = config.getInt("ldap.server.port", SERVER_PORT);
        int initPoolSize = config.getInt("ldap.server.pool.size.init",
            SERVER_POOL_SIZE_INIT);
        int maxPoolSize = config.getInt("ldap.server.pool.size.max",
            SERVER_POOL_SIZE_MAX);
        String bindDn = config.getString("ldap.bind.dn");
        String password = config.getString("ldap.bind.password");
        Object[] params = {address, port, initPoolSize, maxPoolSize};
        logger
            .debug(
                "LDAP Config [address={}, port={}, connection_pool_init={}, connection_pool_max={}",
                params);

        LDAPConnectionPool connPool = null;
        try {
            LDAPConnection conn = new LDAPConnection(address, port, bindDn,
                password);
            connPool = new LDAPConnectionPool(conn, initPoolSize, maxPoolSize);
        } catch (LDAPException e) {
            System.out.println(e);
            logger.error(CONNECT_ERROR_STRING, e);
            throw new IllegalStateException(CONNECT_ERROR_STRING, e);
        }

        logger.debug("LDAPConnectionPool:[{}]", connPool);
        return connPool;
    }

    @Bean(destroyMethod = "close")
    public LdapConnectionPools connectionPools() {
        return new LdapConnectionPools(connection(), connection());
    }
}
