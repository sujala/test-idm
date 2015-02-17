package testHelpers;

import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

@Configuration
public class SingletonTestFileConfiguration extends PropertyFileConfiguration {

    /**
     * Load the jvm singleton configuration as the "config" for the application context. This allow
     * @return
     */
    @Override
    @Primary
    @Bean(name = "staticConfiguration")
    @Scope(value = "singleton")
    public org.apache.commons.configuration.Configuration getConfig(){
        SingletonConfiguration config = SingletonConfiguration.getInstance();
        return config;
    }
}
