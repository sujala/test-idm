package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.AEScopeAccessDao;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.security.AETokenService;
import com.rackspace.idm.domain.security.MarshallTokenException;
import com.rackspace.idm.domain.security.UnmarshallTokenException;
import com.rackspace.idm.exception.IdmException;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

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
            //only log at debug level as old tokens can't be unmarshalled and would throw this error. Don't want to fill
            //up logs with stacktraces of attempts to unmarshall old tokens
            LOGGER.debug("Error unmarshalling the token: " + accessToken, e);
            return null;
        }
    }
}
