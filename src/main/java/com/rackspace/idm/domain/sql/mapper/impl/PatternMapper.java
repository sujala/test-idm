package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.Pattern;
import com.rackspace.idm.domain.sql.entity.SqlPattern;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;

@SQLComponent
public class PatternMapper extends SqlMapper<Pattern, SqlPattern> {

    private static final String FORMAT = "rsId=%s,ou=patterns,ou=configuration,ou=cloud,o=rackspace,dc=rackspace,dc=com";

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected Object[] getIds(SqlPattern sqlPattern) {
        return new Object[] {sqlPattern.getId()};
    }

}
