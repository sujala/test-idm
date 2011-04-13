package com.rackspace.idm.domain.config;

import net.spy.memcached.MemcachedClient;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import com.rackspace.idm.domain.dao.AccessTokenDao;
import com.rackspace.idm.domain.dao.ApiDocDao;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.dao.RefreshTokenDao;
import com.rackspace.idm.domain.dao.RoleDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.dao.XdcAccessTokenDao;
import com.rackspace.idm.domain.dao.impl.FileSystemApiDocRepository;
import com.rackspace.idm.domain.dao.impl.HttpAccessTokenRepository;
import com.rackspace.idm.domain.dao.impl.LdapAuthRepository;
import com.rackspace.idm.domain.dao.impl.LdapClientRepository;
import com.rackspace.idm.domain.dao.impl.LdapConnectionPools;
import com.rackspace.idm.domain.dao.impl.LdapCustomerRepository;
import com.rackspace.idm.domain.dao.impl.LdapEndpointRepository;
import com.rackspace.idm.domain.dao.impl.LdapRefreshTokenRepository;
import com.rackspace.idm.domain.dao.impl.LdapRoleRepository;
import com.rackspace.idm.domain.dao.impl.LdapUserRepository;
import com.rackspace.idm.domain.dao.impl.MemcachedAccessTokenRepository;
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
        return new MemcachedAccessTokenRepository(memcached, appConfig);
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

    @Bean
    public ApiDocDao apiDocDao() {
        return new FileSystemApiDocRepository();
    }
}
