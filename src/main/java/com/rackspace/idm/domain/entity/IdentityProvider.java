package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;

@Data
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER)
public class IdentityProvider implements Auditable, UniqueId {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute = LdapRepository.ATTR_OU, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, inRDN = true, requiredForEncode = true)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_URI, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = true)
    private String uri;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = false)
    private String description;

    @LDAPField(attribute = LdapRepository.ATTR_PUBLIC_KEY, objectClass = LdapRepository.OBJECTCLASS_EXTERNALPROVIDER, requiredForEncode = false)
    private String publicCertificate;

    @Override
    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    @Override
    public String getAuditContext() {
        String format = "identityProviderName=%s";
        return String.format(format, getName());
    }

    @Override
    public String toString() {
        return getAuditContext();
    }
}
