package com.rackspace.idm.domain.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * A wrapper object that returns all relevant information generated when processing a SAML authentication request.
 */
@Getter
@Setter
@AllArgsConstructor
public class SamlAuthResponse {
    /**
     * The federated user for the saml authentication.
     */
    private FederatedBaseUser user;

    /**
     * The roles the user was determined to have. This should be used in lieu of any roles object on the {@link #user} object.
     */
    private List<TenantRole> userRoles;

    /**
     * The endpoints associated with the given user
     */
    private List<OpenstackEndpoint> endpoints;

    /**
     * The token that was generated in response to the authentication request.
     */
    private ScopeAccess token;
}
