package com.rackspace.idm.services;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import com.rackspace.idm.dao.AccessTokenDao;
import com.rackspace.idm.dao.LdapRepository;
import com.rackspace.idm.dao.MemcachedAccessTokenRepository;
import com.rackspace.idm.util.HealthMonitoringServiceMBean;
import com.rackspace.idm.util.PingableService;

public class HealthMonitoringService implements HealthMonitoringServiceMBean {

    private PingableService memcacheService;

    private PingableService ldapRespository;
    
    private Logger logger;
    
    public HealthMonitoringService(PingableService memcacheService, PingableService ldapRepository, 
        Logger logger) {
        this.memcacheService = memcacheService;
        this.ldapRespository = ldapRepository;
        this.logger = logger;
    }
    
    @Override
    public boolean pingMemcache() {
        return memcacheService.isAlive();
    }

    @Override
    public boolean pingLDAP() {
        return ldapRespository.isAlive();
    }
}
