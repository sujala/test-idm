package com.rackspace.idm.aspect;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.api.filter.AuthenticationFilter;
import com.rackspace.idm.api.filter.IdentityRequestFilter;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.IdentityUserService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.event.IdentityApi;
import com.rackspace.idm.exception.ExceptionHandler;
import com.rackspace.idm.exception.ForbiddenException;
import com.sun.jersey.spi.container.ContainerRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import java.util.Arrays;
import java.util.List;

@Aspect
@Component
public class AuthorizationAdviceAspect {
    public static final String MULTI_FACTOR_PATH_PART = AuthenticationFilter.MULTI_FACTOR_PATH_PART;
    public static final String USERS_PATH_PART = AuthenticationFilter.USERS_PATH_PART;
    public static final String DOMAINS_PATH_PART = AuthenticationFilter.DOMAINS_PATH_PART;
    public static final String DEVOPS_SERVICE_PATH_PART = AuthenticationFilter.DEVOPS_SERVICE_PATH_PART;

    private final Logger logger = LoggerFactory.getLogger(AuthorizationAdviceAspect.class);

    private static List<String> allowedDelegationTokenServices = Arrays.asList(
            GlobalConstants.V2_AUTHENTICATE,
            GlobalConstants.V2_VALIDATE_TOKEN,
            GlobalConstants.V2_LIST_TOKEN_ENDPOINTS,
            GlobalConstants.V2_GET_USER_BY_ID,
            GlobalConstants.V2_LIST_EFFECTIVE_ROLES_FOR_USER,
            GlobalConstants.V2_LIST_USER_LEGACY_GROUPS
    );

    @Autowired
    private ExceptionHandler exceptionHandler;

    @Autowired
    private RequestContextHolder requestContextHolder;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    IdentityConfig identityConfig;

    @Autowired
    private IdentityUserService identityUserService;

    @Around("@annotation(identityApi) && com.rackspace.idm.aspect.IdentityPointcuts.identityApiResourceMethodV20()")
    public Object validateAnnotatedField(ProceedingJoinPoint proceedingJoinPoint, IdentityApi identityApi) throws Throwable {
        try {
            ScopeAccess scopeAccess =  requestContextHolder.getRequestContext().getSecurityContext().getEffectiveCallerToken();

            // Only perform if scope access exists. For now individual services verify a scope access is provided.
            if (scopeAccess != null) {
                validateDelegationTokenAllowed(identityApi, scopeAccess);
                if (identityConfig.getReloadableConfig().useAspectForMfaAuthorization()) {
                    validateMfaSpecialtyTokenAllowed(identityApi, scopeAccess);
                }
            }
        } catch (ForbiddenException e) {
            return exceptionHandler.exceptionResponse(e).build();
        }
        return proceedingJoinPoint.proceed();
    }

    private void validateDelegationTokenAllowed(IdentityApi identityApi, ScopeAccess scopeAccess) {
        if (identityConfig.getReloadableConfig().getAuthorizationAdviceAspectEnabled()) {
            if (scopeAccess instanceof BaseUserToken && ((BaseUserToken) scopeAccess).isDelegationToken()) {
                if (!allowedDelegationTokenServices.contains(identityApi.name())) {
                    throw new ForbiddenException(GlobalConstants.FORBIDDEN_DUE_TO_RESTRICTED_TOKEN, ErrorCodes.ERROR_CODE_FORBIDDEN_ACTION);
                }
            }
        }
    }

    /**
     *
     * @param identityApi
     * @param scopeAccess
     */
    private void validateMfaSpecialtyTokenAllowed(IdentityApi identityApi, ScopeAccess scopeAccess) {
        ContainerRequest request = requestContextHolder.getRequestContext().getContainerRequest();
        String path = request.getPath();

        /*
         MFA Setup tokens can only be used with MFA services. Session Ids can't be used as X-Auth-Tokens at all
         */
        if (path.startsWith("cloud") || path.startsWith("devops")) {
            if ((scopeAccessService.isSetupMfaScopedToken(scopeAccess) && !isMfaService(identityApi))
                    || (TokenScopeEnum.fromScope(scopeAccess.getScope()) == TokenScopeEnum.MFA_SESSION_ID)) {

                throwForbiddenErrorForScopedToken();
            }

            //authorize MFA calls - except for devops services
            if (!path.contains(DEVOPS_SERVICE_PATH_PART) && path.contains(MULTI_FACTOR_PATH_PART)) {
                authorizeMultiFactorServiceCall(path);
            }
        }
    }

