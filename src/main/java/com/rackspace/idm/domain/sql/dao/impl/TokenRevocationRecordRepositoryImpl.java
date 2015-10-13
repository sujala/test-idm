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
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
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
        List<TokenRevocationRecord> trrs = buildTrrCriteriaForTokenString(token).list();
        if (!(token instanceof BaseUserToken)) {
            // only apply auth-by filtering to base user tokens
            return trrs;
        }
        trrs.addAll(buildTrrCriteriaForTokenAuthBy(token).list());

        return trrs;
    }

    @Override
    public Long listTokenRevocationRecordsForTokenCount(Token token) {
        Long trrCount = (Long) buildTrrCriteriaForTokenString(token).setProjection(Projections.countDistinct("id")).uniqueResult();
        if (!(token instanceof BaseUserToken)) {
            // only apply auth-by filtering to base user tokens
            return trrCount;
        }
        trrCount = trrCount + (Long) buildTrrCriteriaForTokenAuthBy(token).setProjection(Projections.countDistinct("id")).uniqueResult();

        return trrCount;
    }

    private Criteria buildTrrCriteriaForTokenString(Token token) {
        Session session = entityManager.unwrap(Session.class);
        Criteria criteria = session.createCriteria(SqlTokenRevocationRecord.class, "trr");
        criteria.createAlias("trr.accessToken", "accessTokenRax");
        criteria.add(Restrictions.eq("accessTokenRax.token", token.getAccessTokenString()));

        return criteria;
    }

    private Criteria buildTrrCriteriaForTokenAuthBy(Token token) {
        Session session = entityManager.unwrap(Session.class);
        Criteria criteria = session.createCriteria(SqlTokenRevocationRecord.class, "trr");
        criteria.createAlias("trr.sqlTokenRevocationRecordAuthenticatedBy", "authenticatedByRax",
                JoinType.INNER_JOIN, getRevocationByWildcardTokenFilterCriterion(token.getAuthenticatedBy()));

        criteria.add(Restrictions.eq("targetIssuedToId", ((BaseUserToken) token).getIssuedToUserId()));
        criteria.add(Restrictions.ge("targetCreatedBefore", token.getCreateTimestamp()));

        return criteria;
    }

    private Criterion getRevocationByWildcardTokenFilterCriterion(List<String> authenticatedBy) {
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

        return authByOrFilter;
    }

}
