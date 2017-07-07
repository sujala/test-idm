package com.rackspace.idm.domain.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignmentEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import org.dozer.Mapping;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.HashSet;

@Data
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_CLIENT_ROLE, postEncodeMethod="doPostEncode")
public class ClientRole implements Auditable, UniqueId {

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
        return RoleTypeEnum.PROPAGATE.name().equalsIgnoreCase(roleType);
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
