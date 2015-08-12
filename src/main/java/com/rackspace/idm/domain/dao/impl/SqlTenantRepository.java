package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.TenantDao;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.sql.dao.ProjectRepository;
import com.rackspace.idm.domain.sql.entity.SqlProject;
import com.rackspace.idm.domain.sql.mapper.impl.ProjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@SQLComponent
public class SqlTenantRepository implements TenantDao {

    @Autowired
    IdentityConfig config;

    @Autowired
    ProjectMapper mapper;

    @Autowired
    ProjectRepository projectRepository;

    @Override
    @Transactional
    public void addTenant(Tenant tenant) {
        //TODO: Keystone requires domain_id to be specified
        if (tenant.getDomainId() == null) {
            tenant.setDomainId(config.getReloadableConfig().getTenantDefaultDomainId());
        }
        projectRepository.save(mapper.toSQL(tenant));
    }

    @Override
    @Transactional
    public void deleteTenant(String tenantId) {
        projectRepository.delete(tenantId);
    }

    @Override
    public Tenant getTenant(String tenantId) {
        return mapper.fromSQL(projectRepository.findOne(tenantId));
    }

    @Override
    public Tenant getTenantByName(String name) {
        return mapper.fromSQL(projectRepository.findByName(name));
    }

    @Override
    public Iterable<Tenant> getTenants() {
        return mapper.fromSQL(projectRepository.findAll());
    }

    @Override
    public PaginatorContext<Tenant> getTenantsPaged(int offset, int limit) {
        PaginatorContext<Tenant> page = mapper.getPageRequest(offset, limit);
        while (mapper.fromSQL(projectRepository.findAll(page.getPageRequest()), page)) {}
        return page;
    }

    @Override
    @Transactional
    public void updateTenant(Tenant tenant) {
        SqlProject sqlProject = projectRepository.findOne(tenant.getTenantId());
        projectRepository.save(mapper.toSQL(tenant, sqlProject));
    }
}
