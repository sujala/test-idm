package com.rackspace.idm.config;

import java.security.GeneralSecurityException;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.RoundRobinServerSet;
import com.unboundid.ldap.sdk.ServerSet;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;

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
        
        boolean isTrusted = config.getBoolean("ldap.server.trusted", false);
        if (!isTrusted) {
            return null;
        }
       
        String[] serverList = config.getStringArray("auth.ldap.serverList");
        String[] hosts = new String[0];
        int[] ports = new int[0];

        for(String server : serverList) {
            // split on space and comma
            String[] configAddresses = (String[]) server.split("[ ,]+");
            for (String hostPort : configAddresses) {
                String[] parts = hostPort.split(":");
                hosts = (String[]) ArrayUtils.add(hosts, parts[0]);
                // default LDAP port is 636
                int port = parts.length > 1 && StringUtils.isNotBlank(parts[1]) ? Integer.valueOf(parts[1]) : DEFAULT_SERVER_PORT;
                ports = ArrayUtils.add(ports, port);
            }
        }
        int initPoolSize = config.getInt("auth.ldap.server.pool.size.init",
            SERVER_POOL_SIZE_INIT);
        int maxPoolSize = config.getInt("auth.ldap.server.pool.size.max",
            SERVER_POOL_SIZE_MAX);
        String bindDn = config.getString("auth.ldap.bind.dn");
        String password = config.getString("auth.ldap.bind.password");
        Object[] params = {hosts, ports, initPoolSize, maxPoolSize};
        logger
            .debug(
                "LDAP Config [address={}, port={}, connection_pool_init={}, connection_pool_max={}",
                params);

        LDAPConnectionPool connPool = null;
        try {
            SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
            ServerSet serverSet = new RoundRobinServerSet(hosts, ports, sslUtil.createSSLSocketFactory());
            BindRequest bind = new SimpleBindRequest(bindDn, password);
            connPool = new LDAPConnectionPool(serverSet, bind, initPoolSize, maxPoolSize);
        } catch (LDAPException e) {
            logger.error(CONNECT_ERROR_STRING, e);
            throw new IllegalStateException(CONNECT_ERROR_STRING, e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot Connect Securely", e);
        }

        logger.debug("LDAPConnectionPool:[{}]", connPool);
        return connPool;
    }
}
