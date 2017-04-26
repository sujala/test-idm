package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.dao.TenantTypeDao;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.TenantType;
import com.rackspace.idm.domain.service.TenantTypeService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.modules.endpointassignment.dao.TenantTypeRuleDao;
import com.rackspace.idm.validation.Validator20;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.rackspace.idm.exception.NotFoundException;

@Component
public class DefaultTenantTypeService implements TenantTypeService {

    private static final String ERROR_TENANT_TYPE_MAX_SIZE_EXCEEDED = "Maximum number of tenantTypes '%' is exceeded";
    @Autowired
    TenantTypeDao tenantTypeDao;

    @Autowired
    Validator20 validator;

    @Autowired
    IdentityConfig config;

    @Autowired
    ApplicationRoleDao applicationRoleDao;

    @Autowired
    TenantDao tenantDao;

    @Autowired
    TenantTypeRuleDao tenantTypeRuleDao;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void deleteTenantType(TenantType tenantType) {
        logger.info("Deleting TenantType {}", tenantType.getName());

        if ((applicationRoleDao.countClientRolesByTenantType(tenantType.getName()) > 0) ||
            tenantDao.countTenantsByTenantType(tenantType.getName()) > 0 ||
            tenantTypeRuleDao.countRulesByTenantType(tenantType.getName()) > 0) {
            String errMsg = String.format("TenantType with name %s is referenced and cannot be deleted", tenantType.getName());
            logger.warn(errMsg);
            throw new BadRequestException(errMsg);
        }

        tenantTypeDao.deleteTenantType(tenantType);
        logger.info("Deleted TenantType {}", tenantType.getName());
    }

    @Override
    public void createTenantType(TenantType tenantType) {

        logger.info("Adding TenantType {}", tenantType);

        validator.validateTenantType(tenantType);

        TenantType exists = this.tenantTypeDao.getTenantType(tenantType.getName());
        if (exists != null) {
            String errMsg = String.format("TenantType with name %s already exists", tenantType.getName());
            logger.warn(errMsg);
            throw new DuplicateException(errMsg);
        }

        if (tenantTypeDao.countObjects() >= config.getReloadableConfig().getMaxTenantTypes()) {
            String errMsg = String.format(ERROR_TENANT_TYPE_MAX_SIZE_EXCEEDED, config.getReloadableConfig().getMaxTenantTypes());
            throw new BadRequestException(errMsg);
        }

        this.tenantTypeDao.addTenantType(tenantType);
        logger.info("Added TenantType {}", tenantType);

    }

    @Override
    public PaginatorContext<TenantType> listTenantTypes(Integer marker, Integer limit) {
        logger.info("Getting TenantTypes");
        return this.tenantTypeDao.listTenantTypes(marker, limit);
    }

    @Override
    public TenantType checkAndGetTenantType(String tenantTypeName) {
        TenantType tenantType = getTenantType(tenantTypeName);

        if (tenantType == null) {
            String errMsg = String.format("TenantType with name: '%s' was not found.", tenantTypeName);
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        return tenantType;
    }

    private TenantType getTenantType(String tenantTypeId) {
        logger.debug("Getting TenantType {}", tenantTypeId);
        TenantType tenantType = this.tenantTypeDao.getTenantType(tenantTypeId);
        logger.debug("Got TenantType {}", tenantType);
        return tenantType;
    }
}
