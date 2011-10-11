package com.rackspace.idm.domain.entity;

import org.joda.time.DateTime;

import java.util.Date;

public interface HasAccessToken {

    void setAccessTokenExpired();
    
    boolean isAccessTokenExpired(DateTime time);
    
    String getAccessTokenString();
    
    void setAccessTokenString(String accessTokenString);
    
    Date getAccessTokenExp();

    void setAccessTokenExp(Date accessTokenExp);
    
}
