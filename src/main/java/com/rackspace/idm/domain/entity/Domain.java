package com.rackspace.idm.domain.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.annotation.DeleteNullValues;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;
import org.dozer.Mapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Data
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_DOMAIN, auxiliaryClass = LdapRepository.OBJECTCLASS_METADATA)
public class Domain implements Auditable, UniqueId, Metadata {
    private static final Logger logger = LoggerFactory.getLogger(Domain.class);

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

    @LDAPField(attribute = LdapRepository.ATTR_RS_RACKSPACE_CUSTOMER_NUMBER, objectClass = LdapRepository.OBJECTCLASS_DOMAIN, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String rackspaceCustomerNumber;

    @LDAPField(attribute = LdapRepository.ATTR_PASSWORD_POLICY, objectClass = LdapRepository.OBJECTCLASS_DOMAIN, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private byte[] internalPasswordPolicy;

    @DeleteNullValues
    @LDAPField(attribute=LdapRepository.ATTR_USER_ADMIN_DN, objectClass=LdapRepository.OBJECTCLASS_DOMAIN, filterUsage=FilterUsage.ALWAYS_ALLOWED)
    private DN userAdminDN;

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

    public void setTenantIds(String[] tenantIDs) {
        if (tenantIDs == null) {
            this.tenantIds = null;
        } else {
            this.tenantIds = Arrays.copyOf(tenantIDs, tenantIDs.length);
        }
    }

    @Deprecated
    public String[] getTenantIds() {
        return this.tenantIds;
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

    public PasswordPolicy getPasswordPolicy() {
        return PasswordPolicy.fromBytes(internalPasswordPolicy);
    }

    public void setPasswordPolicy(PasswordPolicy policy) throws JsonProcessingException {
        if (policy == null) {
            internalPasswordPolicy = null;
        } else {
            internalPasswordPolicy = policy.toJsonBytes();
        }
    }

    public String getUserAdminId() {
        return userAdminDN == null ? null : userAdminDN.getRDNString().split("=")[1];
    }
}
