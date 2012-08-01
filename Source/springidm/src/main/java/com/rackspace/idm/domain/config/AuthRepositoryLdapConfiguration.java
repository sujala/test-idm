package com.rackspace.idm.domain.config;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import java.security.GeneralSecurityException;

/**
 * @author john.eo <br/>
 *         Automatic Spring configuration for LDAP to be consumed by Spring.
 */
@org.springframework.context.annotation.Configuration
public class AuthRepositoryLdapConfiguration {
    private static final int DEFAULT_SERVER_PORT = 636;
    private static final int SERVER_POOL_SIZE_INIT = 1;
    private static final int SERVER_POOL_SIZE_MAX = 100;
    private static final String CONNECT_ERROR_STRING = "Could not connect/bind to the LDAP server instance. Make sure that the LDAP server is available and that the bind credential is correct.";
    private static final String LDAP_CONFIG_ERROR_STRING = "Could not configure the LDAP data source. Make sure that the configuration file exists and is readable.";

    @Autowired
    private Configuration config;
    private final Logger logger = LoggerFactory.getLogger(AuthRepositoryLdapConfiguration.class);

    public AuthRepositoryLdapConfiguration() {
        this(false);
    }

    /**
     * Use for testing.
     * 
     * @param isTestMode
     *            Set to <b>true</b> if test LDAP config is to be used.
     */
    public AuthRepositoryLdapConfiguration(boolean isTestMode) {
        if (!isTestMode) {
            return;
        }

        // Unit-test mode setup
        try {
            config = new PropertiesConfiguration("auth.repository.properties");
        } catch (ConfigurationException e) {
            logger.error("Could not load LDAP config file.", e);
            throw new IllegalStateException(LDAP_CONFIG_ERROR_STRING, e);
        }
    }

    @Bean(destroyMethod = "close", name = "connectionToAuth")
    public LDAPConnectionPool connection() {
        
        boolean isTrusted = config.getBoolean("ldap.server.trusted", false);
        if (!isTrusted) {
            return null;
        }
        
        String host = config.getString("auth.ldap.server");
        int port = config.getInt("auth.ldap.server.port", DEFAULT_SERVER_PORT);

        int initPoolSize = config.getInt("auth.ldap.server.pool.size.init",
            SERVER_POOL_SIZE_INIT);
        int maxPoolSize = config.getInt("auth.ldap.server.pool.size.max",
            SERVER_POOL_SIZE_MAX);
        Object[] params = {host, port, initPoolSize, maxPoolSize};
        logger
            .debug(
                "LDAP Config [address={}, port={}, connection_pool_init={}, connection_pool_max={}",
                params);

        LDAPConnectionPool connPool = null;
        try {
            SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
            LDAPConnection conn = new LDAPConnection(sslUtil.createSSLSocketFactory(), host, port);
            connPool = new LDAPConnectionPool(conn, initPoolSize, maxPoolSize);
        } catch (LDAPException e) {
            logger.error(CONNECT_ERROR_STRING, e);
            throw new IllegalStateException(CONNECT_ERROR_STRING, e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot Connect Securely", e);
        }

        logger.debug("LDAPConnectionPool:[{}]", connPool);
        return connPool;
    }

    public Configuration getConfig() {
        return config;
    }

    public void setConfig(Configuration config) {
        this.config = config;
    }
}
