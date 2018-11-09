package com.rackspace.idm.event;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.security.AuthorizationContext;
import com.rackspace.idm.api.security.RequestContext;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.BaseUserToken;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.RackerAuthenticationService;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import com.sun.jersey.spi.container.ContainerRequest;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class ApiEventPostingAdvice {
    private final Logger logger = LoggerFactory.getLogger(ApiEventPostingAdvice.class);

    public static final String DATA_UNAVAILABLE = "<NotAvailable>";

    @Autowired
    RequestContextHolder requestContextHolder;

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    protected ApplicationEventPublisher applicationEventPublisher;

    @After("com.rackspace.idm.aspect.IdentityPointcuts.identityApiResourceMethod()")
    public void postEvent(JoinPoint joinPoint) {
        postApiEvent(joinPoint);
    }

    /**
     * Resource methods should _never_ allow exceptions to be bubbled, but sometimes they are.
     *
     * @param joinPoint
     * @param ex
     */
    @AfterThrowing(pointcut = "com.rackspace.idm.aspect.IdentityPointcuts.identityApiResourceMethod()", throwing = "ex")
    public void postEventWithException(JoinPoint joinPoint, Throwable ex) {
        postApiEvent(joinPoint);
    }

    /**
     * When posting an event should assume that the event will be processed in a different thread asynchronously to be
     * on the safe side. Therefore, all information included in the event must be thread safe.
     * <p>
     * Events must not include full tokens, though may contain PII information that would need to be scrubbed before
     * exposing. The exact requirements for PII handling lies in how that information is used from the event.
     */
    private void postApiEvent(JoinPoint joinPoint) {
        try {
            // Short circuit via flag
            if (!identityConfig.getReloadableConfig().isFeatureSendNewRelicCustomDataEnabled()) {
                return;
            }
            Signature sig = joinPoint.getSignature();
            Method method = null;
            if (sig instanceof MethodSignature) {
                MethodSignature methodSignature = (MethodSignature) sig;
                method = methodSignature.getMethod();
            }
            if (method == null) {
                throw new IllegalArgumentException(String.format("Must provide method to send API event"));
            }

            IdentityApi identityApi = method.getAnnotation(IdentityApi.class);
            if (identityApi == null) {
                throw new IllegalArgumentException(String.format("Method must provide API annotation to send API event. Error method '%s'", method.toString()));
            }

            RequestContext requestContext = requestContextHolder.getRequestContext();
            if (requestContext == null) {
                throw new IllegalArgumentException(String.format("Request context must be set to post API events. Invalid method %s", method.toString()));
            }

            // Must have ContainerRequest
            ContainerRequest request = requestContextHolder.getRequestContext().getContainerRequest();
            if (request == null) {
                throw new IllegalArgumentException(String.format("Resource context must be set on request context to post API events. Invalid method %s", method.toString()));
            }

            IdentityApiResourceRequest resourceContext = new IdentityApiResourceRequest(method, request);

            ApiResourceType eventType = identityApi.apiResourceType();
            ApiEvent event = null;
            switch (eventType) {
                case AUTH:
                    event = createAuthEvent(resourceContext);
                    break;
                case PRIVATE:
                    event = createPrivateEvent(resourceContext);
                    break;
                case PUBLIC:
                    event = createPublicEvent(resourceContext);
                    break;
                default:
                    logger.warn(String.format("Error posting API event. Unsupported event type '%s'", eventType.getReportValue()));
            }

            if (event != null) {
                applicationEventPublisher.publishEvent(new ApiEventSpringWrapper(this, event));
            }
        } catch (Exception e) {
            logger.warn("Error posting API Event from advice. Swallowing error", e);
        }
    }

    private ApiEvent createAuthEvent(IdentityApiResourceRequest resourceContext) {
        String requestId = MDC.get(Audit.GUUID);

        return BasicAuthApiEvent.BasicAuthApiEventBuilder.aBasicAuthApiEvent()
                .requestId(StringUtils.defaultString(requestId, DATA_UNAVAILABLE))
                .remoteIp(StringUtils.defaultString(MDC.get(Audit.REMOTE_IP), DATA_UNAVAILABLE))
                .forwardedForIp(StringUtils.defaultString(MDC.get(Audit.X_FORWARDED_FOR), DATA_UNAVAILABLE))
                .nodeName(StringUtils.defaultString(identityConfig.getReloadableConfig().getAENodeNameForSignoff(), DATA_UNAVAILABLE))
                .userName(StringUtils.defaultString(requestContextHolder.getAuthenticationContext().getUsername(), DATA_UNAVAILABLE))
                .resourceContext(resourceContext)
                .build();
    }

    private ApiEvent createPublicEvent(IdentityApiResourceRequest resourceContext) {
        String requestId = MDC.get(Audit.GUUID);

        return BasicPublicApiEvent.BasicPublicApiEventBuilder.aBasicPublicApiEvent()
                .requestId(StringUtils.defaultString(requestId, DATA_UNAVAILABLE))
                .remoteIp(StringUtils.defaultString(MDC.get(Audit.REMOTE_IP), DATA_UNAVAILABLE))
                .forwardedForIp(StringUtils.defaultString(MDC.get(Audit.X_FORWARDED_FOR), DATA_UNAVAILABLE))
                .nodeName(StringUtils.defaultString(identityConfig.getReloadableConfig().getAENodeNameForSignoff(), DATA_UNAVAILABLE))
                .resourceContext(resourceContext)
                .build();
    }

    /**
     * Currently only support providing details for protected resources that have the request context populated w/
     * the appropriate security context. This is limited to v2.0 resources AFAIK at the moment.
     *
     * @return
     */
    private ApiEvent createPrivateEvent(IdentityApiResourceRequest resourceContext) {
        ContainerRequest request = resourceContext.getContainerRequest();

        String requestId = MDC.get(Audit.GUUID);

        Caller effectiveCaller = generateEffectiveCaller();
        Caller caller = generateCaller(effectiveCaller);
        String callerToken = getCallerToken(request);
        String effectiveCallerToken = getEffectiveCallerToken();

        return BasicPrivateApiEvent.BasicPrivateApiEventBuilder.aBasicPrivateApiEvent()
                .requestId(StringUtils.defaultString(requestId, DATA_UNAVAILABLE))
                .remoteIp(StringUtils.defaultString(MDC.get(Audit.REMOTE_IP), DATA_UNAVAILABLE))
                .forwardedForIp(StringUtils.defaultString(MDC.get(Audit.X_FORWARDED_FOR), DATA_UNAVAILABLE))
                .nodeName(StringUtils.defaultString(identityConfig.getReloadableConfig().getAENodeNameForSignoff(), DATA_UNAVAILABLE))
                .callerToken(StringUtils.defaultString(callerToken, DATA_UNAVAILABLE))
                .caller(caller)
                .effectiveCallerToken(StringUtils.defaultString(effectiveCallerToken, DATA_UNAVAILABLE))
                .effectiveCaller(effectiveCaller)
                .resourceContext(resourceContext)
                .build();
    }

    private String getEffectiveCallerToken() {
        ScopeAccess effectiveCallerToken = requestContextHolder.getRequestContext().getSecurityContext().getEffectiveCallerToken();
        String tokenStr = effectiveCallerToken != null ? effectiveCallerToken.getAccessTokenString() : DATA_UNAVAILABLE;

        if (StringUtils.isBlank(tokenStr)) {
            tokenStr = DATA_UNAVAILABLE;
        }

        return tokenStr;
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
    private String getCallerToken(ContainerRequest request) {
        ScopeAccess callerToken = requestContextHolder.getRequestContext().getSecurityContext().getCallerToken();

        String tokenStr = null;
        if (callerToken == null) {
            // Retrieve v2.0 auth header (which is how security context gets populated in v2.0 requests. This will allow us to provide token
            // even if can't be decrypted (e.g. invalid token)
            String headerToken = request.getHeaderValue(GlobalConstants.X_AUTH_TOKEN);
            if (headerToken != null) {
                ScopeAccess sa = new ScopeAccess();
                sa.setAccessTokenString(headerToken);
                tokenStr = sa.getAccessTokenString();
            }
        } else {
            tokenStr = callerToken.getAccessTokenString();
        }

        if (StringUtils.isBlank(tokenStr)) {
            tokenStr = DATA_UNAVAILABLE;
        }

        return tokenStr;
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

}
