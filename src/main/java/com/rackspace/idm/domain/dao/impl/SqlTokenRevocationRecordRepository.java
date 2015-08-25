package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.TokenRevocationRecordPersistenceStrategy;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.dao.DeltaDao;
import com.rackspace.idm.domain.sql.dao.TokenRevocationRecordRepository;
import com.rackspace.idm.domain.sql.entity.SqlTokenRevocationRecord;
import com.rackspace.idm.domain.sql.entity.SqlTokenRevocationRecordAuthenticatedByRax;
import com.rackspace.idm.domain.sql.mapper.impl.TokenRevocationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;


@SQLComponent("tokenRevocationRecordPersistenceStrategy")
public class SqlTokenRevocationRecordRepository implements TokenRevocationRecordPersistenceStrategy {

    @Autowired
    private TokenRevocationRecordRepository tokenRevocationRecordRepository;

    @Autowired
    private TokenRevocationMapper tokenRevocationMapper;

    @Autowired
    private DeltaDao deltaDao;

    @Override
    @Transactional
    public TokenRevocationRecord addTokenTrrRecord(String tokenStr) {
        SqlTokenRevocationRecord sqlTrr = new SqlTokenRevocationRecord();
        sqlTrr.setId(TokenRevocationRecordUtil.getNextId());
        sqlTrr.setTargetCreatedBefore(new Date());
        sqlTrr.setCreateTimestamp(new Date());

        sqlTrr.setTargetToken(tokenStr);

        sqlTrr = tokenRevocationRecordRepository.save(sqlTrr);

        final LdapTokenRevocationRecord trr = tokenRevocationMapper.fromSQL(sqlTrr);
        deltaDao.save(ChangeType.ADD, trr.getUniqueId(), tokenRevocationMapper.toLDIF(trr));

        return sqlTrr;
    }

    @Override
    @Transactional
    public TokenRevocationRecord addUserTrrRecord(String targetUserId, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        SqlTokenRevocationRecord sqlTrr = new SqlTokenRevocationRecord();
        sqlTrr.setId(TokenRevocationRecordUtil.getNextId());
        sqlTrr.setTargetIssuedToId(targetUserId);
        sqlTrr.setTargetAuthenticatedByMethodGroups(authenticatedByMethodGroups);
        sqlTrr.setTargetCreatedBefore(new Date());
        sqlTrr.setCreateTimestamp(new Date());

        for (SqlTokenRevocationRecordAuthenticatedByRax authBy : sqlTrr.getSqlTokenRevocationRecordAuthenticatedBy()) {
            authBy.setId(TokenRevocationRecordUtil.getNextId());
            authBy.setTokenRevocationRecord(sqlTrr);
        }

        sqlTrr = tokenRevocationRecordRepository.save(sqlTrr);

        final LdapTokenRevocationRecord trr = tokenRevocationMapper.fromSQL(sqlTrr);
        deltaDao.save(ChangeType.ADD, trr.getUniqueId(), tokenRevocationMapper.toLDIF(trr));

        return sqlTrr;
    }

    @Override
    public TokenRevocationRecord getTokenRevocationRecord(String id) {
        return tokenRevocationRecordRepository.findOne(id);
    }

    @Override
    public Iterable<? extends TokenRevocationRecord> getActiveTokenRevocationRecordsMatchingToken(Token token) {
        return tokenRevocationRecordRepository.listTokenRevocationRecordsForToken(token);
    }

    @Override
    public boolean doesActiveTokenRevocationRecordExistMatchingToken(Token token) {
        return tokenRevocationRecordRepository.listTokenRevocationRecordsForTokenCount(token) > 0;
    }

}
