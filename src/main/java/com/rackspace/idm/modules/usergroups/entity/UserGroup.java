package com.rackspace.idm.modules.usergroups.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.Auditable;
import com.rackspace.idm.modules.usergroups.Constants;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents a user group within the LDAP directory. Extending groupOfNames LDAP class for future expansion options when
 * delegation is introduced. Not currently using as group membership (users) is set on the rsPerson/rsFederatedPerson
 * entries directly due to potential turnover of group membership.
 */
@Getter
@Setter
@LDAPObject(structuralClass = Constants.OBJECTCLASS_USER_GROUP, superiorClass={ "groupOfNames", "top" })
public class UserGroup implements Auditable, UniqueId {

    public static final String INVALID_GROUP_DN = "Group dn could not be parsed";

    private final Logger logger = LoggerFactory.getLogger(UserGroup.class);

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

    public DN getGroupDn() {
        try {
            return new DN(getUniqueId());
        } catch (LDAPException e) {
            String errmsg = INVALID_GROUP_DN;
            logger.error(errmsg);
            throw new IllegalArgumentException(errmsg);
        }
    }

}
