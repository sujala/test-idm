package com.rackspace.idm.domain.config;

import com.rackspace.identity.multifactor.providers.MobilePhoneVerification;
import com.rackspace.identity.multifactor.providers.MultiFactorAuthenticationService;
import com.rackspace.identity.multifactor.providers.ProviderAvailability;
import com.rackspace.identity.multifactor.providers.UserManagement;
import com.rackspace.identity.multifactor.providers.duo.config.AdminApiConfig;
import com.rackspace.identity.multifactor.providers.duo.config.AuthApiConfig;
import com.rackspace.identity.multifactor.providers.duo.config.VerifyApiConfig;
import com.rackspace.identity.multifactor.providers.duo.config.apache.ApacheConfigAdminApiConfig;
import com.rackspace.identity.multifactor.providers.duo.config.apache.ApacheConfigAuthApiConfig;
import com.rackspace.identity.multifactor.providers.duo.config.apache.ApacheConfigVerifyApiConfig;
import com.rackspace.identity.multifactor.providers.duo.service.DuoAvailability;
import com.rackspace.identity.multifactor.providers.duo.service.DuoMobilePhoneVerification;
import com.rackspace.identity.multifactor.providers.duo.service.DuoMultiFactorAuthenticationService;
import com.rackspace.identity.multifactor.providers.duo.service.DuoUserManagement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MultiFactorConfiguration {

    @Autowired
    private org.apache.commons.configuration.Configuration config;

    @Bean
    public AdminApiConfig adminApiConfig() {
        return new ApacheConfigAdminApiConfig(config);
    }

    @Bean
    public VerifyApiConfig verifyApiConfig() {
        return new ApacheConfigVerifyApiConfig(config);
    }

    @Bean
    public AuthApiConfig authApiConfig() {
        return new ApacheConfigAuthApiConfig(config);
    }

    @Bean
    public ProviderAvailability duoAvailability() {
        DuoAvailability availability = new DuoAvailability(authApiConfig());
        availability.init();
        return availability;
    }

    @Bean
    public MobilePhoneVerification mobilePhoneVerification() {
        DuoMobilePhoneVerification bean = new DuoMobilePhoneVerification();
        bean.setVerifyApiConfig(verifyApiConfig());
        bean.init();
        return bean;
    }

    @Bean
    public MultiFactorAuthenticationService multiFactorAuthenticationService() {
        DuoMultiFactorAuthenticationService bean = new DuoMultiFactorAuthenticationService();
        bean.setAuthApiConfig(authApiConfig());
        bean.init();
        return bean;
    }

    @Bean
    public UserManagement userManagement() {
        DuoUserManagement bean = new DuoUserManagement();
        bean.setAdminApiConfig(adminApiConfig());
        bean.init();
        return bean;
    }
}
