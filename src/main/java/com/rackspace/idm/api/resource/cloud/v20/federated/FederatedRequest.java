package com.rackspace.idm.api.resource.cloud.v20.federated;

import com.rackspace.idm.domain.entity.FederatedBaseUser;
import com.rackspace.idm.domain.entity.IdentityProvider;
import org.joda.time.DateTime;

/**
 * Contains a request to generate a token for a given "federated" user.
 */
public interface FederatedRequest<T extends FederatedBaseUser> {
    T getUser();
    IdentityProvider getIdentityProvider();
    DateTime getRequestedTokenExpirationDate();

}
