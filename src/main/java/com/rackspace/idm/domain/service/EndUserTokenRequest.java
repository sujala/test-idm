package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.*;
import lombok.Builder;
import lombok.Getter;

import java.util.Date;
import java.time.Instant;

@Builder(builderClassName = "EndUserTokenRequestBuilder")
@Getter
public class EndUserTokenRequest implements TokenRequest<EndUser, UserScopeAccess> {
    private EndUser issuedToUser;

    private Instant expirationDate;

    private Instant creationDate = Instant.now();

    private String clientId;

    private AuthenticatedByMethodGroup authenticatedByMethodGroup;

    private String authenticationDomainId;

    /**
     * An optional scope for the request
     * @return
     */
    private TokenScopeEnum scope;

    @Override
    public UserScopeAccess generateShellScopeAccessForRequest() {
        UserScopeAccess sa = new UserScopeAccess();
        sa.setUserRsId(issuedToUser.getId());
        sa.setClientId(clientId);
        sa.setAccessTokenExp(Date.from(expirationDate));
        sa.setCreateTimestamp(Date.from(creationDate));
        sa.setAuthenticationDomainId(authenticationDomainId);
        if (authenticatedByMethodGroup != null) {
            sa.getAuthenticatedBy().addAll(authenticatedByMethodGroup.getAuthenticatedByMethodsAsValues());
        }

        if (scope != null) {
            sa.setScope(scope.getScope());
        }

        /**
         * Legacy code to support delegation agreements. Should be purged with general DA purge.
         */
        if (issuedToUser instanceof ProvisionedUserDelegate) {
            sa.setDelegationAgreementId(((ProvisionedUserDelegate) issuedToUser).getDelegationAgreement().getId());
        }

        return sa;
    }

    public static class EndUserTokenRequestBuilder {
        private Instant creationDate = Instant.now();

        /**
         * A helper method to set the expiration date based on the "creation" date. The creation date must be set
         * already or an IllegalStateException will be thrown.
         *
         * @param seconds
         * @return
         */
        public EndUserTokenRequestBuilder expireAfterCreation(int seconds) {
            if (creationDate == null) {
                throw new IllegalStateException("Creation date must be set.");
            }
            expirationDate = creationDate.plusSeconds(seconds);
            return this;
        }
    }
}
