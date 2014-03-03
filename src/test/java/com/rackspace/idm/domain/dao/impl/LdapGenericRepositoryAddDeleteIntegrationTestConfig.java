package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.test.SingleTestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * This config is used to wire the dependencies into a dummy generic repository for testing. The context file LdapEndpointRepositoryTest-context.xml
 * loads this file.
 */
@SingleTestConfiguration
class LdapGenericRepositoryAddDeleteIntegrationTestConfig {

    public LdapGenericRepositoryAddDeleteIntegrationTestConfig() {
    }

    /**
     * Raw bean. Nothing overridden.
     * @return
     */
    @Bean
    LdapGenericRepository<Application> genericApplicationRepository() {
        return new LdapGenericRepository<Application>() {
        };
    }

    /**
     * Override the base methods that throw NotImplementedExceptions
     * @return
     */
    @Bean
    LdapGenericRepository<Application> overriddenGenericApplicationRepository() {
        return new LdapGenericRepository<Application>() {
            @Override
            public String getBaseDn(){
                return APPLICATIONS_BASE_DN;
            }
        };
    }
}
