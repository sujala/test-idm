package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.Auditable;
import com.rackspace.idm.domain.entity.KeyMetadata;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@LDAPObject(structuralClass= LdapRepository.OBJECTCLASS_KEY_METADATA, auxiliaryClass = LdapRepository.OBJECTCLASS_METADATA)
public class LdapKeyMetadata implements KeyMetadata, UniqueId, Auditable, Metadata {

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute= LdapRepository.ATTR_COMMON_NAME,
            objectClass=LdapRepository.OBJECTCLASS_KEY_METADATA,
            inRDN=true,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true
    )
    private String name;

    @LDAPField(attribute= LdapRepository.ATTR_KEY_CREATED,
            objectClass=LdapRepository.OBJECTCLASS_KEY_METADATA,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=false
    )
    private Date created;

    @LDAPField(attribute= LdapRepository.ATTR_KEY_DATA,
            objectClass=LdapRepository.OBJECTCLASS_KEY_METADATA,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true
    )
    private String data;

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
        return LdapRepository.ATTR_COMMON_NAME + "=" + name;
    }

}
