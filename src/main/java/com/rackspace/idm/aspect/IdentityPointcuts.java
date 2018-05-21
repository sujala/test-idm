package com.rackspace.idm.aspect;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class IdentityPointcuts {
    @Pointcut("execution(public * *(..))")
    private void anyPublicOperation() {
    }

    /**
     * Pointcut on all method calls on a Spring bean name ending in "Resource" - the IDM standard for classes that define
     * API endpoints.
     */
    @Pointcut("anyPublicOperation() && bean(*Resource)")
    private void webResourceMethod() {
    }

    /**
     * Limits to Identity API calls identified by annotation
     */
    @Pointcut("webResourceMethod() && @annotation(com.rackspace.idm.event.IdentityApi)")
    public void identityApiResourceMethod() { }

    /**
     * Limits to Identity API calls in v20
     */
    @Pointcut("identityApiResourceMethod() && (execution(* com.rackspace.idm.api.resource.cloud.v20..*.*(..)) || execution(* com.rackspace.idm.modules.*.api.resource..*.*(..)))")
    public void identityApiResourceMethodV20() {
    }
}
