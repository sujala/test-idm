package com.rackspace.idm.jython;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;
import org.springframework.web.context.support.WebApplicationObjectSupport;

@Component
public class Objects extends WebApplicationObjectSupport implements InitializingBean, DisposableBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(Objects.class);

    private static final Object LOCK = new Object();

    private static Objects SELF;

    @Override
    public void afterPropertiesSet() {
        synchronized (LOCK) {
            try {
                if (getWebApplicationContext() != null) {
                    SELF = this;
                } else {
                    LOGGER.error("Empty application context");
                }
            } catch (IllegalStateException e) {
                LOGGER.error("Invalid application context type", e);
            }
        }
    }

    @Override
    public void destroy() {
        synchronized (LOCK) {
            if (SELF == this) {
                SELF = null;
            }
        }
    }

    public static Object getBean(Class<?> clazz) {
        return SELF.getWebApplicationContext().getBean(clazz);
    }

    public static Object getBeanByName(String beanName) {
        return SELF.getWebApplicationContext().getBean(beanName);
    }


}
