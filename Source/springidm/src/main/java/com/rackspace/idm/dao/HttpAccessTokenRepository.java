package com.rackspace.idm.dao;

import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.SerializationException;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;

import com.rackspace.idm.config.DataCenterClient;
import com.rackspace.idm.config.DataCenterEndpoints;
import com.rackspace.idm.entities.AccessToken;

public class HttpAccessTokenRepository extends HttpRepository implements XdcAccessTokenDao {
    private Logger logger;

    public HttpAccessTokenRepository(DataCenterEndpoints endpoints, Configuration config, Logger logger) {
        super(endpoints, config);
        this.logger = logger;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.rackspace.idm.dao.HttpAccessTokenDao#findByTokenString(java.lang.
     * String)
     */
    @Override
    public AccessToken findByTokenString(final String tokenString) {
        if (StringUtils.isBlank(tokenString)) {
            throw new IllegalArgumentException("No token String given.");
        }

        logger.debug("Searching for token {}.", tokenString);
        String dc = DataCenterEndpoints.getTokenPrefix(tokenString);
        DataCenterClient client = endpoints.get(dc);

        byte[] tokenBytes = makeHttpCall(new HttpCaller<byte[]>() {
            @Override
            public byte[] execute(String myTokenStr, DataCenterClient client) {
                return client.getResource().path(TOKEN_RESOURCE_PATH + "/" + tokenString)
                    .accept(MediaType.APPLICATION_OCTET_STREAM)
                    .header(HttpHeaders.AUTHORIZATION, getOauthAuthorizationHeader(myTokenStr))
                    .get(byte[].class);
            }
        }, client);

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

    /*
     * (non-Javadoc)
     * 
     * @see com.rackspace.idm.dao.HttpAccessTokenDao#delete(java.lang.String)
     */
    @Override
    public void delete(final String tokenString) {
        if (StringUtils.isBlank(tokenString)) {
            throw new IllegalArgumentException("No token String given.");
        }

        logger.debug("Attempting to delete for token {}.", tokenString);

        List<String> tokenPermutations = endpoints.getAllTokenPermuations(tokenString);

        for (String dcTokenCombo : tokenPermutations) {
            HttpCaller<Object> deleter = new HttpDeleteCaller(dcTokenCombo);

            for (DataCenterClient client : endpoints.getAll()) {
                // Don't make a call against the local (own) IDM instance.
                // That should be done using a memcached DAO instance.
                if (client.getDcPrefix().equals(config.getString("token.dataCenterPrefix"))) {
                    continue;
                }

                makeHttpCall(deleter, client);
            }
        }
    }

    @Override
    public void deleteAllTokensForOwner(final String ownerId) {
        if (StringUtils.isBlank(ownerId)) {
            throw new IllegalArgumentException("No ownerId given.");
        }

        logger.debug("Attempting to delete all access tokens for owner {}.", ownerId);
        for (DataCenterClient client : endpoints.getAll()) {
            // Don't make a call against the local (own) IDM instance.
            if (client.getDcPrefix().equals(config.getString("token.dataCenterPrefix"))) {
                continue;
            }

            makeHttpCall(new HttpCaller<Object>() {

                @Override
                public Object execute(String myTokenStr, DataCenterClient client) {
                    client.getResource().path(TOKEN_RESOURCE_PATH).queryParam("owner", ownerId.trim())
                        .accept(MediaType.APPLICATION_XML)
                        .header(HttpHeaders.AUTHORIZATION, getOauthAuthorizationHeader(myTokenStr)).delete();
                    return null;
                }
            }, client);
        }

    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    private class HttpDeleteCaller implements HttpCaller<Object> {
        private String tokenString;

        HttpDeleteCaller(String tokenString) {
            this.tokenString = tokenString;
        }

        @Override
        public Object execute(String myTokenStr, DataCenterClient client) {
            // Don't trigger another global delete. We are already in the middle
            // of executing one.
            client.getResource().path(TOKEN_RESOURCE_PATH + "/" + tokenString).queryParam("global", "false")
                .accept(MediaType.APPLICATION_XML)
                .header(HttpHeaders.AUTHORIZATION, getOauthAuthorizationHeader(myTokenStr)).delete();

            return null;
        }

    }
}