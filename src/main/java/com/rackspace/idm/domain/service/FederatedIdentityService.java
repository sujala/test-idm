package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.entity.SamlAuthResponse;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.Response;

public interface FederatedIdentityService {
    SamlAuthResponse processSamlResponse(Response samlResponse);

    /**
     * Logs out/terminates the associated federated user, which removes any persistent state for that user and revokes any tokens
     * that may have been created for the user.
     *
     * @param logoutRequest
     * @return
     */
    void processLogoutRequest(LogoutRequest logoutRequest);

    /**
     * Add a new identity provider.
     * @param identityProvider
     * @return
     * @throws com.rackspace.idm.exception.DuplicateException If provider already exists with specified issuer
     */
    void addIdentityProvider(IdentityProvider identityProvider);

    /**
     * Return the identity provider with the given id. The id is synonymous with the identity provider name.
     * @param id
     * @return
     */
    IdentityProvider getIdentityProvider(String id);

    /**
     * Return the identity provider with the given id. The id is synonymous with the identity provider name.
     *
     * @param id
     * @return
     * @throws com.rackspace.idm.exception.NotFoundException If a provider with given id does not exist
     */
    IdentityProvider checkAndGetIdentityProvider(String id);

    /**
     * Return the identity provider with the given issuer.
     * @param issuer
     * @return
     */
    IdentityProvider getIdentityProviderByIssuer(String issuer);

    /**
     * Delete the specified Identity Provider. The id is synonymous with the identity provider name.
     *
     * @param id
     */
    void deleteIdentityProviderById(String id);
}
