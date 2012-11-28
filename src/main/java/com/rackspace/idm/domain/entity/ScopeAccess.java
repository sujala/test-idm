package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import org.joda.time.DateTime;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.Date;

@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_SCOPEACCESS)
public class ScopeAccess implements Auditable, HasAccessToken {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute=LdapRepository.ATTR_CLIENT_ID, objectClass=LdapRepository.OBJECTCLASS_SCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String clientId;

    @LDAPField(attribute=LdapRepository.ATTR_CLIENT_RCN, objectClass=LdapRepository.OBJECTCLASS_SCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String clientRCN;

    @LDAPField(attribute = LdapRepository.ATTR_ACCESS_TOKEN, objectClass=LdapRepository.OBJECTCLASS_SCOPEACCESS, inRDN=true, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String accessTokenString;

    @LDAPField(attribute = LdapRepository.ATTR_ACCESS_TOKEN_EXP, objectClass=LdapRepository.OBJECTCLASS_SCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private Date accessTokenExp;

    public ScopeAccess() {}

    public ReadOnlyEntry getLDAPEntry() {
        return ldapEntry;
    }

    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        }
        else {
            return ldapEntry.getDN();
        }
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientRCN() {
        return clientRCN;
    }

    public void setClientRCN(String clientRCN) {
        this.clientRCN = clientRCN;
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

    public boolean isAccessTokenWithinRefreshWindow(int refreshTokenWindow){
        DateTime accessToken = new DateTime(this.getAccessTokenExp());
        Date refreshWindowStart = accessToken.minusHours(refreshTokenWindow).toDate();
        Date now = new DateTime().toDate();
        return now.after(refreshWindowStart);
    }

    @Override
    public String getAuditContext() {
        final String format = "ScopeAccess(clientId=%s)";
        return String.format(format, getClientId());
    }

    public void setLdapEntry(ReadOnlyEntry ldapEntry) {
        this.ldapEntry = ldapEntry;
    }

    @Override
    public String toString() {
        return getAuditContext() ;
    }
}
