package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.sql.entity.SqlRole;
import com.rackspace.idm.domain.sql.entity.SqlRoleRax;
import com.rackspace.idm.domain.sql.mapper.SqlRaxMapper;

@SQLComponent
public class RoleMapper extends SqlRaxMapper<ClientRole, SqlRole, SqlRoleRax> {
}
