package com.rackspace.idm.domain.entity;

import org.joda.time.DateTime;

public class AuthData {

    private AccessToken accessToken;
    private RefreshToken refreshToken;
    private BaseUser user;
    private BaseClient client;
    private Racker racker;
    private Boolean passwordResetOnlyToken = null;
    private DateTime userPasswordExpirationDate = null;
    
    public AuthData() {
    }
    
    public AuthData(AccessToken accessToken, RefreshToken refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }
    
    public AccessToken getAccessToken() {
        return accessToken;
    }
    
    public void setAccessToken(AccessToken accessToken) {
        this.accessToken = accessToken;
    }
    
    public RefreshToken getRefreshToken() {
        return refreshToken;
    }
    
    public void setRefreshToken(RefreshToken refreshToken) {
        this.refreshToken = refreshToken;
    }
    
    public BaseUser getUser() {
        return user;
    }
    
    public void setUser(BaseUser user) {
        this.user = user;
    }
    
    public Racker getRacker() {
        return racker;
    }
    
    public void setRacker(Racker racker) {
        this.racker = racker;
    }
    
    public BaseClient getClient() {
        return client;
    }
    
    public void setClient(BaseClient client) {
        this.client = client;
    }
    
    public void setPasswordResetOnlyToken(boolean passwordResetOnlyToken) {
        this.passwordResetOnlyToken = passwordResetOnlyToken;
    }
    
    public Boolean getPasswordResetOnlyToken() {
        return passwordResetOnlyToken;
    }
    
    public DateTime getUserPasswordExpirationDate() {
        return userPasswordExpirationDate;
    }
    
    public void setUserPasswordExpirationDate(DateTime passwordExpirationDate) {
        this.userPasswordExpirationDate = passwordExpirationDate;
    }
}
