package com.rackspace.idm.domain.entity;

import java.util.Date;

import org.joda.time.DateTime;

import com.rackspace.idm.domain.entity.AccessToken.IDM_SCOPE;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;

@LDAPObject(structuralClass = "clientScopeAccess")
public class ClientScopeAccessObject extends ScopeAccessObject {

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
    public AccessToken getAccessToken() {
        BaseClient client = new BaseClient(getClientId(), getClientRCN());
        AccessToken token = new AccessToken(getAccessTokenString(), new DateTime(
            getAccessTokenExp()), null, client, IDM_SCOPE.FULL);
        return token;
    }
}
