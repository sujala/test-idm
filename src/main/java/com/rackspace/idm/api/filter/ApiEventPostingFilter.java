package com.rackspace.idm.api.filter;

import com.rackspace.idm.api.resource.IdmPathUtils;
import com.rackspace.idm.api.security.AuthorizationContext;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.BaseUserToken;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthenticationService;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import com.rackspace.idm.event.*;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.regex.Matcher;

/**
 * A response filter that publishes an event to the Spring publishing framework. The events published must be threadsafe.
 */
@Component
public class ApiEventPostingFilter implements ContainerResponseFilter {
    private final Logger logger = LoggerFactory.getLogger(ApiEventPostingFilter.class);

    @Autowired
    private IdmPathUtils idmPathUtils;

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    protected ApplicationEventPublisher applicationEventPublisher;

    public static final String DATA_UNAVAILABLE = "<NotAvailable>";

    @Override
    public ContainerResponse filter(ContainerRequest request, ContainerResponse response) {
        try {
            if (!identityConfig.getReloadableConfig().isFeatureSendNewRelicCustomDataEnabled() || request == null) {
                return response;
            }

            ApiEvent event = null;
            if (idmPathUtils.isAuthenticationResource(request)) {
                event = createAuthApiEvent(request);
            } else if (idmPathUtils.isProtectedResource(request)) {
                event = createProtectedApiEvent(request);
            } else if (idmPathUtils.isUnprotectedResource(request)) {
                event = createUnprotectedApiEvent(request);
            }

            if (event != null) {
                applicationEventPublisher.publishEvent(new ApiEventSpringWrapper(this, event));
            } else {
                String apiPath = scrubRequestUri(request);
                logger.warn(String.format("Unrecognized event type for request '%s'", apiPath));
            }
        } catch (Exception e) {
            logger.warn("Error posting api event", e);
        }
        return response;
    }

    private ApiEvent createAuthApiEvent(ContainerRequest request) {
        String requestUri = scrubRequestUri(request);
        String eventId = MDC.get(Audit.GUUID);

        return BasicAuthApiEvent.BasicAuthApiEventBuilder.aBasicAuthApiEvent()
                .eventId(StringUtils.defaultString(eventId, DATA_UNAVAILABLE))
                .requestUri(StringUtils.defaultString(requestUri, DATA_UNAVAILABLE))
                .remoteIp(StringUtils.defaultString(MDC.get(Audit.REMOTE_IP), DATA_UNAVAILABLE))
                .forwardedForIp(StringUtils.defaultString(MDC.get(Audit.X_FORWARDED_FOR), DATA_UNAVAILABLE))
                .nodeName(StringUtils.defaultString(identityConfig.getReloadableConfig().getAENodeNameForSignoff(), DATA_UNAVAILABLE))
                .userName(StringUtils.defaultString(requestContextHolder.getAuthenticationContext().getUsername(), DATA_UNAVAILABLE))
                .build();
    }

    private ApiEvent createUnprotectedApiEvent(ContainerRequest request) {
        String requestUri = scrubRequestUri(request);
        String eventId = MDC.get(Audit.GUUID);

        return BasicUnprotectedApiEvent.BasicUnprotectedApiEventBuilder.aBasicUnprotectedApiEvent()
                .eventId(StringUtils.defaultString(eventId, DATA_UNAVAILABLE))
                .requestUri(StringUtils.defaultString(requestUri, DATA_UNAVAILABLE))
                .remoteIp(StringUtils.defaultString(MDC.get(Audit.REMOTE_IP), DATA_UNAVAILABLE))
                .forwardedForIp(StringUtils.defaultString(MDC.get(Audit.X_FORWARDED_FOR), DATA_UNAVAILABLE))
                .nodeName(StringUtils.defaultString(identityConfig.getReloadableConfig().getAENodeNameForSignoff(), DATA_UNAVAILABLE))
                .build();
    }

