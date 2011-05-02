package com.rackspace.idm.domain.config;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import com.rackspace.idm.domain.dao.ApiDocDao;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.CustomerDao;
import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.dao.ScopeAccessObjectDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.dao.impl.FileSystemApiDocRepository;
import com.rackspace.idm.domain.dao.impl.LdapAuthRepository;
import com.rackspace.idm.domain.dao.impl.LdapClientRepository;
import com.rackspace.idm.domain.dao.impl.LdapConnectionPools;
import com.rackspace.idm.domain.dao.impl.LdapCustomerRepository;
import com.rackspace.idm.domain.dao.impl.LdapEndpointRepository;
import com.rackspace.idm.domain.dao.impl.LdapScopeAccessPeristenceRepository;
import com.rackspace.idm.domain.dao.impl.LdapUserRepository;
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
    public CustomerDao ldapCustomerRepository() {
        return new LdapCustomerRepository(connPools, appConfig);
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
    public ApiDocDao apiDocDao() {
        return new FileSystemApiDocRepository();
    }

    @Bean
    public ScopeAccessObjectDao scopeAccessObjectRepository() {
        return new LdapScopeAccessPeristenceRepository(connPools, appConfig);
    }

}
