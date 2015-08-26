package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.DomainDao;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.sql.event.SqlMigrationChangeApplicationEvent;
import com.rackspace.idm.domain.sql.dao.DomainRepository;
import com.rackspace.idm.domain.sql.entity.SqlDomain;
import com.rackspace.idm.domain.sql.mapper.impl.DomainMapper;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SQLComponent
public class SqlDomainRepository implements DomainDao {

    @Autowired
    private DomainMapper mapper;

    @Autowired
    private DomainRepository domainRepository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void addDomain(Domain domain) {
        final SqlDomain sqlDomain = domainRepository.save(mapper.toSQL(domain));

        final Domain newDomain = mapper.fromSQL(sqlDomain, domain);
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.ADD, newDomain.getUniqueId(), mapper.toLDIF(newDomain)));
    }

    @Override
    @Transactional
    public void updateDomain(Domain domain) {
        final SqlDomain sqlDomain = domainRepository.save(mapper.toSQL(domain, domainRepository.findOne(domain.getDomainId())));

        final Domain newDomain = mapper.fromSQL(sqlDomain, domain);
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.MODIFY, newDomain.getUniqueId(), mapper.toLDIF(newDomain)));
    }

    @Override
    @Transactional
    public void deleteDomain(String domainId) {
        final SqlDomain sqlDomain = domainRepository.findOne(domainId);
        domainRepository.delete(domainId);

        final Domain newDomain = mapper.fromSQL(sqlDomain);
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.MODIFY, newDomain.getUniqueId(), null));
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
    public Iterable<Domain> getDomainsForTenant(List<Tenant> tenants) {
        return null;
    }

}
