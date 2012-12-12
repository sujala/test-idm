package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.dao.impl.LdapConnectionPools;
import com.unboundid.ldap.sdk.*;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
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
public class LdapConfiguration {
    private static final int DEFAULT_SERVER_PORT = 636;
    private static final int SERVER_POOL_SIZE_INIT = 1;
    private static final int SERVER_POOL_SIZE_MAX = 100;
    private static final String CONNECT_ERROR_STRING = "Could not connect/bind to the LDAP server instance. Make sure that the LDAP server is available and that the bind credential is correct.";
    public static final int PORT = 389;

    @Autowired
    private Configuration config;
    private Logger logger=LoggerFactory.getLogger(LdapConfiguration.class);

    public LdapConfiguration() {
    }

    /**
     * Use for unit testing.
     * 
     * @param config
     */
    public LdapConfiguration(Configuration config) {
        this.config = config;
    }

    /**
     * Convenience method to create LDAPConnectionPool instance.
     * 
     * @return
     */
    LDAPConnectionPool connection() {
        String[] serverList = config.getStringArray("ldap.serverList");
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
                if(port != PORT && config.getBoolean("ldap.server.useSSL")) {
                	isSSL = true;
                }
                ports = ArrayUtils.add(ports, port);
            }
        }
        int initPoolSize = config.getInt("ldap.server.pool.size.init", SERVER_POOL_SIZE_INIT);
        int maxPoolSize = config.getInt("ldap.server.pool.size.max", SERVER_POOL_SIZE_MAX);
        String bindDn = config.getString("ldap.bind.dn");
        String password = config.getString("ldap.bind.password");
        Object[] params = {hosts, ports, initPoolSize, maxPoolSize};
        logger.debug("LDAP Config [address={}, port={}, connection_pool_init={}, connection_pool_max={}", params);

        LDAPConnectionPool connPool = null;
        try {
            SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
            ServerSet serverSet;
            if(isSSL) {
				serverSet = new RoundRobinServerSet(hosts, ports, sslUtil.createSSLSocketFactory());
            } else {
            	serverSet = new RoundRobinServerSet(hosts, ports);
            }
            BindRequest bind = new SimpleBindRequest(bindDn, password);
            connPool = new LDAPConnectionPool(serverSet, bind, initPoolSize, maxPoolSize);

            int maxConnectionAge = config.getInt("ldap.server.pool.age.max", -1);
            if (maxConnectionAge >= 0) {
                connPool.setMaxConnectionAgeMillis(maxConnectionAge);
            }

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
