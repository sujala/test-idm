package com.rackspace.idm.api.security;

import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.exception.NotAuthorizedException;
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

    private boolean impersonatedRequest = false;

    /**
     * The token as provided in the REST request (X-Auth-Token header)
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
     * A request is impersonated if the caller token is not the same as the effective token
     * @return
     */
    public boolean isImpersonatedRequest() {
        return callerToken != effectiveCallerToken;
    }


    /**
     * This is meant to be analogous (and a direct replacement) for {@link com.rackspace.idm.api.resource.cloud.v20.DefaultCloud20Service#getScopeAccessForValidToken(String)}
     * for the use case of retrieving the effective token and verifying that it's still valid, etc. This meant to provide
     * a baby step toward migrating to the use of the securityContext. Eventually, this method should be removed as well once
     * the authenticationFilter performs this validation itself.
     *
     * @param expectedAuthToken the expected token string.
     * @return
     *
     * @throws com.rackspace.idm.exception.NotFoundException if the token is not set in the security context
     * @throws java.lang.IllegalStateException If the specified token string does not match that in the security context
     */
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

}
