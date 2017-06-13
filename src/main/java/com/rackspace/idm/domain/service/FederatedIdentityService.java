package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.entity.SamlAuthResponse;
import com.rackspace.idm.domain.entity.SamlLogoutResponse;
import org.opensaml.saml.saml2.core.LogoutRequest;
import org.opensaml.saml.saml2.core.Response;

import javax.naming.ServiceUnavailableException;
import java.util.List;

public interface FederatedIdentityService {

    /**
     * Authenticate federated user via 1.0 of API.
     *
     * @param samlResponse
     * @return
     * @throws ServiceUnavailableException
     * @deprecated Use @{link processV2SamlResponse}
     */
    @Deprecated
    SamlAuthResponse processSamlResponse(Response samlResponse) throws ServiceUnavailableException;

    /**
     * Process v2 API version
     *
     * @param samlResponse
     * @return
     */
    SamlAuthResponse processV2SamlResponse(Response samlResponse)  throws ServiceUnavailableException;

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
     * Return the identity provider with the given id.
     * @param id
     * @return
     */
    IdentityProvider getIdentityProvider(String id);

    /**
     * Return the identity provider with the given identity provider name.
     * @param name
     * @return
     */
    IdentityProvider getIdentityProviderByName(String name);

    /**
     * Return the identity provider by id with only metadata attribute populated.
     * @param id
     * @return
     */
    IdentityProvider getIdentityProviderWithMetadataById(String id);

    /**
     * Return the identity provider by id with only metadata attribute populated.
     * @param id
     * @return
     * @throws com.rackspace.idm.exception.NotFoundException when the IDP is not found
     */
    IdentityProvider checkAndGetIdentityProviderWithMetadataById(String id);

    /**
     * Return the identity provider by name that can create tokens for the given name and domainId
     *
     * @param name
     * @param domainId
     * @return
     */
    IdentityProvider getIdentityProviderApprovedForDomain(String name, String domainId);

    /**
     * Return the identity provider by name that has an EXPLICIT domain restriction for the given name and domainId.
     *
     * @param name
     * @param domainId
     * @return
     */
    IdentityProvider getIdentityProviderExplicitlyApprovedForDomain(String name, String domainId);

    /**
     * Return the identity provider by issuer that has any EXPLICIT domain restriction.
     *
     * @param name
     * @return
     */
    IdentityProvider getIdentityProviderExplicitlyApprovedForAnyDomain(String name);

    /**
     * Return the identity provider with the given id.
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
     * Return the identity provider by issuer that can create tokens for the given name and domainId
     *
     * @param issuer
     * @param domainId
     * @return
     */
    IdentityProvider getIdentityProviderApprovedForDomainByIssuer(String issuer, String domainId);

    /**
     * Return the identity provider by issuer that has an EXPLICIT domain restriction for the given name and domainId.
     *
     * @param issuer
     * @param domainId
     * @return
     */
    IdentityProvider getIdentityProviderExplicitlyApprovedForDomainByIssuer(String issuer, String domainId);

    /**
     * Return the identity provider by issuer that has any EXPLICIT domain restriction.
     *
     * @param issuer
     * @return
     */
    IdentityProvider getIdentityProviderExplicitlyApprovedForAnyDomainByIssuer(String issuer);

    /**
     * Return the identity providers that can create tokens for the given domainId
     *
     * @param domainId
     * @return
     * @throws com.rackspace.idm.exception.SizeLimitExceededException
     */
    List<IdentityProvider> findIdentityProvidersApprovedForDomain(String domainId);

    /**
     * Return the identity providers that have an EXPLICIT domain restriction for the given domainId.
     *
     * @param domainId
     * @return
     * @throws com.rackspace.idm.exception.SizeLimitExceededException
     */
    List<IdentityProvider> findIdentityProvidersExplicitlyApprovedForDomain(String domainId);

    /**
     * Return the identity providers that have any EXPLICIT domain restriction for a given domainId
     *
     * @return
     * @throws com.rackspace.idm.exception.SizeLimitExceededException
     */
    List<IdentityProvider> findIdentityProvidersExplicitlyApprovedForAnyDomain();

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
