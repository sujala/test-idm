package com.rackspace.idm.api.resource.cloud.v20;

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

    @Autowired
    private Validator20 validator20;

    @Autowired
    private UserService userService;

    public AuthResponseTuple authenticateForAuthResponse(AuthenticationRequest authenticationRequest) {
        UserAuthenticationResult authResult = authenticate(authenticationRequest);
        return createScopeAccessForUserAuthenticationResult(authResult);
    }

    public UserAuthenticationResult authenticate(AuthenticationRequest authenticationRequest) {
        ApiKeyCredentials creds = (ApiKeyCredentials) authenticationRequest.getCredential().getValue();
        validator20.validateApiKeyCredentials(creds);
        String username = creds.getUsername();
        String key = creds.getApiKey();

        UserAuthenticationResult result = this.userService.authenticateWithApiKey(username, key);
        validateUserAuthenticationResult(result);

        /*
        For this use case the instanceof should always return true, but double check for backwards compatibility/refactor sake...
         */
        if (!(result.getUser() instanceof User)) {
            User user = userService.getUserByUsernameForAuthentication(creds.getUsername());
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
            String errorMessage = String.format("Username or api key is invalid.");
            throw new NotAuthenticatedException(errorMessage);
        }
    }

}
