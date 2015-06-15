package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.decorator.SamlResponseDecorator;
import com.rackspace.idm.domain.entity.AuthData;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.entity.SamlAuthResponse;
import com.rackspace.idm.domain.entity.UserScopeAccess;

/**
 * Handles a specified type of user source for an identity provider. For example, a handler for an identity provider
 * that federates with eDir and one for dealing with federation against provisioned users.
 */
public interface FederationHandler {
    SamlAuthResponse processRequestForProvider(SamlResponseDecorator samlResponseDecorator, IdentityProvider provider);
}
