package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPGetter;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_CLIENTSCOPEACCESS,requestAllAttributes=true)
public class ClientScopeAccess extends ScopeAccess {

    // This field must me mapped on every subclass (UnboundID LDAP SDK v2.3.6 limitation)
    @LDAPDNField
    private String uniqueId;

    @Override
    @LDAPGetter(attribute=LdapRepository.ATTR_ACCESS_TOKEN, inRDN=true, filterUsage=FilterUsage.ALWAYS_ALLOWED)
    public String getAccessTokenString() {
        return super.getAccessTokenString();
    }

    @Override
    public String getAuditContext() {
        final String format = "Client(clientId=%s,customerId=%s)";
        return String.format(format, getClientId(), getClientRCN());
    }

}
