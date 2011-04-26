package com.rackspace.idm.domain.entity;

import org.joda.time.DateTime;

public interface hasAccessToken {

    void setAccessTokenExpired();
    
    boolean isAccessTokenExpired(DateTime time);
}
