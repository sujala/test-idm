package com.rackspace.idm.api.resource.cloud.v20.federated;

import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.FederatedUser;
import com.rackspace.idm.domain.entity.IdentityProvider;
import lombok.Data;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

/**
 * Contains a request to generate a token for a given "federated" user
 */
@Data
public class FederatedUserRequest {
    private FederatedUser federatedUser;
    private IdentityProvider identityProvider;
    private DateTime requestedTokenExpirationDate;
    private Map<String, ClientRole> requestClientRoleCache = new HashMap<String, ClientRole>();
}