    /**
     * Currently only support providing details for protected resources that have the request context populated w/
     * the appropriate security context. This is limited to v2.0 resources AFAIK at the moment.
     *
     * @param request
     * @return
     */
    private ApiEvent createProtectedApiEvent(ContainerRequest request) {
        String requestUri = scrubRequestUri(request);
        String eventId = MDC.get(Audit.GUUID);

        Caller effectiveCaller = generateEffectiveCaller();
        Caller caller = generateCaller(effectiveCaller);
        String maskedCallerToken = generateMaskedCallerToken(request);
        String maskedEffectiveCallerToken = generateMaskedEffectiveCallerToken();

        return BasicProtectedApiEvent.BasicProtectedApiEventBuilder.aBasicProtectedApiEvent()
                .eventId(StringUtils.defaultString(eventId, DATA_UNAVAILABLE))
                .requestUri(StringUtils.defaultString(requestUri, DATA_UNAVAILABLE))
                .remoteIp(StringUtils.defaultString(MDC.get(Audit.REMOTE_IP), DATA_UNAVAILABLE))
                .forwardedForIp(StringUtils.defaultString(MDC.get(Audit.X_FORWARDED_FOR), DATA_UNAVAILABLE))
                .nodeName(StringUtils.defaultString(identityConfig.getReloadableConfig().getAENodeNameForSignoff(), DATA_UNAVAILABLE))
                .maskedCallerToken(StringUtils.defaultString(maskedCallerToken, DATA_UNAVAILABLE))
                .caller(caller)
                .maskedEffectiveCallerToken(StringUtils.defaultString(maskedEffectiveCallerToken, DATA_UNAVAILABLE))
                .effectiveCaller(effectiveCaller)
                .build();
    }

    /**
     * Must never log the full token, but want to provide some of it just to allow for easier reconciliation of user
     * requests. So here we'll use the common ScopeAccess means to mask the token. Some services may not populate the
     * security context with the effective caller token. We only provide if readily available.
     */
    private String generateMaskedEffectiveCallerToken() {
        ScopeAccess effectiveCallerToken = requestContextHolder.getRequestContext().getSecurityContext().getEffectiveCallerToken();

        String maskedEffectiveCallerToken = DATA_UNAVAILABLE;
        if (effectiveCallerToken != null) {
            maskedEffectiveCallerToken = effectiveCallerToken.getMaskedAccessTokenString();
        }
        return maskedEffectiveCallerToken;
    }

    private IdentityUserTypeEnum determineEffectiveCallerUserType() {
        IdentityUserTypeEnum userType = null;
        if (requestContextHolder.getRequestContext() != null
                && requestContextHolder.getRequestContext().getSecurityContext() != null
                && requestContextHolder.getRequestContext().getSecurityContext().getEffectiveCallerAuthorizationContext() != null) {
            AuthorizationContext authContext = requestContextHolder.getRequestContext().getSecurityContext().getEffectiveCallerAuthorizationContext();
            userType = authContext.getIdentityUserType();
        }
        return userType;
    }

