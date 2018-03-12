package com.rackspace.idm.domain.config;

import com.unboundid.ldap.sdk.*;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import javax.net.ssl.SSLSocketFactory;
import java.security.GeneralSecurityException;


@org.springframework.context.annotation.Configuration
public class AuthRepositoryLdapConfiguration {
    private static final int DEFAULT_SERVER_PORT = 636;
    private static final int SERVER_POOL_SIZE_INIT = 1;
    private static final int SERVER_POOL_SIZE_MAX = 100;
    private static final String CONNECT_ERROR_STRING = "Could not connect/bind to the LDAP server instance. Make sure that the LDAP server is available and that the bind credential is correct.";

    @Autowired
    private Configuration config;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private EdirConnectionFactory edirConnectionFactory;

    private final Logger logger = LoggerFactory.getLogger(AuthRepositoryLdapConfiguration.class);

    @Bean(destroyMethod = "close", name = "connectionToAuth")
    public LDAPConnectionPool connection() {
        if (!identityConfig.getStaticConfig().getEDirServerTrusted()) {
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

        LDAPConnectionPool connPool;
        try {
            LDAPConnection connection;
            SSLSocketFactory socketFactory = new SSLUtil(new TrustAllTrustManager()).createSSLSocketFactory();
            String bindDn = identityConfig.getStaticConfig().getEdirBindDn();
            String bindPw = identityConfig.getStaticConfig().getEdirBindPassword();
            connection = edirConnectionFactory.createAuthenticatedEncryptedConnection(socketFactory, host, port, bindDn, bindPw);

            LDAPConnectionOptions connectionOptions = new LDAPConnectionOptions();
            connectionOptions.setConnectTimeoutMillis(identityConfig.getStaticConfig().getEDirConnectionConnectTimeout());
            connectionOptions.setResponseTimeoutMillis(OperationType.BIND, identityConfig.getStaticConfig().getEDirConnectionBindTimeout());
            connectionOptions.setResponseTimeoutMillis(OperationType.SEARCH, identityConfig.getStaticConfig().getEDirConnectionSearchTimeout());
            connection.setConnectionOptions(connectionOptions);

            connPool = edirConnectionFactory.createConnectionPool(connection, initPoolSize, maxPoolSize);
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
