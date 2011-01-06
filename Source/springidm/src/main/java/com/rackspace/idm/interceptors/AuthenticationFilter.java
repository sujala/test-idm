package com.rackspace.idm.interceptors;

import java.util.regex.Pattern;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.oauthAuthentication.AuthenticationResult;
import com.rackspace.idm.oauthAuthentication.HttpOauthAuthenticationService;
import com.rackspace.idm.oauthAuthentication.OauthAuthenticationService;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/**
 * @author john.eo Apply token-based authentication to all calls except the
 *         several that are checked for in the if clauses below.
 */
@Component
public class AuthenticationFilter implements ContainerRequestFilter,
    ApplicationContextAware {
    private static final Pattern PASSWORD_CHANGE_PATTERN = Pattern
        .compile("^customers/.+/users/.+/password$");
    private OauthAuthenticationService oauthService;
    private AuthHeaderHelper authHeaderHelper = new AuthHeaderHelper();
    private Logger logger;

    private ApplicationContext springCtx;

    public AuthenticationFilter() {
        logger = LoggerFactory.getLogger(AuthenticationFilter.class);
    }

    AuthenticationFilter(OauthAuthenticationService oauthService, Logger logger) {
        this.oauthService = oauthService;
        this.logger = logger;
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        String path = request.getPath();
        String method = request.getMethod();

        // Skip authentication for the following 5 calls

        if ("GET".equals(method) && "application.wadl".equals(path)) {
            return request;
        }
        
        if ("GET".equals(method) && path.startsWith("xsd")) {
            return request;
        }

        if ("GET".equals(method) && "".equals(path)) {
            return request;
        }

        if ("PUT".equals(method)
            && PASSWORD_CHANGE_PATTERN.matcher(path).matches()) {
            return request;
        }

        if ("POST".equals(method) && "token".equals(path)) {
            return request;
        }

        String authHeader = request.getHeaderValue(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || authHeader.isEmpty()) {
            throw new NotAuthenticatedException(
                "The request for the resource must include the Authorization header.");
        }
        String tokenString = authHeaderHelper
            .getTokenFromAuthHeader(authHeader);
        AuthenticationResult authResult = getOauthService().authenticateToken(
            tokenString);

        if (authResult.getHttpStatusCode() == Status.OK.getStatusCode()) {
            // Authenticated
            return request;
        }

        // authentication failed if we reach this point
        String errorMsg = String.format("Authentication Failed. Reason: %s",
            authResult.getMessage());
        logger.error(errorMsg);
        throw new NotAuthenticatedException(errorMsg);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException {
        springCtx = applicationContext;
    }

    private OauthAuthenticationService getOauthService() {
        if (oauthService == null) {
            oauthService = springCtx
                .getBean(HttpOauthAuthenticationService.class);
        }

        return oauthService;
    }
}
