package com.rackspace.idm.domain.entity;

import java.util.Date;

import org.joda.time.DateTime;

public interface hasAccessToken {

    void setAccessTokenExpired();
    
    boolean isAccessTokenExpired(DateTime time);
    
    String getAccessTokenString();
    
    void setAccessTokenString(String accessTokenString);
    
    Date getAccessTokenExp();

    void setAccessTokenExp(Date accessTokenExp);
    
}
