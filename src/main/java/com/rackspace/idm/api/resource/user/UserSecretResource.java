package com.rackspace.idm.api.resource.user;

import com.rackspace.idm.api.resource.ParentResource;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import java.io.IOException;

/**
 * A users secret question and answer
 *
 */
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class UserSecretResource extends ParentResource {

    private final UserService userService;
    private final AuthorizationService authorizationService;
    
    private final com.rackspace.api.idm.v1.ObjectFactory of = new com.rackspace.api.idm.v1.ObjectFactory();

    @Autowired
    public UserSecretResource(UserService userService, AuthorizationService authorizationService, InputValidator inputValidator) {
    	super(inputValidator);
        this.userService = userService;
        this.authorizationService = authorizationService;
    }

    /**
     * Gets a users secret question and answer
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId userId
     */
    @GET
    public Response getUserSecret(
        @HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId) {

        authorizationService.verifyIdmSuperAdminAccess(authHeader);
    	
        getLogger().debug("Getting Secret Q&A for User: {}", userId);
        
        // get user to update
        User user = this.userService.loadUser(userId);

        com.rackspace.api.idm.v1.UserSecret secret = new com.rackspace.api.idm.v1.UserSecret();
        secret.setSecretAnswer(user.getSecretAnswer());
        secret.setSecretQuestion(user.getSecretQuestion());

        getLogger().debug("Got Secret Q&A for user: {}", user);

        return Response.ok(of.createSecret(secret)).build();
    }

    /**
     * Sets a users secret question and answer
     * 
     * 
     * @param authHeader HTTP Authorization header for authenticating the caller.
     * @param userId userId
     */
    @PUT
    public Response setUserSecret(
    	@HeaderParam("X-Auth-Token") String authHeader,
        @PathParam("userId") String userId, 
        EntityHolder<com.rackspace.api.idm.v1.UserSecret> holder) throws IOException, JAXBException {
        
        authorizationService.verifyIdmSuperAdminAccess(authHeader);
        validateRequestBody(holder);

        getLogger().debug("Updating Secret Q&A for User: {}", userId);
        
        com.rackspace.api.idm.v1.UserSecret jaxbUserSecret = holder.getEntity(); 
        
        User user = this.userService.loadUser(userId);
        user.setSecretQuestion(jaxbUserSecret.getSecretQuestion());
        user.setSecretAnswer(jaxbUserSecret.getSecretAnswer());
        
        this.userService.updateUser(user, false);

        getLogger().debug("Updated Secret Q&A for user: {}", user);

        return Response.noContent().build();
    }

    @Override
    protected void validateRequestBody(EntityHolder<?> holder) {
    	super.validateRequestBody(holder);
    	
    	com.rackspace.api.idm.v1.UserSecret userSecret = (com.rackspace.api.idm.v1.UserSecret) holder.getEntity();
    	String errMsg = "";
        if (StringUtils.isBlank(userSecret.getSecretQuestion())) {
            errMsg = "Secret Question cannot be blank. ";
        }
        
        if (StringUtils.isBlank(userSecret.getSecretAnswer())) {
            errMsg += "Secret Answer cannot be blank.";
        }
            
        if (StringUtils.isNotBlank(errMsg)) {
        	throw new BadRequestException(errMsg);
        }
    }
}
