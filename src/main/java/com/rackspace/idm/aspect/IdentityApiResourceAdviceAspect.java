package com.rackspace.idm.aspect;

import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.event.IdentityApi;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Sets the identityApi annotation into the request context to allow downstream code to know which method is being
 * called.
 */
@Aspect
@Component
@Order(5)
public class IdentityApiResourceAdviceAspect {

    @Autowired
    IdentityConfig identityConfig;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Before("@annotation(identityApi) && com.rackspace.idm.aspect.IdentityPointcuts.identityApiResourceMethod()")
    public void validateAnnotatedField(JoinPoint joinPoint, IdentityApi identityApi) {
        requestContextHolder.getRequestContext().setIdentityApi(identityApi);
    }
}
