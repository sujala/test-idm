package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.*;
import org.joda.time.DateTime;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.Date;

@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_USERSCOPEACCESS,requestAllAttributes=true)
public class UserScopeAccess extends ScopeAccess implements HasRefreshToken {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute=LdapRepository.ATTR_REFRESH_TOKEN, objectClass=LdapRepository.OBJECTCLASS_USERSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String refreshTokenString;

    @LDAPField(attribute=LdapRepository.ATTR_REFRESH_TOKEN_EXP, objectClass=LdapRepository.OBJECTCLASS_USERSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private Date refreshTokenExp;

    @LDAPField(attribute=LdapRepository.ATTR_UID, objectClass=LdapRepository.OBJECTCLASS_USERSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String username;

    @LDAPField(attribute=LdapRepository.ATTR_USER_RS_ID, objectClass=LdapRepository.OBJECTCLASS_USERSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String userRsId;

    @LDAPField(attribute=LdapRepository.ATTR_USER_RCN, objectClass=LdapRepository.OBJECTCLASS_USERSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String userRCN;

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

    private DateTime userPasswordExpirationDate;
    
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

    @Override
    public void setRefreshTokenExpired() {
        this.refreshTokenExp = new DateTime().minusDays(1).toDate();
    }

    public DateTime getUserPasswordExpirationDate() {
        return userPasswordExpirationDate;
    }

    public void setUserPasswordExpirationDate(DateTime userPasswordExpirationDate) {
        this.userPasswordExpirationDate = userPasswordExpirationDate;
    }

    public boolean isAccessTokenWithinRefreshWindow(int refreshTokenWindow){
        DateTime accessToken = new DateTime(this.getAccessTokenExp());
        Date refreshWindowStart = accessToken.minusHours(refreshTokenWindow).toDate();
        Date now = new DateTime().toDate();
        return now.after(refreshWindowStart);
    }

    @Override
    public boolean isRefreshTokenExpired(DateTime time) {
        return StringUtils.isBlank(this.refreshTokenString)
        || this.refreshTokenExp == null
        || new DateTime(this.refreshTokenExp).isBefore(time);
    }

    @Override
    public String getAuditContext() {
        final String format = "User(username=%s,customerId=%s)";
        return String.format(format, this.getUsername(), this.getUserRCN());
    }

    public void setUserRsId(String userRsId) {
        this.userRsId = userRsId;
    }

    public String getUserRsId() {
        return userRsId;
    }

    public void setLdapEntry(ReadOnlyEntry ldapEntry) {
        this.ldapEntry = ldapEntry;
    }

    public ReadOnlyEntry getLDAPEntry() {
        return ldapEntry;
    }
}
