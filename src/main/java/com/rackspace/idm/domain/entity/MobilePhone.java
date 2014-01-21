package com.rackspace.idm.domain.entity;

import com.google.i18n.phonenumbers.Phonenumber;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.multifactor.util.IdmPhoneNumberUtil;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.dozer.Mapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Represents a mobile phone within the LDAP directory
 */
@Getter
@Setter
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_MULTIFACTOR_MOBILE_PHONE, superiorClass={ "groupOfNames",
        "top" })
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

    @LDAPField(attribute=LdapRepository.ATTR_MEMBER,
            objectClass = LdapRepository.OBJECTCLASS_MULTIFACTOR_MOBILE_PHONE,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Set<DN> members;

    @LDAPField(attribute=LdapRepository.ATTR_COMMON_NAME,
            objectClass=LdapRepository.OBJECTCLASS_MULTIFACTOR_MOBILE_PHONE,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED,
            requiredForEncode=true)
    private String cn;

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

    public void setTelephoneNumberAndCn(String telephoneNumber) {
        this.telephoneNumber = telephoneNumber;
        this.cn = telephoneNumber;
    }

    public void addMember(User user) {
        if (members == null) {
            members = new HashSet<DN>(1);
        }
        try {
            members.add(user.getLdapEntry().getParsedDN());
        } catch (LDAPException e) {
            throw new IllegalStateException(String.format("Error retrieving user '%s' DN", user.getId()), e);
        }
    }

    public void removeMember(User user) {
        try {
            if (CollectionUtils.isNotEmpty(members)) {
                DN userDN = user.getLdapEntry().getParsedDN();
                if (members.contains(userDN)) {
                    members.remove(userDN);
                }
            }
        } catch (LDAPException e) {
            throw new IllegalStateException(String.format("Error retrieving user '%s' DN", user.getId()), e);
        }
    }

}
