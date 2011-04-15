package com.rackspace.idm.domain.dao.impl;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang.SerializationException;
import org.apache.commons.lang.SerializationUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.GlobalConstants.TokenDeleteByType;
import com.rackspace.idm.domain.config.DataCenterClient;
import com.rackspace.idm.domain.config.DataCenterEndpoints;
import com.rackspace.idm.domain.dao.XdcAccessTokenDao;
import com.rackspace.idm.domain.entity.AccessToken;
import com.sun.jersey.api.client.ClientHandlerException;

public class HttpAccessTokenRepository extends HttpRepository implements XdcAccessTokenDao {
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    public HttpAccessTokenRepository(DataCenterEndpoints endpoints, Configuration config) {
        super(endpoints, config);
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
            	byte[] tokenResponse = null;
            	try {
                    tokenResponse = client.getResource().path(TOKEN_RESOURCE_PATH + "/" + tokenString)
                    .accept(MediaType.APPLICATION_OCTET_STREAM, MediaType.APPLICATION_XML)
                    .header(HttpHeaders.AUTHORIZATION, getOauthAuthorizationHeader(myTokenStr))
                    .get(byte[].class);
            	} catch (ClientHandlerException e) {
                	getLogger().warn("Client call to another DC was refused.");
                }
            	return tokenResponse;
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

        makeHttpCallToOtherDcs(new HttpCaller<Object>() {
            @Override
            public Object execute(String myTokenStr, DataCenterClient client) {
                // Don't trigger another global delete. We are already in
                // the middle
                // of executing one.
            	try {
                    client.getResource().path(TOKEN_RESOURCE_PATH + "/" + tokenString)
                    .queryParam("global", "false").accept(MediaType.APPLICATION_XML)
                    .header(HttpHeaders.AUTHORIZATION, getOauthAuthorizationHeader(myTokenStr)).delete();
            	} catch (ClientHandlerException e) {
                	getLogger().warn("Client call to another DC was refused.");
                }

                return null;
            }
        });
    }

    @Override
    public void deleteAllTokensForOwner(final String ownerId) {
        if (StringUtils.isBlank(ownerId)) {
            throw new IllegalArgumentException("No ownerId given.");
        }
        logger.debug("Attempting to delete all access tokens for owner {}.", ownerId);

        makeHttpCallToOtherDcs(new HttpCaller<Object>() {
            @Override
            public Object execute(String myTokenStr, DataCenterClient client) {
                return makeHttpDeleteCallById(ownerId, TokenDeleteByType.owner, myTokenStr, client);
            }
        });

    }

    @Override
    public void deleteAllTokensForCustomer(final String customerId) {
        if (StringUtils.isBlank(customerId)) {
            throw new IllegalArgumentException("No customerId given.");
        }
        logger.debug("Attempting to delete all access tokens for customer {}.", customerId);

        makeHttpCallToOtherDcs(new HttpCaller<Object>() {
            @Override
            public Object execute(String myTokenStr, DataCenterClient client) {
                return makeHttpDeleteCallById(customerId, TokenDeleteByType.customer, myTokenStr, client);
            }

        });
    }

    @Override
    protected Logger getLogger() {
        return logger;
    }

    private Object makeHttpDeleteCallById(String id, TokenDeleteByType idType, String myTokenStr,
        DataCenterClient client) {
    	
    	try {
            client.getResource().path(TOKEN_RESOURCE_PATH).queryParam("querytype", idType.toString())
            .queryParam("id", id.trim()).accept(MediaType.APPLICATION_XML)
            .header(HttpHeaders.AUTHORIZATION, getOauthAuthorizationHeader(myTokenStr)).delete();
    	} catch (ClientHandlerException e) {
        	getLogger().warn("Client call to another DC was refused.");
        }
    	
        return null;
    }

    private <T> void makeHttpCallToOtherDcs(HttpCaller<T> caller) {
        for (DataCenterClient client : endpoints.getAll()) {
            // Don't make a call against the local (own) IDM instance.
            if (client.getDcPrefix().equals(config.getString("token.dataCenterPrefix"))) {
                continue;
            }

            makeHttpCall(caller, client);
        }
    }
}