package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.sql.entity.SqlRole;
import com.rackspace.idm.domain.sql.entity.SqlRoleRax;
import com.rackspace.idm.domain.sql.mapper.SqlRaxMapper;

@SQLComponent
public class RoleMapper extends SqlRaxMapper<ClientRole, SqlRole, SqlRoleRax> {

    private static final String FORMAT = "rsId=%s,cn=CLIENT ROLES,clientId=%s,ou=applications,o=rackspace,dc=rackspace,dc=com";

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected Object[] getIds(SqlRole sqlRole) {
        return new Object[] {sqlRole.getId(), sqlRole.getRax().getClientId()};
    }

}
