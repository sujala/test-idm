package com.rackspace.idm.domain.dao.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.sql.dao.RoleRepository;
import com.rackspace.idm.domain.sql.dao.TenantRoleRepository;
import com.rackspace.idm.domain.sql.entity.SqlRole;
import com.rackspace.idm.domain.sql.mapper.impl.RoleMapper;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@SQLComponent
public class SqlApplicationRoleRepository implements ApplicationRoleDao {

    @Autowired
    private RoleMapper mapper;

    @Autowired
    private RoleRepository repository;

    @Autowired
    private TenantRoleRepository tenantRoleRepository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void addClientRole(Application application, ClientRole role) {
        role.setClientId(application.getClientId());
        final SqlRole sqlRole = repository.save(mapper.toSQL(role));

        final ClientRole clientRole = mapper.fromSQL(sqlRole, role);
    }

    @Override
    @Transactional
    public void updateClientRole(ClientRole role) {
        final SqlRole sqlRole = repository.save(mapper.toSQL(role, repository.findOne(role.getId())));

        final ClientRole clientRole = mapper.fromSQL(sqlRole, role);
    }

    @Override
    @Transactional
    public void deleteClientRole(ClientRole role) {
        tenantRoleRepository.deleteBySqlRoleId(role.getId());
        repository.delete(role.getId());

    }

    @Override
    public ClientRole getClientRole(ClientRole role) {
        return mapper.fromSQL(repository.findOne(role.getId()));
    }

    @Override
    public ClientRole getClientRole(String roleId) {
        return mapper.fromSQL(repository.findOne(roleId));
    }

    @Override
    public ClientRole getClientRoleByApplicationAndName(Application application, ClientRole role) {
        return mapper.fromSQL(repository.findByRaxClientIdAndName(application.getClientId(), role.getName()));
    }

    @Override
    public ClientRole getClientRoleByApplicationAndName(String applicationId, String roleName) {
        return mapper.fromSQL(repository.findByRaxClientIdAndName(applicationId, roleName));
    }

    @Override
    public ClientRole getRoleByName(String roleName) {
        return mapper.fromSQL(repository.findByName(roleName));
    }

    @Override
    public Iterable<ClientRole> getClientRolesForApplication(Application application) {
        return mapper.fromSQL(repository.findByRaxClientId(application.getClientId()));
    }

    @Override
    public Iterable<ClientRole> getClientRolesWithRoleType(RoleTypeEnum roleTypeEnum) {
        throw new NotImplementedException();
    }

    @Override
    public PaginatorContext<ClientRole> getAvailableClientRolesByName(String roleName, int maxWeightAvailable, int offset, int limit) {
        throw new NotImplementedException("Not implemented");
    }

    @Override
    public PaginatorContext<ClientRole> getAvailableClientRolesPaged(int offset, int limit, int maxWeightAvailable) {
        PaginatorContext<ClientRole> page = mapper.getPageRequest(offset, limit);
        while (mapper.fromSQL(repository.findByRaxRsWeightGreaterThan(maxWeightAvailable, page.getPageRequest()), page)) {}
        return page;
    }

    @Override
    public PaginatorContext<ClientRole> getAvailableClientRolesPaged(String applicationId, int offset, int limit, int maxWeightAvailable) {
        PaginatorContext<ClientRole> page = mapper.getPageRequest(offset, limit);
        while (mapper.fromSQL(repository.findByRaxClientIdAndRaxRsWeightGreaterThan(applicationId, maxWeightAvailable, page.getPageRequest()), page)) {}
        return page;
    }

    @Override
    public String getNextRoleId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public Iterable<ClientRole> getIdentityRoles(Application application, List<String> roleNames) {
        return mapper.fromSQL(repository.findByNameIn(roleNames));
    }

    @Override
    public Iterable<ClientRole> getClientRoles(List<String> roleIds) {
        return mapper.fromSQL(repository.findByIdIn(roleIds));
    }

    @Override
    public Iterable<ClientRole> getAllIdentityRoles() {
        return mapper.fromSQL(repository.findByNameStartsWith(GlobalConstants.IDENTITY_ROLE_PREFIX));
    }

    @Override
    public int countClientRolesByTenantType(String tenantType) {
        return 0;
    }

}
