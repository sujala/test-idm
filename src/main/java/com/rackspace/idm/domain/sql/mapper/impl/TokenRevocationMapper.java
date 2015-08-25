package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.LdapTokenRevocationRecord;
import com.rackspace.idm.domain.sql.entity.SqlTokenRevocationRecord;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;

@SQLComponent
public class TokenRevocationMapper extends SqlMapper<LdapTokenRevocationRecord, SqlTokenRevocationRecord> {

    private static final String FORMAT = "rsId=%s,ou=TRRs,o=tokens,dc=rackspace,dc=com";

    @Override
    protected String getUniqueIdFormat() {
        return FORMAT;
    }

    @Override
    protected Object[] getIds(SqlTokenRevocationRecord sqlTokenRevocationRecord) {
        return new Object[] {sqlTokenRevocationRecord.getId()};
    }

}
