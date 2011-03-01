package com.rackspace.idm.dao;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.slf4j.Logger;

import com.rackspace.idm.api.converter.TokenConverter;
import com.rackspace.idm.domain.config.DataCenterClient;
import com.rackspace.idm.domain.config.DataCenterEndpoints;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.jaxb.Auth;
import com.rackspace.idm.jaxb.AuthCredentials;
import com.rackspace.idm.jaxb.AuthGrantType;
import com.rackspace.idm.jaxb.IdmFault;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

public abstract class HttpRepository {
    protected static final String TOKEN_RESOURCE_PATH = "token";
    protected Configuration config;
    protected TokenConverter converter = new TokenConverter();
    protected DataCenterEndpoints endpoints;

    private AuthCredentials idmCreds;

    public HttpRepository(DataCenterEndpoints endpoints, Configuration config) {
        this.endpoints = endpoints;
        this.config = config;
    }

    /**
     * Retrieves client access token to be used by the local instance of IDM
     * service. The Token is then used to make client calls to IDM instances
     * in other DCs.
     * @return Access token that represents the local IDM instance.
     */
    protected AccessToken getMyAccessToken(String dc, boolean getFromCache) {
        DataCenterClient client = endpoints.get(dc);
        if (getFromCache) {
            AccessToken myToken = client.getAccessToken();
            if (myToken != null && !myToken.isExpired(new DateTime())) {
                return myToken;
            }
        }

        getLogger().debug("Requesting client access token for Customer IDM");
        ClientResponse resp;
        try {
            resp = client.getResource().path(TOKEN_RESOURCE_PATH).accept(MediaType.APPLICATION_XML_TYPE)
                .type(MediaType.APPLICATION_XML).entity(getIdmCreds()).post(ClientResponse.class);
        } catch (UniformInterfaceException e) {
            handleHttpCallException(e);
            return null;
        }

        return extractMyAccessToken(resp, client);
    }

    protected AccessToken extractMyAccessToken(ClientResponse resp, DataCenterClient client) {
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

    /**
     * Makes the HTTP client call using an access token granted by a remote IDM instance
     * to this local IDM instance. If the first attempt fails due to this token expiring,
     * will try again with a new token.
     * 
     * @param <T> Generic return type
     * @param caller Implementation of the HTTP client call
     * @param dc Prefix of the data center against which the HTTP client call is to be made
     * @return Client call response, if any
     */
    protected <T> T makeHttpCall(HttpCaller<T> caller, DataCenterClient client) {
        if (client == null) {
            getLogger().warn("Request for invalid data center prefix.");
            return null;
        }

        String dc = client.getDcPrefix();

        AccessToken myToken = getMyAccessToken(dc, true);
        if (myToken == null) {
            // No client access token? Not much I can do here.
            return null;
        }

        try {
            return caller.execute(myToken.getTokenString(), client);
        } catch (UniformInterfaceException ue1) {
            boolean isUnauthorized = ue1.getResponse().getStatus() == Status.UNAUTHORIZED.getStatusCode();
            if (isUnauthorized) {
                // Try again with a new token, client token might have just
                // expired.
                myToken = getMyAccessToken(dc, false);
                try {
                    return caller.execute(myToken.getTokenString(), client);
                } catch (UniformInterfaceException ue2) {
                    handleHttpCallException(ue2);
                    return null;
                }
            } else {
                handleHttpCallException(ue1);
                return null;
            }
        }
    }

    protected String getOauthAuthorizationHeader(String myTokenStr) {
        return "OAuth " + myTokenStr;
    }

    protected void handleHttpCallException(UniformInterfaceException e) {
        getLogger().warn("Client call to another DC failed.", e);
        ClientResponse resp = e.getResponse();
        if (resp == null) {
            getLogger().warn("No response returned.");
            return;
        }

        if (MediaType.APPLICATION_JSON_TYPE.equals(resp.getType())
            || MediaType.APPLICATION_XML_TYPE.equals(resp.getType())) {
            IdmFault fault = resp.getEntity(IdmFault.class);
            getLogger().warn("Cause -> {}: {}", fault.getMessage(), fault.getDetails());
        } else {
            getLogger().warn("Cause -> {}", e);
        }
    }

    protected abstract Logger getLogger();

    /**
     * Builds the credentials to obtain an access token for the local IDM instance that will make an
     * HTTP call against remote IDM instance(s).
     * 
     * @return
     */
    private AuthCredentials getIdmCreds() {
        String clientId = config.getString("idm.clientId");
        String clientSecret = config.getString("idm.clientSecret");

        if (idmCreds != null && idmCreds.getClientId().equals(clientId)
            && idmCreds.getClientSecret().equals(clientSecret)) {
            return idmCreds;
        }

        idmCreds = new AuthCredentials();
        idmCreds.setClientId(clientId);
        idmCreds.setClientSecret(clientSecret);
        idmCreds.setGrantType(AuthGrantType.NONE);
        return idmCreds;
    }

    /**
     * Encapsulates HTTP client call.
     * 
     * @author john.eo
     *
     * @param <T> Response type
     */
    protected interface HttpCaller<T> {
        T execute(String myTokenStr, DataCenterClient client);
    }
}