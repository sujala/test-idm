package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.*;
import org.joda.time.DateTime;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 3/14/12
 * Time: 12:14 PM
 * To change this template use File | Settings | File Templates.
 */
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS,requestAllAttributes=true)
public class ImpersonatedScopeAccess extends ScopeAccess implements HasAccessToken {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute= LdapRepository.ATTR_ACCESS_TOKEN, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage= FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String accessTokenString;

    @LDAPField(attribute=LdapRepository.ATTR_ACCESS_TOKEN_EXP, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private Date accessTokenExp;

    @LDAPField(attribute=LdapRepository.ATTR_RACKER_ID, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String rackerId;

    @LDAPField(attribute=LdapRepository.ATTR_UID, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String username;

    @LDAPField(attribute=LdapRepository.ATTR_IMPERSONATING_USERNAME, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=true, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String impersonatingUsername;

    @LDAPField(attribute=LdapRepository.ATTR_IMPERSONATING_TOKEN, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String impersonatingToken;

    @Override
    @LDAPGetter(attribute=LdapRepository.ATTR_CLIENT_ID, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED)
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

    private DateTime userPasswordExpirationDate;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public DateTime getUserPasswordExpirationDate() {
        return userPasswordExpirationDate;
    }

    public void setUserPasswordExpirationDate(DateTime userPasswordExpirationDate) {
        this.userPasswordExpirationDate = userPasswordExpirationDate;
    }

    @Override
    public boolean isAccessTokenExpired(DateTime time) {
        return StringUtils.isBlank(this.accessTokenString)
        || this.accessTokenExp == null
        || new DateTime(this.accessTokenExp).isBefore(time);
    }

    @Override
    public String getAuditContext() {
        final String format = "User(username=%s,impersonating=%s)";
        return String.format(format, this.getUsername(), this.getImpersonatingUsername());
    }

    public String getRackerId() {
        return rackerId;
    }

    public void setRackerId(String rackerId) {
        this.rackerId = rackerId;
    }

    public String getImpersonatingUsername() {
        return impersonatingUsername;
    }

    public void setImpersonatingUsername(String impersonatingUsername) {
        this.impersonatingUsername = impersonatingUsername;
    }

    public String getImpersonatingToken() {
        return impersonatingToken;
    }

    public void setImpersonatingToken(String impersonatingToken) {
        this.impersonatingToken = impersonatingToken;
    }
}
