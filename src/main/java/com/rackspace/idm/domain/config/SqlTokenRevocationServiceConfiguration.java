package com.rackspace.idm.domain.config;


import com.rackspace.idm.domain.service.AETokenRevocationService;
import com.rackspace.idm.domain.service.TokenRevocationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("SQL")
@Configuration
public class SqlTokenRevocationServiceConfiguration {

    @Autowired
    private AETokenRevocationService aeTokenRevocationService;

    @Bean(name = "tokenRevocationService")
    public TokenRevocationService getTokenRevocationService() {
        return aeTokenRevocationService;
    }

}
