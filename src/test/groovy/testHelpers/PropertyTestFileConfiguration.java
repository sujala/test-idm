package testHelpers;

import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PropertyTestFileConfiguration extends PropertyFileConfiguration {

    @Override
    public org.apache.commons.configuration.Configuration getConfig(){
        ConfigurationWrapper configurationWrapper = new ConfigurationWrapper();
        configurationWrapper.setConfig(super.getConfig());
        return configurationWrapper;
    }
}
