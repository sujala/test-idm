package com.rackspace.idm.domain.entity;

public class TokenDefaultAttributes {
    private int expirationSeconds;
    private int maxExpirationSeconds;
    private int minExpirationSeconds;
    private String dataCenterPrefix;
    private boolean isTrustedServer;
    private int couldAuthExpirationSeconds;

    public TokenDefaultAttributes() {

    }

    public TokenDefaultAttributes(int expirationSeconds,
        int cloudAuthExpirationSeconds, int maxExpirationSeconds,
        int minExpirationSeconds, String dataCenterPrefix,
        boolean isTrustedServer) {
        this.couldAuthExpirationSeconds = cloudAuthExpirationSeconds;
        this.expirationSeconds = expirationSeconds;
        this.maxExpirationSeconds = maxExpirationSeconds;
        this.minExpirationSeconds = minExpirationSeconds;
        this.dataCenterPrefix = dataCenterPrefix;
        this.isTrustedServer = isTrustedServer;
    }

    public int getExpirationSeconds() {
        return this.expirationSeconds;
    }

    public void setExpirationSeconds(int expirationSeconds) {
        this.expirationSeconds = expirationSeconds;
    }

    public int getMaxExpirationSeconds() {
        return this.maxExpirationSeconds;
    }

    public void setMaxExpirationSeconds(int maxExpirationSeconds) {
        this.maxExpirationSeconds = maxExpirationSeconds;
    }

    public int getMinExpirationSeconds() {
        return this.minExpirationSeconds;
    }

    public void setMinExpirationSeconds(int minExpirationSeconds) {
        this.minExpirationSeconds = minExpirationSeconds;
    }

    public String getDataCenterPrefix() {
        return this.dataCenterPrefix;
    }

    public void setDataCenterPrefix(String dataCenterPrefix) {
        this.dataCenterPrefix = dataCenterPrefix;
    }

    public boolean getIsTrustedServer() {
        return this.isTrustedServer;
    }

    public void setIsTrustedServer(boolean isTrustedServer) {
        this.isTrustedServer = isTrustedServer;
    }

    public int getCloudAuthExpirationSeconds() {
        return this.couldAuthExpirationSeconds;
    }

    public void setCoudAuthExpirationSeconds(int cloudAuthExpirationSeconds) {
        this.couldAuthExpirationSeconds = cloudAuthExpirationSeconds;
    }
}
