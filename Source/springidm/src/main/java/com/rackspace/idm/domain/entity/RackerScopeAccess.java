package com.rackspace.idm.domain.entity;

import java.util.Date;

import org.joda.time.DateTime;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPGetter;
import com.unboundid.ldap.sdk.persist.LDAPObject;

@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_RACKERSCOPEACCESS,requestAllAttributes=true)
public class RackerScopeAccess extends ScopeAccess implements hasAccessToken, hasRefreshToken {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute=LdapRepository.ATTR_ACCESS_TOKEN, objectClass=LdapRepository.OBJECTCLASS_RACKERSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String accessTokenString;

    @LDAPField(attribute=LdapRepository.ATTR_ACCESS_TOKEN_EXP, objectClass=LdapRepository.OBJECTCLASS_RACKERSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private Date accessTokenExp;

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
        final String format = "Racker(rackerId=%s)";
        return String.format(format, getRackerId());
    }
}
