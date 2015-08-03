
package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.sql.dao.TenantRoleRepository;
import com.rackspace.idm.domain.sql.entity.SqlTenantRole;
import com.rackspace.idm.domain.sql.mapper.impl.TenantRoleMapper;
import com.rackspace.idm.exception.ClientConflictException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.List;

@SQLComponent
public class SqlTenantRoleRepository implements TenantRoleDao {

    @Autowired
    TenantRoleRepository tenantRoleRepository;

    @Autowired
    TenantRoleMapper mapper;

    @Override
    public void addTenantRoleToUser(BaseUser user, TenantRole tenantRole) {
        List<SqlTenantRole> sqlTenantRoles  = tenantRoleRepository.findByActorIdAndRoleId(user.getId(), tenantRole.getRoleRsId());
        tenantRole.setUserId(user.getId());

        if(containsTenantRole(sqlTenantRoles, mapper.toSQL(tenantRole))){
            throw new ClientConflictException();
        }

        tenantRoleRepository.save(mapper.toSQL(tenantRole));
    }

    @Override
    public Iterable<TenantRole> getTenantRolesForUser(BaseUser user) {
        return mapper.fromSQLList(tenantRoleRepository.findByActorId(user.getId()));
    }

    @Override
    public Iterable<TenantRole> getTenantRolesForUser(BaseUser user, String applicationId) {
        return mapper.fromSQLList(tenantRoleRepository.findByActorIdAndSqlRoleRaxClientId(user.getId(), applicationId));
    }

    @Override
    public Iterable<TenantRole> getTenantRolesForScopeAccess(ScopeAccess scopeAccess) {
        if(scopeAccess instanceof UserScopeAccess){
            return mapper.fromSQLList(tenantRoleRepository.findByActorId(((UserScopeAccess) scopeAccess).getUserRsId()));
        }
        return new ArrayList<TenantRole>();
    }

    @Override
    public Iterable<TenantRole> getAllTenantRolesForTenant(String tenantId) {
        return mapper.fromSQLList(tenantRoleRepository.findByTargetId(tenantId));
    }

    @Override
    public Iterable<TenantRole> getAllTenantRolesForTenantAndRole(String tenantId, String roleId) {
        return mapper.fromSQLList(tenantRoleRepository.findByTargetIdAndRoleId(tenantId, roleId));
    }

    @Override
    public Iterable<TenantRole> getAllTenantRolesForClientRole(ClientRole role) {
        return mapper.fromSQLList(tenantRoleRepository.findByRoleId(role.getId()));
    }

    @Override
    public TenantRole getTenantRoleForUser(BaseUser user, String roleId) {
        return mapper.fromSQL(tenantRoleRepository.findByActorIdAndRoleId(user.getId(), roleId));
    }

    @Override
    public void updateTenantRole(TenantRole tenantRole) {
        List<SqlTenantRole> sqlTenantRoles = mapper.toSQLList(tenantRole);
        List<SqlTenantRole> existingSqlTenantRoles = tenantRoleRepository.findByActorIdAndRoleId(tenantRole.getUserId(), tenantRole.getRoleRsId());

        for(SqlTenantRole sqlTenantRole : existingSqlTenantRoles){
            if(!containsTenantRole(sqlTenantRoles, sqlTenantRole)){
                tenantRoleRepository.delete(sqlTenantRole);
            }
        }

        for(SqlTenantRole sqlTenantRole : sqlTenantRoles){
            if(!containsTenantRole(existingSqlTenantRoles, sqlTenantRole)){
                tenantRoleRepository.save(sqlTenantRole);
            }
        }
    }

    @Override
    public void deleteTenantRoleForUser(EndUser user, TenantRole tenantRole) {
        for(SqlTenantRole sqlTenantRole : mapper.toSQLList(tenantRole)){
            sqlTenantRole.setActorId(user.getId());
            tenantRoleRepository.delete(sqlTenantRole);
        }
    }

    @Override
    public void deleteTenantRole(TenantRole tenantRole) {
        tenantRoleRepository.delete(mapper.toSQLList(tenantRole));
    }

    @Override
    public List<String> getIdsForUsersWithTenantRole(String roleId, int maxResult) {
        Pageable resultsSize = new PageRequest(0, maxResult);
        return getUserIds(tenantRoleRepository.findByRoleId(roleId, resultsSize));
    }

    @Override
    public Iterable<TenantRole> getTenantRoleForUser(EndUser user, List<ClientRole> clientRoles) {
        List<String> sqlRoles = new ArrayList<String>();
        for( ClientRole clientRole : clientRoles){
            sqlRoles.add(clientRole.getId());
        }

        return mapper.fromSQLList(tenantRoleRepository.findByActorIdAndRoleIdIn(user.getId(), sqlRoles));
    }

    /*
     * getUserIdForParent will never get called in SQL since tenantRole are created from the assignment table which
     * requires a tenant role to be tied to a userId.
     *
     * TODO: Remove logic using this method.
     */
    @Override
    @Deprecated
    public String getUserIdForParent(TenantRole tenantRole) {
        return null;
    }

    private List<String> getUserIds(List<SqlTenantRole> sqlTenantRoles) {
        List<String> userIds = new ArrayList<String>();
        for(SqlTenantRole sqlTenantRole : sqlTenantRoles){
            userIds.add(sqlTenantRole.getActorId());
        }
        return userIds;
    }

    private boolean containsTenantRole(List<SqlTenantRole> sqlTenantRoles, SqlTenantRole sqlTenantRole){
        for(SqlTenantRole tenantRole : sqlTenantRoles){
            if(tenantRole.getActorId().equalsIgnoreCase(sqlTenantRole.getActorId())
                    && tenantRole.getTargetId().equalsIgnoreCase(sqlTenantRole.getTargetId())
                    && tenantRole.getRoleId().equalsIgnoreCase(sqlTenantRole.getRoleId())){
                return true;
            }
        }
        return false;
    }
}