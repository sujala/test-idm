package com.rackspace.idm.api.resource.cloud.v20;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.validation.Validator20;
import org.apache.commons.configuration.Configuration;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthWithApiKeyCredentials extends BaseUserAuthenticationFactor {

    public static final String AUTH_FAILURE_MSG = "Username or api key is invalid.";

    @Autowired
    private Validator20 validator20;

    @Autowired
    private UserService userService;

    public AuthResponseTuple authenticateForAuthResponse(AuthenticationRequest authenticationRequest) {
        UserAuthenticationResult authResult = authenticate(authenticationRequest);
        return scopeAccessService.createScopeAccessForUserAuthenticationResult(authResult);
    }

    @Trace()
    public UserAuthenticationResult authenticate(AuthenticationRequest authenticationRequest) {
        String scope = null;
        if (authenticationRequest.getScope() != null) {
            scope = authenticationRequest.getScope().value();
        }
        ApiKeyCredentials creds = (ApiKeyCredentials) authenticationRequest.getCredential().getValue();
        validator20.validateApiKeyCredentials(creds);
        String username = creds.getUsername();
        String key = creds.getApiKey();
        UserAuthenticationResult result = authenticate(username, key);
        return new UserAuthenticationResult(result.getUser(), result.isAuthenticated(), result.getAuthenticatedBy(), scope);
    }

    /**
     * Returns a successful authentication result. Unsuccessful attempts will result in an exception being thrown.
     *
     * @param username
     * @param apiKey
     * @throws NotAuthenticatedException if the authentication was unsuccessful
     */
    public UserAuthenticationResult authenticate(String username, String apiKey) {
        UserAuthenticationResult result = this.userService.authenticateWithApiKey(username, apiKey);
        validateUserAuthenticationResult(result);

        /*
        For this use case the instanceof should always return true, but double check for backwards compatibility/refactor sake...
         */
        if (!(result.getUser() instanceof User)) {
            User user = userService.getUserByUsernameForAuthentication(username);
            result = new UserAuthenticationResult(user, result.isAuthenticated(), result.getAuthenticatedBy());
        }
        return result;
    }


    /**
     * Override base class, because must provide different authentication message
     * @param result
     */
    @Override
    public void validateUserAuthenticationResult(final UserAuthenticationResult result) {
        if (!result.isAuthenticated()) {
            String errorMessage = String.format(AUTH_FAILURE_MSG);
            throw new NotAuthenticatedException(errorMessage);
        }
    }

}
