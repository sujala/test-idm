package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.Validator20;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.PasswordCredentialsRequiredUsername;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthWithPasswordCredentials {

    @Autowired
    private Validator20 validator20;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private UserService userService;

    @Autowired
    private Configuration config;

    AuthResponseTuple authenticate(AuthenticationRequest authenticationRequest) {
        AuthResponseTuple authResponseTuple = new AuthResponseTuple();

        PasswordCredentialsRequiredUsername creds = (PasswordCredentialsRequiredUsername) authenticationRequest.getCredential().getValue();
        //TODO username validation breaks validate call
        validator20.validatePasswordCredentials(creds);
        String username = creds.getUsername();
        String password = creds.getPassword();

        authResponseTuple.setUser(userService.getUserByUsernameForAuthentication(username));

        authResponseTuple.setUserScopeAccess(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(username, password, getCloudAuthClientId()));

        return authResponseTuple;
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }
}
