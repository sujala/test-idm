package com.rackspace.idm.domain.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.service.TenantService;

public class DefaultTenantService implements TenantService{
    
    private final TenantDao tenantDao;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public DefaultTenantService(TenantDao tenantDao) {
        this.tenantDao = tenantDao;
    }

    @Override
    public void addTenant(Tenant tenant) {
        logger.info("Adding Tenant {}", tenant);
        this.tenantDao.addTenant(tenant);
        logger.info("Added Tenant {}", tenant);
    }

    @Override
    public void deleteTenant(String tenantId) {
        logger.info("Deleting Tenant {}", tenantId);
        this.tenantDao.deleteTenant(tenantId);
        logger.info("Added Tenant {}", tenantId);
    }

    @Override
    public Tenant getTenant(String tenantId) {
        logger.info("Getting Tenant {}", tenantId);
        Tenant tenant = this.tenantDao.getTenant(tenantId);
        logger.info("Got Tenant {}", tenant);
        return tenant;
    }

    @Override
    public List<Tenant> getTenants() {
        logger.info("Getting Tenants");
        List<Tenant> tenants = this.tenantDao.getTenants();
        logger.info("Got {} tenants", tenants.size());
        return tenants;
    }

    @Override
    public void updateTenant(Tenant tenant) {
        logger.info("Updating Tenant {}", tenant);
        this.tenantDao.updateTenant(tenant);
        logger.info("Updated Tenant {}", tenant);
    }

}
