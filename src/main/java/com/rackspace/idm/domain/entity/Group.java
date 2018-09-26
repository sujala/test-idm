package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(exclude={"uniqueId"})
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_CLOUDGROUP,
        postEncodeMethod="doPostEncode", auxiliaryClass = LdapRepository.OBJECTCLASS_METADATA)
public class Group implements Auditable, UniqueId, Metadata {

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute=LdapRepository.ATTR_ID,
            objectClass=LdapRepository.OBJECTCLASS_CLOUDGROUP,
            inRDN=true,
            filterUsage= FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true)
    private String groupId;

    @LDAPField(attribute=LdapRepository.ATTR_GROUP_NAME,
            objectClass=LdapRepository.OBJECTCLASS_CLOUDGROUP,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED,
            requiredForEncode=true)
    private String name;

    @LDAPField(attribute=LdapRepository.ATTR_DESCRIPTION,
            objectClass=LdapRepository.OBJECTCLASS_CLOUDGROUP,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String description;

    @Override
    public String getAuditContext() {
        return String.format("groupId=%s", groupId);
    }

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

    private void doPostEncode(final Entry entry) throws LDAPPersistException {
        String tenantIds = entry.getAttributeValue(LdapRepository.ATTR_DESCRIPTION);
        if (tenantIds.length() == 0) {
            entry.removeAttribute(LdapRepository.ATTR_DESCRIPTION);
        }
    }
}
