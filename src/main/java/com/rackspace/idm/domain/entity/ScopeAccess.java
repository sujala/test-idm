package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;

@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_SCOPEACCESS)
public class ScopeAccess implements Auditable {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute=LdapRepository.ATTR_CLIENT_ID, objectClass=LdapRepository.OBJECTCLASS_SCOPEACCESS, inRDN=true, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String clientId;

    @LDAPField(attribute=LdapRepository.ATTR_CLIENT_RCN, objectClass=LdapRepository.OBJECTCLASS_SCOPEACCESS, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String clientRCN;


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

    public String getParentDN() throws LDAPException {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getParentDNString();
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
