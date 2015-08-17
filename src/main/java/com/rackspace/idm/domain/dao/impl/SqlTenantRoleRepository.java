
package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.TenantRoleDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.sql.dao.FederatedRoleRepository;
import com.rackspace.idm.domain.sql.dao.TenantRoleRepository;
import com.rackspace.idm.domain.sql.entity.SqlFederatedRoleRax;
import com.rackspace.idm.domain.sql.entity.SqlTenantRole;
import com.rackspace.idm.domain.sql.mapper.impl.FederatedRoleRaxMapper;
import com.rackspace.idm.domain.sql.mapper.impl.TenantRoleMapper;
import com.rackspace.idm.exception.ClientConflictException;
import org.apache.commons.collections.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@SQLComponent
public class SqlTenantRoleRepository implements TenantRoleDao {

    @Autowired
    TenantRoleRepository tenantRoleRepository;

    @Autowired
    FederatedRoleRepository federatedRoleRepository;

    @Autowired
    FederatedRoleRaxMapper federatedRoleRaxMapper;

    @Autowired
    TenantRoleMapper mapper;

    @Override
    @Transactional
    public void addTenantRoleToUser(BaseUser user, TenantRole tenantRole) {
        tenantRole.setUserId(user.getId());
        if(user instanceof FederatedUser) {
            SqlFederatedRoleRax federatedRoleRax = federatedRoleRepository.findOneByRoleRsIdAndUserId(tenantRole.getRoleRsId(), user.getId());
            if(federatedRoleRax != null) {
                if(!CollectionUtils.intersection(federatedRoleRax.getTenantIds(), tenantRole.getTenantIds()).isEmpty()) {
                    throw new ClientConflictException();
                }
                federatedRoleRax.getTenantIds().addAll(tenantRole.getTenantIds());
                federatedRoleRepository.save(federatedRoleRax);
            } else {
                federatedRoleRepository.save(federatedRoleRaxMapper.toSQL(tenantRole));
            }
        } else {
            List<SqlTenantRole> sqlTenantRoles = tenantRoleRepository.findByActorIdAndRoleId(user.getId(), tenantRole.getRoleRsId());
            if (containsTenantRole(sqlTenantRoles, mapper.toSQL(tenantRole))) {
                throw new ClientConflictException();
            }
            tenantRoleRepository.save(mapper.toSQL(tenantRole));
        }
    }

    @Override
    public Iterable<TenantRole> getTenantRolesForUser(BaseUser user) {
        if(user instanceof FederatedUser) {
            return federatedRoleRaxMapper.fromSQL(federatedRoleRepository.findByUserId(user.getId()));
        } else {
            return mapper.fromSQLList(tenantRoleRepository.findByActorId(user.getId()));
        }
    }

    @Override
    public Iterable<TenantRole> getTenantRolesForUser(BaseUser user, String applicationId) {
        if(user instanceof FederatedUser) {
            return federatedRoleRaxMapper.fromSQL(federatedRoleRepository.findByUserIdAndClientId(user.getId(), applicationId));
        } else {
            return mapper.fromSQLList(tenantRoleRepository.findByActorIdAndSqlRoleRaxClientId(user.getId(), applicationId));
        }
    }

    @Override
    public Iterable<TenantRole> getAllTenantRolesForTenant(String tenantId) {
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        tenantRoles.addAll(mapper.fromSQLList(tenantRoleRepository.findByTargetId(tenantId)));
        tenantRoles.addAll(federatedRoleRaxMapper.fromSQL(federatedRoleRepository.findByTenantId(tenantId)));
        return tenantRoles;
    }

    @Override
    public Iterable<TenantRole> getAllTenantRolesForTenantAndRole(String tenantId, String roleId) {
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        tenantRoles.addAll(mapper.fromSQLList(tenantRoleRepository.findByTargetIdAndRoleId(tenantId, roleId)));
        tenantRoles.addAll(federatedRoleRaxMapper.fromSQL(federatedRoleRepository.findByTenantIdAndRoleRsId(tenantId, roleId)));
        return tenantRoles;
    }

