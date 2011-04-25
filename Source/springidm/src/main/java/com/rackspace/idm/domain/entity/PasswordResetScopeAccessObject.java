package com.rackspace.idm.domain.entity;

import java.util.Date;

import org.joda.time.DateTime;

import com.rackspace.idm.domain.entity.AccessToken.IDM_SCOPE;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;

@LDAPObject(structuralClass="passwordResetScopeAccess")
public class PasswordResetScopeAccessObject extends ScopeAccessObject{

    @LDAPField(attribute="accessToken", objectClass="passwordResetScopeAccess", inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String accessTokenString;
    
    @LDAPField(attribute="accesTokenExp", objectClass="passwordResetScopeAccess", inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private Date accessTokenExp;
    
    @LDAPField(attribute="uid", objectClass="passwordResetScopeAccess", inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String username;
    
    @LDAPField(attribute="userRCN", objectClass="passwordResetScopeAccess", inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String userRCN;
    
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
    public AccessToken getAccessToken() {
        BaseUser user = new BaseUser(getUsername(), getUserRCN());
        BaseClient client = new BaseClient(getClientId(), getClientRCN());
        AccessToken token = new AccessToken(getAccessTokenString(), new DateTime(
            getAccessTokenExp()), user, client, IDM_SCOPE.SET_PASSWORD);
        return token;
    }
}
