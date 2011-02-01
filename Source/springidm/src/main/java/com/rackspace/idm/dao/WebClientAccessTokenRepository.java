package com.rackspace.idm.dao;

import javax.ws.rs.core.MediaType;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.rackspace.idm.config.DataCenterEndpoints;
import com.rackspace.idm.converters.TokenConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.RefreshToken;
import com.rackspace.idm.jaxb.Auth;
import com.sun.jersey.api.client.WebResource;

public class WebClientAccessTokenRepository implements GenericTokenDao<AccessToken> {
    private DataCenterEndpoints endpoints;
    private AccessToken myAToken;
    private RefreshToken myRToken;
    private Logger logger;

    @Autowired
    public WebClientAccessTokenRepository(DataCenterEndpoints endpoints, Logger logger) {
        this.endpoints = endpoints;
        this.logger = logger;
    }
    
    @Override
    public AccessToken findByTokenString(String tokenString) {
        logger.debug("Requesting token {}.", tokenString);
        String dc = StringUtils.split(tokenString, "-")[0];
        WebResource wr = endpoints.get(dc);
        Auth auth = wr.path("token").accept(MediaType.APPLICATION_XML_TYPE).get(Auth.class);
        return new TokenConverter().toAccessTokenFromJaxb(auth.getAccessToken());
    }

}
