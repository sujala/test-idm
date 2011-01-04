package com.rackspace.idm.entities;

public class RefreshTokenDefaultAttributes {
    private int expirationSeconds;
    private String dataCenterPrefix;
    
    public RefreshTokenDefaultAttributes() {
        
    }
    
    public RefreshTokenDefaultAttributes(int expirationSeconds,
        String dataCenterPrefix) {
        
        this.expirationSeconds = expirationSeconds;
        this.dataCenterPrefix = dataCenterPrefix;
    }
    
    public int getExpirationSeconds() {
        return this.expirationSeconds;
    }
    
    public void setExpirationSeconds(int expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
    }
    
    public String getDataCenterPrefix() {
        return this.dataCenterPrefix;
    }
    
    public void setDataCenterPrefix(String dataCenterPrefix) {
        this.dataCenterPrefix = dataCenterPrefix;
    }
}
