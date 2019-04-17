package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.*;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

/**
 * Transfer object that provides information necessary to generate a new AE token.
 */
@Builder(builderClassName = "ImpersonationTokenRequestBuilder")
@Getter
public class ImpersonationTokenRequest implements TokenRequest<BaseUser, ImpersonatedScopeAccess> {
    /**
     * The caller wanting the impersonation token. Could be a Racker or EndUser
     */
    private BaseUser issuedToUser;

    private EndUser userToImpersonate;

    private Instant expirationDate;

    private Instant creationDate = Instant.now();

    private String clientId;

    private AuthenticatedByMethodGroup authenticatedByMethodGroup;

    private String authenticationDomainId;

    @Override
    public ImpersonatedScopeAccess generateShellScopeAccessForRequest() {
        ImpersonatedScopeAccess sa = new ImpersonatedScopeAccess();

        if (issuedToUser instanceof Racker) {
            sa.setRackerId(issuedToUser.getId());
        } else {
            //federated users are not allowed to impersonate so safe to cast to user at this point
            sa.setUserRsId(issuedToUser.getId());
        }
        sa.setClientId(clientId);
        sa.setCreateTimestamp(Date.from(creationDate));
        sa.setAccessTokenExp(Date.from(expirationDate));
        sa.setRsImpersonatingRsId(userToImpersonate.getId());
        sa.setAuthenticationDomainId(authenticationDomainId);
        if (authenticatedByMethodGroup != null) {
            sa.getAuthenticatedBy().addAll(authenticatedByMethodGroup.getAuthenticatedByMethodsAsValues());
        }

//        newImpersonatedScopeAccess.setImpersonatingToken(userTokenForImpersonation.getAccessTokenString());

        return sa;
    }

    public static class ImpersonationTokenRequestBuilder {
        private Instant creationDate = Instant.now();

        /**
         * A helper method to set the expiration date based on the "creation" date. The creation date must be set
         * already or an IllegalStateException will be thrown.
         *
         * @param seconds
         * @return
         */
        public ImpersonationTokenRequestBuilder expireAfterCreation(int seconds) {
            if (creationDate == null) {
                throw new IllegalStateException("Creation date must be set.");
            }
            expirationDate = creationDate.plusSeconds(seconds);
            return this;
        }
    }
}
