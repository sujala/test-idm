package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.IdentityProvider;

import java.util.List;

public interface IdentityProviderDao {

    IdentityProvider getIdentityProviderByUri(String uri);

    IdentityProvider getIdentityProviderByName(String name);

    /**
     * Return the identity provider that can received tokens for the given domainId
     *
     * @param domainId
     * @return
     */
    List<IdentityProvider> findIdentityProvidersApprovedForDomain(String domainId);

    /**
     * Find all identity providers
     * @return
     */
    List<IdentityProvider> findAllIdentityProviders();

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
     * Delete the specified Identity Provider
     *
     * @param identityProviderName
     */
    void deleteIdentityProviderById(String identityProviderName);

}
