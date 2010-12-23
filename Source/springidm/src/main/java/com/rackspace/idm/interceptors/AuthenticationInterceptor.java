package com.rackspace.idm.interceptors;

import java.lang.reflect.Method;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.Provider;

import org.jboss.resteasy.annotations.interception.SecurityPrecedence;
import org.jboss.resteasy.annotations.interception.ServerInterceptor;
import org.jboss.resteasy.core.ResourceMethod;
import org.jboss.resteasy.core.ServerResponse;
import org.jboss.resteasy.spi.Failure;
import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.interception.AcceptedByMethod;
import org.jboss.resteasy.spi.interception.PreProcessInterceptor;
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

@Provider
@ServerInterceptor
@SecurityPrecedence
@Component
public class AuthenticationInterceptor implements PreProcessInterceptor,
    AcceptedByMethod, ApplicationContextAware {
    private OauthAuthenticationService oauthService;
    private AuthHeaderHelper authHeaderHelper = new AuthHeaderHelper();
    private Logger logger;

    private ApplicationContext springCtx;

    public AuthenticationInterceptor() {
        logger = LoggerFactory.getLogger(this.getClass());
    }

    /**
     * Use only for unit testing
     * 
     * @param oauthService
     * @param logger
     */
    AuthenticationInterceptor(OauthAuthenticationService oauthService,
        Logger logger) {
        this.oauthService = oauthService;
        this.logger = logger;
    }

    /**
     * This method determines whether the preProcess method should be executed.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean accept(Class declaring, Method method) {
        if (InterceptedCall.GET_APP_VERSIONS.matches(declaring, method)) {
            // Service root resource.
            return false;
        }

        if (InterceptedCall.SET_USER_PASSWORD.matches(declaring, method)) {
            // Password change request. Authenticated by either passing in old
            // password, or, if forgotten, using a restricted token.
            return false;
        }

        if (InterceptedCall.GET_ACCESS_TOKEN.matches(declaring, method)) {
            // Token request
            return false;
        }

        // Authenticate everything else
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.jboss.resteasy.spi.interception.PreProcessInterceptor#preProcess(
     * org.jboss.resteasy.spi.HttpRequest,
     * org.jboss.resteasy.core.ResourceMethod)
     */
    @Override
    public ServerResponse preProcess(HttpRequest request, ResourceMethod method)
        throws Failure, WebApplicationException {
        List<String> authHeaders = request.getHttpHeaders().getRequestHeader(
            HttpHeaders.AUTHORIZATION);
        if (authHeaders == null || authHeaders.isEmpty()) {
            throw new NotAuthenticatedException(
                "The request for the resource must include the Authorization header.");
        }
        String tokenString = authHeaderHelper
            .getTokenFromAuthHeader(authHeaders.get(0));
        AuthenticationResult authResult = getOauthService().authenticateToken(
            tokenString);

        if (authResult.getHttpStatusCode() == Status.OK.getStatusCode()) {
            // Authenticated
            return null;
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