package com.rackspace.idm.api.resource.cloud.v20.federated;

import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.entity.Racker;
import lombok.Getter;
import lombok.Setter;
import org.joda.time.DateTime;

/**
 * Contains a request to generate a token for a given "federated" user
 */
@Getter
@Setter
public class FederatedRackerRequest implements FederatedRequest<Racker> {
    private Racker user;
    private IdentityProvider identityProvider;
    private DateTime requestedTokenExpirationDate;
}