    private boolean isMfaService(IdentityApi identityApi) {
        ContainerRequest request = requestContextHolder.getRequestContext().getContainerRequest();
        String path = request.getPath();

        return  path.contains(MULTI_FACTOR_PATH_PART);


    }

    private void throwForbiddenErrorForScopedToken() {
        String errMsg = "The scope of this token does not allow access to this resource";
        logger.warn(errMsg);
        throw new ForbiddenException(errMsg);
    }

    /**
     * Authorizes that the user is able to make the specified mfa call.
     *
     * There are 2 overall types of MFA calls
     * <ol>
     *     <li>Specific User - These are MFA calls made against a specific user. For example, adding an MFA phone
     *     , verifying phone, enabling MFA, etc. This does NOT include authentication </li>
     *     <li>Domain Setup - setting the domain enforcement level</li>
     * </ol>
     *
     * For the type 1 calls the user against which the call is being made must have access to MFA, but the caller of the
     * service does not. By "access" this means while in "Beta", the target user must have the beta role. When in "general"
     * availability, all users will have access.
     *
     * <p>
     * Additionally, "MFA Scoped Tokens" are only allowed to make MFA calls that are:
     * <ol>
     *     <li>Type 1 calls (directly on a user)</li>
     *     <li>The user associated with the provided token must match the user being acted upon. Impersonation can not be used
     *     to get a mfa scoped token.</li>
     *     <li>The user must not have multi-factor enabled</li>
     * </ol>
     * If any of those conditions are NOT met, then a ForbiddenException is thrown.
     * </p>
     *
     * @param requestPath The path of the REST request
     */
    private void authorizeMultiFactorServiceCall(String requestPath) {
        ScopeAccess effectiveToken = requestContextHolder.getRequestContext().getSecurityContext().getEffectiveCallerToken();

        if (isDomainMfaTargetCall(requestPath)) {
            if (scopeAccessService.isSetupMfaScopedToken(effectiveToken)) {
                //setupmfa token only allows mfa calls with {userid} in path
                throwForbiddenErrorForScopedToken();
            }
        } else if (isUserMfaTargetCall(requestPath)) {
            //it's a user specific service call
            String userIdFromPath = parseUserIdFromPath(requestPath);
            EndUser endUser = setRequestContextTargetUser(userIdFromPath);
            if (scopeAccessService.isSetupMfaScopedToken(effectiveToken)) {
                if (endUser == null || !(endUser instanceof User)) {
                    //only applicable against existing provisioned users
                    throwForbiddenErrorForScopedToken();
                }

                if (!(effectiveToken instanceof UserScopeAccess)) {
                    //really an invalid state
                    throwForbiddenErrorForScopedToken();
                }

                User user = (User)endUser;
                UserScopeAccess userScopeAccess = (UserScopeAccess)effectiveToken;

                if (user.isMultiFactorEnabled() || !user.getId().equalsIgnoreCase(userScopeAccess.getUserRsId())) {
                    throwForbiddenErrorForScopedToken();
                }
            }

        } else {
            //throw exception because it's a mfa call we don't account for
            throw new WebApplicationException(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private boolean isUserMfaTargetCall(String path) {
        if (path != null) {
            List<String> tokens = Arrays.asList(path.split("/"));
            int indexUsers = tokens.indexOf(USERS_PATH_PART);
            int indexMfa = tokens.indexOf(MULTI_FACTOR_PATH_PART);
            //similar to .../users/{userid}/RAX-AUTH/multi-factor
            if (indexUsers >= 0 && indexMfa == (indexUsers + 3)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDomainMfaTargetCall(String path) {
        if (path != null) {
            List<String> tokens = Arrays.asList(path.split("/"));
            int indexUsers = tokens.indexOf(DOMAINS_PATH_PART);
            int indexMfa = tokens.indexOf(MULTI_FACTOR_PATH_PART);
            //similar to .../domains/{domainId}/multi-factor
            if (indexUsers >= 0 && indexMfa == (indexUsers + 2)) {
                return true;
            }
        }
        return false;
    }

    private EndUser setRequestContextTargetUser(String userId) {
        EndUser targetUser = null;
        if (userId != null) {
            targetUser = identityUserService.getEndUserById(userId);
        }
        requestContextHolder.getRequestContext().setTargetEndUser(targetUser);
        return targetUser;
    }

    private String parseUserIdFromPath(String path) {
        if (path != null) {
            List<String> tokens = Arrays.asList(path.split("/"));
            int index = tokens.indexOf(USERS_PATH_PART);
            if (index >= 0) {
                if (index + 1 < tokens.size()) {
                    return tokens.get(index + 1);
                }
            }
        }
        return null;
    }
}