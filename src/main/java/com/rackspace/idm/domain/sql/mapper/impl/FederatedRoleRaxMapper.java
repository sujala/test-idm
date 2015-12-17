package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.sql.dao.FederatedRoleRepository;
import com.rackspace.idm.domain.sql.dao.FederatedUserRepository;
import com.rackspace.idm.domain.sql.dao.IdentityProviderRepository;
import com.rackspace.idm.domain.sql.entity.SqlFederatedRoleRax;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SQLComponent
public class FederatedRoleRaxMapper extends SqlMapper<TenantRole, SqlFederatedRoleRax> {

    private static final String FORMAT = "roleRsId=%s,cn=ROLES,uid=%s,ou=users,ou=%s,o=externalProviders,dc=rackspace,dc=com";

    @Autowired
    private FederatedUserRepository federatedUserRepository;

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected Object[] getIds(SqlFederatedRoleRax sqlFederatedRoleRax) {
        final String idpName = federatedUserRepository.getIdpNameByUserId(sqlFederatedRoleRax.getUserId());
        final String username = federatedUserRepository.getUsernameByUserId(sqlFederatedRoleRax.getUserId());
        return new Object[] {sqlFederatedRoleRax.getRoleRsId(), username, idpName};
    }

    @Override
    public TenantRole fromSQL(SqlFederatedRoleRax sqlRole) {
        if(sqlRole == null) return null;

        TenantRole role = super.fromSQL(sqlRole);
        return role;
    }

    @Override
    public SqlFederatedRoleRax toSQL(TenantRole role, SqlFederatedRoleRax sqlFederatedRoleRax) {
        if(role == null) return null;

        SqlFederatedRoleRax sqlRole = super.toSQL(role, sqlFederatedRoleRax);
        if(sqlRole.getId() == null) {
            sqlRole.setId(getNextId());
        }
        return sqlRole;
    }

    private String getNextId() {
        return UUID.randomUUID().toString().replaceAll("-", "");
    }

}
