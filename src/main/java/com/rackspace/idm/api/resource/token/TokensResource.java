package com.rackspace.idm.api.resource.token;

import com.rackspace.api.idm.v1.AuthCredentials;
import com.rackspace.api.idm.v1.RSACredentials;
import com.rackspace.api.idm.v1.RackerCredentials;
import com.rackspace.idm.api.converter.AuthConverter;
import com.rackspace.idm.api.converter.CredentialsConverter;
import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.entity.AuthData;
import com.rackspace.idm.domain.entity.AuthorizationContext;
import com.rackspace.idm.domain.entity.Credentials;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.AuthenticationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.api.json.JSONConfiguration;
import com.sun.jersey.api.json.JSONJAXBContext;
import com.sun.jersey.api.json.JSONUnmarshaller;
import com.sun.jersey.core.provider.EntityHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;

/**
 * Management of OAuth 2.0 token used by IDM.
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class TokensResource extends ParentResource {

    private final AuthConverter authConverter;
    private final AuthorizationService authorizationService;
    private final CredentialsConverter credentialsConverter;
    private final ScopeAccessService scopeAccessService;
    private final AuthenticationService authenticationService;
    private final Logger logger = LoggerFactory.getLogger(TokensResource.class);

    @Autowired(required = true)
    public TokensResource(AuthConverter authConverter,
                          AuthorizationService authorizationService,
                          ScopeAccessService scopeAccessService,
                          CredentialsConverter credentialsConverter,
                          AuthenticationService authenticationService,
                          InputValidator inputValidator) {

        super(inputValidator);
        this.authConverter = authConverter;
        this.authorizationService = authorizationService;
        this.scopeAccessService = scopeAccessService;
        this.credentialsConverter = credentialsConverter;
        this.authenticationService = authenticationService;
    }

    /**
     * Gets an instance of an access token, a refresh token, and their TTLs.
     * Will return the current access token if it has not expired.
     *
     * @param credentials AuthCredentials for authenticating the token request.
     */
    @POST
    public Response authenticate(@Context HttpHeaders httpHeaders, EntityHolder<String> credentials) {

        validateRequestBody(credentials);

        // Don't rely on jersey to do the marshalling. Different type of credentials can be specified.
        // Jersey will only marshal to the specific param type defined. Manually do the marshalling here.

        JAXBElement<? extends com.rackspace.api.idm.v1.Credentials> creds = null;
        try {
            if (httpHeaders.getMediaType().isCompatible(MediaType.APPLICATION_JSON_TYPE)) {
                JSONConfiguration jsonConfiguration = JSONConfiguration.natural().rootUnwrapping(false).build();
                JSONJAXBContext context = new JSONJAXBContext(jsonConfiguration, "com.rackspace.api.idm.v1");
                JSONUnmarshaller jsonUnmarshaller = context.createJSONUnmarshaller();
                StringReader reader = new StringReader(credentials.getEntity());
                if (credentials.getEntity().contains("\"rackerCredentials\":")) {
                    creds = jsonUnmarshaller.unmarshalJAXBElementFromJSON(reader, RackerCredentials.class);
                }else if (credentials.getEntity().contains("\"rsaCredentials\":")){
                    creds = jsonUnmarshaller.unmarshalJAXBElementFromJSON(reader, RSACredentials.class);
                }else{
                    creds = jsonUnmarshaller.unmarshalJAXBElementFromJSON(reader, AuthCredentials.class);
                }
            } else {
                JAXBContext context = JAXBContextResolver.get();
                Unmarshaller unmarshaller = context.createUnmarshaller();
                StringReader reader = new StringReader(credentials.getEntity());
                creds = (JAXBElement<? extends com.rackspace.api.idm.v1.Credentials>) unmarshaller.unmarshal(reader);
            }
        }catch(Exception ex){
            throw new BadRequestException("Bad Request.", ex);
        }
        Credentials credentialsDO = credentialsConverter.toCredentialsDO(creds.getValue());

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
    public Response validateAccessToken(
                                        @HeaderParam("X-Auth-Token") String authHeader,
                                        @PathParam("tokenString") String tokenString) {

        ScopeAccess scopeAccess = scopeAccessService.getAccessTokenByAuthHeader(authHeader);
        AuthorizationContext context = authorizationService.getAuthorizationContext(scopeAccess);
        authorizationService.authorizeIdmSuperAdminOrRackspaceClient(context);

        logger.debug("Validating Access Token: {}", tokenString);
        AuthData authData = authenticationService.getAuthDataFromToken(tokenString);

        logger.debug("Validated Access Token: {}", tokenString);

        return Response.ok(authConverter.toAuthDataJaxb(authData)).build();
    }
}
