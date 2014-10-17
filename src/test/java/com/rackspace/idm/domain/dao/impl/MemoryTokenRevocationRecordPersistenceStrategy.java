package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.TokenRevocationRecordPersistenceStrategy;
import com.rackspace.idm.domain.entity.LdapTokenRevocationRecord;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.TokenRevocationRecord;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * In-memory revocation storage mechanism for demonstration purposes only to swap out persistence store.
 *
 * Threadsafe due to synchronization, but obviously, not very performant in a concurrent thread situation...
 */
public class MemoryTokenRevocationRecordPersistenceStrategy implements TokenRevocationRecordPersistenceStrategy {

    private Map<String, TokenRevocationRecord> tokenTRRs = new HashMap<String, TokenRevocationRecord>();
    private Map<String, List<TokenRevocationRecord>> userTRRs = new HashMap<String, List<TokenRevocationRecord>>();
    private Map<String, TokenRevocationRecord> trrIdToTRRMap = new HashMap<String, TokenRevocationRecord>();

    @Override
    public TokenRevocationRecord addTokenTrrRecord(String tokenStr) {
        BasicTokenRevocationRecord trr = BasicTokenRevocationRecord.createTokenTrr(tokenStr);
        tokenTRRs.put(tokenStr, trr);
        trrIdToTRRMap.put(trr.getId(), trr);
        return trr;
    }

    @Override
    public TokenRevocationRecord addUserTrrRecord(String targetUserId, List<Set<String>> authenticatedBy) {
        BasicTokenRevocationRecord trr = BasicTokenRevocationRecord.createUserTrr(targetUserId, new Date(), authenticatedBy);
        List<TokenRevocationRecord> userRevList = userTRRs.get(targetUserId);
        if (userRevList == null) {
            userRevList = new ArrayList<TokenRevocationRecord>();
            userTRRs.put(targetUserId, userRevList);
        }
        userRevList.add(trr);
        trrIdToTRRMap.put(trr.getId(), trr);
        return trr;
    }

    @Override
    public synchronized TokenRevocationRecord getTokenRevocationRecord(String id) {
        return trrIdToTRRMap.get(id);
    }

    @Override
    public synchronized Iterable<? extends TokenRevocationRecord> getActiveTokenRevocationRecordsMatchingToken(String userId, ScopeAccess token) {
        List<TokenRevocationRecord> trrList = new ArrayList<TokenRevocationRecord>();

        //look for userId ones that match the token
        List<TokenRevocationRecord> userTrrs = userTRRs.get(userId);
        if (userTrrs != null) {
            for (TokenRevocationRecord trr : userTrrs) {
                if (trr.getTargetCreatedBefore().after(token.getCreateTimestamp())) {
                    //need to check auth by
                    List<Set<String>> authBySets = trr.getTargetAuthenticatedBy();

                    for (Set<String> authBySet : authBySets) {
                        //if trr contains wildcard or explicitly the token's auth by set, it matches
                        if (CollectionUtils.isEqualCollection(TokenRevocationRecord.AUTHENTICATED_BY_WILDCARD_SET, authBySet)
                                || CollectionUtils.isEqualCollection(authBySet, token.getAuthenticatedBy())) {
                            trrList.add(trr);
                            break;
                        }
                    }
                }
            }
        }

        if (tokenTRRs.containsKey(token.getAccessTokenString())) {
            trrList.add(tokenTRRs.get(token.getAccessTokenString()));
        }
        return trrList;
    }

    @Override
    public synchronized boolean doesActiveTokenRevocationRecordExistMatchingToken(String userId, ScopeAccess token) {
        return getActiveTokenRevocationRecordsMatchingToken(userId, token).iterator().hasNext();
    }

    @Getter
    @Setter
    public static class BasicTokenRevocationRecord implements TokenRevocationRecord {
        String id;
        Date createTimestamp;
        String targetToken;
        String targetIssuedToId;
        Date targetCreatedBefore;
        List<Set<String>> targetAuthenticatedBy;

        private static BasicTokenRevocationRecord createTokenTrr(String targetToken) {
            BasicTokenRevocationRecord trr = new BasicTokenRevocationRecord();

            trr.id = UUID.randomUUID().toString();
            trr.targetToken = targetToken;
            trr.createTimestamp = new Date();

            return trr;
        }

        private static BasicTokenRevocationRecord createUserTrr(String targetIssuedToId, Date targetCreatedBefore, List<Set<String>> targetAuthenticatedBy) {
            BasicTokenRevocationRecord trr = new BasicTokenRevocationRecord();

            trr.id = UUID.randomUUID().toString();
            trr.createTimestamp = new Date();

            trr.targetIssuedToId = targetIssuedToId;
            trr.targetCreatedBefore = targetCreatedBefore;
            if (targetAuthenticatedBy != null) {
                trr.targetAuthenticatedBy = targetAuthenticatedBy;
            } else {
                targetAuthenticatedBy = new ArrayList<Set<String>>();
            }
            return trr;
        }
    }


}
