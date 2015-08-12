package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.sql.dao.DomainRepository;
import com.rackspace.idm.domain.sql.entity.SqlDomain;
import com.rackspace.idm.domain.sql.mapper.impl.DomainMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SQLComponent
public class SqlDomainRepository implements DomainDao {

    @Autowired
    DomainMapper mapper;

    @Autowired
    DomainRepository domainRepository;

    @Override
    @Transactional
    public void addDomain(Domain domain) {
        domainRepository.save(mapper.toSQL(domain));
    }

    @Override
    public Domain getDomain(String domainId) {
        if (StringUtils.isBlank(domainId)) {
            return null;
        }
        return mapper.fromSQL(domainRepository.findOne(domainId));
    }

    @Override
    public PaginatorContext<Domain> getAllDomainsPaged(int offset, int limit) {
        PaginatorContext<Domain> page = mapper.getPageRequest(offset, limit);
        while (mapper.fromSQL(domainRepository.findAll(page.getPageRequest()), page)) {}
        return page;
    }

    @Override
    @Transactional
    public void updateDomain(Domain domain) {
        domainRepository.save(mapper.toSQL(domain, domainRepository.findOne(domain.getDomainId())));
    }

    @Override
    @Transactional
    public void deleteDomain(String domainId) {
        domainRepository.delete(domainId);
    }

    @Override
    public Iterable<Domain> getDomainsForTenant(List<Tenant> tenants) {
        return null;
    }

}
