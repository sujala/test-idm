package com.rackspace.idm.domain.service;

import com.rackspace.idm.util.HealthMonitoringServiceMBean;
import com.rackspace.idm.util.PingableService;

public class HealthMonitoringService implements HealthMonitoringServiceMBean {

    private PingableService memcacheService;

    private PingableService ldapRespository;
    
    private static final String memcacheStatusString = "Memcached is ";
    private static final String ldapStatusString = "LDAP is ";
    private static final String downStatus = "down.";
    private static final String upStatus = "up.";
        
    public HealthMonitoringService(PingableService memcacheService, PingableService ldapRepository) {
        this.memcacheService = memcacheService;
        this.ldapRespository = ldapRepository;
    }
    
    @Override
    public String getMemcacheStatus() {
        if (memcacheService.isAlive()) {
            return (memcacheStatusString + upStatus);
        }
        else {
            return (memcacheStatusString + downStatus);
        }
    }

    @Override
    public String getLDAPStatus() {
        if (ldapRespository.isAlive()) {
            return (ldapStatusString + upStatus);
        }
        else {
            return (ldapStatusString + downStatus);
        }
    }
}
