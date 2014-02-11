package com.rackspace.idm.api.resource.cloud.v20;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Purpose of this post processor is to set the status of the multi-factor feature flag
 * in the Configuration of the application. This is useful when the configuration needs to be
 * changed as part of the standard Spring application context setup as opposed to post setup.
 * For example, within Grizzly when test cases do not have access to the Spring context
 * loaded within Grizzly.
 */
public class ConfigOverrideMultiFactorFeatureFlagPostProcessor implements BeanPostProcessor {

    private String overrideValue;

    public ConfigOverrideMultiFactorFeatureFlagPostProcessor(String overrideValue) {
        this.overrideValue = overrideValue;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if(bean instanceof Configuration) {
            Configuration config = (Configuration) bean;
            config.setProperty("multifactor.services.enabled", overrideValue);
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }
}
