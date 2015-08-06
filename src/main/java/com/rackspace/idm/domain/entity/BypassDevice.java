package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;
import java.util.Set;

@Getter
@Setter
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_BYPASS_DEVICE)
public class BypassDevice implements UniqueId, Auditable {

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute = LdapRepository.ATTR_ID,
            objectClass = LdapRepository.OBJECTCLASS_BYPASS_DEVICE,
            inRDN = true,
            filterUsage = FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode = true)
    private String id;

    @LDAPField(attribute = LdapRepository.ATTR_MULTIFACTOR_DEVICE_PIN_EXPIRATION,
            objectClass = LdapRepository.OBJECTCLASS_BYPASS_DEVICE,
            inRDN = false,
            filterUsage = FilterUsage.CONDITIONALLY_ALLOWED,
            requiredForEncode = false)
    private Date multiFactorDevicePinExpiration;

    @LDAPField(attribute = LdapRepository.ATTR_BYPASS_CODE,
            objectClass = LdapRepository.OBJECTCLASS_BYPASS_DEVICE,
            inRDN = false,
            filterUsage = FilterUsage.CONDITIONALLY_ALLOWED,
            requiredForEncode = false)
    private Set<String> bypassCodes;

    @LDAPField(attribute=LdapRepository.ATTR_ENCRYPTION_SALT,
            objectClass=LdapRepository.OBJECTCLASS_BYPASS_DEVICE,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String salt;

    @LDAPField(attribute=LdapRepository.ATTR_ITERATION_COUNT,
            objectClass=LdapRepository.OBJECTCLASS_BYPASS_DEVICE,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Integer iterations;

    @Override
    public String getAuditContext() {
        return "id=" + id;
    }
}
