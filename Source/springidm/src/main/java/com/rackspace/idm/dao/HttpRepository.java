package com.rackspace.idm.dao;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.rackspace.idm.config.DataCenterClient;
import com.rackspace.idm.config.DataCenterEndpoints;
import com.rackspace.idm.converters.TokenConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.jaxb.Auth;
import com.rackspace.idm.jaxb.AuthCredentials;
import com.rackspace.idm.jaxb.IdmFault;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

public abstract class HttpRepository {
    protected static final String TOKEN_RESOURCE_PATH = "token";
    protected DataCenterEndpoints endpoints;
    protected AuthCredentials idmCreds;
    protected TokenConverter converter = new TokenConverter();

    public HttpRepository(DataCenterEndpoints endpoints, AuthCredentials idmCreds) {
        this.endpoints = endpoints;
        this.idmCreds = idmCreds;
    }

    /**
     * Retrieves client access token to be used by the local instance of IDM
     * service. The Token is then used to make client calls to IDM instances
     * in other DCs.
     * @return Access token that represents the local IDM instance.
     */
    protected AccessToken getMyAccessToken(String dc) {
        DataCenterClient client = endpoints.get(dc);
        AccessToken myToken = client.getAccessToken();
        if (myToken != null && !myToken.isExpired(new DateTime())) {
            return myToken;
        }

        getLogger().debug("Requesting client access token for Customer IDM");
        ClientResponse resp;
        try {
            resp = client.getResource().path(TOKEN_RESOURCE_PATH).accept(MediaType.APPLICATION_XML_TYPE)
                .type(MediaType.APPLICATION_XML).entity(idmCreds).post(ClientResponse.class);
        } catch (UniformInterfaceException e) {
            handleHttpCallException(e);
            return null;
        }

        return extractMyAccessToken(resp, client);
    }

    AccessToken extractMyAccessToken(ClientResponse resp, DataCenterClient client) {
        if (Response.Status.OK.getStatusCode() == resp.getStatus()) {
            AccessToken myToken = converter
                .toAccessTokenFromJaxb(resp.getEntity(Auth.class).getAccessToken());
            client.setAccessToken(myToken);
            return myToken;
        } else {
            // Something's wrong. Try to get the fault.
            IdmFault fault = resp.getEntity(IdmFault.class);
            getLogger().warn("Client call to another DC returned an IDM fault.\n{}: {}", fault.getMessage(),
                fault.getDetails());
            return null;
        }
    }

    protected abstract void handleHttpCallException(UniformInterfaceException e);

    protected abstract Logger getLogger();
}