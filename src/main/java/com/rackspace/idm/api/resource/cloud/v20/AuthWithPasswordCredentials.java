package com.rackspace.idm.api.resource.cloud.v20;

import com.newrelic.api.agent.Trace;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.Validator20;
import org.apache.commons.lang.StringUtils;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AuthWithPasswordCredentials extends BaseUserAuthenticationFactor {

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
        PasswordCredentialsBase creds = (PasswordCredentialsBase) authenticationRequest.getCredential().getValue();
        validator20.validatePasswordCredentials(creds);
        String username = creds.getUsername();
        String password = creds.getPassword();

        // Verifies the user's password is correct, user is not locked out, and user isn't required to change password
        UserAuthenticationResult result = this.userService.authenticate(username, password);
        validateUserAuthenticationResult(result);

        /*
        The user must be authorized to authenticate into the requested domain. Initially, users are only authorized
        to authenticate into their own domain. This will be expanded under domain trusts, but for now can keep
        check simple. Any request that doesn't include a domain, defaults to the user's domain
         */
        authorizationService.updateAuthenticationRequestAuthorizationDomainWithDefaultIfNecessary(result.getUser(), authenticationRequest);
        authorizationService.verifyUserAuthorizedToAuthenticateOnDomain(result.getUser(), authenticationRequest.getDomainId());

        result = new UserAuthenticationResult(result.getUser(), result.isAuthenticated(), result.getAuthenticatedBy(), scope);

        /*
        For this use case the instanceof should always return true, but double check for backwards compatibility/refactor sake...
         */
        if (!(result.getUser() instanceof User)) {
            User user = userService.getUserByUsernameForAuthentication(creds.getUsername());
            result = new UserAuthenticationResult(user, result.isAuthenticated(), result.getAuthenticatedBy(), scope);
        }
        return result;
    }


}
