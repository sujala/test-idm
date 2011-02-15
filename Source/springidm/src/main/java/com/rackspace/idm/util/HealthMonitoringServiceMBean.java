package com.rackspace.idm.util;

public interface HealthMonitoringServiceMBean {
    
    public String getMemcacheStatus();
    
    public String getLDAPStatus();

}
