package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.TokenRevocationRecordPersistenceStrategy;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.IdmException;
import com.rackspace.idm.exception.SizeLimitExceededException;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.controls.SimplePagedResultsControl;
import com.unboundid.util.StaticUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

/**
 * Responsible for storing and retrieving TRRs to/from CA LDAP repository
 */
@LDAPComponent("tokenRevocationRecordPersistenceStrategy")
public class LdapTokenRevocationRecordRepository extends LdapGenericRepository<LdapTokenRevocationRecord> implements TokenRevocationRecordPersistenceStrategy<LdapTokenRevocationRecord> {

    @Autowired
    private IdentityConfig identityConfig;

    @Override
    public void deleteTokenRevocationRecord(LdapTokenRevocationRecord record) {
        deleteObject(record);
    }

    @Override
    public Iterable<LdapTokenRevocationRecord> getActiveTokenRevocationRecordsMatchingToken(Token token) {
        return getObjects(searchForRevokedToken(token), getBaseDn(), SearchScope.SUB);
    }

    @Override
    public boolean doesActiveTokenRevocationRecordExistMatchingToken(Token token) {
        return countObjects(searchForRevokedToken(token), getBaseDn(), SearchScope.SUB) > 0;
    }

    @Override
    public List<LdapTokenRevocationRecord> findObsoleteTrrs(int max) {
        if(max > LdapPagingIterator.PAGE_SIZE) {
            getLogger().debug("Aborting search request due to requested max results of {} exceeding maximum limit of {}", max, LdapPagingIterator.PAGE_SIZE);
            throw new IllegalArgumentException("Max results must not exceed " + LdapPagingIterator.PAGE_SIZE);
        }

        Filter filter = searchForObsoleteTokenRevocationRecords();
        SearchResult searchResult;
        SearchRequest searchRequest = new SearchRequest(getBaseDn(), SearchScope.SUB, filter);
        searchRequest.setSizeLimit(max);

        List<SearchResultEntry> searchResultEntries;
        try {
            searchResult = getAppInterface().search(searchRequest);
            searchResultEntries = searchResult.getSearchEntries();
        } catch (LDAPSearchException ldapEx) {
            if (ldapEx.getResultCode() == ResultCode.SIZE_LIMIT_EXCEEDED) {
                //who cares. we only want the specified limit and don't care that there are more results
                searchResultEntries = ldapEx.getSearchEntries();
            } else {
                String loggerMsg = String.format("Error searching for obsolete TRRs - %s",  filter);
                getLogger().error(loggerMsg);
                throw new IdmException("Error retrieving obsolete TRRs", "TRRP-0001");
            }
        }

        List<LdapTokenRevocationRecord> objects;
        if (CollectionUtils.isNotEmpty(searchResultEntries)) {
            objects = processSearchResult(searchResultEntries);
        } else {
            objects = Collections.EMPTY_LIST;
        }
        return objects;
    }

    @Override
    public LdapTokenRevocationRecord addTokenTrrRecord(String tokenStr) {
        LdapTokenRevocationRecord trr = new LdapTokenRevocationRecord();
        trr.setId(getNextId());
        trr.setTargetToken(tokenStr);
        trr.setTargetCreatedBefore(new Date());

        addObject(trr);
        return trr;
    }

    @Override
    public LdapTokenRevocationRecord addUserTrrRecord(String targetUserId, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        LdapTokenRevocationRecord trr = new LdapTokenRevocationRecord();
        trr.setId(getNextId());
        trr.setTargetIssuedToId(targetUserId);
        trr.setTargetAuthenticatedByMethodGroups(authenticatedByMethodGroups);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);
        trr.setTargetCreatedBefore(cal.getTime());

        addObject(trr);

        return trr;
    }

    @Override
    public LdapTokenRevocationRecord getTokenRevocationRecord(String id) {
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


    private Filter searchForObsoleteTokenRevocationRecords() {
        DateTime obsoleteTrrCreationDate = new DateTime().minusHours(identityConfig.getReloadableConfig().getPurgeTokenRevocationRecordsObsoleteAfterHours());
        return new LdapSearchBuilder()
                .addLessOrEqualAttribute(ATTR_ACCESS_TOKEN_EXP, StaticUtils.encodeGeneralizedTime(obsoleteTrrCreationDate.toDate()))
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TOKEN_REVOCATION_RECORD).build();
    }

    private Filter searchForRevokedToken(Token accessToken) {
        List<Filter> filters = new ArrayList<Filter>();
        filters.add(searchForRevocationByToken(accessToken));

        //add user token TRR search if is user token
        if (accessToken instanceof BaseUserToken) {
            filters.add(searchForRevocationByUserWithWildcardTokenFilter(((BaseUserToken)accessToken).getIssuedToUserId(), accessToken.getAuthenticatedBy(), accessToken.getCreateTimestamp()));
        }
        return Filter.createORFilter(filters);
    }

    private Filter searchByIdFilter(String rsId) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ID, rsId)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TOKEN_REVOCATION_RECORD).build();
    }

    private Filter searchForRevocationByToken(Token accessToken) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_ACCESS_TOKEN, accessToken.getAccessTokenString())
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_TOKEN_REVOCATION_RECORD).build();
    }

    private Filter searchForRevocationByUserWithWildcardTokenFilter(String userId, List<String> authenticatedBy, Date tokenExpiration) {
        Filter authByExactMatchesFilter;
        boolean isTokenForImpersonation = false;

        if (CollectionUtils.isNotEmpty(authenticatedBy)) {
            isTokenForImpersonation = authenticatedBy.contains(AuthenticatedByMethodEnum.IMPERSONATION.getValue());
            LdapSearchBuilder builder = new LdapSearchBuilder();
            String flattenedAuthBy = StringUtils.join(authenticatedBy, ",");
            builder.addEqualAttribute(ATTR_RS_TYPE, flattenedAuthBy);
            authByExactMatchesFilter = builder.build();
        } else {
            //if no auth by provided in token, limit to those TRRs that have <empty> set
            authByExactMatchesFilter = Filter.createEqualityFilter(ATTR_RS_TYPE, LdapTokenRevocationRecord.AUTHENTICATED_BY_EMPTY_LIST_SUBSTITUTE);
        }

        /*
        if checking on revocation of an impersonation token, the TRR must explicitly specify IMPERSONATION. Wildcard not allowed. An unsupported
        edge case is if the token's authBy is multivalued with IMPERSONATION and something else (e.g. PASSWORD). This is NOT supported. IMPERSONATION
        authBy can not be combined with any other authBy.

        However, in such a case, if a TRR explicitly specified both authBy values (not just a wildcard, or IMPERSONATION and a wildcard), the
        token would ultimately be considered revoked since the filter would match.
         */
        Filter authByOrFilter = null;
        if (!isTokenForImpersonation) {
            /*
            if not checking an impersonation token, all authBy fields can be matched via the 'wildcard'
             */
            authByOrFilter = Filter.createORFilter(
                    Filter.createEqualityFilter(ATTR_RS_TYPE, TokenRevocationRecord.AUTHENTICATED_BY_WILDCARD_VALUE),
                    authByExactMatchesFilter
            );
        } else {
            authByOrFilter = authByExactMatchesFilter;
        }

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

