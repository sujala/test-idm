package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.sql.entity.SqlFederatedRoleRax;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;

import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SQLComponent
public class FederatedRoleRaxMapper extends SqlMapper<TenantRole, SqlFederatedRoleRax> {

    private static final String ID_FORMAT = "([a-z0-9]+)";
    private static final String DN_FORMAT = "roleRsId=%s,cn=ROLES,uid=%s,ou=users,ou=test,o=externalProviders,dc=rackspace,dc=com";
    private static final Pattern REGEXP = Pattern.compile(String.format(DN_FORMAT, ID_FORMAT, ID_FORMAT));

    @Override
    public TenantRole fromSQL(SqlFederatedRoleRax sqlRole) {
        if(sqlRole == null) return null;

        TenantRole role = super.fromSQL(sqlRole);
        role.setUniqueId(getUniqueId(sqlRole));
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

    public String getUniqueId(SqlFederatedRoleRax role) {
        if (role != null) {
            return String.format(DN_FORMAT, role.getRoleRsId(), role.getUserId());
        }
        return null;
    }

}
