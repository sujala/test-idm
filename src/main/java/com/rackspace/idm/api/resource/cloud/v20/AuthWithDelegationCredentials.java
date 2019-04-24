package com.rackspace.idm.api.resource.cloud.v20;

import com.newrelic.api.agent.Trace;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DelegationCredentials;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.DomainDefaultException;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.Validator20;
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
    public static final String ERROR_MSG_MISSING_AGREEMENT = "The specified agreement does not exist for this user";
    public static final String ERROR_MSG_DISABLED_OR_MISSING_DOMAIN = "Delegation domain is missing or disabled";
    public static final String ERROR_MSG_INVALID_DOMAIN = "Delegation domain is invalid.";


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

    @Autowired
    private Validator20 validator20;

    @Autowired
    private DomainService domainService;

    @Trace()
    public AuthResponseTuple authenticate(AuthenticationRequest authenticationRequest) {
        DelegationCredentials creds = (DelegationCredentials) authenticationRequest.getCredential().getValue();

        validator20.validateStringNotBlank("token", creds.getToken());
        validator20.validateStringNotBlank("delegationAgreementId", creds.getDelegationAgreementId());

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

        if (token.isDelegationToken()) {
            logger.debug("Delegation tokens not allowed. Rejecting.");
            throw new ForbiddenException(GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN);
        }

        EndUser user = identityUserService.getEndUserById(token.getIssuedToUserId());

        if (user == null) {
            logger.debug("User associated with token not found. Rejecting");
            throw new NotAuthenticatedException(ERROR_MSG_INVALID_TOKEN);
        }
        
        DelegationAgreement delegationAgreement = delegationService.getDelegationAgreementById(creds.getDelegationAgreementId());
        if (delegationAgreement == null) {
            throw new NotFoundException(ERROR_MSG_MISSING_AGREEMENT);
        }

        // User must be authorized to authenticate under DA.
        if (!delegationAgreement.isEffectiveDelegate(user)) {
            logger.info(String.format("User '%s' not authorized to use DA '%s'", user.getId(), delegationAgreement.getId()));
            Audit.logFailedDelegationAuth(user, delegationAgreement.getId(), "Not authorized for DA");
            throw new NotFoundException(ERROR_MSG_MISSING_AGREEMENT); // Return standard not found rather than expose that DA exists
        }

        // Target domain must be enabled to delegate under
        Domain domain = domainService.getDomain(delegationAgreement.getDomainId());
        if (domain == null || !Boolean.TRUE.equals(domain.getEnabled())) {
            Audit.logFailedDelegationAuth(user, delegationAgreement.getId(), "DA domain is disabled");
            throw new ForbiddenException(ERROR_MSG_DISABLED_OR_MISSING_DOMAIN);
        }

        // User has access to delegation, and user is verified, so create delegation token

        // Step 1: Create Delegate User
        DomainSubUserDefaults subUserDefaults = null;
        try {
            subUserDefaults = createSubUserService.calculateDomainSubUserDefaults(delegationAgreement.getDomainId());

        } catch (DomainDefaultException ex) {
            Audit.logFailedDelegationAuth(user, delegationAgreement.getId(), ex.getMessage());
            throw new ForbiddenException(ERROR_MSG_DISABLED_OR_MISSING_DOMAIN, ex.getErrorCode());
        }

        ProvisionedUserDelegate delegate = new ProvisionedUserDelegate(subUserDefaults, delegationAgreement, user);

        List<String> authByList = new ArrayList<>(token.getAuthenticatedBy());
        authByList.add(AuthenticatedByMethodEnum.DELEGATE.getValue());

        ScopeAccess delegateToken = scopeAccessService.addScopedScopeAccess(delegate,
                identityConfig.getStaticConfig().getCloudAuthClientId(),
                authByList,
                token.getAccessTokenExp(),
                null);

        requestContextHolder.getAuthenticationContext().setUsername(user.getUsername());

        Audit.logSuccessfulDelegationAuth(delegate);
        return new AuthResponseTuple(delegate, (UserScopeAccess) delegateToken, null);
    }
}
