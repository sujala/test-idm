package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.service.DomainService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 8/6/12
 * Time: 3:42 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultDomainService implements DomainService{

    @Autowired
    private Configuration config;

    public static final String DOMAIN_CANNOT_BE_NULL = "Domain cannot be null";
    public static final String DOMAIN_NAME_CANNOT_BE_NULL = "Domain name cannot be null or empty";
    private final DomainDao domainDao;
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public DefaultDomainService(DomainDao domainDao) {
        this.domainDao = domainDao;
    }

    @Override
    public void addDomain(Domain domain) {
        if(domain == null){
            throw new BadRequestException(DOMAIN_CANNOT_BE_NULL);
        }
        if(StringUtils.isBlank(domain.getName())) {
            throw new BadRequestException(DOMAIN_NAME_CANNOT_BE_NULL);
        }
        logger.info("Adding Domain: {}", domain);
        domain.setDomainId(this.domainDao.getNextDomainId());
        domainDao.addDomain(domain);
    }

    @Override
    public Domain getDomain(String domainId) {
        return domainDao.getDomain(domainId);
    }

    @Override
    public void updateDomain(Domain domain) {
        if(domain == null){
            throw new BadRequestException(DOMAIN_CANNOT_BE_NULL);
        }
        if(StringUtils.isBlank(domain.getName())) {
            throw new BadRequestException(DOMAIN_NAME_CANNOT_BE_NULL);
        }
        domainDao.updateDomain(domain);
    }

    @Override
    public void deleteDomain(String domainId) {
        domainDao.deleteDomain(domainId);
    }

    @Override
    public void addTenantToDomain(String tenantId, String domainId) {
        Domain domain = getDomain(domainId);
        if(domain == null)
            throw new NotFoundException("Domain could not be found");

        List<String> tenantIds = setTenantIdList(domain, tenantId);
        tenantIds.add(tenantId);
        domain.setTenantIds(tenantIds.toArray(new String[tenantIds.size()]));
        domainDao.updateDomain(domain);
    }

    @Override
    public void removeTenantFromDomain(String tenantId, String domainId) {
        Domain domain = getDomain(domainId);
        if(domain == null)
            throw new NotFoundException("Domain could not be found");

        List<String> tenantIds = setTenantIdList(domain, tenantId);
        domain.setTenantIds(tenantIds.toArray(new String[tenantIds.size()]));
        domainDao.updateDomain(domain);
    }

    private List<String> setTenantIdList(Domain domain, String tenantId) {
        List<String> tenantIds = new ArrayList<String>();
        if(domain.getTenantIds() != null){
            for(String tenant : domain.getTenantIds()){
                if(!tenant.equals(tenantId)){
                    tenantIds.add(tenant);
                }
            }
        }
        return tenantIds;
    }

}
