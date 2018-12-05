package com.rackspace.idm.event;

import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.aspect.IdentityPointcuts;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.config.OpenTracingConfiguration;
import io.opentracing.Scope;
import io.opentracing.tag.Tags;
import org.apache.commons.lang.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class OpenTracingDaoAdvice {
    private final Logger logger = LoggerFactory.getLogger(IdentityPointcuts.class);

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    RequestContextHolder requestContextHolder;

    @Autowired
    private OpenTracingConfiguration openTracingConfiguration;

    @Autowired
    protected ApplicationEventPublisher applicationEventPublisher;

    @Around("com.rackspace.idm.aspect.IdentityPointcuts.daoResource()")
    @Order(200)
    public Object traceResource(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean tracingIsEnabled = identityConfig.getReloadableConfig().isOpenTracingForDaoResourcesFeatureEnabled() &&
                identityConfig.getStaticConfig().getOpenTracingEnabledFlag();

        Scope activeScope = null;
        if (tracingIsEnabled) {
            try {
                activeScope = buildChildSpanAsNecessary(joinPoint);
            } catch (Exception e) {
                logger.warn("Attempting to establish tracing, but encountered error. Eating exception.", e);
            }
        }

        try {
            Object response = joinPoint.proceed();
            if (activeScope != null) {
                closeScope(activeScope);
            }
            return response;
        } catch (Throwable ex) {
            if (activeScope != null) {
                closeScopeWithException(activeScope, ex);
            }
            // Rethrow exception
            throw ex;
        }
    }

    /**
     * Builds a child span on the current active scope if one exists. Else, does nothing.
     * @param joinPoint
     * @return
     */
    private Scope buildChildSpanAsNecessary(ProceedingJoinPoint joinPoint) {
        // check that the span didn't start yet
        Scope activeScope = getActiveScope();

        if (activeScope == null) {
            logger.debug(String.format("Dao resource on joinpoint '%s' is being excluded from tracing due to no active scope.", joinPoint.getSourceLocation()));
            return null;
        }

        Method daoRequest = null;
        try {
            // Could throw IllegalArgument/State exceptions
            daoRequest = getMethodForJoinPoint(joinPoint);
        } catch (Exception e) {
            logger.warn(String.format("Error retrieving dao method for join point %s", joinPoint.getSourceLocation().toString()), e);
            return null;
        }

        String name = daoRequest.getName();
        if (StringUtils.isBlank(name)) {
            logger.debug(String.format("Dao resource on joinpoint '%s' is being excluded from tracing due to no method name.", joinPoint.getSourceLocation()));
            return null;
        }

        Scope childScope = openTracingConfiguration.getGlobalTracer().buildSpan(
                name)
                .asChildOf(activeScope.span())
                .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT)
                .withTag(Tags.COMPONENT.getKey(), "LDAP")
                .startActive(true);

        return childScope;
    }

    private void closeScope(Scope activeScope) {
        try {
            activeScope.close();
        } catch (Exception e) {
            logger.warn("Attempting to close open tracing scope caused error. Eating exception.", e);
        }
    }

    private void closeScopeWithException(Scope activeScope, Throwable ex) {
        logger.debug("Closing opentrace scope with exception.", ex);
        try {
            activeScope.span().setTag(Tags.ERROR.getKey(), true);
            activeScope.close();
        } catch (Exception e) {
            logger.warn("Attempting to close open tracing scope caused error. Eating exception.", e);
        }
    }

    private Method getMethodForJoinPoint(JoinPoint joinPoint) {
        Method method = null;
        Signature sig = joinPoint.getSignature();
        if (sig instanceof MethodSignature) {
            MethodSignature methodSignature = (MethodSignature) sig;
            method = methodSignature.getMethod();
        }

        return method;
    }

    private Scope getActiveScope() {
        try {
            return openTracingConfiguration.getGlobalTracer().scopeManager().active();
        } catch (Exception e) {
            logger.debug("Unable to get opentracing active scope for dao advice. Returning null.");
        }
        return null;
    }
}