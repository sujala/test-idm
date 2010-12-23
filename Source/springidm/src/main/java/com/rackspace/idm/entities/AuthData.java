package com.rackspace.idm.entities;

import java.util.List;

public class AuthData {

    private AccessToken accessToken;
    private RefreshToken refreshToken;
    private User user;
    private Client client;
    private List<Permission> permissions;
    
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
    
    public User getUser() {
        return user;
    }
    
    public void setUser(User user) {
        this.user = user;
    }
    
    public Client getClient() {
        return client;
    }
    
    public void setClient(Client client) {
        this.client = client;
    }
    
    public List<Permission> getPermissions() {
        return permissions;
    }
    
    public void setPermissions(List<Permission> permissions) {
        this.permissions = permissions;
    }
    
}
