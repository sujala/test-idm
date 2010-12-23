package com.rackspace.idm.oauth;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.rackspace.idm.validation.ApiCredentialsCheck;
import com.rackspace.idm.validation.BasicCredentialsCheck;
import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RefreshTokenCredentialsCheck;
import com.rackspace.idm.validation.RegexPatterns;

public class AuthCredentials {

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String grantType;

    @NotNull(groups = {ApiCredentialsCheck.class, BasicCredentialsCheck.class})
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY, groups = {
        ApiCredentialsCheck.class, BasicCredentialsCheck.class})
    private String username;

    @NotNull(groups = {ApiCredentialsCheck.class, BasicCredentialsCheck.class})
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY, groups = {
        ApiCredentialsCheck.class, BasicCredentialsCheck.class})
    private String password;

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String clientId;

    @NotNull
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY)
    private String clientSecret;

    @Min(value = 0)
    private int expirationInSec;
    
    @NotNull(groups = {RefreshTokenCredentialsCheck.class})
    @Pattern(regexp = RegexPatterns.NOT_EMPTY, message = MessageTexts.NOT_EMPTY, groups = {
        RefreshTokenCredentialsCheck.class})
    private String refreshToken;

    public AuthCredentials() {
        // Needed by JAXB
    }

    public String getUsername() {
        return username;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public int getExpirationInSec() {
        return expirationInSec;
    }

    public void setExpirationInSec(int expirationInSec) {
        this.expirationInSec = expirationInSec;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }
    
    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
