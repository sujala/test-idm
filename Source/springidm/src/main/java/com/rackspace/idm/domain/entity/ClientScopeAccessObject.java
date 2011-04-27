package com.rackspace.idm.domain.entity;

import java.util.Date;

import org.joda.time.DateTime;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPGetter;
import com.unboundid.ldap.sdk.persist.LDAPObject;

@LDAPObject(structuralClass="clientScopeAccess",requestAllAttributes=true)
public class ClientScopeAccessObject extends ScopeAccessObject implements hasAccessToken {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute="accessToken", objectClass="clientScopeAccess", inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String accessTokenString;

    @LDAPField(attribute="accessTokenExp", objectClass="clientScopeAccess", inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)

    private Date accessTokenExp;

    @LDAPField(attribute="tokenScope", objectClass="clientScopeAccess", inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String tokenScope;

    @Override
    @LDAPGetter(attribute="clientId", inRDN=true)
    public String getClientId() {
        return super.getClientId();
    }

    @Override
    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        }
        else {
            return ldapEntry.getDN();
        }
    }

    public String getTokenScope() {
        return tokenScope;
    }

    public void setTokenScope(String tokenScope) {
        this.tokenScope = tokenScope;
    }



    @Override
    public String getAccessTokenString() {
        return accessTokenString;
    }

    @Override
    public void setAccessTokenString(String accessTokenString) {
        this.accessTokenString = accessTokenString;
    }

    @Override
    public Date getAccessTokenExp() {
        return accessTokenExp;
    }

    @Override
    public void setAccessTokenExp(Date accessTokenExp) {
        this.accessTokenExp = accessTokenExp;
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
    public String getAuditContext() {
        final String format = "Client(clientId=%s,customerId=%s)";
        return String.format(format, getClientId(), getClientRCN());
    }
}
