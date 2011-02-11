package com.rackspace.idm.dao;

import java.io.ByteArrayInputStream;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

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
    public AccessToken findByTokenString(final String tokenString) {
        if (StringUtils.isBlank(tokenString)) {
            throw new IllegalArgumentException("No token String given.");
        }

        logger.debug("Searching for token {}.", tokenString);
        String dc = endpoints.getTokenPrefix(tokenString);

        byte[] tokenBytes = makeHttpCall(new HttpCaller<byte[]>() {
            public byte[] execute(String myTokenStr, DataCenterClient client) {
                return client.getResource().path(TOKEN_RESOURCE_PATH + "/" + tokenString)
                    .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, getOauthAuthorizationHeader(myTokenStr))
                    .get(byte[].class);
            }
        }, dc);

        if (tokenBytes == null || tokenBytes.length == 0) {
            return null;
        }

        AccessToken xdcToken = null;
        try {
            xdcToken = (AccessToken) SerializationUtils.deserialize(tokenBytes);
        } catch (SerializationException se) {
            logger.warn("Could not deserilize the response.");
        }

        return xdcToken;
    }

    @Override
    public void delete(String tokenString) {
        if (StringUtils.isBlank(tokenString)) {
            throw new IllegalArgumentException("No token String given.");
        }

        logger.debug("Attempting to delete for token {}.", tokenString);

        for (DataCenterClient client : endpoints.getAll()){
            // TODO Determine whether this client is the local DC.
        }

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