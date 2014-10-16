package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.domain.dao.UUIDScopeAccessDao;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.RevokeTokenService;
import com.rackspace.idm.domain.service.UUIDRevokeTokenService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.collections4.CollectionUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class LdapUUIDRevokeTokenService implements UUIDRevokeTokenService {
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
    public void revokeTokensForEndUser(String userId, List<Set<String>> authenticatedByList) {
        EndUser user = identityUserService.getEndUserById(userId);
        revokeTokensForEndUser(user, authenticatedByList);
    }

    @Override
    public void revokeTokensForEndUser(EndUser user, List<Set<String>> authenticatedByList) {
        if (user == null) return;

        for (final ScopeAccess sa : this.scopeAccessDao.getScopeAccesses(user)) {
            List<String> tokenAuthBy = sa.getAuthenticatedBy();
            for (Set<String> revokeAuthBy : authenticatedByList) {
                if (CollectionUtils.isEqualCollection(tokenAuthBy, revokeAuthBy)) {
                    revokeToken(user, sa);
                }
            }
        }
    }

    @Override
    public void revokeAllTokensForEndUser(String userId) {
        EndUser user = identityUserService.getEndUserById(userId);
        revokeAllTokensForEndUser(user);
    }

    @Override
    public void revokeAllTokensForEndUser(EndUser user) {
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
