package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.dao.impl.*;
import com.unboundid.ldap.sdk.LDAPConnectionPool;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

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
    public ApplicationDao ldapClientRepository() {
        return new LdapApplicationRepository(connPools, appConfig);
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
    public LdapCloudAdminRepository cloudAdminRepository() {
        return new LdapCloudAdminRepository(connPools, appConfig);
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
    public ScopeAccessDao scopeAccessObjectRepository() {
        return new LdapScopeAccessPeristenceRepository(connPools, appConfig);
    }

    @Bean
    public TenantDao ldapTenantRepository() {
        return new LdapTenantRepository(connPools, appConfig);
    }
}
