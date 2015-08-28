package com.rackspace.idm.domain.sql.dao.impl;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.sql.dao.TokenRevocationRecordRepositoryCustom;
import com.rackspace.idm.domain.sql.entity.SqlTokenRevocationRecord;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Criterion;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.sql.JoinType;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Transactional
@SQLRepository
public class TokenRevocationRecordRepositoryImpl implements TokenRevocationRecordRepositoryCustom {

    @Autowired
    private EntityManager entityManager;

    @Override
    public List<TokenRevocationRecord> listTokenRevocationRecordsForToken(Token token) {
        return buildTrrCriteriaForToken(token).list();
    }

    @Override
    public Long listTokenRevocationRecordsForTokenCount(Token token) {
        Criteria criteria = buildTrrCriteriaForToken(token);
        return (Long) criteria.setProjection(Projections.countDistinct("id")).uniqueResult();
    }

    private Criteria buildTrrCriteriaForToken(Token token) {
        List<Criterion> criterion = new ArrayList<Criterion>();
        Session session = entityManager.unwrap(Session.class);
        Criteria criteria = session.createCriteria(SqlTokenRevocationRecord.class, "trr");
        criteria.createAlias("trr.sqlTokenRevocationRecordAuthenticatedBy", "authenticatedByRax", JoinType.LEFT_OUTER_JOIN);
        criteria.createAlias("trr.accessToken", "accessTokenRax", JoinType.LEFT_OUTER_JOIN);

        criterion.add(getRevocationByTokenCriterion(token));

        if (token instanceof BaseUserToken) {
            criterion.add(getRevocationByUserWithWildcardTokenFilterCriterion(((BaseUserToken) token).getIssuedToUserId(), token.getAuthenticatedBy(), token.getCreateTimestamp()));
        }

        criteria.add(Restrictions.or(criterion.toArray(new Criterion[criterion.size()])));

        return criteria;
    }

    private Criterion getRevocationByTokenCriterion(Token token) {
        return Restrictions.eq("accessTokenRax.token", token.getAccessTokenString());
    }

    private Criterion getRevocationByUserWithWildcardTokenFilterCriterion(String userId, List<String> authenticatedBy, Date tokenCreationTimestamp) {
        Criterion authByExactMatchesFilter;
        boolean isTokenForImpersonation = false;

        if (CollectionUtils.isNotEmpty(authenticatedBy)) {
            isTokenForImpersonation = authenticatedBy.contains(AuthenticatedByMethodEnum.IMPERSONATION.getValue());
            String flattenedAuthBy = StringUtils.join(authenticatedBy, ",");
            authByExactMatchesFilter = Restrictions.eq("authenticatedByRax.authenticatedBy", flattenedAuthBy);
        } else {
            //if no auth by provided in token, limit to those TRRs that have <empty> set
            authByExactMatchesFilter = Restrictions.eq("authenticatedByRax.authenticatedBy", LdapTokenRevocationRecord.AUTHENTICATED_BY_EMPTY_LIST_SUBSTITUTE);
        }

        /*
        if checking on revocation of an impersonation token, the TRR must explicitly specify IMPERSONATION. Wildcard not allowed. An unsupported
        edge case is if the token's authBy is multivalued with IMPERSONATION and something else (e.g. PASSWORD). This is NOT supported. IMPERSONATION
        authBy can not be combined with any other authBy.

        However, in such a case, if a TRR explicitly specified both authBy values (not just a wildcard, or IMPERSONATION and a wildcard), the
        token would ultimately be considered revoked since the filter would match.
         */
        Criterion authByOrFilter;
        if (!isTokenForImpersonation) {
            /*
            if not checking an impersonation token, all authBy fields can be matched via the 'wildcard'
             */
            authByOrFilter = Restrictions.or(
                    Restrictions.eq("authenticatedByRax.authenticatedBy", TokenRevocationRecord.AUTHENTICATED_BY_WILDCARD_VALUE),
                    authByExactMatchesFilter);
        } else {
            authByOrFilter = authByExactMatchesFilter;
        }

        return Restrictions.and(
                Restrictions.eq("targetIssuedToId", userId),
                Restrictions.isNull("accessTokenRax.id"),
                Restrictions.ge("targetCreatedBefore", tokenCreationTimestamp),
                authByOrFilter
        );
    }

}
