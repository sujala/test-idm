package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.TokenRevocationRecordPersistenceStrategy;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.sql.dao.TokenRevocationRecordRepository;
import com.rackspace.idm.domain.sql.entity.SqlTokenRevocationRecord;
import com.rackspace.idm.domain.sql.mapper.impl.TokenRevocationMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.Date;
import java.util.List;


@SQLComponent("tokenRevocationRecordPersistenceStrategy")
public class SqlTokenRevocationRecordRepository implements TokenRevocationRecordPersistenceStrategy<TokenRevocationRecord> {

    @Autowired
    private TokenRevocationRecordRepository tokenRevocationRecordRepository;

    @Autowired
    private TokenRevocationMapper tokenRevocationMapper;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

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

        return sqlTrr;
    }

    @Override
    @Transactional
    public TokenRevocationRecord addUserTrrRecord(String targetUserId, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        SqlTokenRevocationRecord sqlTrr = new SqlTokenRevocationRecord();
        sqlTrr.setId(TokenRevocationRecordUtil.getNextId());
        sqlTrr.setTargetIssuedToId(targetUserId);
        sqlTrr.setTargetAuthenticatedByMethodGroups(authenticatedByMethodGroups);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        sqlTrr.setTargetCreatedBefore(cal.getTime());
        sqlTrr.setCreateTimestamp(cal.getTime());

        sqlTrr = tokenRevocationRecordRepository.save(sqlTrr);

        final LdapTokenRevocationRecord trr = tokenRevocationMapper.fromSQL(sqlTrr);

        return sqlTrr;
    }

    @Override
    public TokenRevocationRecord addIdentityProviderTrrRecord(String identityProviderId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public TokenRevocationRecord getTokenRevocationRecord(String id) {
        return tokenRevocationRecordRepository.findOne(id);
    }

    @Override
    public Iterable<TokenRevocationRecord> getActiveTokenRevocationRecordsMatchingToken(Token token) {
        return tokenRevocationRecordRepository.listTokenRevocationRecordsForToken(token);
    }

    @Override
    public boolean doesActiveTokenRevocationRecordExistMatchingToken(Token token) {
        return tokenRevocationRecordRepository.listTokenRevocationRecordsForTokenCount(token) > 0;
    }

    @Override
    public List findObsoleteTrrs(int max) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteTokenRevocationRecord(TokenRevocationRecord record) {
        throw new UnsupportedOperationException();
    }
}
