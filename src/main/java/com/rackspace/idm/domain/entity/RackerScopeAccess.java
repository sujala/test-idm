package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.*;
import org.joda.time.DateTime;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.Date;

@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_RACKERSCOPEACCESS,requestAllAttributes=true)
public class RackerScopeAccess extends ScopeAccess implements HasRefreshToken {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute=LdapRepository.ATTR_REFRESH_TOKEN, objectClass=LdapRepository.OBJECTCLASS_RACKERSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String refreshTokenString;

    @LDAPField(attribute=LdapRepository.ATTR_REFRESH_TOKEN_EXP, objectClass=LdapRepository.OBJECTCLASS_RACKERSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private Date refreshTokenExp;

    @LDAPField(attribute=LdapRepository.ATTR_RACKER_ID, objectClass=LdapRepository.OBJECTCLASS_RACKERSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String rackerId;

    @Override
    @LDAPGetter(attribute=LdapRepository.ATTR_CLIENT_ID, inRDN=true, filterUsage=FilterUsage.ALWAYS_ALLOWED)
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

    @Override
    public String getRefreshTokenString() {
        return refreshTokenString;
    }

    @Override
    public void setRefreshTokenString(String refreshTokenString) {
        this.refreshTokenString = refreshTokenString;
    }

    @Override
    public Date getRefreshTokenExp() {
        return refreshTokenExp;
    }

    @Override
    public void setRefreshTokenExp(Date refreshTokenExp) {
        this.refreshTokenExp = refreshTokenExp;
    }

    public String getRackerId() {
        return rackerId;
    }

    public void setRackerId(String rackerId) {
        this.rackerId = rackerId;
    }

    @Override
    public void setRefreshTokenExpired() {
        this.refreshTokenExp = new DateTime().minusDays(1).toDate();
    }

    @Override
    public boolean isRefreshTokenExpired(DateTime time) {
        return StringUtils.isBlank(this.refreshTokenString)
        || this.refreshTokenExp == null
        || new DateTime(this.refreshTokenExp).isBefore(time);
    }

    @Override
    public String getAuditContext() {
        final String format = "Racker(rackerId=%s)";
        return String.format(format, getRackerId());
    }

    public void setLdapEntry(ReadOnlyEntry ldapEntry) {
        this.ldapEntry = ldapEntry;
    }
}
