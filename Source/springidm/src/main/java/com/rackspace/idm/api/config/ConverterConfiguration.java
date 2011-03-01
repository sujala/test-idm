package com.rackspace.idm.api.config;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import com.rackspace.idm.converters.AuthConverter;
import com.rackspace.idm.converters.ClientConverter;
import com.rackspace.idm.converters.CustomerConverter;
import com.rackspace.idm.converters.EndPointConverter;
import com.rackspace.idm.converters.GroupConverter;
import com.rackspace.idm.converters.PasswordConverter;
import com.rackspace.idm.converters.PasswordRulesConverter;
import com.rackspace.idm.converters.PermissionConverter;
import com.rackspace.idm.converters.RoleConverter;
import com.rackspace.idm.converters.TokenConverter;
import com.rackspace.idm.converters.UserConverter;

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
    public RoleConverter roleConverter() {
        return new RoleConverter(permissionConverter());
    }

    @Bean
    ClientConverter clientConverter() {
        return new ClientConverter(permissionConverter());
    }
    
    @Bean
    GroupConverter groupConverter() {
        return new GroupConverter();
    }

    @Bean
    UserConverter userConverter() {
        return new UserConverter(groupConverter());
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
        return new AuthConverter(tokenConverter(), permissionConverter(),
            clientConverter(), userConverter(), endpointConverter());
    }
}
