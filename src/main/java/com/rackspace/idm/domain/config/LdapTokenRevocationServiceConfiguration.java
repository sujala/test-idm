package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.service.AETokenRevocationService;
import com.rackspace.idm.domain.service.TokenRevocationService;
import com.rackspace.idm.domain.service.impl.RouterTokenRevocationService;
import com.rackspace.idm.domain.service.impl.SimpleAETokenRevocationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile({"LDAP", "default"})
@Configuration
public class LdapTokenRevocationServiceConfiguration {

    @Bean(name = "aeTokenRevocationService")
    public AETokenRevocationService getAETokenRevocationService() {
        return new SimpleAETokenRevocationService();
    }

    @Bean(name = "tokenRevocationService")
    public TokenRevocationService getTokenRevocationService() {
        return new RouterTokenRevocationService();
    }

}
