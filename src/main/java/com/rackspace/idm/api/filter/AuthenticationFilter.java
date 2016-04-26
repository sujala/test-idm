package com.rackspace.idm.api.filter;

import com.rackspace.idm.api.resource.cloud.v20.MultiFactorCloud20Service;
import com.rackspace.idm.api.security.RequestContextHolder;
import com.rackspace.idm.api.security.SecurityContext;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.ForbiddenException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * @author john.eo Apply token-based authentication to all calls except the
 *         several that are checked for in the if clauses below.
 */
@Component
public class AuthenticationFilter implements ContainerRequestFilter {

    private static final String GET = "GET";
    private static final String POST = "POST";
    public static final String MULTI_FACTOR_PATH_PART = "multi-factor";
    public static final String USERS_PATH_PART = "users";
    public static final String DOMAINS_PATH_PART = "domains";
    public static final String DEVOPS_SERVICE_PATH_PART = "devops";

    /**
     * Pattern to recognize validate call against AE or UUID tokens
     */
    private static Pattern tokenValidationPathPattern = Pattern.compile("^cloud/v2.0/tokens/[\\w-_=]+$");
    private static Pattern tokenEndpointPathPattern = Pattern.compile("^cloud/v2.0/tokens/[\\w-_=]+/endpoints/?$");

    private final AuthHeaderHelper authHeaderHelper = new AuthHeaderHelper();
    private final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Context
    private HttpServletRequest req;

    @Autowired
    private MultiFactorCloud20Service multiFactorCloud20Service;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private IdentityUserService identityUserService;

    @Autowired
    private UserService userService;

    @Autowired
    private Configuration config;

    @Autowired
    private AuthorizationService authorizationService;

    @Autowired
    private RequestContextHolder requestContextHolder;

    AuthenticationFilter() {
    }

    AuthenticationFilter(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }


    //TODO: Refactor this method. It's way too complex
    @Override
    public ContainerRequest filter(ContainerRequest request) {
        //immediately replace the security context. Will be populated as appropriate later on
        final SecurityContext securityContext = new SecurityContext();
        requestContextHolder.getRequestContext().setSecurityContext(securityContext);

        String path = request.getPath();
        final String method = request.getMethod();

        if (req != null) {
            MDC.put(Audit.REMOTE_IP, req.getRemoteAddr());
            MDC.put(Audit.HOST_IP, req.getLocalAddr());
            MDC.put(Audit.PATH, path);
            MDC.put(Audit.GUUID, UUID.randomUUID().toString());
            String xForwardedFor = req.getHeader("X-Forwarded-For");
            if(StringUtils.isNotBlank(xForwardedFor)){
                MDC.put(Audit.X_FORWARDED_FOR, xForwardedFor);
            }else {
                MDC.put(Audit.X_FORWARDED_FOR, req.getRemoteAddr());
            }
        }

        // skip token authentication for any url that ends with public.
        // convention for public documentation is /*/*/public
        // also if path is /cloud we want to ensure we show the splash page
        // TODO: double check that this is an efficient check and will not cause collisions
        if (path.endsWith("public") || path.equals("cloud")) {
            return request;
        }

        //Cloud v1.0/v1.1/v2.0 checks
        if (path.startsWith("cloud") || path.startsWith("devops")) {
            // Return if call is authentication or validation
            if (tokenValidationPathPattern.matcher(path).matches() && (!tokenEndpointPathPattern.matcher(path).matches())) {
                //validate call. Don't replace impersonation token
                populateSecurityContextForValidateCall(request, securityContext);
                return request;
            } else if (path.startsWith("cloud/v2.0/tokens") && (!tokenEndpointPathPattern.matcher(path).matches())) {
                //auth call
                return request;
            }

            final String authToken = request.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER);

            /*
             We only do checks here when authToken is provided and results in a found auth token (UUID exists, AE can be
             decrypted).

             Underlying service implementations are expected to throw exception if no auth token is provided (or found given provided token string)
             and it's required.

             //TODO: Move the no token error check up to this filter after verifying that all services except authenticate require an auth token to be provided
             */
            if (!StringUtils.isEmpty(authToken)) {
                ScopeAccess callerToken = scopeAccessService.getScopeAccessByAccessToken(authToken); //throws NotFoundException token string is empty/null
                ScopeAccess effectiveToken = callerToken; //assume effective token will be same as caller.

                //PWD-RESET tokens are only allowed to be used for the password reset call
                if (callerToken != null && TokenScopeEnum.PWD_RESET.getScope().equals(callerToken.getScope()) && !path.startsWith("cloud/v2.0/users/RAX-AUTH/pwd-reset")) {
                    throw new ForbiddenException("Not Authorized");
                }

                //check for impersonation
                if(callerToken instanceof ImpersonatedScopeAccess){
                    // Check Expiration of impersonated token
                    if (callerToken.isAccessTokenExpired(new DateTime())) {
                        throw new NotAuthorizedException("No valid token provided. Please use the 'X-Auth-Token' header with a valid token.");
                    }

                    // Swap token out and Log
                    final ImpersonatedScopeAccess tk = (ImpersonatedScopeAccess) callerToken;
                    final String impersonatedTokenStr = tk.getImpersonatingToken();
                    request.getRequestHeaders().putSingle(AuthenticationService.AUTH_TOKEN_HEADER.toLowerCase(), impersonatedTokenStr);
                    logger.info("Impersonating token {} with token {} ", authToken, impersonatedTokenStr);

                    /*
                    replace effective token with the token being impersonated.
                     */
                    effectiveToken = scopeAccessService.getScopeAccessByAccessToken(impersonatedTokenStr); //
                }

                //set the tokens in the security context
                securityContext.setCallerToken(callerToken);
                securityContext.setEffectiveCallerToken(effectiveToken);

                if (effectiveToken != null && ((scopeAccessService.isSetupMfaScopedToken(effectiveToken) &&
                        !path.contains(MULTI_FACTOR_PATH_PART)) || TokenScopeEnum.fromScope(effectiveToken.getScope()) == TokenScopeEnum.MFA_SESSION_ID)) {
                    //mfa session scope tokens can only be used to auth w/ passcode. SETUP-MFA, only to set up MFA
                    throwForbiddenErrorForScopedToken();
                }

                //authorize MFA calls - except for devops services
                if(path.contains(MULTI_FACTOR_PATH_PART)) {
                    authorizeMultiFactorServiceCall(path);
                }
            }

            return request;
        }

