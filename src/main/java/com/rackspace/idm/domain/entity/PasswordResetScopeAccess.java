package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import org.joda.time.DateTime;

@Data
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_PASSWORDRESETSCOPEACCESS ,requestAllAttributes=true)
public class PasswordResetScopeAccess extends ScopeAccess {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute=LdapRepository.ATTR_UID, objectClass=LdapRepository.OBJECTCLASS_PASSWORDRESETSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String username;
    
    @LDAPField(attribute=LdapRepository.ATTR_USER_RS_ID, objectClass=LdapRepository.OBJECTCLASS_PASSWORDRESETSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String userRsId;

    @LDAPField(attribute=LdapRepository.ATTR_USER_RCN, objectClass=LdapRepository.OBJECTCLASS_PASSWORDRESETSCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String userRCN;

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

    @Override
    public String getAuditContext() {
        final String format = "PasswordReset(username=%s)";
        return String.format(format, getUsername());
    }
}
