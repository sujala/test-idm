package com.rackspace.idm.domain.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.service.IdentityUserTypeEnum;
import com.rackspace.idm.domain.service.RoleLevelEnum;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import org.dozer.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Data
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_CLIENT_ROLE, postEncodeMethod="doPostEncode", auxiliaryClass = LdapRepository.OBJECTCLASS_METADATA)
public class ClientRole implements Auditable, UniqueId, Metadata {
    private static final Logger log = LoggerFactory.getLogger(ClientRole.class);

	public static final String SUPER_ADMIN_ROLE = "3";
	public static final String RACKER = "RackerVirtualRole";

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute=LdapRepository.ATTR_ID, objectClass=LdapRepository.OBJECTCLASS_CLIENT_ROLE, inRDN=true, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String id;

    @LDAPField(attribute=LdapRepository.ATTR_NAME, objectClass=LdapRepository.OBJECTCLASS_CLIENT_ROLE, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String name;

    @LDAPField(attribute=LdapRepository.ATTR_CLIENT_ID, objectClass=LdapRepository.OBJECTCLASS_CLIENT_ROLE, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=true)
    private String clientId;

    @LDAPField(attribute=LdapRepository.ATTR_DESCRIPTION, objectClass=LdapRepository.OBJECTCLASS_CLIENT_ROLE, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String description;

    @LDAPField(attribute=LdapRepository.ATTR_RS_WEIGHT, objectClass=LdapRepository.OBJECTCLASS_CLIENT_ROLE, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private int rsWeight;

    @LDAPField(attribute = LdapRepository.ATTR_ASSIGNMENT, objectClass = LdapRepository.OBJECTCLASS_CLIENT_ROLE, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String assignmentType;

    @LDAPField(attribute=LdapRepository.ATTR_RS_TYPE, objectClass=LdapRepository.OBJECTCLASS_CLIENT_ROLE, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private String roleType;

    @LDAPField(attribute=LdapRepository.ATTR_RS_TENANT_TYPE, objectClass=LdapRepository.OBJECTCLASS_CLIENT_ROLE, inRDN=false, filterUsage=FilterUsage.ALWAYS_ALLOWED, requiredForEncode=false)
    private HashSet<String> tenantTypes;

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

    public IdentityUserTypeEnum getAdministratorRole() {
        RoleLevelEnum roleLevelEnum = RoleLevelEnum.fromInt(rsWeight);

        IdentityUserTypeEnum administratorRole = null;
        if (roleLevelEnum == RoleLevelEnum.LEVEL_50) {
            administratorRole = IdentityUserTypeEnum.SERVICE_ADMIN;
        } else if (roleLevelEnum == RoleLevelEnum.LEVEL_500) {
            administratorRole = IdentityUserTypeEnum.IDENTITY_ADMIN;
        } else if (roleLevelEnum == RoleLevelEnum.LEVEL_1000) {
            administratorRole = IdentityUserTypeEnum.USER_MANAGER;
        } else if (roleLevelEnum == IdentityUserTypeEnum.IDENTITY_ADMIN.getLevel()) {
            administratorRole = IdentityUserTypeEnum.SERVICE_ADMIN;
        } else if (roleLevelEnum == IdentityUserTypeEnum.USER_ADMIN.getLevel()) {
            administratorRole = IdentityUserTypeEnum.IDENTITY_ADMIN;
        } else if (roleLevelEnum == IdentityUserTypeEnum.USER_MANAGER.getLevel()) {
            administratorRole = IdentityUserTypeEnum.USER_ADMIN;
        } else if (roleLevelEnum == IdentityUserTypeEnum.DEFAULT_USER.getLevel()) {
            administratorRole = IdentityUserTypeEnum.USER_MANAGER;
        } else {
            log.error(String.format("Client role with id '%s' has an invalid weight of %d", id,  rsWeight));
            administratorRole = null;
        }
        return administratorRole;
    }

    public RoleTypeEnum getRoleType() {
        RoleTypeEnum result = RoleTypeEnum.STANDARD;
        if (roleType != null) {
            try {
                result = RoleTypeEnum.fromValue(roleType);
            } catch (Exception e) {
                // If LDAP contains invalid data, just override to be interpreted as STANDARD role
                result=RoleTypeEnum.STANDARD;
            }
        }

        return result;
    }

    /**
     * This is solely here to provide access to the non-enum set value to verify backwards compatibility. Do not use for
     * anything other than tests...
     * @return
     * @deprecated
     */
    @Deprecated
    public String getRawRoleType() {
        return roleType;
    }

    public void setRoleType(RoleTypeEnum type) {
        if (type != null) {
            roleType = type.value();
        } else {
            roleType = RoleTypeEnum.STANDARD.value();
        }
    }

    public Boolean getPropagate() {
        return RoleTypeEnum.PROPAGATE == getRoleType();
    }

    public RoleAssignmentEnum getAssignmentTypeAsEnum() {
        RoleAssignmentEnum result = RoleAssignmentEnum.BOTH;
        if (!StringUtils.isBlank(assignmentType)) {
            try {
                result = RoleAssignmentEnum.fromValue(assignmentType);
            } catch (IllegalArgumentException e) {
                // Eat and return BOTH
            }
        }
        return result;
    }

    /** +
     * Tenant types is only valid for RCN roles.
     * @return
     */
    public HashSet<String> getTenantTypes() {
        if (tenantTypes == null) {
            tenantTypes = new HashSet<String>();
        }
        return tenantTypes;
    }

    /**
     * This is called by unboundId library after the java object has been converted to an LDAP entry. Used to
     * modify the entry prior to saving/updating to ldap.
     * @param entry
     * @throws LDAPPersistException
     * @see <a href="https://docs.ldap.com/ldap-sdk/docs/persist/LDAPObject.html">https://docs.ldap.com/ldap-sdk/docs/persist/LDAPObject.html
     */
    private void doPostEncode(final Entry entry) throws LDAPPersistException {
        String[] tenantTypes = entry.getAttributeValues(LdapRepository.ATTR_RS_TENANT_TYPE);
        if (tenantTypes != null && tenantTypes.length == 0) {
            entry.removeAttribute(LdapRepository.ATTR_TENANT_RS_ID);
        }
    }

    @Override
    public String getAuditContext() {
        String format = "role=%s,clientId=%s";
        return String.format(format, getName(), getClientId());
    }

    @Override
    public String toString() {
        return getAuditContext();
    }
}
