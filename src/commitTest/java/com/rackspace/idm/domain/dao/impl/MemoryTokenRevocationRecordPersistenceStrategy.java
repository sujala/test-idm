package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.TokenRevocationRecordPersistenceStrategy;
import com.rackspace.idm.domain.entity.*;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

/**
 * In-memory revocation storage mechanism for demonstration purposes only to swap out persistence store.
 *
 * Threadsafe due to synchronization, but obviously, not very performant in a concurrent thread situation...
 */
public class MemoryTokenRevocationRecordPersistenceStrategy implements TokenRevocationRecordPersistenceStrategy<TokenRevocationRecord> {

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
    public TokenRevocationRecord addUserTrrRecord(String targetUserId, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        BasicTokenRevocationRecord trr = BasicTokenRevocationRecord.createUserTrr(targetUserId, new Date(), authenticatedByMethodGroups);
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
    public TokenRevocationRecord addIdentityProviderTrrRecord(String identityProviderId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized TokenRevocationRecord getTokenRevocationRecord(String id) {
        return trrIdToTRRMap.get(id);
    }

    @Override
    public synchronized Iterable<TokenRevocationRecord> getActiveTokenRevocationRecordsMatchingToken(Token token) {
        List<TokenRevocationRecord> trrList = new ArrayList<TokenRevocationRecord>();

        boolean isTokenImpersonation = token != null && token.getAuthenticatedBy() != null && token.getAuthenticatedBy().contains(AuthenticatedByMethodEnum.IMPERSONATION.getValue()) ? true : false;

        //look for userId ones that match the token
        if (token instanceof BaseUserToken) {
            List<TokenRevocationRecord> userTrrs = userTRRs.get(((BaseUserToken) token).getIssuedToUserId());
            if (userTrrs != null) {
                for (TokenRevocationRecord trr : userTrrs) {
                    if (trr.getTargetCreatedBefore().after(token.getCreateTimestamp())) {
                        //need to check auth by
                        List<AuthenticatedByMethodGroup> authBySets = trr.getTargetAuthenticatedByMethodGroups();

                        for (AuthenticatedByMethodGroup authByGroup : authBySets) {
                            //if trr contains wildcard or explicitly the token's auth by set, it matches
                            if ((!isTokenImpersonation && authByGroup.matches(AuthenticatedByMethodGroup.ALL))
                                    || authByGroup.matches(AuthenticatedByMethodGroup.getGroup(token.getAuthenticatedBy()))) {
                                trrList.add(trr);
                                break;
                            }
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
    public synchronized boolean doesActiveTokenRevocationRecordExistMatchingToken(Token token) {
        return getActiveTokenRevocationRecordsMatchingToken(token).iterator().hasNext();
    }

    @Getter
    @Setter
    public static class BasicTokenRevocationRecord implements TokenRevocationRecord {
        String id;
        Date createTimestamp;
        String targetToken;
        String targetIssuedToId;
        Date targetCreatedBefore;
        List<AuthenticatedByMethodGroup> targetAuthenticatedByMethodGroups;

        private static BasicTokenRevocationRecord createTokenTrr(String targetToken) {
            BasicTokenRevocationRecord trr = new BasicTokenRevocationRecord();

            trr.id = UUID.randomUUID().toString();
            trr.targetToken = targetToken;
            trr.createTimestamp = new Date();

            return trr;
        }

        private static BasicTokenRevocationRecord createUserTrr(String targetIssuedToId, Date targetCreatedBefore, List<AuthenticatedByMethodGroup> targetAuthenticatedByMethodGroups) {
            BasicTokenRevocationRecord trr = new BasicTokenRevocationRecord();

            trr.id = UUID.randomUUID().toString();
            trr.createTimestamp = new Date();

            trr.targetIssuedToId = targetIssuedToId;
            trr.targetCreatedBefore = targetCreatedBefore;
            if (targetAuthenticatedByMethodGroups != null) {
                trr.targetAuthenticatedByMethodGroups = targetAuthenticatedByMethodGroups;
            } else {
                targetAuthenticatedByMethodGroups = new ArrayList<AuthenticatedByMethodGroup>();
            }
            return trr;
        }
    }

    @Override
    public List<TokenRevocationRecord> findObsoleteTrrs(int max) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteTokenRevocationRecord(TokenRevocationRecord record) {
        throw new UnsupportedOperationException();
    }
}
