package com.rackspace.idm.domain.entity;

import org.joda.time.DateTime;

public interface hasRefreshToken {
    
    void setRefreshTokenExpired();
    
    boolean isRefreshTokenExpired(DateTime time);
}
