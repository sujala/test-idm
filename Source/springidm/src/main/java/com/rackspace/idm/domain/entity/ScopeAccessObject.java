package com.rackspace.idm.domain.entity;

import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;

@LDAPObject(structuralClass="scopeAccess")
public class ScopeAccessObject implements Auditable {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute="clientId", objectClass="scopeAccess", inRDN=true, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String clientId;

    @LDAPField(attribute="clientRCN", objectClass="scopeAccess", inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String clientRCN;

    @LDAPField(attribute="objectClass", objectClass="scopeAccess", inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String[] objectClass;

    public ScopeAccessObject() {}

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
    public String getAuditContext() {
        final String format = "ScopeAccess(clientId=%s)";
        return String.format(format, getClientId());
    }
    
    @Override
    public String toString() {
        return getAuditContext() ;
    }
}
