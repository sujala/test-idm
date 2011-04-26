package com.rackspace.idm.domain.entity;

import java.util.Date;

import org.joda.time.DateTime;

public interface hasRefreshToken {
    
    void setRefreshTokenExpired();
    
    boolean isRefreshTokenExpired(DateTime time);
    
    String getRefreshTokenString();
    
    void setRefreshTokenString(String refreshTokenString);
    
    void setRefreshTokenExp(Date refreshTokenExp);
}
