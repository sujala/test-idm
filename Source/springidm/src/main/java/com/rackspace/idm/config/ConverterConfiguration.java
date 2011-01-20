package com.rackspace.idm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.rackspace.idm.converters.AuthConverter;
import com.rackspace.idm.converters.ClientConverter;
import com.rackspace.idm.converters.CustomerConverter;
import com.rackspace.idm.converters.EndPointConverter;
import com.rackspace.idm.converters.PasswordConverter;
import com.rackspace.idm.converters.PasswordRulesConverter;
import com.rackspace.idm.converters.PermissionConverter;
import com.rackspace.idm.converters.RoleConverter;
import com.rackspace.idm.converters.TokenConverter;
import com.rackspace.idm.converters.UserConverter;

@Configuration
public class ConverterConfiguration {
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
    public RoleConverter roleConverter() {
        return new RoleConverter(permissionConverter());
    }

    @Bean
    ClientConverter clientConverter() {
        return new ClientConverter(permissionConverter());
    }

    @Bean
    UserConverter userConverter() {
        return new UserConverter(roleConverter());
    }

    @Bean
    CustomerConverter customerConverter() {
        return new CustomerConverter();
    }

    @Bean
    EndPointConverter endpointConverter() {
        return new EndPointConverter();
    }

    @Bean
    TokenConverter tokenConverter() {
        return new TokenConverter();
    }

    @Bean
    AuthConverter authConverter() {
        return new AuthConverter(tokenConverter(), permissionConverter(),
            clientConverter(), userConverter(), endpointConverter());
    }
}
