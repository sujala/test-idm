package com.rackspace.idm.domain.entity;

import java.util.Date;

import org.joda.time.DateTime;

import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;

@LDAPObject(structuralClass = "clientScopeAccess")
public class ClientScopeAccessObject extends ScopeAccessObject implements hasAccessToken {

    @LDAPField(attribute = "accessToken", objectClass = "clientScopeAccess", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String accessTokenString;

    @LDAPField(attribute = "accesTokenExp", objectClass = "clientScopeAccess", inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private Date accessTokenExp;

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
    public void setAccessTokenExpired() {
        this.accessTokenExp = new DateTime().minusDays(1).toDate();
    }
}
