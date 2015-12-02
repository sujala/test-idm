package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.entity.SamlAuthResponse;
import com.rackspace.idm.domain.entity.SamlLogoutResponse;
import org.opensaml.saml2.core.LogoutRequest;
import org.opensaml.saml2.core.LogoutResponse;
import org.opensaml.saml2.core.Response;

import java.util.List;

public interface FederatedIdentityService {
    SamlAuthResponse processSamlResponse(Response samlResponse);

    /**
     * Logs out/terminates the associated federated user, which removes any persistent state for that user and revokes any tokens
     * that may have been created for the user.
     *
     * @param logoutRequest
     * @return
     */
    SamlLogoutResponse processLogoutRequest(LogoutRequest logoutRequest);

    /**
     * Add a new identity provider.
     * @param identityProvider
     * @return
     * @throws com.rackspace.idm.exception.DuplicateException If provider already exists with specified issuer
     */
    void addIdentityProvider(IdentityProvider identityProvider);

    /**
     * Save updates to an existing identity provider.
     * @param identityProvider
     * @return
     */
    void updateIdentityProvider(IdentityProvider identityProvider);

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
     * Return the identity provider that can received tokens for the given domainId
     *
     * @param domainId
     * @return
     * @throws com.rackspace.idm.exception.SizeLimitExceededException
     */
    List<IdentityProvider> findIdentityProvidersApprovedForDomain(String domainId);

    /**
     * Return all identity providers
     * @return
     *
     * @throws com.rackspace.idm.exception.SizeLimitExceededException
     */
    List<IdentityProvider> findAllIdentityProviders();

    /**
     * Delete the specified Identity Provider. The id is synonymous with the identity provider name.
     *
     * @param id
     */
    void deleteIdentityProviderById(String id);
}
