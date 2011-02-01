package com.rackspace.idm.dao;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.rackspace.idm.config.DataCenterClient;
import com.rackspace.idm.config.DataCenterEndpoints;
import com.rackspace.idm.converters.TokenConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.jaxb.Auth;
import com.rackspace.idm.jaxb.AuthCredentials;
import com.rackspace.idm.jaxb.IdmFault;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

public class WebClientAccessTokenRepository implements
    GenericTokenDao<AccessToken> {
    private static final String TOKEN_RESOURCE_PATH = "token";
    private DataCenterEndpoints endpoints;
    private AuthCredentials idmCreds;
    private TokenConverter converter = new TokenConverter();
    private Logger logger;

    @Autowired
    public WebClientAccessTokenRepository(DataCenterEndpoints endpoints,
        AuthCredentials idmCreds, Logger logger) {
        this.endpoints = endpoints;
        this.idmCreds = idmCreds;
        this.logger = logger;
    }

    @Override
    public AccessToken findByTokenString(String tokenString) {
        logger.debug("Requesting token {}.", tokenString);
        String dc = StringUtils.split(tokenString, "-")[0];
        DataCenterClient client = endpoints.get(dc);
        ClientResponse resp;
        try {
            resp = client
                .getResource()
                .path(TOKEN_RESOURCE_PATH + "/" + tokenString)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .header(HttpHeaders.AUTHORIZATION,
                    "OAuth " + getMyAccessToken(dc).getTokenString())
                .get(ClientResponse.class);
        } catch (UniformInterfaceException e) {
            throw getClientCallException(e);
        }

        return getClientCallResponse(resp);
    }

    /**
     * Retrieves client access token to be used by the local instance of IDM
     * service. The Token is then used to make client calls to IDM instances
     * in other DCs.
     * @return Access token that represents the local IDM instance.
     */
    AccessToken getMyAccessToken(String dc) {
        DataCenterClient client = endpoints.get(dc);
        if (client.getAccessToken() != null
            && !client.getAccessToken().isExpired(new DateTime())) {
            return client.getAccessToken();
        }

        ClientResponse resp;
        try {
            resp = client.getResource().path(TOKEN_RESOURCE_PATH)
                .accept(MediaType.APPLICATION_XML_TYPE)
                .type(MediaType.APPLICATION_XML).entity(idmCreds)
                .post(ClientResponse.class);
        } catch (UniformInterfaceException e) {
            throw getClientCallException(e);
        }

        return getClientCallResponse(resp);
    }

    private RuntimeException getClientCallException(UniformInterfaceException e) {
        ClientResponse resp = e.getResponse();
        if (resp == null) {
            return new IllegalStateException("Request failed:\n" + e);
        }
        return new IllegalStateException(resp.getEntity(IdmFault.class)
            .getMessage());
    }

    private AccessToken getClientCallResponse(ClientResponse resp) {
        if (Response.Status.OK.getStatusCode() == resp.getStatus()) {
            return converter.toAccessTokenFromJaxb(resp.getEntity(Auth.class)
                .getAccessToken());
        } else {
            // Something's wrong. Try to get the fault.
            IdmFault fault = resp.getEntity(IdmFault.class);
            logger.warn(fault.getMessage() + ": " + fault.getDetails());
            throw new IllegalStateException(fault.getMessage());
        }
    }
}
