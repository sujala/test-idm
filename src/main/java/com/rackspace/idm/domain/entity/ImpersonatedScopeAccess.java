package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 3/14/12
 * Time: 12:14 PM
 * To change this template use File | Settings | File Templates.
 */
@Data
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS,requestAllAttributes=true)
public class ImpersonatedScopeAccess extends ScopeAccess {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute=LdapRepository.ATTR_RACKER_ID, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String rackerId;

    @LDAPField(attribute=LdapRepository.ATTR_UID, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String username;

    @LDAPField(attribute=LdapRepository.ATTR_USER_RS_ID, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String userRsId;

    @Deprecated
    @LDAPField(attribute=LdapRepository.ATTR_IMPERSONATING_USERNAME, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String impersonatingUsername;

    @LDAPField(attribute=LdapRepository.ATTR_IMPERSONATING_RS_ID, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String rsImpersonatingRsId;

    @LDAPField(attribute=LdapRepository.ATTR_IMPERSONATING_TOKEN, objectClass=LdapRepository.OBJECTCLASS_IMPERSONATEDSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String impersonatingToken;

    @Override
    @LDAPGetter(attribute=LdapRepository.ATTR_ACCESS_TOKEN, inRDN=true, filterUsage=FilterUsage.ALWAYS_ALLOWED)
    public String getAccessTokenString() {
        return super.getAccessTokenString();
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

    public DateTime getUserPasswordExpirationDate() {
        return userPasswordExpirationDate;
    }

    public void setUserPasswordExpirationDate(DateTime userPasswordExpirationDate) {
        this.userPasswordExpirationDate = userPasswordExpirationDate;
    }

    @Override
    public String getAuditContext() {
        if(StringUtils.isNotBlank(this.getRackerId())) {
            final String format = "User(rackerId=%s,impersonating=%s)";
            return String.format(format, this.getRackerId(), this.getImpersonatingUsername());
        } else {
            final String format = "User(userRsId=%s,impersonating=%s)";
            return String.format(format, this.getUserRsId(), this.getImpersonatingUsername());
        }
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

    public void setLdapEntry(ReadOnlyEntry ldapEntry) {
        this.ldapEntry = ldapEntry;
    }
}
