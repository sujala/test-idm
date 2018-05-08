package com.rackspace.idm.event;

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
}
