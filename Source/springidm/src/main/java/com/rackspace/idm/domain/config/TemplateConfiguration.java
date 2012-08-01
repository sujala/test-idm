package com.rackspace.idm.domain.config;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;

import java.io.IOException;

/**
 * @author john.eo <br/>
 *         Add dependency configurations for services here.<br/>
 *         Note that the @Autowired object is automatically instantiated by
 *         Spring. The methods with @Bean are used by Spring to satisfy for
 *         objects with dependency for the return type.
 */
@org.springframework.context.annotation.Configuration
public class TemplateConfiguration {
  
    @Bean
    @Scope(value = "singleton")
    public Configuration freeMakerConfiguration() throws IOException {
		Configuration cfg = new Configuration();
		//cfg.setDirectoryForTemplateLoading(new File("/docs"));
		cfg.setObjectWrapper(new DefaultObjectWrapper());
        return cfg;
    }
}
