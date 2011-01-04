package com.rackspace.idm.rest.resources;

import java.util.Map;

import javax.servlet.http.HttpServletResponse;
import javax.validation.groups.Default;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.ErrorMsg;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.AuthConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AuthData;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.ClientStatus;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserStatus;
import com.rackspace.idm.errors.ApiError;
import com.rackspace.idm.exceptions.ApiException;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.ClientDisabledException;
import com.rackspace.idm.exceptions.NotAuthenticatedException;
import com.rackspace.idm.exceptions.NotAuthorizedException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.exceptions.TokenExpiredException;
import com.rackspace.idm.exceptions.UserDisabledException;
import com.rackspace.idm.oauth.AuthCredentials;
import com.rackspace.idm.oauth.OAuthGrantType;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.validation.ApiCredentialsCheck;
import com.rackspace.idm.validation.BasicCredentialsCheck;
import com.rackspace.idm.validation.InputValidator;
import com.rackspace.idm.validation.RefreshTokenCredentialsCheck;

/**
 * Management of OAuth 2.0 token used by IDM.
 * 
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class TokenResource {
    private AccessTokenService tokenService;
    private OAuthService oauthService;
    private UserService userService;
    private ClientService clientService;
    private AuthHeaderHelper authHeaderHelper;
    private IDMAuthorizationHelper authorizationHelper;
    private InputValidator inputValidator;
    private AuthConverter authConverter;
    private Logger logger;

    @Autowired(required = true)
    public TokenResource(AccessTokenService tokenService,
        UserService userService, ClientService clientService,
        OAuthService oauthService, AuthHeaderHelper authHeaderHelper,
        IDMAuthorizationHelper idmAuthHelper, InputValidator inputValidator,
        AuthConverter authConverter, LoggerFactoryWrapper logger) {

        this.tokenService = tokenService;
        this.oauthService = oauthService;
        this.userService = userService;
        this.clientService = clientService;
        this.oauthService = oauthService;
        this.authHeaderHelper = authHeaderHelper;
        this.authorizationHelper = idmAuthHelper;
        this.inputValidator = inputValidator;
        this.authConverter = authConverter;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * Gets an instance of an access token, a refresh token, and their TTLs.
     * Will return the current access token if it has not expired.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}authCredentials
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}auth
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * 
     * @param authHeader HTTP Authorization header for authenticating the calling client.
     * @param creds AuthCredentials for authenticating the token request.
     * @return The response with an auth representation that contains access and refresh token, along with their TTLs.
     */
    @POST
    public Response getAccessToken(
        @HeaderParam("Authorization") String authHeader,
        com.rackspace.idm.jaxb.AuthCredentials creds) {
        AuthCredentials trParam = new AuthCredentials();
        trParam.setClientId(creds.getClientId());
        trParam.setClientSecret(creds.getClientSecret());
        trParam.setGrantType(creds.getGrantType().value());
        trParam.setPassword(creds.getPassword());
        trParam.setRefreshToken(creds.getRefreshToken());
        trParam.setUsername(creds.getUsername());

        int expirationSeconds = tokenService.getDefaultTokenExpirationSeconds();

        // if request includes an authHeader then the values for clientId and
        // clientSecret need to be parsed out. Also, the AuthHeader values will
        // override the values for client_id and client_secret passed in the
        // request
        if (!StringUtils.isBlank(authHeader)) {
            Map<String, String> authParams = authHeaderHelper
                .parseBasicParams(authHeader);
            if (authParams != null) {
                trParam.setClientId(authParams.get("username"));
                trParam.setClientSecret(authParams.get("password"));
            }
        }

        String clientId = trParam.getClientId();
        String clientSecret = trParam.getClientSecret();
        String grantTypeStrVal = trParam.getGrantType();

        OAuthGrantType grantType = getGrantType(grantTypeStrVal);

        ApiError err = validate(trParam, grantType);
        if (err != null) {
            throw new BadRequestException(err.getMessage());
        }

        if (!oauthService.authenticateClient(clientId, clientSecret)) {
            String errorMsg = String.format("Unauthorized Client For: %s",
                trParam.getClientId());
            logger.error(errorMsg);
            throw new NotAuthorizedException(errorMsg);
        }

        AuthData authData = null;
        try {
            DateTime currentTime = this.getCurrentTime();
            authData = oauthService.getTokens(grantType, trParam,
                expirationSeconds, currentTime);
        } catch (NotAuthenticatedException e) {
            String errorMsg = String.format("Unauthorized User For: %s",
                trParam.getUsername());
            logger.error(errorMsg);
            throw new NotAuthenticatedException(errorMsg);
        }
        return Response.ok(authConverter.toAuthDataJaxb(authData)).build();
    }

    /**
     * Validates token and then, if valid, returns the access token, a refresh token, and their TTLs.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}authCredentials
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}auth
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userDisabled
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     * @response.representation.503.doc Service could not be reached. See the error message for details.
     *  
     * @param authHeader HTTP Authorization header for authenticating the calling client.
     * @param tokenString Token to be validated.
     * @return The response with an authData representation that contains access and refresh token, along with their TTLs.
     */
    @GET
    @Path("{tokenString}")
    public Response validateAccessToken(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("tokenString") String tokenString) {

        logger.debug("Validating Access Token: {}", tokenString);

        AuthData auth = new AuthData();

        // Validate Token exists and is valid
        AccessToken token = tokenService.validateToken(tokenString);
        if (token == null) {
            String errorMsg = String
                .format("Token not found : %s", tokenString);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        auth.setAccessToken(token);

        // Validate Client exists and is valid
        String clientId = this.tokenService
            .getClientIdByTokenString(tokenString);

        if (StringUtils.isBlank(clientId)) {
            String errorMsg = String.format("ClientId not found for token: %s",
                tokenString);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        Client client = this.clientService.getById(clientId);
        if (client == null) {
            String errorMsg = String.format(
                "Client \"%s\" does not exist for token %s", clientId,
                tokenString);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        } else if (client.getStatus() != ClientStatus.ACTIVE
            || client.getSoftDeleted()) {
            String errorMsg = String.format(
                "Client \"%s\" does not have a valid status", clientId,
                tokenString);
            logger.error(errorMsg);
            throw new ClientDisabledException(errorMsg);
        }

        auth.setClient(client);

        // If token is for a User and not just a client
        if (token.getOwner() != token.getRequestor()) {

            // Validate User exists and is valid
            String username = this.tokenService
                .getUsernameByTokenString(tokenString);

            if (StringUtils.isBlank(username)) {
                String errorMsg = String.format(
                    "Username not found for token: %s", tokenString);
                logger.error(errorMsg);
                throw new NotFoundException(errorMsg);
            }

            User tokenOwner = null;
            if (token.getIsTrusted()) {
                tokenOwner = new User.Builder().setUsername(token.getOwner())
                    .setEmail(GlobalConstants.NO_REPLY_EMAIL)
                    .setCisIds(GlobalConstants.RACKSPACE_CUSTOMER_ID,
                        token.getOwner()).build();
            } else {
                tokenOwner = this.userService.getUser(username);
                if (tokenOwner == null) {
                    String errorMsg = String.format(
                        "User \"%s\" does not exist for token %s", username,
                        tokenString);
                    logger.error(errorMsg);
                    throw new NotFoundException(errorMsg);
                } else if (tokenOwner.getStatus() != UserStatus.ACTIVE
                    || tokenOwner.getSoftDeleted()) {
                    String errorMsg = String.format(
                        "User \"%s\" does not have a valid status", username,
                        tokenString);
                    logger.error(errorMsg);
                    throw new UserDisabledException(errorMsg);
                }
            }
            auth.setUser(tokenOwner);
        }

        logger.debug("Validated Access Token: {}", tokenString);

        return Response.ok(authConverter.toAuthDataJaxb(auth)).build();
    }

    /**
     * Removes the token from IDM.
     * 
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}authCredentials
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userDisabled
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     *  
     * @param authHeader HTTP Authorization header for authenticating the calling client.
     * @param tokenString Token to be revoked.
     * @return The response with an authData representation that contains access and refresh token, along with their TTLs. 
     */
    @DELETE
    @Path("{tokenString}")
    public Response revokeAccessToken(
        @HeaderParam("Authorization") String authHeader,
        @PathParam("tokenString") String tokenString) {

        logger.debug("Revoking Token: {}", tokenString);

        if (!authorizeRevokeToken(authHeader, tokenString, "revokeAccessToken")) {
            if (!authorizationHelper
                .checkRackspaceEmployeeAuthorization(authHeader)) {
                authorizationHelper.handleAuthorizationFailure();
            }
        }

        try {
            logger.debug("Parsing Auth Header");
            String authTokenString = authHeaderHelper
                .getTokenFromAuthHeader(authHeader);
            logger.debug("Parsed Auth Header - Token: {}", authTokenString);

            tokenService.revokeToken(authTokenString, tokenString);

            logger.info("Revoked Token: {}", tokenString);

        } catch (TokenExpiredException ex) {
            String errorMsg = String.format(
                "Authorization failed, token is expired: %s", authHeader);
            logger.error(errorMsg);
            throw new ApiException(HttpServletResponse.SC_UNAUTHORIZED,
                ErrorMsg.UNAUTHORIZED, errorMsg);

        } catch (IllegalStateException ex) {
            String errorMsg = String
                .format("IllegalState encountered when revoking token: %s",
                    tokenString);
            logger.error(errorMsg);
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST,
                ErrorMsg.BAD_REQUEST, errorMsg);

        }
        
        return Response.noContent().build();
    }

    // private funcs
    protected DateTime getCurrentTime() {
        return new DateTime();
    }

    private boolean authorizeRevokeToken(String authHeader,
        String tokenToRevoke, String methodName) {

        String subjectUsername = oauthService
            .getUsernameFromAuthHeaderToken(authHeader);
        String username = tokenService.getUsernameByTokenString(tokenToRevoke);

        if (subjectUsername == null) {
            // Condition 1: RACKSPACE Company can revoke token.
            return authorizationHelper.checkRackspaceClientAuthorization(
                authHeader, methodName);
        } else {
            // Condition 2: User can revoke his/her own token.
            return authorizationHelper.checkUserAuthorization(subjectUsername,
                username, methodName);
        }
    }

    private OAuthGrantType getGrantType(String grantTypeStrVal) {
        OAuthGrantType grantType = OAuthGrantType.valueOf(grantTypeStrVal
            .replace("-", "_").toUpperCase());
        logger.debug("Verified GrantType: {}", grantTypeStrVal);
        return grantType;
    }

    private ApiError validate(AuthCredentials trParam, OAuthGrantType grantType) {
        if (OAuthGrantType.API_CREDENTIALS == grantType) {
            return inputValidator.validate(trParam, Default.class,
                ApiCredentialsCheck.class);
        }

        if (OAuthGrantType.PASSWORD == grantType) {
            return inputValidator.validate(trParam, Default.class,
                BasicCredentialsCheck.class);
        }

        if (OAuthGrantType.REFRESH_TOKEN == grantType) {
            return inputValidator.validate(trParam, Default.class,
                RefreshTokenCredentialsCheck.class);
        }

        return inputValidator.validate(trParam);
    }
}
