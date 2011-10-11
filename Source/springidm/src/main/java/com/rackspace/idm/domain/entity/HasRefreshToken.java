package com.rackspace.idm.domain.entity;

import java.util.Date;

import org.joda.time.DateTime;

public interface HasRefreshToken {
    
    void setRefreshTokenExpired();
    
    boolean isRefreshTokenExpired(DateTime time);
    
    String getRefreshTokenString();
    
    void setRefreshTokenString(String refreshTokenString);
    
    Date getRefreshTokenExp();
    
    void setRefreshTokenExp(Date refreshTokenExp);
}
