package com.rackspace.idm.api.resource.cloud.v20;

import com.newrelic.api.agent.Trace;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationCredentials;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.lang.StringUtils;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class AuthWithDelegationCredentials {
    private static final Logger logger = LoggerFactory.getLogger(AuthWithDelegationCredentials.class);

    public static String ERROR_MSG_INVALID_TOKEN = "Token not authenticated";

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private DelegationService delegationService;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Autowired
    private CreateSubUserService createSubUserService;

    @Trace()
    public AuthResponseTuple authenticate(AuthenticationRequest authenticationRequest) {
        DelegationCredentials creds = (DelegationCredentials) authenticationRequest.getCredential().getValue();

        ScopeAccess rawToken = scopeAccessService.getScopeAccessByAccessToken(creds.getToken());

        if (rawToken == null || rawToken.isAccessTokenExpired()) {
            logger.debug("Token expired or undecryptable");
            String errMsg = ERROR_MSG_INVALID_TOKEN;
            throw new NotAuthenticatedException(errMsg);
        }

        if (!(rawToken instanceof UserScopeAccess)) {
            logger.debug("Must be a user based token. Rejecting");
            throw new ForbiddenException(ERROR_MSG_INVALID_TOKEN);
        }

        //restricted (scoped) tokens can not be used for auth w/ token
        if (StringUtils.isNotBlank(rawToken.getScope())) {
            logger.debug("Scoped tokens not allowed for service. Rejecting.");
            throw new ForbiddenException(GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN);
        }

        UserScopeAccess token = (UserScopeAccess) rawToken;

        User user = identityUserService.getProvisionedUserById(token.getIssuedToUserId());

        if (user == null) {
            logger.debug("User associated with token not found. Rejecting");
            throw new NotAuthenticatedException(ERROR_MSG_INVALID_TOKEN);
        }
        
        DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(creds.getDelegationAgreementId());
        if (delegationAgreement == null) {
            throw new NotFoundException("The specified agreement does not exist for this user");
        }

        // User must be authorized to authenticate under DA. Currently only support provisioned users so direct
        // comparison of DNs possible.
        if (!delegationAgreement.getDelegates().contains(user.getDn())) {
            logger.info(String.format("User '%s' not authorized to use DA '%s'", user.getId(), delegationAgreement.getId()));
            throw new NotFoundException("The specified agreement does not exist for this user"); // Return standard not found rather than expose that DA exists
        }

        // User has access to delegation, and user is verified, so create delegation token

        // Step 1: Create Delegate User
        DomainSubUserDefaults subUserDefaults = createSubUserService.calculateDomainSubUserDefaults(delegationAgreement.getDomainId());
        ProvisionedUserDelegate delegate = new ProvisionedUserDelegate(subUserDefaults, delegationAgreement, user);

        List<String> authByList = new ArrayList<>(token.getAuthenticatedBy());
        authByList.add(AuthenticatedByMethodEnum.DELEGATE.getValue());

        ScopeAccess delegateToken = scopeAccessService.addScopedScopeAccess(delegate,
                identityConfig.getCloudAuthClientId(),
                authByList,
                token.getAccessTokenExp(),
                null);

        requestContextHolder.getAuthenticationContext().setUsername(user.getUsername());

        return new AuthResponseTuple(delegate, (UserScopeAccess) delegateToken, null);
    }
}
