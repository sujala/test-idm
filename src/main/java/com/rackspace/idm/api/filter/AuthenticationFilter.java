package com.rackspace.idm.api.filter;

import com.rackspace.idm.api.resource.cloud.v20.MultiFactorCloud20Service;
import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.BaseUser;
import com.rackspace.idm.domain.entity.ImpersonatedScopeAccess;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthenticationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotAuthorizedException;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
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
import java.util.UUID;

/**
 * @author john.eo Apply token-based authentication to all calls except the
 *         several that are checked for in the if clauses below.
 */
@Component
public class AuthenticationFilter implements ContainerRequestFilter {
    private static final String GET = "GET";
    private static final String POST = "POST";
    private final AuthHeaderHelper authHeaderHelper = new AuthHeaderHelper();
    private final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    @Context
    private HttpServletRequest req;

    @Autowired
    private MultiFactorCloud20Service multiFactorCloud20Service;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private UserService userService;

    AuthenticationFilter() {

    }

    AuthenticationFilter(ScopeAccessService scopeAccessService) {
        this.scopeAccessService = scopeAccessService;
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {
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

        // Skip token authentication for cloud resources, but check for impersonation
        if (path.startsWith("cloud")) {

            // Return if call is authentication or validation
            if(path.startsWith("cloud/v2.0/tokens") && !path.contains("endpoints")) {
                return request;
            }

            final String authToken = request.getHeaderValue(AuthenticationService.AUTH_TOKEN_HEADER);
            if (authToken != null) {
                //check for impersonation
                ScopeAccess sa = scopeAccessService.getScopeAccessByAccessToken(authToken);
                if(sa instanceof ImpersonatedScopeAccess){
                    // Check Expiration of impersonated token
                    if (sa.isAccessTokenExpired(new DateTime())) {
                        throw new NotAuthorizedException("No valid token provided. Please use the 'X-Auth-Token' header with a valid token.");
                    }
                    // Swap token out and Log
                    String newToken = ((ImpersonatedScopeAccess) sa).getImpersonatingToken();
                    request.getRequestHeaders().putSingle(AuthenticationService.AUTH_TOKEN_HEADER.toLowerCase(), newToken);
                    logger.info("Impersonating token {} with token {} ", authToken, newToken);
                }

                if(path.contains("multi-factor")) {

                    //We first need to check if multifactor services are enabled. If they are disabled, we allow
                    //the request to pass through in order for the application server to return a 404.
                    //This is possible because the multifactor resource is not exposed when mfa is turned off.
                    if(!multiFactorCloud20Service.isMultiFactorGloballyEnabled()) {
                        //we need to check isMultiFactorEnabled here as well because the above call
                        //only checks for the case where mfa = true and beta = false. It could still be
                        //the case that mfa = true and beta = true.
                        if(multiFactorCloud20Service.isMultiFactorEnabled()) {
                            BaseUser user;
                            if(sa instanceof ImpersonatedScopeAccess) {
                                ScopeAccess impersonatedScopeAccess = scopeAccessService.getScopeAccessByAccessToken(((ImpersonatedScopeAccess) sa).getImpersonatingToken());
                                user = userService.getUserByScopeAccess(impersonatedScopeAccess);
                            } else {
                                user = userService.getUserByScopeAccess(sa);
                            }
                            if(!multiFactorCloud20Service.isMultiFactorEnabledForUser(user)) {
                                throw new WebApplicationException(HttpServletResponse.SC_UNAUTHORIZED);
                            }
                        }
                    }

                }

            }
            return request;
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

    public void setHttpServletRequest(HttpServletRequest httpServletRequest) {
        this.req = httpServletRequest;
    }

}
