package com.rackspace.idm.domain.security.tokencache;

import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.security.tokenproviders.TokenProvider;

public interface TokenCache {
    String getOrCreateTokenForUser(BaseUser user, ScopeAccess token, TokenProvider provider);

    boolean isTokenCacheableForUser(BaseUser user, ScopeAccess token);
}
