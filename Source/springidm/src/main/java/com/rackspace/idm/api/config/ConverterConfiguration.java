package com.rackspace.idm.api.config;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

import com.rackspace.idm.api.converter.AuthConverter;
import com.rackspace.idm.api.converter.ClientConverter;
import com.rackspace.idm.api.converter.CustomerConverter;
import com.rackspace.idm.api.converter.GroupConverter;
import com.rackspace.idm.api.converter.PasswordConverter;
import com.rackspace.idm.api.converter.PasswordRulesConverter;
import com.rackspace.idm.api.converter.PermissionConverter;
import com.rackspace.idm.api.converter.TokenConverter;
import com.rackspace.idm.api.converter.UserConverter;
import com.rackspace.idm.api.converter.cloudv11.AuthConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.TokenConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11;

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
    TokenConverter tokenConverter() {
        return new TokenConverter(permissionConverter());
    }

    @Bean
    AuthConverter authConverter() {
        return new AuthConverter(tokenConverter(), clientConverter(),
            userConverter());
    }

    @Bean
    AuthConverterCloudV11 authConverterCloudV11() {
        return new AuthConverterCloudV11(tokenConverterCloudV11(),
            endpointConverterCloudV11());
    }

    @Bean
    EndpointConverterCloudV11 endpointConverterCloudV11() {
        return new EndpointConverterCloudV11(config);
    }

    @Bean
    TokenConverterCloudV11 tokenConverterCloudV11() {
        return new TokenConverterCloudV11();
    }
    
    @Bean
    UserConverterCloudV11 userConverterCloudC11() {
        return new UserConverterCloudV11();
    }
}
