package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.TokenRevocationRecordPersistenceStrategy;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.StaticUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

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
    public TokenRevocationRecord addUserTrrRecord(String targetUserId, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        LdapTokenRevocationRecord trr = new LdapTokenRevocationRecord();
        trr.setId(getNextId());
        trr.setTargetIssuedToId(targetUserId);
        trr.setTargetAuthenticatedByMethodGroups(authenticatedByMethodGroups);
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
        Filter authByExactMatchesFilter;
        if (CollectionUtils.isNotEmpty(authenticatedBy)) {
            LdapSearchBuilder builder = new LdapSearchBuilder();
            String flattenedAuthBy = StringUtils.join(authenticatedBy, ",");
            builder.addEqualAttribute(ATTR_RS_TYPE, flattenedAuthBy);
            authByExactMatchesFilter = builder.build();
        } else {
            //if no auth by provided in token, limit to those TRRs that have <empty> set
            authByExactMatchesFilter = Filter.createEqualityFilter(ATTR_RS_TYPE, LdapTokenRevocationRecord.AUTHENTICATED_BY_EMPTY_LIST_SUBSTITUTE);
        }

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