        if( (path.startsWith("v1") || path.startsWith("v1.0")) && !isFoundationEnabled()){
            String errMsg = String.format("Resource Not Found");
            logger.warn(errMsg);
            throw new NotFoundException(errMsg);
        }

        // Skip authentication for the following calls
        int index = path.indexOf('/');
        path = index > 0 ? path.substring(index + 1) : ""; //TODO: "/asdf/afafw/fwa" -> "" is correct behavior?

        if (GET.equals(method) && "application.wadl".equals(path)) {
            return request;
        }

        if (GET.equals(method) && "idm.wadl".equals(path)) {
            return request;
        }

        if (GET.equals(method) && path.startsWith("xsd")) {
            return request;
        }

        if (GET.equals(method) && path.startsWith("xslt")) {
            return request;
        }

        if (GET.equals(method) && "".equals(path)) {
            return request;
        }

        if (POST.equals(method) && "tokens".equals(path)) {
            return request;
        }

        final String authHeader = request.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER);
        if (StringUtils.isBlank(authHeader)) {
            throw new NotAuthenticatedException("The request for the resource must include the Authorization header.");
        }

        final String tokenString = authHeaderHelper.getTokenFromAuthHeader(authHeader);
        final boolean authResult = scopeAccessService.authenticateAccessToken(tokenString);

        if (authResult) {
            // Authenticated
            return request;
        }

        // authentication failed if we reach this point
        logger.warn("Authentication Failed for {} ", authHeader);
        throw new NotAuthenticatedException("Authentication Failed.");

    }

    /**
     * Populate the security context for a validate call. This must NOT replace the x-auth-token header if the caller
     * is an impersonator.
     *
     * @param request
     * @param securityContext
     */
    private void populateSecurityContextForValidateCall(ContainerRequest request, SecurityContext securityContext) {
        final String authToken = request.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER);
            /*
             We only do checks here when authToken is provided. Underlying service implementations are expected to throw
             exception if no auth token is provided and it's required.
             //TODO: Move the no token error check up to this filter after verifying that all services except authenticate require an auth token to be provided
             */
        if (authToken != null) {
            ScopeAccess callerToken = scopeAccessService.getScopeAccessByAccessToken(authToken);

            //set the tokens in the security context
            securityContext.setCallerToken(callerToken);
            securityContext.setEffectiveCallerToken(callerToken);
        }
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

        if (requestPath.contains(DEVOPS_SERVICE_PATH_PART)) {
            return;
        } else if (isDomainMfaTargetCall(requestPath)) {
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

    public void setHttpServletRequest(HttpServletRequest httpServletRequest) {
        this.req = httpServletRequest;
    }

    public void setConfiguration(Configuration config){
        this.config = config;
    }

    public void setRequestContextHolder(RequestContextHolder requestContextHolder) {
        this.requestContextHolder = requestContextHolder;
    }

    private boolean isFoundationEnabled(){
        return config.getBoolean("feature.access.to.foundation.api", true);
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

    private void throwForbiddenErrorForScopedToken() {
        String errMsg = "The scope of this token does not allow access to this resource";
        logger.warn(errMsg);
        throw new ForbiddenException(errMsg);
    }
}
