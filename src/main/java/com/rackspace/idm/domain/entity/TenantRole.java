package com.rackspace.idm.domain.entity;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleTypeEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Types;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.modules.usergroups.Constants;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.RDN;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.utils.CloneUtils;
import org.dozer.Mapping;

import java.util.HashSet;
import java.util.Set;

@Data
@EqualsAndHashCode(exclude={"uniqueId"})
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_TENANT_ROLE,
        postEncodeMethod="doPostEncode")
public class TenantRole implements Auditable, UniqueId {

    @LDAPDNField
    private String uniqueId;

    @Mapping("id")
    @LDAPField(attribute = LdapRepository.ATTR_ROLE_RS_ID, objectClass = LdapRepository.OBJECTCLASS_TENANT_ROLE, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String roleRsId;

    @LDAPField(attribute = LdapRepository.ATTR_TENANT_RS_ID, objectClass = LdapRepository.OBJECTCLASS_TENANT_ROLE, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private Set<String> tenantIds = new HashSet<String>();

    @Mapping("serviceId")
    @LDAPField(attribute = LdapRepository.ATTR_CLIENT_ID, objectClass = LdapRepository.OBJECTCLASS_TENANT_ROLE, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String clientId;

    @LDAPField(attribute = LdapRepository.ATTR_USER_RS_ID, objectClass = LdapRepository.OBJECTCLASS_TENANT_ROLE, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String userId;

    private String name;
    private String description;
    private RoleTypeEnum roleType;
    private Types types;

    @Override
    public String toString() {
        return getAuditContext();
    }

    public Boolean getPropagate() {
        return roleType != null && roleType.equals(RoleTypeEnum.PROPAGATE);
    }

    @Override
    public String getAuditContext() {
        String format = "roleRsId=%s,clientId=%s,tenantRsIds=%s";
        return String.format(format, getRoleRsId(), getClientId(), getTenantIds());
    }

    public Set<String> getTenantIds() {
        if (tenantIds == null) {
            tenantIds = new HashSet<String>();
        }
        return tenantIds;
    }

    private void doPostEncode(final Entry entry) throws LDAPPersistException {
        String[] tenantIds = entry.getAttributeValues(LdapRepository.ATTR_TENANT_RS_ID);
        if (tenantIds != null && tenantIds.length == 0) {
            entry.removeAttribute(LdapRepository.ATTR_TENANT_RS_ID);
        }
    }

    public boolean isUserGroupAssignedRole() {
        return StringUtils.endsWithIgnoreCase(getUniqueId(), Constants.USER_GROUP_BASE_DN);
    }

    public boolean isUserAssignedRole() {
        return !isUserGroupAssignedRole();
    }

    /**
     * Retrieve the userId or groupId assigned the role
     *
     * @return
     */
    public String getIdOfEntityAssignedRole() {
        try{
            DN dn = new DN(getUniqueId());
            RDN rdn = dn.getParent().getParent().getRDN();
            if(rdn.hasAttribute("rsId")){
                String id = rdn.getAttributeValues()[0];
                if(!StringUtils.isBlank(id)){
                    return id;
                }
            }
        }catch (Exception ignored){
        }
        return null;
    }

}
