package com.rackspace.idm.domain.config;

import com.rackspace.idm.util.LdapRouterMBean;
import com.rackspace.idm.util.LoggerMBean;
import org.springframework.context.annotation.Bean;
import org.springframework.jmx.export.MBeanExporter;

import java.util.HashMap;
import java.util.Map;

@org.springframework.context.annotation.Configuration
public class  MBeanExporterConfiguration {

    @Bean
    public LoggerMBean loggerMonitoringBean() {
        return new LoggerMBean();
    }

    @Bean
    public LdapRouterMBean ldapRouterMonitoringBean() {
        return new LdapRouterMBean();
    }

    @Bean
    public MBeanExporter exporter() {
        final MBeanExporter exp = new MBeanExporter();
        final Map<String, Object> beans = new HashMap<String, Object>();
        beans.put("com.rackspace.idm:name=loggerMonitoringBean",
            loggerMonitoringBean());
        beans.put("com.rackspace.idm:name=ldapRouterMonitoringBean",
            ldapRouterMonitoringBean());
        exp.setBeans(beans);
        return exp;
    }
}

