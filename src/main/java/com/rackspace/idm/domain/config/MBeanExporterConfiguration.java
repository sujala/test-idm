package com.rackspace.idm.domain.config;

@org.springframework.context.annotation.Configuration
public class  MBeanExporterConfiguration {

     /*
    @Autowired
    public LoggerMBean loggerMonitoringBean;

    @Autowired
    public LdapRouterMBean ldapRouterMonitoringBean;

    @Autowired
    public MBeanExporter exporter() {
        final MBeanExporter exp = new MBeanExporter();
        final Map<String, Object> beans = new HashMap<String, Object>();
        beans.put("com.rackspace.idm:name=loggerMonitoringBean",
            loggerMonitoringBean);
        beans.put("com.rackspace.idm:name=ldapRouterMonitoringBean",
            ldapRouterMonitoringBean);
        exp.setBeans(beans);
        return exp;
    }
    */
}

