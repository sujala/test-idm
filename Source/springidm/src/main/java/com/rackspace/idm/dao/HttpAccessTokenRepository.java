package com.rackspace.idm.dao;

import java.io.ByteArrayInputStream;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.SerializationException;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.rackspace.idm.config.DataCenterClient;
import com.rackspace.idm.config.DataCenterEndpoints;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.jaxb.AuthCredentials;
import com.rackspace.idm.jaxb.IdmFault;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;

public class HttpAccessTokenRepository extends HttpRepository implements TokenFindDeleteDao<AccessToken> {
    private Logger logger;

    @Autowired
    public HttpAccessTokenRepository(DataCenterEndpoints endpoints, AuthCredentials idmCreds, Logger logger) {
        super(endpoints, idmCreds);
        this.logger = logger;
    }

    @Override
    public AccessToken findByTokenString(String tokenString) {
        if (StringUtils.isBlank(tokenString)) {
            throw new IllegalArgumentException("No token String given.");
        }

        logger.debug("Searching for token {}.", tokenString);
        String dc = endpoints.getTokenPrefix(tokenString);
        DataCenterClient client = endpoints.get(dc);
        if (client == null) {
            logger.warn("Invalid prefix " + dc + " given");
            return null;
        }

        byte[] tokenBytes;
        try {
            tokenBytes = getRemoteToken(tokenString, dc, client);
        } catch (UniformInterfaceException ue1) {
            if (ue1.getResponse().getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                // Try again, client token might have just expired.
                try {
                    tokenBytes = getRemoteToken(tokenString, dc, client);
                } catch (UniformInterfaceException ue2) {
                    handleHttpCallException(ue2);
                    return null;
                }
            } else {
                handleHttpCallException(ue1);
                return null;
            }
        }

        if (tokenBytes == null || tokenBytes.length == 0) {
            return null;
        }

        AccessToken token = null;
        try {
            token = (AccessToken) SerializationUtils.deserialize(tokenBytes);
        } catch (SerializationException se) {
            logger.warn("Could not deserilize the response.");
        }

        return token;
    }

    private byte[] getRemoteToken(String tokenString, String dc, DataCenterClient client) {
        byte[] tokenBytes;
        tokenBytes = client.getResource().path(TOKEN_RESOURCE_PATH + "/" + tokenString)
            .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON)
            .header(HttpHeaders.AUTHORIZATION, "OAuth " + getMyAccessToken(dc).getTokenString())
            .get(byte[].class);
        return tokenBytes;
    }

    @Override
    public void delete(String tokenString) {
        throw new NotImplementedException();
    }

    @Override
    protected void handleHttpCallException(UniformInterfaceException e) {
        getLogger().warn("Client call to another DC failed.", e);
        ClientResponse resp = e.getResponse();
        if (resp != null) {
            try {
                ByteArrayInputStream out = resp.getEntity(ByteArrayInputStream.class);
                Object responseObj = SerializationUtils.deserialize(out);
                getLogger().warn("Error response -> {}", responseObj);
            } catch (Exception ex) {
                IdmFault fault = resp.getEntity(IdmFault.class);
                getLogger().warn("Cause -> {}: {}", fault.getMessage(), fault.getDetails());
            }
        }
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}
