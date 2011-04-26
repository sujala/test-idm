package com.rackspace.idm.domain.entity;

import java.util.Date;

import org.joda.time.DateTime;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;

@LDAPObject(structuralClass = "userScopeAccess")
public class UserScopeAccessObject extends ScopeAccessObject implements hasAccessToken, hasRefreshToken {

    @LDAPField(attribute = "accessToken", objectClass = "userScopeAccess", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String accessTokenString;

    @LDAPField(attribute = "accesTokenExp", objectClass = "userScopeAccess", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private Date accessTokenExp;

    @LDAPField(attribute = "refreshToken", objectClass = "userScopeAccess", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String refreshTokenString;

    @LDAPField(attribute = "refreshTokenExp", objectClass = "userScopeAccess", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private Date refreshTokenExp;

    @LDAPField(attribute = "uid", objectClass = "userScopeAccess", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String username;

    @LDAPField(attribute = "userRCN", objectClass = "userScopeAccess", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String userRCN;

    public String getRefreshTokenString() {
        return refreshTokenString;
    }

    public void setRefreshTokenString(String refreshTokenString) {
        this.refreshTokenString = refreshTokenString;
    }

    public Date getRefreshTokenExp() {
        return refreshTokenExp;
    }

    public void setRefreshTokenExp(Date refreshTokenExp) {
        this.refreshTokenExp = refreshTokenExp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserRCN() {
        return userRCN;
    }

    public void setUserRCN(String userRCN) {
        this.userRCN = userRCN;
    }

    public String getAccessTokenString() {
        return accessTokenString;
    }

    public void setAccessTokenString(String accessTokenString) {
        this.accessTokenString = accessTokenString;
    }

    public Date getAccessTokenExp() {
        return accessTokenExp;
    }

    public void setAccessTokenExp(Date accessTokenExp) {
        this.accessTokenExp = accessTokenExp;
    }

    @Override
    public void setRefreshTokenExpired() {
        this.refreshTokenExp = new DateTime().minusDays(1).toDate();
    }

    @Override
    public void setAccessTokenExpired() {
        this.accessTokenExp = new DateTime().minusDays(1).toDate();
    }
    
    @Override
    public boolean isAccessTokenExpired(DateTime time) {
        return StringUtils.isBlank(this.accessTokenString)
            || this.accessTokenExp == null
            || new DateTime(this.accessTokenExp).isBefore(time);
    }
    
    @Override
    public boolean isRefreshTokenExpired(DateTime time) {
        return StringUtils.isBlank(this.refreshTokenString)
            || this.refreshTokenExp == null
            || new DateTime(this.refreshTokenExp).isBefore(time);
    }
    
    @Override
    public String getAuditContext() {
        String format = "User(username=%s,customerId=%s)";
        return String.format(format, this.getUsername(), this.getUserRCN());
    }
}
