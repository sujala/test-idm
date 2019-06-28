package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspacecloud.docs.auth.api.v1.*;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.net.URISyntaxException;

public interface Cloud11Service {

    // Token Methods
    Response.ResponseBuilder validateToken(HttpServletRequest request, String tokenId, String belongsTo, String type, HttpHeaders httpHeaders) throws IOException;

    // Authenticate Methods
    Response.ResponseBuilder authenticate(HttpServletRequest request, UriInfo uriInfo, HttpHeaders httpHeaders, String body) throws IOException, JAXBException, URISyntaxException;
    Response.ResponseBuilder adminAuthenticate(HttpServletRequest request, UriInfo uriInfo, HttpHeaders httpHeaders, String body) throws IOException, JAXBException, URISyntaxException;
    
    // User Methods  
    Response.ResponseBuilder getUser(HttpServletRequest request, String userId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder getUserFromMossoId(HttpServletRequest request, int mossoId, HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder getUserFromNastId(HttpServletRequest request, String nastId, HttpHeaders httpHeaders) throws IOException;

    //extensions
    Response.ResponseBuilder extensions(HttpHeaders httpHeaders) throws IOException;
    Response.ResponseBuilder getExtension(HttpHeaders httpHeaders, String alias) throws IOException;
}
