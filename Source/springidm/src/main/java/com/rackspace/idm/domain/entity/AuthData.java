package com.rackspace.idm.domain.entity;

import java.util.List;

public class AuthData {

    private AccessToken accessToken;
    private RefreshToken refreshToken;
    private BaseUser user;
    private BaseClient client;
    private List<Permission> permissions;
    private String message;
    
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
    
    public BaseClient getClient() {
        return client;
    }
    
    public void setClient(BaseClient client) {
        this.client = client;
    }
    
    public List<Permission> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getMessage() {
        return message;
    }
    
}