    /**
     * Must never log the full token, but want to provide some of it just to allow for easier reconciliation of user
     * requests. So here we'll use the common ScopeAccess means to mask the token. Some services may not populate the
     * security context with the caller token. We only provide if readily available w/ a fallback to any header value
     * provided.
     */
    private String generateMaskedCallerToken(ContainerRequest request) {
        ScopeAccess callerToken = requestContextHolder.getRequestContext().getSecurityContext().getCallerToken();

        String maskedCallerToken = DATA_UNAVAILABLE;
        if (callerToken == null) {
            // Retrieve v2.0 auth header (which is how security context gets populated). This will allow us to provide token
            // even if can't be decrypted (e.g. invalid token)
            String headerToken = request.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER);
            if (headerToken != null) {
                ScopeAccess sa = new ScopeAccess();
                sa.setAccessTokenString(headerToken);
                maskedCallerToken = sa.getMaskedAccessTokenString();
            }
        } else {
            maskedCallerToken = callerToken.getMaskedAccessTokenString();
        }
        return maskedCallerToken;
    }

    /**
     * Get the readily available information on the caller. The security context currently only stores the caller token,
     * not the caller itself (just the effective caller). However, if (caller token == effective token) the users will
     * also be the same. Otherwise, similar to effective caller, if caller token is stored, can potentially retrieve the caller
     * userId from the token itself.
     */
    private Caller generateCaller(Caller effectiveCaller) {
        ScopeAccess callerToken = requestContextHolder.getRequestContext().getSecurityContext().getCallerToken();
        ScopeAccess effectiveCallerToken = requestContextHolder.getRequestContext().getSecurityContext().getEffectiveCallerToken();

        Caller caller;
        if (callerToken != null && effectiveCallerToken != null && callerToken.getAccessTokenString().equals(effectiveCallerToken.getAccessTokenString())) {
            // The caller token has the same value as the effective token so copy the effective caller
            caller = effectiveCaller;
        } else {
            String callerUserId = DATA_UNAVAILABLE;
            if (callerToken != null && callerToken instanceof BaseUserToken) {
                callerUserId = ((BaseUserToken) callerToken).getIssuedToUserId();
            }
            caller = new Caller(callerUserId, DATA_UNAVAILABLE, null);
        }
        return caller;
    }

    /**
     * Get the readily available information on the effective caller. Not all services populate the effective caller in the security
     * context so it may not be available. If not, may be able to fall back to effective token (which could contain the userid)
     * However, not all services populate the effective token either, so it may be unavailable as well.
     */
    private Caller generateEffectiveCaller() {
        ScopeAccess effectiveCallerToken = requestContextHolder.getRequestContext().getSecurityContext().getEffectiveCallerToken();
        BaseUser secContextEffectiveCaller = requestContextHolder.getRequestContext().getSecurityContext().getEffectiveCaller();

        IdentityUserTypeEnum effectiveCallerUserType = determineEffectiveCallerUserType();
        String effectiveCallerUserId = DATA_UNAVAILABLE;
        String effectiveCallerUserName = DATA_UNAVAILABLE;
        if (secContextEffectiveCaller == null) {
            if (effectiveCallerToken != null && effectiveCallerToken instanceof BaseUserToken) {
                /*
                 * If effective caller is null but effective token isn't, then the service doesn't retrieve the effective caller.
                 * Don't want logging to cause additional queries calls so ginny up a dummy user based on token data.
                 */
                effectiveCallerUserId = ((BaseUserToken) effectiveCallerToken).getIssuedToUserId();
            }
        } else {
            effectiveCallerUserId = secContextEffectiveCaller.getId();
            effectiveCallerUserName = secContextEffectiveCaller.getUsername();
        }
        return new Caller(effectiveCallerUserId, effectiveCallerUserName, effectiveCallerUserType);
    }

    /**
     * Want to provide the request uri, but a couple v2 services include the token as part of the URI which we definitely
     * do not want to log. In these cases we want to perform the same scrubbing logic used to mask caller tokens.
     *
     * @param request
     * @return
     */
    private String scrubRequestUri(ContainerRequest request) {
        URI requestUri = request.getRequestUri();

        String maskedRequestUri = "";
        if (requestUri != null) {
            String requestUriStr = requestUri.toString();
            maskedRequestUri = requestUriStr;

            //v1.1/v2 validate/revoke includes token in URL, don't want that in log so scrub out
            Matcher matcher = null;
            if (idmPathUtils.isV2ValidateTokenResource(request) || idmPathUtils.isV2RevokeOtherTokenResource(request)) {
                matcher = IdmPathUtils.v2TokenValidationPathPattern.matcher(request.getPath());
            } else if (idmPathUtils.isV11ValidateTokenResource(request) || idmPathUtils.isV11RevokeTokenResource(request)) {
                matcher = IdmPathUtils.v11TokenValidationPathPattern.matcher(request.getPath());
            }

            if (matcher != null && matcher.matches() && matcher.groupCount() == 1) {
                String rawToken = matcher.group(1);
                ScopeAccess sa = new ScopeAccess();
                sa.setAccessTokenString(rawToken);
                String maskedToken = sa.getMaskedAccessTokenString();
                maskedRequestUri = StringUtils.replace(requestUriStr, rawToken, maskedToken);
            }
        }
        return maskedRequestUri;
    }

}
