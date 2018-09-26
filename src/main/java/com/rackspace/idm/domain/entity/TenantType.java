package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(exclude={"uniqueId"})
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_TENANT_TYPE, auxiliaryClass = LdapRepository.OBJECTCLASS_METADATA)
public class TenantType implements Auditable, UniqueId, Metadata {

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_TENANT_TYPE, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = LdapRepository.OBJECTCLASS_TENANT_TYPE, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String description;

    @LDAPField(attribute=LdapRepository.ATTR_METADATA_ATTRIBUTE,
               objectClass=LdapRepository.OBJECTCLASS_METADATA,
               filterUsage=FilterUsage.CONDITIONALLY_ALLOWED
    )
    private Set<String> metadata;

    public Set<String> getMedatadata() {
        if (metadata == null) {
            metadata = new HashSet<String>();
        }
        return metadata;
    }

    @Override
    public String getAuditContext() {
        String format = "tenantTypeId=%s";
        return String.format(format, getName());
    }
}
