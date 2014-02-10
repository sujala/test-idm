package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.v20.multifactor.EncryptedSessionIdReaderWriter;
import org.apache.commons.configuration.Configuration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;

/**
 * Purpose of this post processor is to set the location of the keyczar key directory in the Configuration of the application to
 * the classpath location. This is useful when the configuration needs to be changed as part of the standard spring
 * application context setup as opposed to post setup. For example, within Grizzly when test cases do not have access
 * to the spring context loaded within grizzly.
 */
public class ConfigOverrideKeyLocationPostProcessor implements BeanPostProcessor {

    public static final String TEST_KEYS_LOCATION = "/com/rackspace/idm/api/resource/cloud/v20/keys";

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof Configuration) {
            try {
                Configuration config = (Configuration) bean;
                ClassPathResource resource = new ClassPathResource(TEST_KEYS_LOCATION);
                String pathLocation = resource.getFile().getAbsolutePath();

                config.setProperty(EncryptedSessionIdReaderWriter.MULTIFACTOR_ENCRYPTION_KEY_LOCATION_PROP_NAME, pathLocation);
            } catch (IOException e) {
                throw new RuntimeException("Error setting up config with multifactor key location");
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
