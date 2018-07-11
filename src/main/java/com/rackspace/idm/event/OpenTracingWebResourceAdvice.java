package com.rackspace.idm.event;

import com.rackspace.idm.api.security.RequestContext;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.aspect.IdentityPointcuts;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.config.OpenTracingConfiguration;
import com.rackspace.idm.util.HttpHeaderExtractor;
import com.sun.jersey.spi.container.ContainerRequest;
import io.opentracing.Scope;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import io.opentracing.tag.Tags;
import org.apache.commons.collections4.CollectionUtils;
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

import javax.ws.rs.core.Response;
import java.lang.reflect.Method;
import java.util.Set;

@Aspect
@Component
public class OpenTracingWebResourceAdvice {
    private final Logger logger = LoggerFactory.getLogger(IdentityPointcuts.class);

    private static final String WILDCARD = "*";

    @Autowired
    private IdentityConfig identityConfig;

    @Autowired
    RequestContextHolder requestContextHolder;

    @Autowired
    private OpenTracingConfiguration openTracingConfiguration;

    @Autowired
    protected ApplicationEventPublisher applicationEventPublisher;

    @Around("com.rackspace.idm.aspect.IdentityPointcuts.identityApiResourceMethod()")
    @Order(200)
    public Object traceResource(ProceedingJoinPoint joinPoint) throws Throwable {
        boolean tracingIsEnabled = identityConfig.getReloadableConfig().isOpenTracingForWebResourcesFeatureEnabled() &&
                identityConfig.getStaticConfig().getOpenTracingEnabledFlag();

        Scope activeScope = null;
        if (tracingIsEnabled) {
            try {
                activeScope = establishSpanIfNecessary(joinPoint);
            } catch (Exception e) {
                logger.warn("Attempting to establish tracing, but encountered error. Eating exception.", e);
            }
        }

        try {
            Object response = joinPoint.proceed();
            if (activeScope != null) {
                closeScope(activeScope, response);
            }
            return response;
        } catch (Throwable ex) {
            // TODO will test for active scope non-null once integrate with Dimitry's stuff, but right now want to get all the logs
            if (activeScope != null) {
                closeScopeWithException(activeScope, ex);
            }
            // Rethrow exception
            throw ex;
        }
    }

    private Scope establishSpanIfNecessary(ProceedingJoinPoint joinPoint) {
        Scope activeScope = null;
        IdentityApiResourceRequest apiRequest = null;
        try {
            // Could throw IllegalArgument/State exceptions
            apiRequest = getIdentityApiResourceForJoinPoint(joinPoint);
        } catch (Exception e) {
            logger.warn("Error retrieving api resource for join point", e);
        }

        if (apiRequest != null) {
            String name = apiRequest.getIdentityApiAnnotation().name();
            if (shouldTraceResource(name)) {
                activeScope = buildSpan(name, apiRequest);
            } else {
                logger.debug(String.format("Resource with name '%s' on method '%s' is excluded from tracing.", name, apiRequest.getApiMethod().getName()));
            }
        }
        return activeScope;
    }

    private Scope buildSpan(String resourceName, IdentityApiResourceRequest apiRequest) {
        logger.debug(String.format("Building opentrace scope for resource '%s'", resourceName));
        SpanContext context = openTracingConfiguration.getGlobalTracer().extract(
                Format.Builtin.HTTP_HEADERS, new HttpHeaderExtractor(apiRequest.getContainerRequest()));

        Tracer.SpanBuilder spanBuilder;

        if (context == null)
            spanBuilder = openTracingConfiguration.getGlobalTracer()
                    .buildSpan(resourceName)
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);
        else
            spanBuilder = openTracingConfiguration.getGlobalTracer()
                    .buildSpan(resourceName)
                    .asChildOf(context)
                    .withTag(Tags.SPAN_KIND.getKey(), Tags.SPAN_KIND_CLIENT);

        Scope activeScope = spanBuilder.startActive(true);
        return activeScope;
    }

    private void closeScope(Scope activeScope, Object resourceResponse) {
        try {
            if (resourceResponse instanceof Response) {
                Response response = (Response) resourceResponse;
                logger.debug(String.format("Closing opentrace scope w/ response code %s.", response.getStatus()));
                activeScope.span().setTag(Tags.HTTP_STATUS.getKey(), response.getStatus());
                activeScope.close();
            } else {
                logger.warn(String.format("Unexpected response type '%s' from web resource. Closing opentrace scope.", resourceResponse.getClass().getSimpleName()));
                activeScope.close();
            }
        } catch (Exception e) {
            logger.warn("Attempting to close open tracing scope caused error. Eating exception.", e);
        }
    }

    private void closeScopeWithException(Scope activeScope, Throwable ex) {
        try {
            logger.debug("Closing opentrace scope with exception.", ex);
            activeScope.span().setTag(Tags.ERROR.getKey(), true);
            activeScope.close();
        } catch (Exception e) {
            logger.warn("Attempting to close open tracing scope caused error. Eating exception.", e);
        }
    }

    private IdentityApiResourceRequest getIdentityApiResourceForJoinPoint(JoinPoint joinPoint) {
        Method method = null;
        Signature sig = joinPoint.getSignature();
        if (sig instanceof MethodSignature) {
            MethodSignature methodSignature = (MethodSignature) sig;
            method = methodSignature.getMethod();
        }

        if (method == null) {
            throw new IllegalArgumentException(String.format("Must provide method to start opentracing context"));
        }

        IdentityApi identityApi = method.getAnnotation(IdentityApi.class);
        if (identityApi == null) {
            throw new IllegalArgumentException(String.format("Method must provide API annotation to set open tracing context. Error method '%s'", method.toString()));
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

        return new IdentityApiResourceRequest(method, request);
    }

    public boolean shouldTraceResource(String apiResourceName) {
        if (StringUtils.isBlank(apiResourceName)) {
            return false;
        }

        Set<String> excludedResourceNames = identityConfig.getReloadableConfig().getOpenTracingExcludedWebResources();
        Set<String> includedResourceNames = identityConfig.getReloadableConfig().getOpenTracingIncludedWebResources();

        return inListWithWildcardSupport(includedResourceNames, apiResourceName) && !inListWithWildcardSupport(excludedResourceNames, apiResourceName);
    }

    private boolean inListWithWildcardSupport(Set<String> list, String tofind) {
        boolean found = false;
        if (CollectionUtils.isEmpty(list)) {
            found = false;
        } else if (list.contains(WILDCARD)) {
            found = true;
        } else {
            for (String attributeInList : list) {
                if (attributeInList.equalsIgnoreCase(tofind)) {
                    found = true;
                    break;
                }
            }
        }
        return found;
    }
}