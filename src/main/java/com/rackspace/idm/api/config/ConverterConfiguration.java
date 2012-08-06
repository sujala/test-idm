package com.rackspace.idm.api.config;

import com.rackspace.idm.api.converter.*;
import com.rackspace.idm.api.converter.cloudv11.AuthConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.EndpointConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.TokenConverterCloudV11;
import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;

@org.springframework.context.annotation.Configuration
public class ConverterConfiguration {
    @Autowired
    private Configuration config;

    @Bean
    RolesConverter rolesConverter() {
        return new RolesConverter();
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
    ApplicationConverter clientConverter() {
        return new ApplicationConverter(rolesConverter());
    }

    @Bean
    UserConverter userConverter() {
        return new UserConverter(rolesConverter());
    }

    @Bean
    CustomerConverter customerConverter() {
        return new CustomerConverter();
    }

    @Bean
    TokenConverter tokenConverter() {
        return new TokenConverter();
    }

    @Bean
    AuthConverter authConverter() {
        return new AuthConverter(tokenConverter(), clientConverter(),
            userConverter());
    }

    @Bean
    AuthConverterCloudV11 authConverterCloudV11() {
        return new AuthConverterCloudV11(config, tokenConverterCloudV11(),
            endpointConverterCloudV11());
    }
    
    @Bean
    CredentialsConverter credentialsConverter() {
        return new CredentialsConverter();
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
    UserConverterCloudV11 userConverterCloudV11() {
        return new UserConverterCloudV11(endpointConverterCloudV11());
    }
}
