package com.rackspace.idm.domain.entity;

import java.util.Date;

import org.joda.time.DateTime;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;

@LDAPObject(structuralClass="rackerScopeAccess")
public class RackerScopeAccessObject extends ScopeAccessObject implements hasAccessToken, hasRefreshToken {

    @LDAPField(attribute="accessToken", objectClass="rackerScopeAccess", inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String accessTokenString;
    
    @LDAPField(attribute="accesTokenExp", objectClass="rackerScopeAccess", inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private Date accessTokenExp;
    
    @LDAPField(attribute="refreshToken", objectClass="rackerScopeAccess", inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String refreshTokenString;
    
    @LDAPField(attribute="refreshTokenExp", objectClass="rackerScopeAccess", inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private Date refreshTokenExp;
    
    @LDAPField(attribute="rackerId", objectClass="rackerScopeAccess", inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String rackerId;
    
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
    
    public String getRackerId() {
        return rackerId;
    }
    
    public void setRackerId(String rackerId) {
        this.rackerId = rackerId;
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
        String format = "Racker(rackerId=%s)";
        return String.format(format, getRackerId());
    }
}
