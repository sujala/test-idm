package com.rackspace.idm.api.resource.token;

import java.io.StringReader;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.api.converter.AuthConverter;
import com.rackspace.idm.api.converter.CredentialsConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.entity.AuthData;
import com.rackspace.idm.domain.entity.Credentials;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthenticationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TokenService;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;

/**
 * Management of OAuth 2.0 token used by IDM.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class TokensResource extends ParentResource {
	
    private final AuthHeaderHelper authHeaderHelper;
    private final AuthConverter authConverter;
    private final TokenService tokenService;
    private final AuthorizationService authorizationService;
    private final CredentialsConverter credentialsConverter;
    private final ScopeAccessService scopeAccessService;
    private final AuthenticationService authenticationService;
    final private Logger logger = LoggerFactory.getLogger(TokensResource.class);

    @Autowired(required = true)
    public TokensResource(TokenService oauthService,
        AuthHeaderHelper authHeaderHelper, AuthConverter authConverter,
        AuthorizationService authorizationService,
        ScopeAccessService scopeAccessService,
        CredentialsConverter credentialsConverter,
        AuthenticationService authenticationService,
        InputValidator inputValidator, TokenService tokenService) {
    	
    	super(inputValidator);
        this.authHeaderHelper = authHeaderHelper;
        this.authConverter = authConverter;
        this.authorizationService = authorizationService;
        this.scopeAccessService = scopeAccessService;
        this.credentialsConverter = credentialsConverter;
        this.authenticationService = authenticationService;
        this.tokenService = tokenService;
    }

    /**
     * Gets an instance of an access token, a refresh token, and their TTLs.
     * Will return the current access token if it has not expired.
     *
     * @param authHeader HTTP Authorization header for authenticating the calling client.
     * @param creds      AuthCredentials for authenticating the token request.
     */
    @POST
    public Response authenticate(@HeaderParam("X-Auth-Token") String authHeader, EntityHolder<String> holder) throws Throwable {
    	validateRequestBody(holder);
    	
		// Don't rely on jersey to do the marshalling. Different type of credentials can be specified.
    	// Jersey will only marshal to the specific param type defined. Manually do the marshalling here.
    	JAXBContext context = JAXBContextResolver.get();
    	Unmarshaller unmarshaller = context.createUnmarshaller();
    	StringReader reader = new StringReader(holder.getEntity());
    	
    	com.rackspace.api.idm.v1.Credentials creds 
    		= (com.rackspace.api.idm.v1.Credentials) unmarshaller.unmarshal(reader);
    	
    	Credentials credentialsDO = credentialsConverter.toCredentialsDO(creds);
        
    	AuthData authData = authenticationService.authenticate(credentialsDO);
           
        return Response.ok(authConverter.toAuthDataJaxb(authData)).build();  
    }
    
    /**
     * Validates token and then, if valid, returns the access token and its ttl.
     *
     * @param authHeader  HTTP Authorization header for authenticating the calling client.
     * @param tokenString Token to be validated.
     */
    @GET
    @Path("{tokenString}")
    public Response validateAccessToken(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("tokenString") String tokenString) {

        logger.debug("Validating Access Token: {}", tokenString);

        ScopeAccess authToken = this.scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        // Only Rackers, Rackspace Clients and Specific Clients are authorized
        // Racker's, Rackspace Clients and Specific Clients are authorized
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);

        AuthData authData = authenticationService.validateAuthToken(tokenString);
       
        logger.debug("Validated Access Token: {}", tokenString);

        return Response.ok(authConverter.toAuthDataJaxb(authData)).build();    }

    /**
     * Removes the token from IDM, across all DCs.
     *
     * @param authHeader  HTTP Authorization header for authenticating the calling client.
     * @param tokenString Token to be revoked.
     */
    @DELETE
    @Path("{tokenString}")
    public Response revokeAccessToken(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("tokenString") String tokenString) {

        logger.debug("Revoking Token: {}", tokenString);
        
        //TODO: investigate if we should use authorization to ensure user can make call
        //rather than passing in the auth token header
        String authTokenString = authHeaderHelper.getTokenFromAuthHeader(authHeader);
        tokenService.revokeAccessToken(authTokenString, tokenString);

        logger.warn("Revoked Token: {}", tokenString);

        return Response.noContent().build();
    }

    /**
     * Check if the given access token has the specific application.
     *
     * @param authHeader HTTP Authorization header for authenticating the calling client.
     * @param tokenString The token to check for permission
     * @param applicationId The applicationId for the service that defines the permission
     */
    @GET
    @Path("{tokenString}/applications/{applicationId}")
    public Response doesTokenHaveApplicationAccess(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("tokenString") String tokenString,
        @PathParam("applicationId") String applicationId) {

        logger.debug("Checking whether token {} has service {}", tokenString,
            applicationId);

        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);

        boolean hasApplicationAccess = 
        		tokenService.doesTokenHaveAccessToApplication(tokenString, applicationId);
        
        if (!hasApplicationAccess) {
            String errorMsg = String.format("Token does not have access : %s", token);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }
        
        return Response.noContent().build();
    }

    /**
     * Check if the given access token has the specified application role.
     *
     * @param authHeader HTTP Authorization header for authenticating the calling client.
     * @param tokenString The token to check for permission
     * @param applicationId The serviceId for the service that defines the permission
     * @param roleId The role to check
     */
    @GET
    @Path("{tokenString}/applications/{applicationId}/roles/{roleId}")
    public Response doesTokenHaveApplicationRole(@Context Request request,
        @Context UriInfo uriInfo,
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("tokenString") String tokenString,
        @PathParam("applicationId") String applicationId,
        @PathParam("roleId") String roleId) {

    	//TODO: IMPLEMENT THIS API
    	
        logger.debug("Checking whether token {} has application role {}", tokenString, roleId);

        ScopeAccess token = this.scopeAccessService
            .getAccessTokenByAuthHeader(authHeader);
        //TODO: Implement authorization rules
        //authorizationService.authorizeToken(token, uriInfo);
        boolean hasApplicationRole = 
        	this.tokenService.doesTokenHaveAplicationRole(tokenString, applicationId, roleId);

        if (!hasApplicationRole) {
            String errorMsg = String.format("Token does not have access : %s", token);
            logger.warn(errorMsg);
            throw new NotFoundException(errorMsg);
        }
        
        return Response.noContent().build();
    }

}
