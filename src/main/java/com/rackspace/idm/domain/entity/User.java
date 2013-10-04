package com.rackspace.idm.domain.entity;

import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.annotation.DeleteNullValues;
import com.rackspace.idm.domain.dao.UniqueId;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.FilterUsage;
import com.unboundid.ldap.sdk.persist.LDAPEntryField;
import com.unboundid.ldap.sdk.persist.LDAPField;
import com.unboundid.ldap.sdk.persist.LDAPObject;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.dozer.Mapping;
import org.hibernate.validator.constraints.Length;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

@Data
@LDAPObject(structuralClass= LdapRepository.OBJECTCLASS_RACKSPACEPERSON)
public class User  extends BaseUser implements Auditable, UniqueId {

    @LDAPEntryField()
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute= LdapRepository.ATTR_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            inRDN=true,
            filterUsage= FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true)
    private String id;

    @NotNull
    @Length(min = 1, max = 32)
    @Pattern(regexp = RegexPatterns.USERNAME, message = MessageTexts.USERNAME)
    @LDAPField(attribute=LdapRepository.ATTR_UID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String username;

    @LDAPField(attribute=LdapRepository.ATTR_RACKSPACE_CUSTOMER_NUMBER,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String customerId;

    @LDAPField(attribute=LdapRepository.ATTR_MAIL,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String email;

    @LDAPField(attribute=LdapRepository.ATTR_CLEAR_PASSWORD,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] encryptedPassword;
    private String password;

    private boolean passwordIsNew = true;

    @LDAPField(attribute=LdapRepository.ATTR_PASSWORD_UPDATED_TIMESTAMP,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Date passwordLastUpdated = new Date();

    @LDAPField(attribute=LdapRepository.ATTR_PASSWORD_SELF_UPDATED,
             objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
             filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private boolean passwordWasSelfUpdated;

    @LDAPField(attribute=LdapRepository.ATTR_PASSWORD_SECRET_Q,
              objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
              filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] encryptedSecretQuestion;
    private String secretQuestion;

    @LDAPField(attribute=LdapRepository.ATTR_PASSWORD_SECRET_A,
              objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
              filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] encryptedSecretAnswer;
    private String secretAnswer;

    @LDAPField(attribute=LdapRepository.ATTR_PASSWORD_SECRET_Q_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] encryptedSecretQuestionId;
    private String secretQuestionId;

    @LDAPField(attribute=LdapRepository.ATTR_PASSWORD,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String userPassword;

    @LDAPField(attribute=LdapRepository.ATTR_RACKSPACE_PERSON_NUMBER,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String personId;

    @LDAPField(attribute=LdapRepository.ATTR_GIVEN_NAME,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] encryptedFirstName;
    private String firstname;

    @LDAPField(attribute=LdapRepository.ATTR_MIDDLE_NAME,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String middlename;

    @LDAPField(attribute=LdapRepository.ATTR_SN,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] encryptedLastname;
    private String lastname;

    @LDAPField(attribute=LdapRepository.ATTR_LANG,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED,
            defaultEncodeValue = GlobalConstants.USER_PREFERRED_LANG_DEFAULT,
            defaultDecodeValue = GlobalConstants.USER_PREFERRED_LANG_DEFAULT)
    private String preferredLang;

    @LDAPField(attribute=LdapRepository.ATTR_TIME_ZONE,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED,
            defaultEncodeValue = GlobalConstants.USER_TIME_ZONE_DEFAULT,
            defaultDecodeValue = GlobalConstants.USER_TIME_ZONE_DEFAULT)
    private String timeZoneId;

     @LDAPField(attribute=LdapRepository.ATTR_C,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String country;

    @LDAPField(attribute=LdapRepository.ATTR_DISPLAY_NAME,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] encryptedDisplayName;
    private String displayName;

    @LDAPField(attribute=LdapRepository.ATTR_RACKSPACE_API_KEY,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] encryptedApiKey;
    private String apiKey;

    @Mapping("defaultRegion")
    @LDAPField(attribute=LdapRepository.ATTR_REGION,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String region;

    @LDAPField(attribute=LdapRepository.ATTR_NAST_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String nastId;

    @LDAPField(attribute=LdapRepository.ATTR_MOSSO_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Integer mossoId;

    @LDAPField(attribute=LdapRepository.ATTR_CREATED_DATE,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Date created;

    @LDAPField(attribute=LdapRepository.ATTR_UPDATED_DATE,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Date updated;

    @LDAPField(attribute=LdapRepository.ATTR_SOFT_DELETED_DATE,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Date softDeletedTimestamp;

    @LDAPField(attribute=LdapRepository.ATTR_PWD_ACCOUNT_LOCKOUT_TIME,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Date passwordFailureDate;
    private Boolean maxLoginFailuresExceeded;

    @LDAPField(attribute=LdapRepository.ATTR_SECURE_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String secureId;

    @Mapping("enabled")
    @LDAPField(attribute=LdapRepository.ATTR_ENABLED,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED,
            defaultEncodeValue = "TRUE",
            defaultDecodeValue = "TRUE")
    protected Boolean enabled;

    @LDAPField(attribute=LdapRepository.ATTR_DOMAIN_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String domainId;

    @LDAPField(attribute=LdapRepository.ATTR_ENCRYPTION_VERSION_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED,
            defaultDecodeValue = "0")
    private String encryptionVersion;

    @LDAPField(attribute=LdapRepository.ATTR_ENCRYPTION_SALT,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String salt;

    @DeleteNullValues
    @LDAPField(attribute=LdapRepository.ATTR_GROUP_ID,
               objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
               filterUsage=FilterUsage.CONDITIONALLY_ALLOWED
    )
    private HashSet<String> rsGroupId;

    @LDAPField(attribute=LdapRepository.ATTR_MEMBER_OF,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private DN rsGroupDN;

    private List<TenantRole> roles;

    public User() {
    }

    public Locale getPreferredLangObj() {
        return UserLocale.parseLocale(preferredLang);
    }

    public DateTimeZone getTimeZoneObj() {
        return DateTimeZone.forID(timeZoneId);
    }

    public boolean isDisabled() {
    	return this.enabled == null || !this.enabled;
    }

    public Password getPasswordObj() {
        return new Password(password, passwordIsNew, passwordLastUpdated, passwordWasSelfUpdated);
    }

    @Override
    public String toString() {
        return getAuditContext();
    }

    @Override
    public String getAuditContext() {
        String format = "username=%s, customer=%s";
        return String.format(format, getUsername(), getCustomerId());
    }

    @Override
    public String getUniqueId() {
        if (uniqueId != null) {
            return uniqueId;
        } else if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    public HashSet<String> getRsGroupId() {
        if (rsGroupId == null) {
            rsGroupId = new HashSet<String>();
        }
        return rsGroupId;
    }
}
