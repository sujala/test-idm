package com.rackspace.idm.dao;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.SerializationException;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.rackspace.idm.config.DataCenterClient;
import com.rackspace.idm.entities.AccessToken;

public class HttpAccessTokenRepository extends HttpRepository implements TokenFindDeleteDao<AccessToken> {
    private Logger logger;

    public HttpAccessTokenRepository(Configuration config, Logger logger) {
        super(config);
        this.logger = logger;
    }

    @Override
    public AccessToken findByTokenString(final String tokenString) {
        if (StringUtils.isBlank(tokenString)) {
            throw new IllegalArgumentException("No token String given.");
        }

        logger.debug("Searching for token {}.", tokenString);
        String dc = getEndpoints().getTokenPrefix(tokenString);

        byte[] tokenBytes = makeHttpCall(new HttpCaller<byte[]>() {
            public byte[] execute(String myTokenStr, DataCenterClient client) {
                return client.getResource().path(TOKEN_RESOURCE_PATH + "/" + tokenString)
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
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
    public void delete(final String tokenString) {
        if (StringUtils.isBlank(tokenString)) {
            throw new IllegalArgumentException("No token String given.");
        }

        logger.debug("Attempting to delete for token {}.", tokenString);

        for (DataCenterClient dccl : getEndpoints().getAll()) {
            if (dccl.getDcPrefix().equals(config.getString("token.dataCenterPrefix"))) {
                continue;
            }

            String dc = dccl.getDcPrefix();
            makeHttpCall(new HttpCaller<Object>() {
                @Override
                public Object execute(String myTokenStr, DataCenterClient client) {
                    client.getResource().path(TOKEN_RESOURCE_PATH + "/" + tokenString)
                        .accept(MediaType.APPLICATION_JSON)
                        .header(HttpHeaders.AUTHORIZATION, getOauthAuthorizationHeader(myTokenStr)).delete();

                    return null;
                }
            }, dc);
        }

        throw new NotImplementedException();
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }
}