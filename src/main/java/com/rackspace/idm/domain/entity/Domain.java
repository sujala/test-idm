package com.rackspace.idm.domain.entity;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import org.dozer.Mapping;

import java.util.Arrays;

@Data
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_DOMAIN)
public class Domain implements Auditable, UniqueId {

    @LDAPDNField
    private String uniqueId;

    @Mapping("id")
    @LDAPField(attribute = LdapRepository.ATTR_ID, objectClass = LdapRepository.OBJECTCLASS_DOMAIN, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String domainId;

    @LDAPField(attribute = LdapRepository.ATTR_NAME, objectClass = LdapRepository.OBJECTCLASS_DOMAIN, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String name;

    @Mapping("enabled")
    @LDAPField(attribute = LdapRepository.ATTR_ENABLED, objectClass = LdapRepository.OBJECTCLASS_DOMAIN, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private Boolean enabled;

    @LDAPField(attribute = LdapRepository.ATTR_DESCRIPTION, objectClass = LdapRepository.OBJECTCLASS_DOMAIN, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String description;

    @LDAPField(attribute = LdapRepository.ATTR_TENANT_RS_ID, objectClass = LdapRepository.OBJECTCLASS_DOMAIN, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String[] tenantIds;

    @LDAPField(attribute = LdapRepository.ATTR_MULTIFACTOR_DOMAIN_ENFORCEMENT_LEVEL, objectClass = LdapRepository.OBJECTCLASS_DOMAIN, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String domainMultiFactorEnforcementLevel;

    @LDAPField(attribute = LdapRepository.ATTR_SESSION_INACTIVITY_TIMEOUT, objectClass = LdapRepository.OBJECTCLASS_DOMAIN, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String sessionInactivityTimeout;

    public void setTenantIds(String[] tenantIDs) {
        if (tenantIDs == null) {
            this.tenantIds = null;
        } else {
            this.tenantIds = Arrays.copyOf(tenantIDs, tenantIDs.length);
        }
    }

    @Override
    public String getAuditContext() {
        String format = "domainId=%s";
        return String.format(format, getDomainId());
    }

    @Override
    public String toString() {
        return getAuditContext();
    }

    public String getDomainMultiFactorEnforcementLevelIfNullWillReturnOptional() {
        return domainMultiFactorEnforcementLevel == null ? GlobalConstants.DOMAIN_MULTI_FACTOR_ENFORCEMENT_LEVEL_OPTIONAL : domainMultiFactorEnforcementLevel;
    }
}
