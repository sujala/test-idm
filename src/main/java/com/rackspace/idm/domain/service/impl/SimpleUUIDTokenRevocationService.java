package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.domain.dao.UUIDScopeAccessDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.UUIDTokenRevocationService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.exception.UnrecognizedAuthenticationMethodException;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * Simple in the sense that it goes against a single backend persistence mechanism for UUID token revocation.
 */
@Component
public class SimpleUUIDTokenRevocationService implements UUIDTokenRevocationService {
    private final Logger LOG = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private AtomHopperClient atomHopperClient;

    @Autowired
    private UUIDScopeAccessDao scopeAccessDao;

    @Autowired
    private UserService userService;

    @Autowired
    private IdentityUserService identityUserService;

    public void revokeToken(String token) {
        LOG.debug("Revoking access token {}", token);

        final ScopeAccess scopeAccess = scopeAccessDao.getScopeAccessByAccessToken(token);
        if (scopeAccess == null) {
            return;
        }

        revokeToken(scopeAccess);
    }

    @Override
    public void revokeToken(ScopeAccess scopeAccess) {
        if (scopeAccess == null) {
            return;
        }

        if (scopeAccess.getAccessTokenExp().after(new Date())) {
            try {
                /*
                TODO: This is here for backwards compatibility with existing (wrong, IMO) code. When a token is to be revoked
                if the user associated with the token is disabled (or associated domain is disabled) then the token will
                be updated (expired), but a 404 NotFoundException will be thrown. There's an existing test case for this
                behavior (see Cloud20RevokeTokenIntegrationTest) so need to maintain this behavior..for now. Will request
                that A DEFECT be created for this
                 */
                BaseUser user = userService.getUserByScopeAccess(scopeAccess, true);
                if (user != null) {
                    revokeToken(user, scopeAccess);
                } else {
                    revokeTokenInternal(scopeAccess);
                }
            } catch (NotFoundException e) {
                revokeTokenInternal(scopeAccess);
                throw e;
            }
        } else {
                /*
                TODO: This is here for backwards compatibility with existing (wrong, IMO) code. When a token is to be revoked
                if the user associated with the token is disabled (or associated domain is disabled) a 404 NotFoundException
                will be thrown regardless of whether or not the token actually needs to be expired. There's an existing test case for this
                behavior (see Cloud20RevokeTokenIntegrationTest) so need to maintain this behavior..for now. Will request
                that A DEFECT be created for this
                 */
            BaseUser user = userService.getUserByScopeAccess(scopeAccess, true);
            LOG.debug("Revoking access token was not required. Already expired {}", scopeAccess);
        }
    }

    @Override
    public void revokeToken(BaseUser user, ScopeAccess scopeAccess) {
        if (scopeAccess == null) {
            return;
        }

        LOG.debug("Revoking access token {}", scopeAccess);
        if (scopeAccess.getAccessTokenExp().after(new Date())) {
            revokeTokenInternal(scopeAccess);
            if(user != null && user instanceof User){
                sendRevokeTokenFeedEvent((User) user, scopeAccess.getAccessTokenString());
            }
        }
        LOG.debug("Done revoking access token {}", scopeAccess.getAccessTokenString());
    }

    private void revokeTokenInternal(ScopeAccess scopeAccess) {
        scopeAccess.setAccessTokenExpired();
        scopeAccessDao.updateScopeAccess(scopeAccess);
    }

    @Override
    public void revokeTokensForBaseUser(String userId, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        //TODO: exapnd this to support rackers. Original implementation only supported EndUsers
        EndUser user = identityUserService.getEndUserById(userId);
        revokeTokensForBaseUser(user, authenticatedByMethodGroups);
    }

    @Override
    public void revokeTokensForBaseUser(BaseUser user, List<AuthenticatedByMethodGroup> authenticatedByMethodGroups) {
        if (user == null) return;

        //first determine in need to revoke all
        boolean revokeAll = false;
        for (AuthenticatedByMethodGroup revokeAuthBy : authenticatedByMethodGroups) {
            if (revokeAuthBy.matches(AuthenticatedByMethodGroup.ALL)) {
                revokeAll = true;
            }
        }

        /*
        for every token, loop through to see if it matches one of the groups being revoked.
         */
        DateTime now = new DateTime();
        for (final ScopeAccess sa : this.scopeAccessDao.getScopeAccesses(user)) {
            if (sa.isAccessTokenExpired(now)) {
                //no point revoking token already expired
                continue;
            }
            else if (revokeAll) {
                revokeToken(user, sa);
            } else {
                try {
                    AuthenticatedByMethodGroup tokenAuthBy  = AuthenticatedByMethodGroup.getGroup(sa.getAuthenticatedBy());
                    for (AuthenticatedByMethodGroup revokeAuthBy : authenticatedByMethodGroups) {
                        if (revokeAuthBy.matches(tokenAuthBy)) {
                            revokeToken(user, sa);
                        }
                    }
                } catch (UnrecognizedAuthenticationMethodException e) {
                    LOG.error("Error attempting to revoke token - unknown authBy", e);
                }
            }
        }
    }

    @Override
    public void revokeAllTokensForBaseUser(String userId) {
        //TODO: exapnd this to support rackers. Original implementation only supported EndUsers
        EndUser user = identityUserService.getEndUserById(userId);
        revokeAllTokensForBaseUser(user);
    }

    @Override
    public void revokeAllTokensForBaseUser(BaseUser user) {
        if (user == null) return;

        for (final ScopeAccess sa : this.scopeAccessDao.getScopeAccesses(user)) {
            revokeToken(user, sa);
        }
    }

    @Override
    public boolean isTokenRevoked(String token) {
        ScopeAccess sa = scopeAccessDao.getScopeAccessByAccessToken(token);
        return isTokenRevoked(sa);
    }

    @Override
    public boolean isTokenRevoked(ScopeAccess token) {
        return token.isAccessTokenExpired(new DateTime());
    }

    private void sendRevokeTokenFeedEvent(User user, String tokenString) {
        if (user != null) {
            LOG.warn("Sending token feed to atom hopper.");
            atomHopperClient.asyncTokenPost(user, tokenString);
        }
    }
}
