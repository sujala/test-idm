package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.KeyVersion;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
@LDAPObject(structuralClass= LdapRepository.OBJECTCLASS_KEY_DESCRIPTOR, auxiliaryClass = LdapRepository.OBJECTCLASS_METADATA)
public class LdapKeyVersion implements KeyVersion, UniqueId, Metadata {

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute= LdapRepository.ATTR_KEY_VERSION,
            objectClass=LdapRepository.OBJECTCLASS_KEY_DESCRIPTOR,
            inRDN=true,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true
    )
    private Integer version;

    @LDAPField(attribute= LdapRepository.ATTR_CREATED_DATE,
            objectClass=LdapRepository.OBJECTCLASS_KEY_DESCRIPTOR,
            filterUsage=FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=false
    )
    private Date created;

    @LDAPField(attribute= LdapRepository.ATTR_KEY_DATA,
            objectClass=LdapRepository.OBJECTCLASS_KEY_DESCRIPTOR,
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
}
