package com.rackspace.idm.config;

import net.spy.memcached.MemcachedClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rackspace.idm.dao.AccessTokenDao;
import com.rackspace.idm.dao.ClientDao;
import com.rackspace.idm.dao.CustomerDao;
import com.rackspace.idm.dao.LdapClientRepository;
import com.rackspace.idm.dao.LdapConnectionPools;
import com.rackspace.idm.dao.LdapCustomerRepository;
import com.rackspace.idm.dao.LdapAuthRepository;
import com.rackspace.idm.dao.LdapRefreshTokenRepository;
import com.rackspace.idm.dao.LdapRoleRepository;
import com.rackspace.idm.dao.LdapUserRepository;
import com.rackspace.idm.dao.MemcachedAccessTokenRepository;
import com.rackspace.idm.dao.AuthDao;
import com.rackspace.idm.dao.RefreshTokenDao;
import com.rackspace.idm.dao.RoleDao;
import com.rackspace.idm.dao.UserDao;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;

/**
 * @author john.eo <br/>
 *         Add dependency configurations for repositories here.<br/>
 *         Note that the @Autowired object is automatically instantiated by
 *         Spring. The methods with @Bean are used by Spring to satisfy for
 *         objects with dependency for the return type.
 */
@Configuration
public class RepositoryConfiguration {

    @Autowired
    private LdapConnectionPools connPools;
    @Autowired
    private MemcachedClient memcached;
    @Autowired
    private LDAPConnectionPool authReposConnPool;
    @Autowired
    private StartTLSExtendedRequest startTLSExtendedRequest;

    @Bean
    public UserDao ldapUserRepository() {
        Logger logger = LoggerFactory.getLogger(LdapUserRepository.class);
        return new LdapUserRepository(connPools, logger);
    }

    @Bean
    public ClientDao ldapClientRepository() {
        Logger logger = LoggerFactory.getLogger(LdapClientRepository.class);
        return new LdapClientRepository(connPools, logger);
    }

    @Bean
    public RefreshTokenDao refreshTokenRepository() {
        Logger logger = LoggerFactory
            .getLogger(LdapRefreshTokenRepository.class);
        return new LdapRefreshTokenRepository(connPools, logger);
    }

    @Bean
    public CustomerDao ldapCustomerRepository() {
        Logger logger = LoggerFactory.getLogger(LdapCustomerRepository.class);
        return new LdapCustomerRepository(connPools, logger);
    }

    @Bean
    public RoleDao roleDao() {
        Logger logger = LoggerFactory.getLogger(LdapRoleRepository.class);
        return new LdapRoleRepository(connPools, logger);
    }

    @Bean
    public AccessTokenDao accessTokenRepository() {
        Logger logger = LoggerFactory
            .getLogger(MemcachedAccessTokenRepository.class);
        return new MemcachedAccessTokenRepository(memcached, logger);
    }

    @Bean
    public AuthDao authenticationRepository() {
        Logger logger = LoggerFactory.getLogger(LdapAuthRepository.class);
        return new LdapAuthRepository(authReposConnPool, 
            startTLSExtendedRequest, logger);
    }
}
