package com.rackspace.idm.domain.entity;

import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.multifactor.util.IdmPhoneNumberUtil;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang.StringUtils;
import org.dozer.Mapping;

/**
 * Represents a mobile phone within the LDAP directory
 */
@Getter
@Setter
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_MULTIFACTOR_MOBILE_PHONE)
public class MobilePhone implements Auditable, UniqueId {
    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute = LdapRepository.ATTR_ID, objectClass = LdapRepository.OBJECTCLASS_MULTIFACTOR_MOBILE_PHONE, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String id;

    @Mapping("number")
    @LDAPField(attribute = LdapRepository.ATTR_TELEPHONE_NUMBER, objectClass = LdapRepository.OBJECTCLASS_MULTIFACTOR_MOBILE_PHONE, inRDN = true, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = true)
    private String telephoneNumber;

    @LDAPField(attribute = LdapRepository.ATTR_EXTERNAL_MULTIFACTOR_PHONE_ID, objectClass = LdapRepository.OBJECTCLASS_MULTIFACTOR_MOBILE_PHONE, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String externalMultiFactorPhoneId;

    @Override
    public String getAuditContext() {
        return String.format("mobilePhoneId=%s", id);
    }

    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    public Phonenumber.PhoneNumber getStandardizedTelephoneNumber() {
        if (StringUtils.isBlank(telephoneNumber)) {
            return null;
        }
        return IdmPhoneNumberUtil.getInstance().parsePhoneNumber(telephoneNumber);
    }
}
