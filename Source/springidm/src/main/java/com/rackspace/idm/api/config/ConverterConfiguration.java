package com.rackspace.idm.api.config;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import com.rackspace.idm.api.converter.AuthConverter;
import com.rackspace.idm.api.converter.ClientConverter;
import com.rackspace.idm.api.converter.CustomerConverter;
import com.rackspace.idm.api.converter.EndPointConverter;
import com.rackspace.idm.api.converter.GroupConverter;
import com.rackspace.idm.api.converter.PasswordConverter;
import com.rackspace.idm.api.converter.PasswordRulesConverter;
import com.rackspace.idm.api.converter.PermissionConverter;
import com.rackspace.idm.api.converter.TokenConverter;
import com.rackspace.idm.api.converter.UserConverter;

@org.springframework.context.annotation.Configuration
public class ConverterConfiguration {
    @Autowired
    private Configuration config;

    @Bean
    public PermissionConverter permissionConverter() {
        return new PermissionConverter();
    }

    @Bean
    public PasswordConverter passwordConverter() {
        return new PasswordConverter();
    }

    @Bean
    public PasswordRulesConverter passwordRulesConverter() {
        return new PasswordRulesConverter();
    }

    @Bean
    ClientConverter clientConverter() {
        return new ClientConverter();
    }

    @Bean
    GroupConverter groupConverter() {
        return new GroupConverter();
    }

    @Bean
    UserConverter userConverter() {
        return new UserConverter();
    }

    @Bean
    CustomerConverter customerConverter() {
        return new CustomerConverter();
    }

    @Bean
    EndPointConverter endpointConverter() {
        return new EndPointConverter(config);
    }

    @Bean
    TokenConverter tokenConverter() {
        return new TokenConverter();
    }

    @Bean
    AuthConverter authConverter() {
        return new AuthConverter(tokenConverter(), clientConverter(),
            userConverter(), endpointConverter());
    }
}
