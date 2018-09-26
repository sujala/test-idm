package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_PATTERN, auxiliaryClass = LdapRepository.OBJECTCLASS_METADATA)
public class Pattern implements UniqueId, Metadata {

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute = LdapRepository.ATTR_ID, objectClass = LdapRepository.OBJECTCLASS_PATTERN, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String id;

    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_PATTERN, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_REGEX, objectClass = LdapRepository.OBJECTCLASS_PATTERN, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String regex;

    @LDAPField(attribute = LdapRepository.ATTR_ERRMSG, objectClass = LdapRepository.OBJECTCLASS_PATTERN, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String errMsg;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = LdapRepository.OBJECTCLASS_PATTERN, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
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
}
