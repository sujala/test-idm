package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.sql.entity.SqlGroup;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;

@SQLComponent
public class GroupMapper extends SqlMapper<Group, SqlGroup> {

    private static final String FORMAT = "rsId=%s,ou=groups,ou=cloud,o=rackspace,dc=rackspace,dc=com";

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected String[] getIds(SqlGroup sqlGroup) {
        return new String[] {sqlGroup.getGroupId()};
    }

}
