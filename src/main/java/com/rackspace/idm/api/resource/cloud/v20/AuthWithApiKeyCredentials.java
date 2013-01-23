package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.Validator20;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthWithApiKeyCredentials {

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

        ApiKeyCredentials creds = (ApiKeyCredentials) authenticationRequest.getCredential().getValue();
        validator20.validateApiKeyCredentials(creds);
        String username = creds.getUsername();
        String key = creds.getApiKey();

        authResponseTuple.setUser(userService.getUserByUsernameForAuthentication(username));

        authResponseTuple.setUserScopeAccess(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndApiCredentials(username, key, getCloudAuthClientId()));
        //Check if authentication is within 12hrs of experation if so create a new one

        return authResponseTuple;
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }
}
