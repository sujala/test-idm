package com.rackspace.idm.domain.entity;

import org.joda.time.DateTime;

import java.util.Date;

public interface HasRefreshToken {
    
    void setRefreshTokenExpired();
    
    boolean isRefreshTokenExpired(DateTime time);
    
    String getRefreshTokenString();
    
    void setRefreshTokenString(String refreshTokenString);
    
    Date getRefreshTokenExp();
    
    void setRefreshTokenExp(Date refreshTokenExp);
}
