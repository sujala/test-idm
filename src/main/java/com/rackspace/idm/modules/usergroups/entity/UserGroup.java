package com.rackspace.idm.modules.usergroups.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.Auditable;
import com.rackspace.idm.modules.usergroups.Constants;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a user group within the LDAP directory. Extending groupOfNames LDAP class for future expansion options when
 * delegation is introduced. Not currently using as group membership (users) is set on the rsPerson/rsFederatedPerson
 * entries directly due to potential turnover of group membership.
 */
@Getter
@Setter
@LDAPObject(structuralClass = Constants.OBJECTCLASS_USER_GROUP, superiorClass={ "groupOfNames", "top" })
public class UserGroup implements Auditable, UniqueId {
    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute = LdapRepository.ATTR_ID, objectClass = Constants.OBJECTCLASS_USER_GROUP, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String id;

    @LDAPField(attribute = LdapRepository.ATTR_DOMAIN_ID, objectClass = Constants.OBJECTCLASS_USER_GROUP, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String domainId;

    @LDAPField(attribute = LdapRepository.ATTR_COMMON_NAME, objectClass = Constants.OBJECTCLASS_USER_GROUP, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String name;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = Constants.OBJECTCLASS_USER_GROUP, inRDN = false, filterUsage = FilterUsage.CONDITIONALLY_ALLOWED, requiredForEncode = false)
    private String description;

    @Override
    public String getAuditContext() {
        return String.format("id=%s", id);
    }
}
