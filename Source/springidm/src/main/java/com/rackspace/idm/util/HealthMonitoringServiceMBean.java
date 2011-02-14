package com.rackspace.idm.util;

public interface HealthMonitoringServiceMBean {
    
    public boolean pingMemcache();
    
    public boolean pingLDAP();

}
