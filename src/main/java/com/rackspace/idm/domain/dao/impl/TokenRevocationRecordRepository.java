package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.TokenRevocationRecordPersistenceStrategy;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.StaticUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Responsible for storing and retrieving TRRs to/from CA LDAP repository
 */
@Component("tokenRevocationRecordPersistenceStrategy")
public class TokenRevocationRecordRepository extends LdapGenericRepository<LdapTokenRevocationRecord> implements TokenRevocationRecordPersistenceStrategy {

    @Override
    public Iterable<LdapTokenRevocationRecord> getActiveTokenRevocationRecordsMatchingToken(String userId, ScopeAccess token) {
        return getObjects(searchForRevokedUserToken(userId, token), getBaseDn(), SearchScope.SUB);
    }

    @Override
    public boolean doesActiveTokenRevocationRecordExistMatchingToken(String userId, ScopeAccess token) {
        return countObjects(searchForRevokedUserToken(userId, token), getBaseDn(), SearchScope.SUB) > 0;
    }

    @Override
    public TokenRevocationRecord addTokenTrrRecord(String tokenStr) {
        LdapTokenRevocationRecord trr = new LdapTokenRevocationRecord();
        trr.setId(getNextId());
        trr.setTargetToken(tokenStr);
        trr.setTargetCreatedBefore(new Date());

        addObject(trr);
        return trr;
    }

    @Override
    public TokenRevocationRecord addUserTrrRecord(String targetUserId, List<Set<String>> authenticatedBy) {
        LdapTokenRevocationRecord trr = new LdapTokenRevocationRecord();
        trr.setId(getNextId());
        trr.setTargetIssuedToId(targetUserId);
        trr.setTargetAuthenticatedBy(authenticatedBy);
        trr.setTargetCreatedBefore(new Date());

        addObject(trr);

        return trr;
    }

    @Override
    public TokenRevocationRecord getTokenRevocationRecord(String id) {
        return getObject(searchByIdFilter(id));
    }

    @Autowired
    private UserDao userDao;

    @Override
    public String getBaseDn(){
        return TOKEN_REVOCATION_BASE_DN;
    }

    @Override
    public String getLdapEntityClass(){
        return OBJECTCLASS_TOKEN_REVOCATION_RECORD;
    }

    @Override
    public String getSortAttribute() {
        return ATTR_ID;
    }

    @Override
    public String getNextId() {
        return super.getUuid();
    }

    private boolean isRevoked(ImpersonatedScopeAccess token) {
        String userId;
        if (StringUtils.isNotBlank(token.getRackerId())) {
            userId = token.getRackerId();
        } else if (StringUtils.isNotBlank(token.getUsername())) {
            //must be a provisioned user
            User user = userDao.getUserByUsername(token.getUsername());
            if (user == null) {
                return true; //no user returned for token. Consider it revoked;
            }
            userId = user.getId();
        } else {
            return true; //if neither rackerId OR username is populated, token not tied to a user so consider it revoked.
        }

        return countObjects(searchForRevokedUserToken(userId, token), getBaseDn(), SearchScope.SUB) > 0;
    }

    private Filter searchForRevokedUserToken(String userId, ScopeAccess accessToken) {
        return Filter.createORFilter(searchForRevocationByToken(accessToken)
                , searchForRevocationByUserWithWildcardTokenFilter(userId, accessToken.getAuthenticatedBy(), accessToken.getCreateTimestamp()));
    }

    private Filter searchByIdFilter(String rsId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, rsId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TOKEN_REVOCATION_RECORD).build();
    }

    private Filter searchForRevocationByToken(ScopeAccess accessToken) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ACCESS_TOKEN, accessToken.getAccessTokenString())
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TOKEN_REVOCATION_RECORD).build();
    }

    private Filter searchForRevocationByUserWithWildcardTokenFilter(String userId, List<String> authenticatedBy, Date tokenExpiration) {
        LdapSearchBuilder builder = new LdapSearchBuilder();
        for (String authByValue : authenticatedBy) {
            builder.addEqualAttribute(ATTR_RS_TYPE, authByValue);
        }
        Filter authByExactMatchesFilter = builder.build();

        Filter authByOrFilter = Filter.createORFilter(
                Filter.createEqualityFilter(ATTR_RS_TYPE, TokenRevocationRecord.AUTHENTICATED_BY_WILDCARD_VALUE),
                authByExactMatchesFilter
        );

        Filter baseFilter = Filter.createANDFilter(
                Filter.createEqualityFilter(ATTR_OBJECT_CLASS, OBJECTCLASS_TOKEN_REVOCATION_RECORD),
                Filter.createEqualityFilter(ATTR_USER_RS_ID, userId),
                Filter.createNOTFilter(Filter.createPresenceFilter(ATTR_ACCESS_TOKEN)),
                Filter.createGreaterOrEqualFilter(ATTR_ACCESS_TOKEN_EXP, StaticUtils.encodeGeneralizedTime(tokenExpiration)),
                authByOrFilter
        );

        return baseFilter;
    }
}

