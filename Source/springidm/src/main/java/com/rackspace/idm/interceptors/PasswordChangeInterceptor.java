package com.rackspace.idm.interceptors;

import java.lang.reflect.Method;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;

import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.annotations.interception.SecurityPrecedence;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.UnauthorizedException;
import org.jboss.resteasy.spi.interception.AcceptedByMethod;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.util.AuthHeaderHelper;

/**
 * Prevent access to resources using restricted token, except for password
 * reset.
 * 
 * @author john.eo
 */
@Provider
@ServerInterceptor
@SecurityPrecedence
@Component
public class PasswordChangeInterceptor implements AcceptedByMethod,
    ApplicationContextAware, PreProcessInterceptor {
    private AccessTokenService tokenService;
    private AuthHeaderHelper authHeaderHelper;
    private Logger logger;

    private ApplicationContext springCtx;

    public PasswordChangeInterceptor() {
        logger = LoggerFactory.getLogger(PasswordChangeInterceptor.class);
    }

    /**
     * Use for unit testing only.
     * 
     * @param accessTokenService
     * @param authHeaderHelper
     * @param logger
     */
    PasswordChangeInterceptor(AccessTokenService accessTokenService,
        AuthHeaderHelper authHeaderHelper, Logger logger) {
        this.tokenService = accessTokenService;
        this.authHeaderHelper = authHeaderHelper;
        this.logger = logger;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean accept(Class declaring, Method method) {
        // Is it the password change method?
        if (InterceptedCall.SET_USER_PASSWORD.matches(declaring, method)) {
            // Handle the token evaluation in the controller method
            return false;
        }

        // Check for restricted token and prevent access to other restricted
        // resources.
        return true;
    }

    @Override
    public ServerResponse preProcess(HttpRequest request, ResourceMethod method)
        throws Failure, WebApplicationException {
        // By the time the execution reaches this point, the URI being requested
        // is an operation other than "set password"
        List<String> authHeaders = request.getHttpHeaders().getRequestHeader(
            HttpHeaders.AUTHORIZATION);
        if (authHeaders == null || authHeaders.isEmpty()) {
            // No biggie. As long as it's not a restricted token.
            logger
                .debug("No Authorization header found, not checking for restricted token.");
            return null;
        }
        String authHeader = authHeaders.get(0);
        logger.debug("Evaluating auth header {}", authHeader);
        String tokenString = getAuthHeaderHelper().parseTokenParams(authHeader)
            .get("token");
        if (StringUtils.isBlank(tokenString)) {
            // No biggie. As long as it's not a restricted token.
            logger
                .debug("No access token found, not checking for restricted token.");
            return null;
        }

        AccessToken token = getTokenService()
            .getTokenByTokenString(tokenString);
        if (token == null) {
            String errMsg = "Token " + tokenString + " expired.";
            logger.info(errMsg);
            throw new UnauthorizedException(errMsg);
        }

        if (token.isRestrictedToSetPassword()) {
            String errMsg = "Token " + tokenString + " is restricted. Error";
            logger.info(errMsg);
            throw new UnauthorizedException(errMsg);
        }

        return null;
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
