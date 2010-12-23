package com.rackspace.idm.interceptors;

import java.lang.reflect.Method;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.HttpHeaders;
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

import com.rackspace.idm.authorizationService.AuthorizationResult;
import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.ClientService;

@Provider
@ServerInterceptor
@SecurityPrecedence
@Component
public class AuthorizationInterceptor implements PreProcessInterceptor,
    AcceptedByMethod, ApplicationContextAware {
    private ClientService clientService;
    private OAuthService oauthService;
    private IDMAuthorizationHelper authorizationHelper;
    private Logger logger;

    private ApplicationContext springCtx;

    public AuthorizationInterceptor() {
        logger = LoggerFactory.getLogger(AuthorizationInterceptor.class);
    }

    /**
     * Use only for unit testing.
     * 
     * @param clientService
     * @param oauthService
     * @param oauthAuthenticationService
     * @param authorizationHelper
     * @param logger
     */
    AuthorizationInterceptor(ClientService clientService,
        OAuthService oauthService, IDMAuthorizationHelper authorizationHelper,
        Logger logger) {
        this.clientService = clientService;
        this.oauthService = oauthService;
        this.authorizationHelper = authorizationHelper;
        this.logger = logger;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean accept(Class declaring, Method method) {
        if (InterceptedCall.SET_LOCK_STATUS.matches(declaring, method)) {
            return true;
        }

        if (InterceptedCall.ADD_FIRST_USER.matches(declaring, method)) {
            return true;
        }

        return false;
    }

    @Override
    public ServerResponse preProcess(HttpRequest request, ResourceMethod method)
        throws Failure, WebApplicationException {
        String httpMethod = request.getHttpMethod();
        String lookupPath = request.getUri().getPath();

        AuthorizationResult authResult = null;
        List<String> authHeaders = request.getHttpHeaders().getRequestHeader(
            HttpHeaders.AUTHORIZATION);

        if (authHeaders == null || authHeaders.isEmpty()) {
            String errorMsg = String
                .format("Bad Request. Authorization header is null.");
            logger.debug(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        authResult = authorize(authHeaders.get(0), httpMethod, lookupPath);
        if (authResult.getResultValue()) {
            return null;
        }

        // Authorization failed.
        String errorMsg = String.format("Authorization Failed. "
            + authResult.getMessage());
        logger.debug(errorMsg);
        throw new ForbiddenException(errorMsg);
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException {
        springCtx = applicationContext;
    }

    private ClientService getClientService() {
        if (clientService == null) {
            clientService = springCtx.getBean(ClientService.class);
        }

        return clientService;
    }

    private OAuthService getOauthService() {
        if (oauthService == null) {
            oauthService = springCtx.getBean(OAuthService.class);
        }

        return oauthService;
    }

    private IDMAuthorizationHelper getIdmAuthorizationHelper() {
        if (authorizationHelper == null) {
            authorizationHelper = springCtx
                .getBean(IDMAuthorizationHelper.class);
        }

        return authorizationHelper;
    }

    private AuthorizationResult authorize(String authHeader, String httpMethod,
        String lookupPath) {
        AuthorizationResult authorizationResult = null;

        if (authHeader != null) {
            String clientId = getOauthService().getClientIdFromAuthHeaderToken(
                authHeader);

            if (clientId == null) {
                String errorMsg = String
                    .format("Null or empty clientId parameter");
                authorizationResult = new AuthorizationResult(false, errorMsg);
                return authorizationResult;
            }

            Client client = getClientService().getById(clientId);

            if (client == null) {
                String errorMsg = String.format("No client found for clientId "
                    + clientId);
                authorizationResult = new AuthorizationResult(false, errorMsg);
                return authorizationResult;
            }

            List<Permission> permissionList = client.getPermissions();

            boolean result = getIdmAuthorizationHelper().checkPermission(
                permissionList, httpMethod, lookupPath);
            
            // If authorization fails for the client, then we'll check to see if the token
            // belongs to a Racker because Racker's are authorized to make all method calls.
            if (!result) {
                result = authorizationHelper.checkRackspaceEmployeeAuthorization(authHeader);
            }

            authorizationResult = new AuthorizationResult(result);

            return authorizationResult;
        } else {
            String errorMsg = String
                .format("Bad Request. Authorization header is null.");
            logger.debug(errorMsg);
            authorizationResult = new AuthorizationResult(false, errorMsg);
            return authorizationResult;
        }
    }
}
