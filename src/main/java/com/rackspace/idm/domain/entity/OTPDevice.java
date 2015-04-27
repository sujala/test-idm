package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPDNField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_OTP_DEVICE)
public class OTPDevice implements UniqueId, Auditable, MultiFactorDevice {

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute = LdapRepository.ATTR_ID,
            objectClass = LdapRepository.OBJECTCLASS_OTP_DEVICE,
            inRDN = true,
            filterUsage = FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode = true)
    private String id;

    @LDAPField(attribute = LdapRepository.ATTR_MULTIFACTOR_DEVICE_VERIFIED,
            objectClass = LdapRepository.OBJECTCLASS_OTP_DEVICE,
            inRDN = false,
            filterUsage = FilterUsage.CONDITIONALLY_ALLOWED)
    private Boolean multiFactorDeviceVerified;

    @LDAPField(attribute = LdapRepository.ATTR_OTP_NAME,
            objectClass = LdapRepository.OBJECTCLASS_OTP_DEVICE,
            inRDN = false,
            filterUsage = FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode = false)
    private String name;

    @LDAPField(attribute=LdapRepository.ATTR_OTP_KEY,
            objectClass=LdapRepository.OBJECTCLASS_OTP_DEVICE,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] key;

    @Override
    public String getAuditContext() {
        return "otpDevice=" + id;
    }

}
