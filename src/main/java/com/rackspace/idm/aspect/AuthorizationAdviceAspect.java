package com.rackspace.idm.aspect;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.BaseUserToken;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.event.IdentityApi;
import com.rackspace.idm.exception.ExceptionHandler;
import com.rackspace.idm.exception.ForbiddenException;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Aspect
@Component
public class AuthorizationAdviceAspect {

    private static List<String> allowedDelegationTokenServices = Arrays.asList(
            GlobalConstants.V2_AUTHENTICATE,
            GlobalConstants.V2_VALIDATE_TOKEN,
            GlobalConstants.V2_LIST_TOKEN_ENDPOINTS,
            GlobalConstants.V2_GET_USER_BY_ID,
            GlobalConstants.V2_LIST_EFFECTIVE_ROLES_FOR_USER
    );

    @Autowired
    private ExceptionHandler exceptionHandler;

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Autowired
    IdentityConfig identityConfig;

    @Around("@annotation(identityApi) && com.rackspace.idm.aspect.IdentityPointcuts.identityApiResourceMethodV20()")
    public Object validateAnnotatedField(ProceedingJoinPoint proceedingJoinPoint, IdentityApi identityApi) throws Throwable {
        try {
            validateDelegationTokenAllowed(identityApi);
        } catch (ForbiddenException e) {
            return exceptionHandler.exceptionResponse(e).build();
        }
        return proceedingJoinPoint.proceed();
    }

    private void validateDelegationTokenAllowed(IdentityApi identityApi) {
        if (identityConfig.getReloadableConfig().getAuthorizationAdviceAspectEnabled()) {
            ScopeAccess scopeAccess = requestContextHolder.getRequestContext().getSecurityContext().getEffectiveCallerToken();

            if (scopeAccess instanceof BaseUserToken && ((BaseUserToken) scopeAccess).isDelegationToken()) {
                if (!allowedDelegationTokenServices.contains(identityApi.name())) {
                    throw new ForbiddenException(GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION);
                }
            }
        }
    }
}
