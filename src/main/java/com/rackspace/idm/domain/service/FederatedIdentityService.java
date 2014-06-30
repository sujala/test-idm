package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.AuthData;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import org.opensaml.saml2.core.Response;

public interface FederatedIdentityService {

    public AuthData processSamlResponse(Response samlResponse);

    public AuthData getAuthenticationInfo(UserScopeAccess tokenInfo);
}
