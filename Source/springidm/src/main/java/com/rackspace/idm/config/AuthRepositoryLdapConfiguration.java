package com.rackspace.idm.config;

import java.net.URL;
import java.security.GeneralSecurityException;

import javax.net.ssl.SSLContext;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustStoreTrustManager;

/**
 * @author john.eo <br/>
 *         Automatic Spring configuration for LDAP to be consumed by Spring.
 */
@org.springframework.context.annotation.Configuration
public class AuthRepositoryLdapConfiguration {
    private static final int SERVER_PORT = 389;
    private static final int SERVER_POOL_SIZE_INIT = 1;
    private static final int SERVER_POOL_SIZE_MAX = 100;
    private static final String CONNECT_ERROR_STRING = "Could not connect/bind to the LDAP server instance. Make sure that the LDAP server is available and that the bind credential is correct.";
    private static final String LDAP_CONFIG_ERROR_STRING = "Could not configure the LDAP data source. Make sure that the configuration file exists and is readable.";

    @Autowired
    private Configuration config;
    private Logger logger;

    public AuthRepositoryLdapConfiguration() {
        this(false, LoggerFactory
            .getLogger(AuthRepositoryLdapConfiguration.class));
    }

    /**
     * Use for testing.
     * 
     * @param isTestMode
     *            Set to <b>true</b> if test LDAP config is to be used.
     */
    public AuthRepositoryLdapConfiguration(boolean isTestMode, Logger logger) {
        this.logger = logger;
        if (!isTestMode) {
            return;
        }

        // Unit-test mode setup
        try {
            config = new PropertiesConfiguration("auth.repository.properties");
        } catch (ConfigurationException e) {
            System.out.println(e);
            logger.error("Could not load LDAP config file.", e);
            throw new IllegalStateException(LDAP_CONFIG_ERROR_STRING, e);
        }
    }

    @Bean(destroyMethod = "close", name = "connectionToAuth")
    public LDAPConnectionPool connection() {
        String address = config.getString("ldap.server.address");
        int port = config.getInt("ldap.server.port", SERVER_PORT);
        int initPoolSize = config.getInt("ldap.server.pool.size.init",
            SERVER_POOL_SIZE_INIT);
        int maxPoolSize = config.getInt("ldap.server.pool.size.max",
            SERVER_POOL_SIZE_MAX);
        Object[] params = {address, port, initPoolSize, maxPoolSize};
        logger
            .debug(
                "LDAP Config [address={}, port={}, connection_pool_init={}, connection_pool_max={}",
                params);

        LDAPConnectionPool connPool = null;
        try {
            LDAPConnection conn = new LDAPConnection(address, port);
            connPool = new LDAPConnectionPool(conn, initPoolSize, maxPoolSize);
        } catch (LDAPException e) {
            System.out.println(e);
            logger.error(CONNECT_ERROR_STRING, e);
            throw new IllegalStateException(CONNECT_ERROR_STRING, e);
        }

        logger.debug("LDAPConnectionPool:[{}]", connPool);
        return connPool;
    }

    @Bean
    public StartTLSExtendedRequest startTLSExtendedRequest() {
        URL url = getClass().getResource("/cacerts");
        SSLUtil sslUtil = new SSLUtil(new TrustStoreTrustManager(url.getFile()));
        SSLContext sslContext;
        try {
            sslContext = sslUtil.createSSLContext();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
        try {
            return new StartTLSExtendedRequest(sslContext);
        } catch (LDAPException e) {
            throw new IllegalStateException(e);
        }
    }
}
