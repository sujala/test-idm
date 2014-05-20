package testHelpers;

import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SingletonTestFileConfiguration extends PropertyFileConfiguration {

    /**
     * Load the jvm singleton configuration as the "config" for the application context. This allow
     * @return
     */
    @Override
    public org.apache.commons.configuration.Configuration getConfig(){
        SingletonConfiguration config = SingletonConfiguration.getInstance();
        return config;
    }
}
