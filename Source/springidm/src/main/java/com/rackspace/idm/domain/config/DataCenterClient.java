package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.entity.AccessToken;
import com.sun.jersey.api.client.WebResource;

public class DataCenterClient {
    private String dc;
    private WebResource wr;
    private AccessToken at;

    public DataCenterClient(String dc, WebResource wr) {
        this.dc = dc;
        this.wr = wr;
    }

    public String getDcPrefix() {
        return dc;
    }

    public WebResource getResource() {
        return wr;
    }

    public AccessToken getAccessToken() {
        return at;
    }

    public void setAccessToken(AccessToken at) {
        this.at = at;
    }
}