    @Override
    public Iterable<TenantRole> getAllTenantRolesForClientRole(ClientRole role) {
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        tenantRoles.addAll(mapper.fromSQLList(tenantRoleRepository.findByRoleId(role.getId())));
        tenantRoles.addAll(federatedRoleRaxMapper.fromSQL(federatedRoleRepository.findByRoleRsId(role.getId())));
        return tenantRoles;
    }

    @Override
    public TenantRole getTenantRoleForUser(BaseUser user, String roleId) {
        if(user instanceof FederatedUser) {
            return federatedRoleRaxMapper.fromSQL(federatedRoleRepository.findOneByRoleRsIdAndUserId(roleId, user.getId()));
        } else {
            return mapper.fromSQL(tenantRoleRepository.findByActorIdAndRoleId(user.getId(), roleId));
        }
    }

    @Override
    @Transactional
    public void updateTenantRole(TenantRole tenantRole) {
        //check the assignments table to see if this tenant role is for a provisioned user
        List<SqlTenantRole> existingSqlTenantRoles = tenantRoleRepository.findByActorIdAndRoleId(tenantRole.getUserId(), tenantRole.getRoleRsId());
        if(!CollectionUtils.isEmpty(existingSqlTenantRoles)) {
            List<SqlTenantRole> sqlTenantRoles = mapper.toSQLList(tenantRole);

            for (SqlTenantRole sqlTenantRole : existingSqlTenantRoles) {
                if (!containsTenantRole(sqlTenantRoles, sqlTenantRole)) {
                    tenantRoleRepository.delete(sqlTenantRole);
                }
            }

            for (SqlTenantRole sqlTenantRole : sqlTenantRoles) {
                if (!containsTenantRole(existingSqlTenantRoles, sqlTenantRole)) {
                    tenantRoleRepository.save(sqlTenantRole);
                }
            }
            return;
        }

        //if we get here, then the role was not found for a provisioned user. Now check to see if this is for a federated user
        SqlFederatedRoleRax federatedRoleRax = federatedRoleRepository.findOneByRoleRsIdAndUserId(tenantRole.getRoleRsId(), tenantRole.getUserId());
        if(federatedRoleRax != null) {
            federatedRoleRepository.save(federatedRoleRaxMapper.toSQL(tenantRole, federatedRoleRax));
        }
    }

    @Override
    @Transactional
    public void deleteTenantRoleForUser(EndUser user, TenantRole tenantRole) {
        if(user instanceof FederatedUser) {
            federatedRoleRepository.deleteByUserIdAndRoleRsId(user.getId(), tenantRole.getRoleRsId());
        } else {
            for (SqlTenantRole sqlTenantRole : mapper.toSQLList(tenantRole)) {
                sqlTenantRole.setActorId(user.getId());
                tenantRoleRepository.delete(sqlTenantRole);
            }
        }
    }

    @Override
    @Transactional
    public void deleteTenantRole(TenantRole tenantRole) {
        if(federatedRoleRepository.findOneByRoleRsIdAndUserId(tenantRole.getRoleRsId(), tenantRole.getUserId()) != null) {
            federatedRoleRepository.deleteByUserIdAndRoleRsId(tenantRole.getUserId(), tenantRole.getRoleRsId());
        } else {
            tenantRoleRepository.delete(mapper.toSQLList(tenantRole));
        }
    }

    @Override
    public List<String> getIdsForUsersWithTenantRole(String roleId, int maxResult) {
        //TODO: update to work with federated user roles
        Pageable resultsSize = new PageRequest(0, maxResult);
        return getUserIds(tenantRoleRepository.findByRoleId(roleId, resultsSize));
    }

    @Override
    public Iterable<TenantRole> getTenantRoleForUser(EndUser user, List<ClientRole> clientRoles) {
        //TODO: update to work with federated user roles
        List<String> sqlRoles = new ArrayList<String>();
        for( ClientRole clientRole : clientRoles){
            sqlRoles.add(clientRole.getId());
        }

        if(user instanceof FederatedUser) {
            return federatedRoleRaxMapper.fromSQL(federatedRoleRepository.findByRoleRsIds(sqlRoles));
        } else {
            return mapper.fromSQLList(tenantRoleRepository.findByActorIdAndRoleIdIn(user.getId(), sqlRoles));
        }
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
        return tenantRole.getUserId();
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
