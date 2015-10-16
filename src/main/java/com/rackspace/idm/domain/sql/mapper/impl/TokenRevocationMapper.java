package com.rackspace.idm.domain.sql.mapper.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.entity.LdapTokenRevocationRecord;
import com.rackspace.idm.domain.entity.TokenRevocationRecord;
import com.rackspace.idm.domain.sql.entity.SqlTokenRevocationRecord;
import com.rackspace.idm.domain.sql.mapper.SqlMapper;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;

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

    @Override
    public SqlTokenRevocationRecord toSQL(LdapTokenRevocationRecord ldapTrr, SqlTokenRevocationRecord sqlTrr) {
        if(ldapTrr == null) {
            return null;
        }

        sqlTrr = super.toSQL(ldapTrr, sqlTrr);
        setTokenOrAuthBy(ldapTrr, sqlTrr);
        return sqlTrr;
    }

    @Override
    public LdapTokenRevocationRecord fromSQL(SqlTokenRevocationRecord sqlTrr) {
        if (sqlTrr == null) {
            return null;
        }

        LdapTokenRevocationRecord ldapTrr = super.fromSQL(sqlTrr);
        setTokenOrAuthBy(sqlTrr, ldapTrr);
        return ldapTrr;
    }

    private void setTokenOrAuthBy(TokenRevocationRecord trrFrom, TokenRevocationRecord trrTo) {
        if(!CollectionUtils.isEmpty(trrFrom.getTargetAuthenticatedByMethodGroups())) {
            trrTo.setTargetAuthenticatedByMethodGroups(trrFrom.getTargetAuthenticatedByMethodGroups());
        } else if(StringUtils.isNotBlank(trrFrom.getTargetToken())) {
            trrTo.setTargetToken(trrFrom.getTargetToken());
        }
    }

}
