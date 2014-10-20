package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.AEScopeAccessDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.security.AETokenService;
import com.rackspace.idm.domain.security.MarshallTokenException;
import com.rackspace.idm.domain.security.UnmarshallTokenException;
import com.rackspace.idm.exception.IdmException;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

public class AEScopeAccessRepository implements AEScopeAccessDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(AEScopeAccessRepository.class);

    @Autowired
    private AETokenService aeTokenService;

    @Override
    public boolean supportsCreatingTokenFor(UniqueId object, ScopeAccess scopeAccess) {
        return aeTokenService.supportsCreatingTokenFor(object, scopeAccess);
    }

    @Override
    public void addScopeAccess(UniqueId object, ScopeAccess scopeAccess) {
        if (object instanceof BaseUser) {
            try {
                aeTokenService.marshallTokenForUser((BaseUser) object, scopeAccess);
            } catch (MarshallTokenException e) {
                throw new IdmException("Error creating the token.", e);
            }
        } else {
            throw new IdmException("AEScopeAccessRepository was called with an unsupported type of UniqueId: " + object.getClass());
        }
    }

    @Override
    public ScopeAccess getScopeAccessByAccessToken(String accessToken) {
        try {
            return aeTokenService.unmarshallTokenAndCheckRevoked(accessToken);
        } catch (UnmarshallTokenException e) {
            LOGGER.error("Error unmarshalling the token: " + accessToken, e);
            return null;
        }
    }

    /* Dummy methods (don't make sense for AE tokens) */

    @Override
    public void deleteScopeAccess(ScopeAccess scopeAccess) {
    }

    @Override
    public void updateScopeAccess(ScopeAccess scopeAccess) {
    }

    @Override
    public ScopeAccess getMostRecentScopeAccessForUser(User user) {
        return null;
    }

    @Override
    public ScopeAccess getScopeAccessByRefreshToken(String refreshToken) {
        return null;
    }

    @Override
    public ScopeAccess getMostRecentScopeAccessByClientId(UniqueId object, String clientId) {
        return null;
    }

    @Override
    public ScopeAccess getMostRecentImpersonatedScopeAccessForUserRsId(BaseUser user, String impersonatingRsId) {
        return null;
    }

    @Override
    public ScopeAccess getMostRecentScopeAccessByClientIdAndAuthenticatedBy(UniqueId object, String clientId, List<String> authenticatedBy) {
        return null;
    }

    @Override
    public ScopeAccess getMostRecentImpersonatedScopeAccessForUserOfUser(BaseUser user, String impersonatingUsername) {
        return null;
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccessesByUserId(String userId) {
        return new ArrayList<ScopeAccess>();
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccesses(UniqueId object) {
        return new ArrayList<ScopeAccess>();
    }

    @Override
    public Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUser(BaseUser user) {
        return new ArrayList<ScopeAccess>();
    }

    @Override
    public Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUserOfUserByRsId(BaseUser user, String impersonatingRsId) {
        return new ArrayList<ScopeAccess>();
    }

    @Override
    public Iterable<ScopeAccess> getAllImpersonatedScopeAccessForUserOfUserByUsername(BaseUser user, String impersonatingUsername) {
        return new ArrayList<ScopeAccess>();
    }

    @Override
    public Iterable<ScopeAccess> getScopeAccessesByClientId(UniqueId object, String clientId) {
        return new ArrayList<ScopeAccess>();
    }

    /* LDAP compatibility methods */

    @Override
    public String getClientIdForParent(ScopeAccess scopeAccess) {
        // TODO: Remove dependency with UnboundID
        String parentDn = null;
        try {
            parentDn = new DN(scopeAccess.getUniqueId()).getParentString();
        } catch (LDAPException e) {
            //noop
        }
        return parseDNForClientId(parentDn);
    }

    private String parseDNForClientId(String parentDn) {
        String clientId = null;
        try {
            if (parentDn != null) {
                String[] DN = parentDn.split(",");
                if (DN.length > 0) {
                    clientId = DN[0].split("=")[1];
                }
            }
        } catch (Exception e) {
            //noop
        }
        return clientId;
    }

}
