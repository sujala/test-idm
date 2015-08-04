package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.sql.entity.SqlFederatedRoleRax;
import com.rackspace.idm.domain.sql.entity.SqlFederatedUserRax;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.UUID;

@SQLComponent
public class FederatedUserRaxMapper extends SqlMapper<FederatedUser, SqlFederatedUserRax> {

    @Autowired
    private FederatedRoleRaxMapper federatedRoleRaxMapper;

    @Override
    public FederatedUser fromSQL(SqlFederatedUserRax sqlFederatedUserRax) {
        final FederatedUser federatedUser = super.fromSQL(sqlFederatedUserRax);
        if (federatedUser != null && sqlFederatedUserRax != null && sqlFederatedUserRax.getFederatedRoles() != null) {
            if (federatedUser.getRoles() == null) {
                federatedUser.setRoles(new ArrayList<TenantRole>());
            }
            for (SqlFederatedRoleRax sqlFederatedRoleRax : sqlFederatedUserRax.getFederatedRoles()) {
                federatedUser.getRoles().add(federatedRoleRaxMapper.fromSQL(sqlFederatedRoleRax));
            }
        }
        return federatedUser;
    }

    @Override
    public SqlFederatedUserRax toSQL(FederatedUser federatedUser, SqlFederatedUserRax sqlFederatedUserRax) {
        sqlFederatedUserRax = super.toSQL(federatedUser, sqlFederatedUserRax);
        if (sqlFederatedUserRax != null && federatedUser != null && federatedUser.getRoles() != null) {
            if (sqlFederatedUserRax.getFederatedRoles() == null) {
                sqlFederatedUserRax.setFederatedRoles(new HashSet<SqlFederatedRoleRax>());
            }
            for (TenantRole tenantRole : federatedUser.getRoles()) {
                final SqlFederatedRoleRax sqlFederatedRoleRax = federatedRoleRaxMapper.toSQL(tenantRole);
                sqlFederatedRoleRax.setId(getNextId());
                sqlFederatedUserRax.getFederatedRoles().add(sqlFederatedRoleRax);
            }
        }
        return sqlFederatedUserRax;
    }

    private String getNextId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

}
