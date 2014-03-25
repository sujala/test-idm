package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.validation.Validator20;
import org.apache.commons.configuration.Configuration;
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

    public AuthResponseTuple authenticateForAuthResponse(AuthenticationRequest authenticationRequest) {
        UserAuthenticationResult authResult = authenticate(authenticationRequest);
        return createScopeAccessForUserAuthenticationResult(authResult);
    }

    public UserAuthenticationResult authenticate(AuthenticationRequest authenticationRequest) {
        PasswordCredentialsBase creds = (PasswordCredentialsBase) authenticationRequest.getCredential().getValue();
        validator20.validatePasswordCredentials(creds);
        String username = creds.getUsername();
        String password = creds.getPassword();

        UserAuthenticationResult result = this.userService.authenticate(username, password);
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


}
