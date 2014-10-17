package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.TokenRevocationRecordPersistenceStrategy;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.security.AETokenService;
import com.rackspace.idm.domain.service.AETokenRevocationService;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Simple in the sense that it goes against a single backend persistence mechanism for ae token revocation.
 */
@Component
public class SimpleAETokenRevocationService implements AETokenRevocationService {

    @Qualifier("tokenRevocationRecordPersistenceStrategy")
    @Autowired
    private TokenRevocationRecordPersistenceStrategy tokenRevocationRecordPersistenceStrategy;

    @Autowired
    private AETokenService aeTokenService;

    @Autowired
    private UserDao userDao;

    @Override
    public boolean supportsRevokingFor(UniqueId obj, ScopeAccess sa) {
        return false;
    }

    @Override
    public void revokeToken(String tokenString) {
        tokenRevocationRecordPersistenceStrategy.addTokenTrrRecord(tokenString);
    }

    @Override
    public void revokeToken(ScopeAccess token) {
        tokenRevocationRecordPersistenceStrategy.addTokenTrrRecord(token.getAccessTokenString());
    }

    @Override
    public void revokeToken(BaseUser user, ScopeAccess token) {
        tokenRevocationRecordPersistenceStrategy.addTokenTrrRecord(token.getAccessTokenString());
    }

    @Override
    public void revokeTokensForBaseUser(String userId, List<Set<String>> authenticatedByList) {
        tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(userId, authenticatedByList);
    }

    @Override
    public void revokeTokensForBaseUser(BaseUser user, List<Set<String>> authenticatedByList) {
        tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.getId(), authenticatedByList);
    }

    @Override
    public void revokeAllTokensForBaseUser(String userId) {
        tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(userId, TokenRevocationRecord.AUTHENTICATED_BY_ALL_TOKENS_LIST);
    }

    @Override
    public void revokeAllTokensForBaseUser(BaseUser user) {
        tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.getId(), TokenRevocationRecord.AUTHENTICATED_BY_ALL_TOKENS_LIST);
    }

    @Override
    public boolean isTokenRevoked(String tokenStr) {
        ScopeAccess token = aeTokenService.unmarshallToken(tokenStr);
        return isTokenRevoked(token);
    }

    @Override
    public boolean isTokenRevoked(ScopeAccess token) {
        if (token instanceof UserScopeAccess) {
            return isTokenRevokedInternal((UserScopeAccess) token);
        } else if (token instanceof ImpersonatedScopeAccess) {
            return isTokenRevokedInternal((ImpersonatedScopeAccess) token);
        }
        throw new IllegalArgumentException(String.format("Unsupported scope access '%s'", token.getClass().getSimpleName()));
    }

    private boolean isTokenRevokedInternal(UserScopeAccess token) {
        return tokenRevocationRecordPersistenceStrategy.doesActiveTokenRevocationRecordExistMatchingToken(
                token.getUserRsId(), token);
    }

    private boolean isTokenRevokedInternal(ImpersonatedScopeAccess token) {
        String userId;
        if (StringUtils.isNotBlank(token.getRackerId())) {
            userId = token.getRackerId();
        } else if (StringUtils.isNotBlank(token.getUsername())) {
            //TODO: Once impersonation tokens are fixed to specify userid change to it.
            //must be a provisioned user
            User user = userDao.getUserByUsername(token.getUsername());
            if (user == null) {
                return true; //no user returned for token. Consider it revoked;
            }
            userId = user.getId();
        } else {
            return true; //if neither rackerId OR username is populated, token not tied to a user so consider it revoked.
        }

        return tokenRevocationRecordPersistenceStrategy.doesActiveTokenRevocationRecordExistMatchingToken(
                userId, token);
    }
}
