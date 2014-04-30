package com.rackspace.idm.api.resource.cloud.v20;

import org.apache.commons.configuration.Configuration;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.util.Map;

/**
 * Purpose of this post processor is to set the status of the multi-factor feature flag
 * in the Configuration of the application. This is useful when the configuration needs to be
 * changed as part of the standard Spring application context setup as opposed to post setup.
 * For example, within Grizzly when test cases do not have access to the Spring context
 * loaded within Grizzly.
 */
public class ConfigOverridePostProcessor implements BeanPostProcessor {

    private Map<String, Object> overrideValues;

    public ConfigOverridePostProcessor(Map<String, Object> overrideValues) {
        this.overrideValues = overrideValues;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        for(Map.Entry<String, Object> overrideValueEntry : overrideValues.entrySet()) {
            if(bean instanceof Configuration) {
                Configuration config = (Configuration) bean;
                config.setProperty(overrideValueEntry.getKey(), overrideValueEntry.getValue());
            }
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

}
