package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.dao.impl.RackerConnectionPoolDelegate;
import com.unboundid.ldap.sdk.*;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import javax.net.ssl.SSLSocketFactory;
import java.security.GeneralSecurityException;


@org.springframework.context.annotation.Configuration
public class RackerAuthRepositoryLdapConfiguration {
    private static final String CONNECT_ERROR_STRING = "Could not connect/bind to the Racker LDAP server instance. Make sure that the LDAP server is available and that the bind credential is correct.";

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private RackerConnectionFactory rackerConnectionFactory;

    private final Logger logger = LoggerFactory.getLogger(RackerAuthRepositoryLdapConfiguration.class);

    @Bean(destroyMethod = "close")
    public RackerConnectionPoolDelegate connection() {
        if (!identityConfig.getStaticConfig().isRackerAuthAllowed()) {
            return null;
        }
        
        String host = identityConfig.getStaticConfig().getRackerAuthServer();
        int port = identityConfig.getStaticConfig().getRackerAuthServerPort();

        int initPoolSize = identityConfig.getStaticConfig().getRackerAuthPoolInitialSize();
        int maxPoolSize = identityConfig.getStaticConfig().getRackerAuthPoolMaxSize();

        Object[] params = {host, port, initPoolSize, maxPoolSize};
        logger
            .debug(
                "LDAP Config [address={}, port={}, connection_pool_init={}, connection_pool_max={}",
                params);

        LDAPConnectionPool connPool;
        try {
            LDAPConnection connection;
            SSLSocketFactory socketFactory = new SSLUtil(new TrustAllTrustManager()).createSSLSocketFactory();
            String bindDn = identityConfig.getStaticConfig().getRackerAuthBindDn();
            String bindPw = identityConfig.getStaticConfig().getRackerAuthBindPassword();
            connection = rackerConnectionFactory.createAuthenticatedEncryptedConnection(socketFactory, host, port, bindDn, bindPw);
            connPool = rackerConnectionFactory.createConnectionPool(connection, initPoolSize, maxPoolSize);
        } catch (LDAPException e) {
            logger.error(CONNECT_ERROR_STRING, e);
            throw new IllegalStateException(CONNECT_ERROR_STRING, e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot Connect Securely", e);
        }

        logger.debug("LDAPConnectionPool:[{}]", connPool);
        return new RackerConnectionPoolDelegate(connPool);
    }
}
