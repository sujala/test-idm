package com.rackspace.idm.rest.resources;

import com.rackspace.idm.ErrorMsg;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.AuthConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.AuthData;
import com.rackspace.idm.errors.ApiError;
import com.rackspace.idm.exceptions.*;
import com.rackspace.idm.oauth.AuthCredentials;
import com.rackspace.idm.oauth.OAuthGrantType;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.validation.BasicCredentialsCheck;
import com.rackspace.idm.validation.InputValidator;
import com.rackspace.idm.validation.RefreshTokenCredentialsCheck;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletResponse;
import javax.validation.groups.Default;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.util.Map;

/**
 * Management of OAuth 2.0 token used by IDM.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class TokenResource {
    private AccessTokenService tokenService;
    private OAuthService oauthService;
    private ClientService clientService;
    private UserService userService;
    private AuthHeaderHelper authHeaderHelper;
    private InputValidator inputValidator;
    private AuthConverter authConverter;
    private AuthorizationService authorizationService;
    private Logger logger;

    @Autowired(required = true)
    public TokenResource(AccessTokenService tokenService, OAuthService oauthService, AuthHeaderHelper authHeaderHelper,
                         InputValidator inputValidator, AuthConverter authConverter,
                         AuthorizationService authorizationService, LoggerFactoryWrapper logger) {
        this.tokenService = tokenService;
        this.oauthService = oauthService;
        this.authHeaderHelper = authHeaderHelper;
        this.inputValidator = inputValidator;
        this.authConverter = authConverter;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
    }

    /**
     * Gets an instance of an access token, a refresh token, and their TTLs.
     * Will return the current access token if it has not expired.
     *
     * @param authHeader HTTP Authorization header for authenticating the calling client.
     * @param creds      AuthCredentials for authenticating the token request.
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}authCredentials
     * @response.representation.200.qname {http://docs.rackspacecloud.com/idm/api/v1.0}auth
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @POST
    public Response getAccessToken(@HeaderParam("Authorization") String authHeader,
                                   com.rackspace.idm.jaxb.AuthCredentials creds) {
        AuthCredentials trParam = new AuthCredentials();
        trParam.setClientId(creds.getClientId());
        trParam.setClientSecret(creds.getClientSecret());
        trParam.setGrantType(creds.getGrantType().value());
        trParam.setPassword(creds.getPassword());
        trParam.setRefreshToken(creds.getRefreshToken());
        trParam.setUsername(creds.getUsername());

        // if request includes an authHeader then the values for clientId and
        // clientSecret need to be parsed out. Also, the AuthHeader values will
        // override the values for client_id and client_secret passed in the
        // request
        if (!StringUtils.isBlank(authHeader)) {
            Map<String, String> authParams = authHeaderHelper.parseBasicParams(authHeader);
            if (authParams != null) {
                trParam.setClientId(authParams.get("username"));
                trParam.setClientSecret(authParams.get("password"));
            }
        }

        OAuthGrantType grantType = getGrantType(trParam.getGrantType());
        ApiError err = validate(trParam, grantType);
        if (err != null) {
            String msg = String.format("Bad request parameters: %s", err.getMessage());
            logger.debug(msg);
            throw new BadRequestException(msg);
        }

        DateTime currentTime = this.getCurrentTime();
        AuthData authData = oauthService.getTokens(grantType, trParam, currentTime);
        return Response.ok(authConverter.toAuthDataJaxb(authData)).build();
    }


    /**
     * Validates token and then, if valid, returns the access token and its ttl.
     *
     * @param authHeader  HTTP Authorization header for authenticating the calling client.
     * @param tokenString Token to be validated.
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
     */
    @GET
    @Path("{tokenString}")
    public Response validateAccessToken(@Context Request request, @Context UriInfo uriInfo,
                                        @HeaderParam("Authorization") String authHeader,
                                        @PathParam("tokenString") String tokenString) {

        logger.debug("Validating Access Token: {}", tokenString);

        AccessToken authToken = this.tokenService.getAccessTokenByAuthHeader(authHeader);

        // Only Rackers, Rackspace Clients and Specific Clients are authorized
        boolean authorized = authorizationService.authorizeRacker(authToken) ||
                authorizationService.authorizeRackspaceClient(authToken) ||
                authorizationService.authorizeClient(authToken, request.getMethod(), uriInfo.getPath());

        if (!authorized) {
            String errMsg = String.format("Token %s Forbidden from this call", authToken);
            logger.error(errMsg);
            throw new ForbiddenException(errMsg);
        }

        AuthData auth = new AuthData();

        // Validate Token exists and is valid
        AccessToken token = tokenService.validateToken(tokenString);
        if (token == null) {
            String errorMsg = String.format("Token not found : %s", tokenString);
            logger.error(errorMsg);
            throw new NotFoundException(errorMsg);
        }

        auth.setAccessToken(token);
        auth.setUser(token.getTokenUser());
        auth.setClient(token.getTokenClient());

        logger.debug("Validated Access Token: {}", tokenString);

        return Response.ok(authConverter.toAuthDataJaxb(auth)).build();
    }

    /**
     * Removes the token from IDM.
     *
     * @param authHeader  HTTP Authorization header for authenticating the calling client.
     * @param tokenString Token to be revoked.
     * @request.representation.qname {http://docs.rackspacecloud.com/idm/api/v1.0}authCredentials
     * @response.representation.400.qname {http://docs.rackspacecloud.com/idm/api/v1.0}badRequest
     * @response.representation.401.qname {http://docs.rackspacecloud.com/idm/api/v1.0}unauthorized
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}forbidden
     * @response.representation.403.qname {http://docs.rackspacecloud.com/idm/api/v1.0}userDisabled
     * @response.representation.404.qname {http://docs.rackspacecloud.com/idm/api/v1.0}itemNotFound
     * @response.representation.500.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serverError
     * @response.representation.503.qname {http://docs.rackspacecloud.com/idm/api/v1.0}serviceUnavailable
     */
    @DELETE
    @Path("{tokenString}")
    public Response revokeAccessToken(@Context Request request, @Context UriInfo uriInfo,
                                      @HeaderParam("Authorization") String authHeader,
                                      @PathParam("tokenString") String tokenString) {

        logger.debug("Revoking Token: {}", tokenString);

        try {
            logger.debug("Parsing Auth Header");
            String authTokenString = authHeaderHelper.getTokenFromAuthHeader(authHeader);
            logger.debug("Parsed Auth Header - Token: {}", authTokenString);

            oauthService.revokeToken(authTokenString, tokenString);

            logger.info("Revoked Token: {}", tokenString);

        } catch (TokenExpiredException ex) {
            String errorMsg = String.format("Authorization failed, token is expired: %s", authHeader);
            logger.error(errorMsg);
            throw new ApiException(HttpServletResponse.SC_UNAUTHORIZED, ErrorMsg.UNAUTHORIZED, errorMsg);

        } catch (IllegalStateException ex) {
            String errorMsg = String.format("IllegalState encountered when revoking token: %s", tokenString);
            logger.error(errorMsg);
            throw new ApiException(HttpServletResponse.SC_BAD_REQUEST, ErrorMsg.BAD_REQUEST, errorMsg);

        }

        return Response.noContent().build();
    }

    // private funcs
    protected DateTime getCurrentTime() {
        return new DateTime();
    }

    private OAuthGrantType getGrantType(String grantTypeStrVal) {
        OAuthGrantType grantType = OAuthGrantType.valueOf(grantTypeStrVal.replace("-", "_").toUpperCase());
        logger.debug("Verified GrantType: {}", grantTypeStrVal);
        return grantType;
    }

    private ApiError validate(AuthCredentials trParam, OAuthGrantType grantType) {

        if (OAuthGrantType.PASSWORD == grantType) {
            return inputValidator.validate(trParam, Default.class, BasicCredentialsCheck.class);
        }

        if (OAuthGrantType.REFRESH_TOKEN == grantType) {
            return inputValidator.validate(trParam, Default.class, RefreshTokenCredentialsCheck.class);
        }

        return inputValidator.validate(trParam);
    }
}
