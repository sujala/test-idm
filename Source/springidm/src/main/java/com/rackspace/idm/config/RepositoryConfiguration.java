package com.rackspace.idm.config;

import net.spy.memcached.MemcachedClient;

import org.apache.commons.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import com.rackspace.idm.dao.AccessTokenDao;
import com.rackspace.idm.dao.AuthDao;
import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.dao.CustomerDao;
import com.rackspace.idm.dao.EndpointDao;
import com.rackspace.idm.dao.XdcAccessTokenDao;
import com.rackspace.idm.dao.HttpAccessTokenRepository;
import com.rackspace.idm.dao.LdapAuthRepository;
import com.rackspace.idm.dao.LdapClientRepository;
import com.rackspace.idm.dao.LdapConnectionPools;
import com.rackspace.idm.dao.LdapCustomerRepository;
import com.rackspace.idm.dao.LdapEndpointRepository;
import com.rackspace.idm.dao.LdapRefreshTokenRepository;
import com.rackspace.idm.dao.LdapRoleRepository;
import com.rackspace.idm.dao.LdapStatusRepository;
import com.rackspace.idm.dao.LdapUserRepository;
import com.rackspace.idm.dao.MemcachedAccessTokenRepository;
import com.rackspace.idm.dao.RefreshTokenDao;
import com.rackspace.idm.dao.RoleDao;
import com.rackspace.idm.dao.UserDao;
import com.rackspace.idm.util.PingableService;
import com.unboundid.ldap.sdk.LDAPConnectionPool;

/**
 * @author john.eo <br/>
 *         Add dependency configurations for repositories here.<br/>
 *         Note that the @Autowired object is automatically instantiated by
 *         Spring. The methods with @Bean are used by Spring to satisfy for
 *         objects with dependency for the return type.
 */
@org.springframework.context.annotation.Configuration
public class RepositoryConfiguration {

    @Autowired
    private LdapConnectionPools connPools;
    @Autowired
    private LDAPConnectionPool authReposConnPool;
    @Autowired
    private MemcachedClient memcached;
    @Autowired
    private Configuration appConfig;

    @Bean
    public UserDao ldapUserRepository() {
        return new LdapUserRepository(connPools, appConfig);
    }

    @Bean
    public ClientDao ldapClientRepository() {
        return new LdapClientRepository(connPools, appConfig);
    }

    @Bean
    public RefreshTokenDao refreshTokenRepository() {
        return new LdapRefreshTokenRepository(connPools, appConfig);
    }

    @Bean
    public CustomerDao ldapCustomerRepository() {
        return new LdapCustomerRepository(connPools, appConfig);
    }

    @Bean
    public RoleDao roleDao() {
        return new LdapRoleRepository(connPools, appConfig);
    }

    @Bean
    public AccessTokenDao accessTokenRepository() {
        return new MemcachedAccessTokenRepository(memcached);
    }

    @Bean
    public AuthDao authenticationRepository() {
        return new LdapAuthRepository(authReposConnPool, appConfig);
    }

    @Bean
    public EndpointDao endpointDao() {
        return new LdapEndpointRepository(connPools, appConfig);
    }

    @Bean
    public DataCenterEndpoints dcEnpoints() {
        return new DataCenterEndpoints(appConfig);
    }

    @Bean(name = "xdcTokenDao")
    public XdcAccessTokenDao xdcTokenDao() {
        return new HttpAccessTokenRepository(dcEnpoints(), appConfig);
    }
    
    @Bean(name = "ldapStatusRepository")
    public PingableService ldapStatusRepository() {
        return new LdapStatusRepository(connPools,appConfig);
    }
    
    @Bean(name = "memcacheStatusRepository")
    public PingableService memcacheStatusRepository() {
        return new MemcachedAccessTokenRepository(memcached);
    }
}
