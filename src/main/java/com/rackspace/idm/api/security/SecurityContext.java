package com.rackspace.idm.api.security;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.exception.UnrecoverableIdmException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Stores security related information about the current request.
 *
 * Not thread safe
 */
@Getter
@Setter
public class SecurityContext {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String TOKEN_CONTEXT_ERROR = "Security context error. The token stored in the security context does" +
            "not match the expected token.";

    /**
     * The token as provided in the initial REST request (X-Auth-Token header) by the consumer
     */
    private ScopeAccess callerToken;

    /**
     * Provides the token that should be used to authorize requests. This will be the same as the {@link #callerToken} unless
     * impersonation was used. In that case, this will be the underlying impersonated token (the one the impersonation token
     * links to).
     *
     */
    private ScopeAccess effectiveCallerToken;

    /**
     * The user associated with the effective token.
     */
    private BaseUser effectiveCaller;

    /**
     * The domain of the effectiveCaller
     */
    private Domain effectiveCallerDomain;

    /**
     * The authorization context of the effective caller
     */
    private AuthorizationContext effectiveCallerAuthorizationContext;

    /**
     * A request is impersonated if the caller token is not the same as the effective token
     * @return
     */
    public boolean isImpersonatedRequest() {
        return callerToken != effectiveCallerToken;
    }

    /**
     * Returns TRUE if the callerToken is an ImpersonationScopeAccess and the Impersonator is a Racker
     * @return
     */
    public boolean isRackerImpersonatedRequest() {
        return isImpersonatedRequest() && callerToken instanceof ImpersonatedScopeAccess && StringUtils.isNotBlank(((ImpersonatedScopeAccess) callerToken).getRackerId());
    }

    /**
     * This is meant to be analogous (and a direct replacement) for {@link com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service#getScopeAccessForValidToken(String)}
     * for the use case of retrieving the effective token and verifying that it's still valid, etc. This meant to provide
     * a baby step toward migrating to the use of the securityContext. Eventually, this method should be removed as well once
     * the authenticationFilter performs this validation itself.
     *
     * @param expectedAuthToken the expected token string.
     * @return
     * @deprecated use {@link #getAndVerifyEffectiveCallerTokenAsBaseToken} when possible
     * @throws com.rackspace.idm.exception.NotFoundException if the token is not set in the security context
     * @throws java.lang.IllegalStateException If the specified token string does not match that in the security context
     */
    @Deprecated
    public ScopeAccess getAndVerifyEffectiveCallerToken(String expectedAuthToken) {
        if (StringUtils.isBlank(expectedAuthToken) || effectiveCallerToken == null || effectiveCallerToken.isAccessTokenExpired(new DateTime())) {
            String errMsg = "No valid token provided. Please use the 'X-Auth-Token' header with a valid token.";
            throw new NotAuthorizedException(errMsg);
        }
        if (!effectiveCallerToken.getAccessTokenString().equalsIgnoreCase(expectedAuthToken)) {
            String errMsg = String.format(TOKEN_CONTEXT_ERROR);
            logger.error(errMsg);
            throw new IllegalStateException(errMsg);
        }
        return effectiveCallerToken;
    }

    /**
     * Returns interface rather than a concrete class to avoid coupling. Ideally all calls would migrate to using this
     * call over {@link #getAndVerifyEffectiveCallerToken(String)} and the use of ScopeAccess would be eliminated...
     *
     * @return
     */
    public BaseUserToken getAndVerifyEffectiveCallerTokenAsBaseToken(String expectedAuthToken) {
        ScopeAccess token = getAndVerifyEffectiveCallerToken(expectedAuthToken);

        // The only ScopeAccess that is not a BaseUserToken is the unused ClientScopeAccess to this should never occur
        if (!(token instanceof BaseUserToken)) {
            throw new UnrecoverableIdmException("Unrecognized token type");
        }

        return (BaseUserToken) token;
    }
}
