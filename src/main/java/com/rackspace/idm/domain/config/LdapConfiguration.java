package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.dao.impl.LdapConnectionPools;
import com.unboundid.ldap.sdk.*;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;

import java.security.GeneralSecurityException;

/**
 * @author john.eo <br/>
 *         Automatic Spring configuration for LDAP to be consumed by Spring.
 */
@Profile({"LDAP", "default"})
@org.springframework.context.annotation.Configuration
public class LdapConfiguration {
    private static final int DEFAULT_SERVER_PORT = 636;
    private static final String CONNECT_ERROR_STRING = "Could not connect/bind to the LDAP server instance. Make sure that the LDAP server is available and that the bind credential is correct.";
    public static final int PORT = 389;

    @Autowired
    private IdentityConfig identityConfig;

    private Logger logger=LoggerFactory.getLogger(LdapConfiguration.class);

    public LdapConfiguration() {
    }

    /**
     * Use for unit testing.
     *
     */
    public LdapConfiguration(IdentityConfig identityConfig) {
        this.identityConfig = identityConfig;
    }

    /**
     * Convenience method to create LDAPConnectionPool instance.
     * 
     * @return
     */
    LDAPConnectionPool connection() {
        String[] serverList = identityConfig.getStaticConfig().getLDAPServerList();
        String[] hosts = new String[0];
        int[] ports = new int[0];
        boolean isSSL = false;

        logger.info("Creating LDAP client pool for servers {} ", ArrayUtils.toString(serverList));
        for(String server : serverList) {
            // split on space and comma
            String[] configAddresses = (String[]) server.split("[ ,]+");
            for (String hostPort : configAddresses) {
                String[] parts = hostPort.split(":");
                hosts = (String[]) ArrayUtils.add(hosts, parts[0]);
                // default LDAP port is 636
                int port = parts.length > 1 && StringUtils.isNotBlank(parts[1]) ? Integer.valueOf(parts[1]) : DEFAULT_SERVER_PORT;
                if(port != PORT && identityConfig.getStaticConfig().getLDAPServerUseSSL()) {
                	isSSL = true;
                }
                ports = ArrayUtils.add(ports, port);
            }
        }
        int initPoolSize = identityConfig.getStaticConfig().getLDAPServerPoolSizeInit();
        int maxPoolSize = identityConfig.getStaticConfig().getLDAPServerPoolSizeMax();
        String bindDn = identityConfig.getStaticConfig().getLDAPServerBindDN();
        String password = identityConfig.getStaticConfig().getLDAPServerBindPassword();
        Object[] params = {hosts, ports, initPoolSize, maxPoolSize};
        logger.debug("LDAP Config [address={}, port={}, connection_pool_init={}, connection_pool_max={}", params);

        LDAPConnectionPool connPool = null;
        try {
            SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
            LDAPConnectionOptions ldapConnectionOptions = new LDAPConnectionOptions();
            ldapConnectionOptions.setAllowConcurrentSocketFactoryUse(identityConfig.getStaticConfig().getLDAPServerPoolAllowConcurrentSocketFactoryUse());
            ServerSet serverSet;
            if(isSSL) {
				serverSet = new RoundRobinServerSet(hosts, ports, sslUtil.createSSLSocketFactory(), ldapConnectionOptions);
            } else {
            	serverSet = new RoundRobinServerSet(hosts, ports, ldapConnectionOptions);
            }
            BindRequest bind = new SimpleBindRequest(bindDn, password);
            connPool = new LDAPConnectionPool(serverSet, bind, initPoolSize, maxPoolSize);
            connPool.setRetryFailedOperationsDueToInvalidConnections(true);
            connPool.setMaxConnectionAgeMillis(identityConfig.getStaticConfig().getLDAPServerPoolAgeMax());
            connPool.setCreateIfNecessary(identityConfig.getStaticConfig().getLDAPServerPoolCreateIfNecessary());
            connPool.setMaxWaitTimeMillis(identityConfig.getStaticConfig().getLDAPServerPoolMaxWaitTime());
            connPool.setHealthCheckIntervalMillis(identityConfig.getStaticConfig().getLDAPServerPoolHeathCheckInterval());
            connPool.setCheckConnectionAgeOnRelease(identityConfig.getStaticConfig().getLDAPServerPoolCheckConnectionAgeOnRelease());
        } catch (LDAPException e) {
            logger.error(CONNECT_ERROR_STRING, e);
            throw new IllegalStateException(CONNECT_ERROR_STRING, e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Cannot Connect Securely", e);
        }

        logger.debug("LDAPConnectionPool:[{}]", connPool);
        return connPool;
    }

    @Bean(destroyMethod = "close")
    public LdapConnectionPools connectionPools() {
        return new LdapConnectionPools(connection(), connection());
    }
}
