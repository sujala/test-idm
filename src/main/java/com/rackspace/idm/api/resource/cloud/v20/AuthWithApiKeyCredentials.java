package com.rackspace.idm.api.resource.cloud.v20;

import com.newrelic.api.agent.Trace;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.validation.Validator20;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthWithApiKeyCredentials extends BaseUserAuthenticationFactor {
    @Autowired
    private Validator20 validator20;

    @Autowired
    private UserService userService;

    @Autowired
    private AuthorizationService authorizationService;

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

        // Verify authorized for the specified domain
        authorizationService.updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary(result.getUser(), authenticationRequest);
        authorizationService.verifyUserAuthorizedToAuthenticateOnDomain(result.getUser(), authenticationRequest.getDomainId());

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
            throw new NotAuthenticatedException(ErrorCodes.ERROR_CODE_AUTH_INVALID_API_KEY_MSG, ErrorCodes.ERROR_CODE_AUTH_INVALID_API_KEY);
        }
    }

}
