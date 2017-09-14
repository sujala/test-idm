package com.rackspace.idm.domain.entity;

import com.google.common.collect.ImmutableSet;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.FactorTypeEnum;
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenFormatEnum;
import com.rackspace.idm.GlobalConstants;
import com.rackspace.idm.annotation.DeleteNullValues;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.dozer.converters.TokenFormatConverter;
import com.rackspace.idm.validation.MessageTexts;
import com.rackspace.idm.validation.RegexPatterns;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.dozer.Mapping;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.util.*;

@Data
@LDAPObject(structuralClass= LdapRepository.OBJECTCLASS_RACKSPACEPERSON)
public class User implements EndUser {

    // TODO: Remove those as soon as we remove the LDAP dependencies.
    @LDAPEntryField
    private ReadOnlyEntry readOnlyEntry;

    @LDAPDNField
    private String uniqueId;

    @LDAPField(attribute= LdapRepository.ATTR_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            inRDN=true,
            filterUsage= FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true)
    private String id;

    @NotNull
    @Length(min = 1, max = 100)
    @Pattern(regexp = RegexPatterns.USERNAME, message = MessageTexts.USERNAME)
    @LDAPField(attribute=LdapRepository.ATTR_UID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String username;

    @LDAPField(attribute=LdapRepository.ATTR_MAIL,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String email;

    private String password;

    private boolean passwordIsNew = true;

    private boolean federated = false;

    private String federatedIdp;

    @LDAPField(attribute=LdapRepository.ATTR_PASSWORD_UPDATED_TIMESTAMP,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Date passwordLastUpdated;

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
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED, inModify = false)
    private Date created;

    @LDAPField(attribute=LdapRepository.ATTR_UPDATED_DATE,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED, inModify = false)
    private Date updated;

    @LDAPField(attribute=LdapRepository.ATTR_PWD_ACCOUNT_LOCKOUT_TIME,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Date passwordFailureDate;
    private Boolean maxLoginFailuresExceeded;

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
    private Set<String> rsGroupId;

    @DeleteNullValues
    @LDAPField(attribute=LdapRepository.ATTR_USER_GROUP_DNS,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED
    )
    private Set<DN> userGroupDNs;

    @LDAPField(attribute = LdapRepository.ATTR_MULTIFACTOR_MOBILE_PHONE_RSID, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEPERSON, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED)
    private String multiFactorMobilePhoneRsId;

    @LDAPField(attribute = LdapRepository.ATTR_MULTIFACTOR_DEVICE_PIN, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEPERSON, inRDN = false, filterUsage = FilterUsage.CONDITIONALLY_ALLOWED)
    private String multiFactorDevicePin;

    @LDAPField(attribute = LdapRepository.ATTR_MULTIFACTOR_DEVICE_PIN_EXPIRATION, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEPERSON, inRDN = false, filterUsage = FilterUsage.CONDITIONALLY_ALLOWED)
    private Date multiFactorDevicePinExpiration;

    @LDAPField(attribute = LdapRepository.ATTR_MULTIFACTOR_DEVICE_VERIFIED, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEPERSON, inRDN = false, filterUsage = FilterUsage.CONDITIONALLY_ALLOWED)
    private Boolean multiFactorDeviceVerified;

    @Mapping("multiFactorEnabled")
    @LDAPField(attribute = LdapRepository.ATTR_MULTI_FACTOR_ENABLED, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEPERSON, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED)
    private Boolean multifactorEnabled;

    @LDAPField(attribute = LdapRepository.ATTR_EXTERNAL_MULTIFACTOR_USER_ID, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEPERSON, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED)
    private String externalMultiFactorUserId;

    @LDAPField(attribute = LdapRepository.ATTR_MULTIFACTOR_USER_ENFORCEMENT_LEVEL, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEPERSON, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String userMultiFactorEnforcementLevel;

    @LDAPField(attribute = LdapRepository.ATTR_TOKEN_FORMAT, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEPERSON, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String tokenFormat;

    @Mapping("factorType")
    @LDAPField(attribute = LdapRepository.ATTR_MULTIFACTOR_TYPE, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEPERSON, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String multiFactorType;

    @LDAPField(attribute = LdapRepository.ATTR_CONTACT_ID, objectClass = LdapRepository.OBJECTCLASS_RACKSPACEPERSON, inRDN = false, filterUsage = FilterUsage.ALWAYS_ALLOWED, requiredForEncode = false)
    private String contactId;

    @LDAPField(attribute=LdapRepository.ATTR_MULTI_FACTOR_LAST_FAILED_TIMESTAMP,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            inRDN = false,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Date multiFactorLastFailedTimestamp;

    @LDAPField(attribute=LdapRepository.ATTR_MULTI_FACTOR_FAILED_ATTEMPT_COUNT,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            inRDN = false,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Integer multiFactorFailedAttemptCount;

    @LDAPField(attribute=LdapRepository.ATTR_PASSWORD_HISTORY,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED
    )
    private List<String> passwordHistory;

    private List<TenantRole> roles;

    TokenFormatConverter tfEnumConverter = new TokenFormatConverter();

    public User() {
    }

    public boolean isDisabled() {
    	return this.enabled == null || !this.enabled;
    }

    public boolean isMultiFactorEnabled() {
        return !(multifactorEnabled == null) && multifactorEnabled;
    }

    public Boolean isMultiFactorDeviceVerified() {
        return !(multiFactorDeviceVerified == null) && multiFactorDeviceVerified;
    }

    public Password getPasswordObj() {
        return new Password(password, passwordIsNew, passwordLastUpdated, passwordWasSelfUpdated);
    }

    public void setUserPassword(String password) {
        if (StringUtils.isNotBlank(password)) {
            this.userPassword = password;
            this.password = password;
        }
    }

    public TokenFormatEnum getTokenFormatAsEnum() {
        return tfEnumConverter.convertTo(tokenFormat, null);
    }

    /**
     * Return the logic setting for factor type - where default is SMS is mfa is enabled.
     *
     * @return
     */
    public FactorTypeEnum getMultiFactorTypeAsEnum() {
        if (multiFactorType == null) {
            if (isMultiFactorEnabled()) {
                return FactorTypeEnum.SMS;
            }
            return null;
        }

        try {
            return FactorTypeEnum.fromValue(multiFactorType);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Invalid value for token format: '%s' on user '%s'", multiFactorType, id));
        }
    }

    @Override
    public String toString() {
        return getAuditContext();
    }

    @Override
    public String getAuditContext() {
        String format = "username=%s";
        return String.format(format, getUsername());
    }

    public Set<String> getRsGroupId() {
        if (rsGroupId == null) {
            rsGroupId = new HashSet<String>();
        }
        return rsGroupId;
    }

    public List<TenantRole> getRoles() {
        if (roles == null) {
            roles = new ArrayList<TenantRole>();
        }

        return roles;
    }

    public Set<DN> getUserGroupDNs() {
        if (userGroupDNs == null) {
            userGroupDNs = new HashSet<DN>();
        }
        return userGroupDNs;
    }

    public ImmutableSet<String> getUserGroupIds() {
        Set<String> ids = new HashSet<>();

        for (DN dn : getUserGroupDNs()) {
            ids.add(dn.getRDNString().split("=")[1]);
        }
        return ImmutableSet.copyOf(ids);
    }

    public String getUserMultiFactorEnforcementLevelIfNullWillReturnDefault() {
        return StringUtils.isEmpty(userMultiFactorEnforcementLevel) ? GlobalConstants.USER_MULTI_FACTOR_ENFORCEMENT_LEVEL_DEFAULT : userMultiFactorEnforcementLevel;
    }
}
