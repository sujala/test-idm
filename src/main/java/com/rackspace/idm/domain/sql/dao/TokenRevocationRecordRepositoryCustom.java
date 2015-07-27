package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.domain.entity.Token;
import com.rackspace.idm.domain.entity.TokenRevocationRecord;
import com.rackspace.idm.domain.sql.entity.SqlTokenRevocationRecord;

import java.util.List;

public interface TokenRevocationRecordRepositoryCustom {

    List<TokenRevocationRecord> listTokenRevocationRecordsForToken(Token token);

    Long listTokenRevocationRecordsForTokenCount(Token token);

}
