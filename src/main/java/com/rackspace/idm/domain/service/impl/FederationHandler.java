package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.decorator.LogoutRequestDecorator;
import com.rackspace.idm.domain.decorator.SamlResponseDecorator;
import com.rackspace.idm.domain.entity.*;
import org.opensaml.saml2.core.LogoutResponse;

/**
 * Handles a specified type of user source for an identity provider. For example, a handler for an identity provider
 * that federates with eDir and one for dealing with federation against provisioned users.
 */
public interface FederationHandler {
    SamlAuthResponse processRequestForProvider(SamlResponseDecorator samlResponseDecorator, IdentityProvider provider);

    /**
     * Logs out/terminatres the federated user, which removes any persistent state for that user and revokes any tokens
     * that may have been created for the user.
     *
     * @param logoutRequestDecorator
     * @param provider
     * @return
     *
     * @throws com.rackspace.idm.exception.NotFoundException if the requested user does not exist
     */
    SamlLogoutResponse processLogoutRequestForProvider(LogoutRequestDecorator logoutRequestDecorator, IdentityProvider provider);
}
