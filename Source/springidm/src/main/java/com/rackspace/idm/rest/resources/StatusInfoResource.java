package com.rackspace.idm.rest.resources;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.services.AccessTokenService;
import com.rackspace.idm.services.AuthorizationService;
import com.rackspace.idm.services.HealthMonitoringService;
import com.rackspace.idm.services.HealthMonitoringServiceInterface;

@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
@Component
public class StatusInfoResource {

    private AccessTokenService accessTokenService;
    private AuthorizationService authorizationService;
    private HealthMonitoringService healthMonitoringService;
    private Logger logger;
    
    @Autowired
    public StatusInfoResource(HealthMonitoringService healthMonitoringService, 
        AccessTokenService accessTokenService, AuthorizationService authorizationService,
        LoggerFactoryWrapper logger) {
        this.healthMonitoringService = healthMonitoringService;
        this.accessTokenService = accessTokenService;
        this.authorizationService = authorizationService;
        this.logger = logger.getLogger(this.getClass());
    }
    
    @GET
    public Response getStatusInformation(@Context Request request, @Context UriInfo uriInfo,
        @HeaderParam("Authorization") String authHeader) {

        //AccessToken token = this.accessTokenService
        //    .getAccessTokenByAuthHeader(authHeader);

        // Racker's, Rackspace Clients
        /*boolean authorized = authorizationService.authorizeRacker(token)
            || authorizationService.authorizeRackspaceClient(token)
            || authorizationService.authorizeClient(token, request.getMethod(),
                uriInfo.getPath());
         */
        
        boolean memcacheStatus = healthMonitoringService.pingMemcache();
        
        boolean ldapStatus = healthMonitoringService.pingLDAP();
        
        if (!memcacheStatus || !ldapStatus) {
            StringBuilder criticalError = new StringBuilder("Critical situation. Memcache and LDAP both are down!!!!");
            logger.debug(criticalError.toString());
            Response.ok(new String(criticalError.toString()));
        }
        
        
        return Response.ok().build();
    }
}
