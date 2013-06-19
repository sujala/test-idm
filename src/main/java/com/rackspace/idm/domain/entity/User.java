package com.rackspace.idm.domain.entity;

import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.util.CryptHelper;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.persist.*;
import lombok.Data;
import org.apache.commons.configuration.Configuration;
import org.bouncycastle.crypto.InvalidCipherTextException;
import org.joda.time.DateTime;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.security.GeneralSecurityException;
import java.util.Date;

@Data
@LDAPObject(structuralClass = LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
        postDecodeMethod="doPostDecode",
        postEncodeMethod="doPostEncode")
public class User implements Auditable, ApplicationContextAware {

    private ApplicationContext applicationContext;
    private CryptHelper cryptHelper;
    private Configuration config;

    @LDAPEntryField
    private ReadOnlyEntry ldapEntry;

    @LDAPField(attribute=LdapRepository.ATTR_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            inRDN=true,
            filterUsage= FilterUsage.ALWAYS_ALLOWED,
            requiredForEncode=true)
    private String id;

    @LDAPField(attribute=LdapRepository.ATTR_C,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String country;

    @LDAPField(attribute=LdapRepository.ATTR_CLEAR_PASSWORD,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] encryptedClearPassword;
    private String clearPassword;

    @LDAPField(attribute=LdapRepository.ATTR_ENABLED,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Boolean enabled;

    @LDAPField(attribute=LdapRepository.ATTR_ENCRYPTION_SALT,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String encryptionSalt;

    @LDAPField(attribute=LdapRepository.ATTR_ENCRYPTION_VERSION_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String encryptionVersionId;

    @LDAPField(attribute=LdapRepository.ATTR_ENDPOINT,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String[] endpoint;

    @LDAPField(attribute=LdapRepository.ATTR_IN_MIGRATION,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Boolean inMigration;

    @LDAPField(attribute=LdapRepository.ATTR_MAIL,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String email;

    @LDAPField(attribute=LdapRepository.ATTR_MIDDLE_NAME,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String middleName;

    @LDAPField(attribute=LdapRepository.ATTR_MIGRATION_DATE,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private DateTime migrationDate;

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
    private byte[] encryptedApiKey;
    private String apiKey;

    @LDAPField(attribute=LdapRepository.ATTR_DISPLAY_NAME,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] encryptedDisplayName;
    private String displayName;

    @LDAPField(attribute=LdapRepository.ATTR_DOMAIN_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String rsDomainId;

    @LDAPField(attribute=LdapRepository.ATTR_GIVEN_NAME,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] encryptedFirstName;
    private String firstName;

    @LDAPField(attribute=LdapRepository.ATTR_GROUP_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String[] rsGroupId;

    @LDAPField(attribute=LdapRepository.ATTR_MOSSO_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private Long rsMossoId;

    @LDAPField(attribute=LdapRepository.ATTR_NAST_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String rsNastId;

    @LDAPField(attribute=LdapRepository.ATTR_REGION,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String rsRegion;

    @LDAPField(attribute=LdapRepository.ATTR_SECURE_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private String rsSecureId;

    @LDAPField(attribute=LdapRepository.ATTR_SN,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] encryptedLastName;
    private String lastName;

    @LDAPField(attribute=LdapRepository.ATTR_PASSWORD_SECRET_A,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] encryptedSecretAnswer;
    private String secretAnswer;

    @LDAPField(attribute=LdapRepository.ATTR_PASSWORD_SECRET_Q,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] encryptedSecretQuestion;
    private String secretQuestion;

    @LDAPField(attribute=LdapRepository.ATTR_PASSWORD_SECRET_Q_ID,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private byte[] encryptedSecretQuestionId;
    private String secretQuestionId;

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
    private byte[] encryptedUserPassword;
    private String userPassword;

    @LDAPField(attribute=LdapRepository.ATTR_CREATED_DATE,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private DateTime created;

    @LDAPField(attribute=LdapRepository.ATTR_UPDATED_DATE,
            objectClass=LdapRepository.OBJECTCLASS_RACKSPACEPERSON,
            filterUsage=FilterUsage.CONDITIONALLY_ALLOWED)
    private DateTime updated;

    public String getUniqueId() {
        if (ldapEntry == null) {
            return null;
        } else {
            return ldapEntry.getDN();
        }
    }

    @Override
    public String getAuditContext() {
        String format = "username=%s, customer=%s";
        return String.format(format, getUsername(), getCustomerId());
    }

    private void doPostDecode() throws LDAPPersistException {
        if (encryptionVersionId == null) {
            encryptionVersionId = "0";
        }

        if (encryptionSalt == null) {
            encryptionSalt = getConfig().getString("crypto.salt");
        }

        try {
            clearPassword = getCryptHelper().decrypt(encryptedClearPassword, encryptionVersionId, encryptionSalt);
            apiKey = getCryptHelper().decrypt(encryptedApiKey, encryptionVersionId, encryptionSalt);
            displayName = getCryptHelper().decrypt(encryptedDisplayName, encryptionVersionId, encryptionSalt);
            firstName = getCryptHelper().decrypt(encryptedFirstName, encryptionVersionId, encryptionSalt);
            lastName = getCryptHelper().decrypt(encryptedLastName, encryptionVersionId, encryptionSalt);
            secretAnswer = getCryptHelper().decrypt(encryptedSecretAnswer, encryptionVersionId, encryptionSalt);
            secretQuestion = getCryptHelper().decrypt(encryptedSecretQuestion, encryptionVersionId, encryptionSalt);
            secretQuestionId = getCryptHelper().decrypt(encryptedSecretQuestionId, encryptionVersionId, encryptionSalt);
            userPassword = getCryptHelper().decrypt(encryptedUserPassword, encryptionVersionId, encryptionSalt);
        } catch (GeneralSecurityException e) {
        } catch (InvalidCipherTextException e) {
        }
    }

    private void doPostEncode(final Entry entry) throws LDAPPersistException {
        if (encryptionVersionId == null) {
            encryptionVersionId = "0";
        }

        if (encryptionSalt == null) {
            encryptionSalt = getConfig().getString("crypto.salt");
        }

        try {
            encryptedClearPassword = getCryptHelper().encrypt(clearPassword, encryptionVersionId, encryptionSalt);
            encryptedApiKey = getCryptHelper().encrypt(apiKey, encryptionVersionId, encryptionSalt);
            encryptedDisplayName = getCryptHelper().encrypt(displayName, encryptionVersionId, encryptionSalt);
            encryptedFirstName = getCryptHelper().encrypt(firstName, encryptionVersionId, encryptionSalt);
            encryptedLastName = getCryptHelper().encrypt(lastName, encryptionVersionId, encryptionSalt);
            encryptedSecretAnswer = getCryptHelper().encrypt(secretAnswer, encryptionVersionId, encryptionSalt);
            encryptedSecretQuestion = getCryptHelper().encrypt(secretQuestion, encryptionVersionId, encryptionSalt);
            encryptedSecretQuestionId = getCryptHelper().encrypt(secretQuestionId, encryptionVersionId, encryptionSalt);
            encryptedUserPassword = getCryptHelper().encrypt(userPassword, encryptionVersionId, encryptionSalt);
        } catch (GeneralSecurityException e) {
        } catch (InvalidCipherTextException e) {
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private CryptHelper getCryptHelper() {
        if (cryptHelper == null) {
            cryptHelper = applicationContext.getBean(CryptHelper.class);
        }
        return cryptHelper;
    }

    private Configuration getConfig() {
        if (config == null) {
            config = applicationContext.getBean(Configuration.class);
        }
        return config;
    }
}
