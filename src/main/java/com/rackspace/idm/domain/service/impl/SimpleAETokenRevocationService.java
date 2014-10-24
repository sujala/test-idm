package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.dao.TokenRevocationRecordPersistenceStrategy;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.security.AETokenService;
import com.rackspace.idm.domain.security.UnmarshallTokenException;
import com.rackspace.idm.domain.service.AETokenRevocationService;
import com.rackspace.idm.domain.service.UUIDTokenRevocationService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Simple in the sense that it goes against a single backend persistence mechanism for ae token revocation.
 */
@Component
public class SimpleAETokenRevocationService implements AETokenRevocationService {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Qualifier("tokenRevocationRecordPersistenceStrategy")
    @Autowired
    private TokenRevocationRecordPersistenceStrategy tokenRevocationRecordPersistenceStrategy;

    @Autowired
    private AETokenService aeTokenService;

    @Autowired
    private UUIDTokenRevocationService uuidTokenRevocationService;

    @Autowired
    private UserDao userDao;

    @Autowired
    private IdentityConfig identityConfig;

    @Lazy
    @Autowired
    private AtomHopperClient atomHopperClient;

    @Autowired
    private UserService userService;

    @Override
    public boolean supportsRevokingFor(UniqueId obj, ScopeAccess sa) {
        return false;
    }

    @Override
    public void revokeToken(String tokenString) {
        if (StringUtils.isBlank(tokenString)) {
            return;
        }
        try {
            //unmarshall the token to make sure it's a "real" token
            ScopeAccess token = aeTokenService.unmarshallToken(tokenString);
            revokeToken(token);
        } catch (UnmarshallTokenException e) {
            LOG.warn("Error unmarshalling token provided to revoke.", e);
            return;
        }
    }

    @Override
    public void revokeToken(ScopeAccess token) {
        if (token == null) {
            return;
        }
        boolean tokenWasRevoked = revokeTokenIfNecessary(token);
        if (tokenWasRevoked) {
            try {
                BaseUser user = userService.getUserByScopeAccess(token, false);
                sendRevokeTokenFeedIfNecessary(user, token);
            } catch (NotFoundException e) {
                //ignore
            }
        }
    }

    @Override
    public void revokeToken(BaseUser user, ScopeAccess token) {
        if (token == null) {
            return;
        }
        LOG.debug("Revoking access token {}", token);
        boolean tokenWasRevoked = revokeTokenIfNecessary(token);
        if (tokenWasRevoked) {
            //we just add a new record - regardless of whether there's already an existing record covering this token.
            tokenRevocationRecordPersistenceStrategy.addTokenTrrRecord(token.getAccessTokenString());
            sendRevokeTokenFeedIfNecessary(user, token);
        }
        LOG.debug("Done revoking access token {}", token.getAccessTokenString());
    }

    private boolean revokeTokenIfNecessary(ScopeAccess token) {
        if (isTokenValid(token)) {
            tokenRevocationRecordPersistenceStrategy.addTokenTrrRecord(token.getAccessTokenString());
            return true;
        }
        return false;
    }

    private boolean isTokenValid(ScopeAccess token) {
        return token.getAccessTokenExp().after(new Date()) && !isTokenRevoked(token);
    }

    private void sendRevokeTokenFeedIfNecessary(BaseUser user, ScopeAccess token) {
        if (user != null && user instanceof User) {
            sendRevokeTokenFeedEvent((User) user, token.getAccessTokenString());
        }
    }

    @Override
    public void revokeTokensForBaseUser(String userId, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(userId, authenticatedByMethodGroups);

        //TODO: Add feed event for User TRR

        if (identityConfig.getFeatureAeTokenCleanupUuidOnRevokes()) {
            uuidTokenRevocationService.revokeTokensForBaseUser(userId, authenticatedByMethodGroups);
        }
    }

    @Override
    public void revokeTokensForBaseUser(BaseUser user, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.getId(), authenticatedByMethodGroups);

        //TODO: Add feed event for User TRR

        if (identityConfig.getFeatureAeTokenCleanupUuidOnRevokes()) {
            uuidTokenRevocationService.revokeTokensForBaseUser(user, authenticatedByMethodGroups);
        }
    }

    @Override
    public void revokeAllTokensForBaseUser(String userId) {
        tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(userId, Arrays.asList(AuthenticatedByMethodGroup.ALL));

        //TODO: Add feed event for User TRR

        if (identityConfig.getFeatureAeTokenCleanupUuidOnRevokes()) {
            uuidTokenRevocationService.revokeAllTokensForBaseUser(userId);
        }
    }

    @Override
    public void revokeAllTokensForBaseUser(BaseUser user) {
        tokenRevocationRecordPersistenceStrategy.addUserTrrRecord(user.getId(), Arrays.asList(AuthenticatedByMethodGroup.ALL));

        //TODO: Add feed event for User TRR

        if (identityConfig.getFeatureAeTokenCleanupUuidOnRevokes()) {
            uuidTokenRevocationService.revokeAllTokensForBaseUser(user);
        }
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

    private void sendRevokeTokenFeedEvent(User user, String tokenString) {
        if (user != null) {
            LOG.warn("Sending token feed to atom hopper.");
            atomHopperClient.asyncTokenPost(user, tokenString);
        }
    }
}
