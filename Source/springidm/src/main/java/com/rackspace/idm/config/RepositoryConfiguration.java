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
import com.rackspace.idm.dao.HttpAccessTokenRepository;
import com.rackspace.idm.dao.LdapAuthRepository;
import com.rackspace.idm.dao.LdapClientRepository;
import com.rackspace.idm.dao.LdapConnectionPools;
import com.rackspace.idm.dao.LdapCustomerRepository;
import com.rackspace.idm.dao.LdapEndpointRepository;
import com.rackspace.idm.dao.LdapRefreshTokenRepository;
import com.rackspace.idm.dao.LdapRoleRepository;
import com.rackspace.idm.dao.LdapUserRepository;
import com.rackspace.idm.dao.MemcachedAccessTokenRepository;
import com.rackspace.idm.dao.RefreshTokenDao;
import com.rackspace.idm.dao.RoleDao;
import com.rackspace.idm.dao.TokenFindDeleteDao;
import com.rackspace.idm.dao.UserDao;
import com.rackspace.idm.entities.AccessToken;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;

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
    private MemcachedClient memcached;
    @Autowired
    private LDAPConnectionPool authReposConnPool;
    @Autowired
    private StartTLSExtendedRequest startTLSExtendedRequest;
    @Autowired
    private Configuration appConfig;

    @Bean
    public UserDao ldapUserRepository() {
        Logger logger = LoggerFactory.getLogger(LdapUserRepository.class);
        return new LdapUserRepository(connPools, appConfig, logger);
    }

    @Bean
    public ClientDao ldapClientRepository() {
        Logger logger = LoggerFactory.getLogger(LdapClientRepository.class);
        return new LdapClientRepository(connPools, appConfig, logger);
    }

    @Bean
    public RefreshTokenDao refreshTokenRepository() {
        Logger logger = LoggerFactory.getLogger(LdapRefreshTokenRepository.class);
        return new LdapRefreshTokenRepository(connPools, appConfig, logger);
    }

    @Bean
    public CustomerDao ldapCustomerRepository() {
        Logger logger = LoggerFactory.getLogger(LdapCustomerRepository.class);
        return new LdapCustomerRepository(connPools, appConfig, logger);
    }

    @Bean
    public RoleDao roleDao() {
        Logger logger = LoggerFactory.getLogger(LdapRoleRepository.class);
        return new LdapRoleRepository(connPools, appConfig, logger);
    }

    @Bean
    public AccessTokenDao accessTokenRepository() {
        Logger logger = LoggerFactory.getLogger(MemcachedAccessTokenRepository.class);
        return new MemcachedAccessTokenRepository(memcached, logger);
    }

    @Bean
    public AuthDao authenticationRepository() {
        Logger logger = LoggerFactory.getLogger(LdapAuthRepository.class);
        return new LdapAuthRepository(authReposConnPool, startTLSExtendedRequest, logger);
    }

    @Bean
    public EndpointDao endpointDao() {
        Logger logger = LoggerFactory.getLogger(LdapRoleRepository.class);
        return new LdapEndpointRepository(connPools, appConfig, logger);
    }

    @Bean
    public DataCenterEndpoints dcEnpoints() {
        return new DataCenterEndpoints(appConfig);
    }

    @Bean(name = "xdcTokenDao")
    public TokenFindDeleteDao<AccessToken> xdcTokenDao() {
        Logger logger = LoggerFactory.getLogger(HttpAccessTokenRepository.class);
        return new HttpAccessTokenRepository(dcEnpoints(), appConfig, logger);
    }
}
