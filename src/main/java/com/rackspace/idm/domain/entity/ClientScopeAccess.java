package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPGetter;
import com.unboundid.ldap.sdk.persist.LDAPObject;

@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_CLIENTSCOPEACCESS,requestAllAttributes=true)
public class ClientScopeAccess extends ScopeAccess {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

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

    public void setLdapEntry(ReadOnlyEntry ldapEntry) {
        this.ldapEntry = ldapEntry;
    }

    @Override
    public String getAuditContext() {
        final String format = "Client(clientId=%s,customerId=%s)";
        return String.format(format, getClientId(), getClientRCN());
    }
}
