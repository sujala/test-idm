package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.AuthData;
import com.rackspace.idm.domain.entity.FederatedToken;
import org.opensaml.saml2.core.Response;

public interface FederatedIdentityService {

    public AuthData generateAuthenticationInfo(Response samlResponse) throws Throwable;

    public AuthData getAuthenticationInfo(FederatedToken tokenInfo);
}
