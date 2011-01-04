package com.rackspace.idm.interceptors;

import java.util.List;
import java.util.regex.Pattern;

import javax.ws.rs.core.HttpHeaders;

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
import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;

/**
 * @author john.eo Apply authorization to customer lock and the first-user add
 *         calls but skip others.
 */
@Component
public class AuthorizationFilter implements ContainerRequestFilter,
    ApplicationContextAware {
    private static final Pattern CUSTOMER_LOCK_PATTERN = Pattern
        .compile("customers/.+/actions/lock");

    private ClientService clientService;
    private OAuthService oauthService;
    private IDMAuthorizationHelper authorizationHelper;
    private Logger logger;

    private ApplicationContext springCtx;

    public AuthorizationFilter() {
        logger = LoggerFactory.getLogger(AuthorizationFilter.class);
    }

    AuthorizationFilter(ClientService clientService, OAuthService oauthService,
        IDMAuthorizationHelper authorizationHelper, Logger logger) {
        this.clientService = clientService;
        this.oauthService = oauthService;
        this.authorizationHelper = authorizationHelper;
        this.logger = logger;
    }

    @Override
    public ContainerRequest filter(ContainerRequest request) {
        String path = request.getPath();
        String method = request.getMethod();

        boolean isCustomerLockCall = "PUT".equals(method)
            && CUSTOMER_LOCK_PATTERN.matcher(path).matches();
        boolean is1stUserAddCall = "POST".equals(method)
            && "users".equals(path);

        if (!isCustomerLockCall && !is1stUserAddCall) {
            // Only the two calls are authorized by this interceptors.
            return request;
        }

        String httpMethod = request.getMethod();
        AuthorizationResult authResult = null;
        List<String> authHeaders = request
            .getRequestHeader(HttpHeaders.AUTHORIZATION);

        if (authHeaders == null || authHeaders.isEmpty()) {
            String errorMsg = String
                .format("Bad Request. Authorization header is null.");
            logger.debug(errorMsg);
            throw new BadRequestException(errorMsg);
        }

        authResult = authorize(authHeaders.get(0), httpMethod, path);
        if (authResult.getResultValue()) {
            return request;
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

            // If authorization fails for the client, then we'll check to see if
            // the token
            // belongs to a Racker because Racker's are authorized to make all
            // method calls.
            if (!result) {
                result = authorizationHelper
                    .checkRackspaceEmployeeAuthorization(authHeader);
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
