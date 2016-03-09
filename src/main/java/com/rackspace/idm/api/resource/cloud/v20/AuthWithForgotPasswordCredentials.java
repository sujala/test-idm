package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ForgotPasswordCredentials;
import com.rackspace.idm.api.resource.cloud.email.EmailClient;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.Validator20;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang.StringUtils;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.PasswordCredentialsBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.Set;

@Component
public class AuthWithForgotPasswordCredentials extends BaseUserAuthenticationFactor {

    @Autowired
    private UserService userService;

    @Autowired
    private EmailClient emailClient;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private IdentityConfig identityConfig;

    public AuthResponseTuple authenticateForAuthResponse(AuthenticationRequest authenticationRequest) {
        ForgotPasswordCredentials forgotPasswordCredentials = (ForgotPasswordCredentials) authenticationRequest.getCredential().getValue();

        UserAuthenticationResult authResult = authenticate(authenticationRequest);

        AuthResponseTuple authResponse = null;
        if (authResult.isAuthenticated()) {
            authResponse = scopeAccessService.createScopeAccessForUserAuthenticationResult(authResult);
            User user = (User)authResponse.getUser();
            sendForgotPasswordEmail(user, authResponse.getUserScopeAccess(), forgotPasswordCredentials.getPortal());
            Audit.logSuccessfulForgotPasswordRequest(forgotPasswordCredentials, user);
        }

        return authResponse;
    }

    public UserAuthenticationResult authenticate(AuthenticationRequest authenticationRequest) {
        ForgotPasswordCredentials creds = (ForgotPasswordCredentials) authenticationRequest.getCredential().getValue();
        validateForgotPasswordRequest(creds);

        String username = creds.getUsername();
        User user = userService.getUser(username);

        if (user == null) {
            Audit.logFailedForgotPasswordRequest(creds, "User not found");
            return new UserAuthenticationResult(user, false);
        }

        if (user.isDisabled()) {
            Audit.logFailedForgotPasswordRequest(creds, String.format("User disabled"));
            return new UserAuthenticationResult(user, false);
        }

        IdentityUserTypeEnum userType = authorizationService.getIdentityTypeRoleAsEnum(user);
        if (userType == IdentityUserTypeEnum.SERVICE_ADMIN || userType == IdentityUserTypeEnum.IDENTITY_ADMIN) {
            Audit.logFailedForgotPasswordRequest(creds, String.format("User is %s", userType));
            return new UserAuthenticationResult(user, false);
        }

        return new UserAuthenticationResult(user, true, AuthenticatedByMethodGroup.EMAIL.getAuthenticatedByMethodsAsValues(), TokenScopeEnum.PWD_RESET.getScope());
    }

    private void validateForgotPasswordRequest(ForgotPasswordCredentials creds) {
        if (StringUtils.isBlank(creds.getPortal())) {
            creds.setPortal("default");
        } else {
            //verify provided portal is valid (case insensitive)
            creds.setPortal(creds.getPortal().toLowerCase());
            Set<String> portals = identityConfig.getReloadableConfig().getForgotPasswordValidPortals();
            boolean found = false;
            for (Iterator<String> it = portals.iterator(); it.hasNext() && !found; ) {
                if (it.next().equalsIgnoreCase(creds.getPortal())) {
                    found = true;
                }
            }

            if (!found) {
                throw new BadRequestException(String.format("Portal '%s' is not valid", creds.getPortal()));
            }
        }
    }

    private void sendForgotPasswordEmail(User user, ScopeAccess token, String portal) {
        emailClient.asyncSendForgotPasswordMessage(user, token, portal);
    }
}
