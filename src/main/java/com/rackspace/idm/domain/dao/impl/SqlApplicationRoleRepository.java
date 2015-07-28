package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.sql.dao.RoleRepository;
import com.rackspace.idm.domain.sql.mapper.impl.RoleMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.UUID;

@SQLComponent
public class SqlApplicationRoleRepository implements ApplicationRoleDao {

    @Autowired
    RoleMapper mapper;

    @Autowired
    RoleRepository repository;

    @Override
    public void addClientRole(Application application, ClientRole role) {
        role.setClientId(application.getClientId());
        repository.save(mapper.toSQL(role));
    }

    @Override
    public void updateClientRole(ClientRole role) {
        repository.save(mapper.toSQL(role));
    }

    @Override
    public void deleteClientRole(ClientRole role) {
        repository.delete(mapper.toSQL(role));
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
    public PaginatorContext<ClientRole> getAvailableClientRolesPaged(int limit, int offset, int maxWeightAvailable) {
        PaginatorContext<ClientRole> page = mapper.getPageRequest(offset, limit);
        while (mapper.fromSQL(repository.findByRaxRsWeightGreaterThan(maxWeightAvailable, page.getPageRequest()), page)) {}
        return page;
    }

    @Override
    public PaginatorContext<ClientRole> getAvailableClientRolesPaged(String applicationId, int limit, int offset, int maxWeightAvailable) {
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
        return mapper.fromSQL(repository.findAll());
    }

}
