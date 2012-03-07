package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.RSACredentials;
import com.rackspace.idm.domain.entity.AuthCredentials;
import com.rackspace.idm.domain.entity.Credentials;
import com.rackspace.idm.domain.entity.RackerCredentials;

public class CredentialsConverter {

    public CredentialsConverter() {
    }

    public Credentials toCredentialsDO(com.rackspace.api.idm.v1.Credentials credentials) {
        Credentials credentialsDO;
        if (credentials instanceof com.rackspace.api.idm.v1.RackerCredentials) {
            credentialsDO = new RackerCredentials();
        } else if (credentials instanceof RSACredentials) {
            credentialsDO = new com.rackspace.idm.domain.entity.RSACredentials();
        } else {
            credentialsDO = new AuthCredentials();
        }
        credentialsDO.setClientId(credentials.getClientId());
        credentialsDO.setClientSecret(credentials.getClientSecret());
        credentialsDO.setPassword(credentials.getPassword());
        credentialsDO.setRefreshToken(credentials.getRefreshToken());
        credentialsDO.setUsername(credentials.getUsername());
        credentialsDO.setAuthorizationCode(credentials.getAuthorizationCode());
        if (credentials.getGrantType() != null) {
            credentialsDO.setGrantType(credentials.getGrantType().value());
        }
        return credentialsDO;
    }
}
