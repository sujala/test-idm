package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.*;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections4.CollectionUtils;

import java.time.Instant;
import java.util.Date;

@Getter
@Builder(builderClassName = "RackerTokenRequestBuilder")
public class RackerTokenRequest implements TokenRequest<Racker, RackerScopeAccess> {
    private Racker issuedToUser;

    private Instant expirationDate;

    private Instant creationDate = Instant.now();

    private AuthenticatedByMethodGroup authenticatedByMethodGroup;

    /**
     * This is a legacy holdover from OAuth based initial architecture. It's, for all current scenarios, the standard
     * Identity "client". However, the clientId can change per environment.
     * @return
     */
    private String clientId;

    /**
     * The authentication domain is hardcoded to the rackspace domain
     * @return
     */
    @Override
    public String getAuthenticationDomainId() {
        return RackerScopeAccess.RACKSPACE_DOMAIN;
    }

    @Override
    public RackerScopeAccess generateShellScopeAccessForRequest() {
        RackerScopeAccess sa = new RackerScopeAccess();
        sa.setClientId(clientId);
        sa.setRackerId(issuedToUser.getRackerId());
        sa.setAccessTokenExp(Date.from(expirationDate));
        sa.setCreateTimestamp(Date.from(creationDate));
        if (authenticatedByMethodGroup != null) {
            sa.getAuthenticatedBy().addAll(authenticatedByMethodGroup.getAuthenticatedByMethodsAsValues());
        }
        return sa;
    }

    public static class RackerTokenRequestBuilder {
        private Instant creationDate = Instant.now();

        /**
         * A helper method to set the expiration date based on the "creation" date. The creation date must be set
         * already or an IllegalStateException will be thrown.
         *
         * @param seconds
         * @return
         */
        public RackerTokenRequestBuilder expireAfterCreation(int seconds) {
            if (creationDate == null) {
                throw new IllegalStateException("Creation date must be set.");
            }
            expirationDate = creationDate.plusSeconds(seconds);
            return this;
        }
    }
}
