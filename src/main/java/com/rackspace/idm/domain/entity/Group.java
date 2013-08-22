package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(exclude={"ldapEntry"})
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_CLOUDGROUP,
        postEncodeMethod="doPostEncode")
public class Group implements Auditable, UniqueId {
    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

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

    @Override
    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    private void doPostEncode(final Entry entry) throws LDAPPersistException {
        String tenantIds = entry.getAttributeValue(LdapRepository.ATTR_DESCRIPTION);
        if (tenantIds.length() == 0) {
            entry.removeAttribute(LdapRepository.ATTR_DESCRIPTION);
        }
    }
}
