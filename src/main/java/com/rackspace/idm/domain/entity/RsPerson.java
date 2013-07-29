package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;

import java.util.Date;

@Data
@LDAPObject(structuralClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            superiorClass="top",
            postDecodeMethod="doPostDecode",
            postEncodeMethod="doPostEncode")
public class RsPerson implements Auditable, UniqueId{

  @LDAPEntryField()
  private ReadOnlyEntry ldapEntry;

  @LDAPField(attribute= LdapRepository.ATTR_ID,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             inRDN=true,
             filterUsage=FilterUsage.ALWAYS_ALLOWED,
             requiredForEncode=true)
  private String id;

  @LDAPField(attribute=LdapRepository.ATTR_C,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private String country;

  @LDAPField(attribute=LdapRepository.ATTR_CLEAR_PASSWORD,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private byte[] clearPassword;

  @LDAPField(attribute=LdapRepository.ATTR_ENABLED,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private Boolean enabled;

  @LDAPField(attribute=LdapRepository.ATTR_ENCRYPTION_SALT,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private String salt;

  @LDAPField(attribute=LdapRepository.ATTR_ENCRYPTION_VERSION_ID,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private String encryptionVersion;

  @LDAPField(attribute=LdapRepository.ATTR_MAIL,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private String email;

  @LDAPField(attribute=LdapRepository.ATTR_MIDDLE_NAME,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private String middleName;

  @LDAPField(attribute=LdapRepository.ATTR_PASSWORD_SELF_UPDATED,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private Boolean passwordSelfUpdated;

  @LDAPField(attribute=LdapRepository.ATTR_PASSWORD_UPDATED_TIMESTAMP,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private Date passwordUpdatedTimestamp;

  @LDAPField(attribute=LdapRepository.ATTR_LANG,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private String preferredLanguage;

  @LDAPField(attribute=LdapRepository.ATTR_RACKSPACE_CUSTOMER_NUMBER,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private String customerId;

  @LDAPField(attribute=LdapRepository.ATTR_RACKSPACE_PERSON_NUMBER,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private String personId;

  @LDAPField(attribute=LdapRepository.ATTR_RACKSPACE_API_KEY,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private byte[] apiKey;

  @LDAPField(attribute=LdapRepository.ATTR_DISPLAY_NAME,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private byte[] displayName;

  @LDAPField(attribute=LdapRepository.ATTR_DOMAIN_ID,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private String domainId;

  @LDAPField(attribute=LdapRepository.ATTR_GIVEN_NAME,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private byte[] firstName;

  @LDAPField(attribute=LdapRepository.ATTR_MEMBER_OF,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private DN rsGroupDN;

  @LDAPField(attribute=LdapRepository.ATTR_GROUP_ID,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private String rsGroupId;

  @LDAPField(attribute=LdapRepository.ATTR_MOSSO_ID,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private Long mossoId;

  @LDAPField(attribute=LdapRepository.ATTR_NAST_ID,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private String nastId;

  @LDAPField(attribute=LdapRepository.ATTR_REGION,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private String region;

  @LDAPField(attribute=LdapRepository.ATTR_SECURE_ID,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private String secureId;

  @LDAPField(attribute=LdapRepository.ATTR_SN,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private byte[] lastName;

  @LDAPField(attribute=LdapRepository.ATTR_PASSWORD_SECRET_A,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private byte[] secretAnswer;

  @LDAPField(attribute=LdapRepository.ATTR_PASSWORD_SECRET_Q,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private byte[] secretQuestion;

  @LDAPField(attribute=LdapRepository.ATTR_PASSWORD_SECRET_Q_ID,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private byte[] secretQuestionId;

  @LDAPField(attribute=LdapRepository.ATTR_SOFT_DELETED_DATE,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private String softDeletedTimestamp;

  @LDAPField(attribute=LdapRepository.ATTR_TIME_ZONE,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private String timeZone;

  @LDAPField(attribute=LdapRepository.ATTR_UID,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private String username;

  @LDAPField(attribute=LdapRepository.ATTR_PASSWORD,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
  private byte[] password;

    @Override
    public String getAuditContext() {
        return String.format("username=%s, customer=%s", username, customerId);
    }

    @Override
    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    @Override
    public String toString() {
        return getAuditContext();
    }
}


