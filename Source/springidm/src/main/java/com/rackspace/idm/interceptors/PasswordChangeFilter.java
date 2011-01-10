package com.rackspace.idm.interceptors;

import java.util.List;
import java.util.regex.Pattern;

import javax.ws.rs.core.HttpHeaders;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.exceptions.NotAuthorizedException;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/**
 * @author john.eo Allow the user password change flow to go through and prevent
 *         restricted token from doing anything other than set the user
 *         password.
 */
@Component
public class PasswordChangeFilter implements ContainerRequestFilter,
    ApplicationContextAware {
    private static final Pattern SET_USER_PASSWORD_PATTERN = Pattern
        .compile("^customers/.+/users/.+/password$");
    private AccessTokenService tokenService;
    private AuthHeaderHelper authHeaderHelper;
    private Logger logger;

    private ApplicationContext springCtx;

    public PasswordChangeFilter() {
        logger = LoggerFactory.getLogger(PasswordChangeFilter.class);
    }

    PasswordChangeFilter(AccessTokenService accessTokenService,
        AuthHeaderHelper authHeaderHelper, Logger logger) {
        this.tokenService = accessTokenService;
        this.authHeaderHelper = authHeaderHelper;
        this.logger = logger;
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        String path = request.getPath();
        String method = request.getMethod();
        if (!"PUT".equals(method)
            || !SET_USER_PASSWORD_PATTERN.matcher(path).matches()) {
            return request;
        }

        // By the time the execution reaches this point, the URI being requested
        // is an operation other than "set password"
        List<String> authHeaders = request
            .getRequestHeader(HttpHeaders.AUTHORIZATION);
        if (authHeaders == null || authHeaders.isEmpty()) {
            // No biggie. As long as it's not a restricted token.
            logger
                .debug("No Authorization header found, not checking for restricted token.");
            return request;
        }
        String authHeader = authHeaders.get(0);
        logger.debug("Evaluating auth header {}", authHeader);
        String tokenString = getAuthHeaderHelper().parseTokenParams(authHeader)
            .get("token");
        if (StringUtils.isBlank(tokenString)) {
            // No biggie. As long as it's not a restricted token.
            logger
                .debug("No access token found, not checking for restricted token.");
            return request;
        }

        AccessToken token = getTokenService()
            .getAccessTokenByTokenString(tokenString);
        if (token == null) {
            String errMsg = "Token " + tokenString + " expired.";
            logger.info(errMsg);
            throw new NotAuthorizedException(errMsg);
        }

        if (token.isRestrictedToSetPassword()) {
            String errMsg = "Token " + tokenString + " is restricted. Error";
            logger.info(errMsg);
            throw new NotAuthorizedException(errMsg);
        }

        return request;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException {
        springCtx = applicationContext;
    }

    private AccessTokenService getTokenService() {
        if (tokenService == null) {
            tokenService = springCtx.getBean(AccessTokenService.class);
        }

        return tokenService;
    }

    private AuthHeaderHelper getAuthHeaderHelper() {
        if (authHeaderHelper == null) {
            authHeaderHelper = springCtx.getBean(AuthHeaderHelper.class);
        }

        return authHeaderHelper;
    }
}
