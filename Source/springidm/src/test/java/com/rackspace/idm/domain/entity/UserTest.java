package com.rackspace.idm.domain.entity;

import java.util.Locale;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import com.rackspace.idm.GlobalConstants;
import junit.framework.Assert;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

public class UserTest {

    User user;

    @Before
    public void setUp() throws Exception {
        user = new User();
    }

    private User getTestUser() {
        Password pwd = Password.newInstance("delete_my_password");
        User newUser = new User("delete.me", "RCN-DELETE-ME_NOW", "bademail@example.com", new UserHumanName(
            "delete_my_firstname", "delete_my_middlename", "delete_my_lastname"), new UserLocale(
            Locale.KOREA, DateTimeZone.UTC), new UserCredential(pwd, "What is your favourite colur?",
            "Yellow. No, Blue! Arrrrgh!"), "USA", "MY DISPLAY NAME", "XXX", "RPN-111-222-333");
        return newUser;
    }

    @Test
    public void shouldCompareCorrectlyWhenPasswordStatesAreDifferent() {
        User unmodUser = getUnmodifiedUser();

        User modUser = getModifiedUser();

        // isNew flag should be different
        Assert.assertTrue(!unmodUser.equals(modUser));
    }

    private User getModifiedUser() {
        Password plainPwd = Password.newInstance("Hi. I'm a plaintext password.");
        User modUser = new User("joe_user");
        modUser.setPasswordObj(plainPwd);
        return modUser;
    }

    private User getUnmodifiedUser() {
        Password fakeHashedPwd = Password.existingInstance("Hello. Just imagine that I'm an one-way hash.",
            new DateTime(), false);
        User unmodUser = new User("joe_user");
        unmodUser.setPasswordObj(fakeHashedPwd);
        return unmodUser;
    }

    @Test
    public void shouldCompareCorrectlyWhenPasswordsAreBothHashed() {
        Password anotherFakeHashedPwd = Password.existingInstance(
            "Hello. Just imagine that I'm also an one-way hash. But different.", new DateTime(), false);
        User unmodUserWithDiffPwd = new User("joe_user");
        unmodUserWithDiffPwd.setPasswordObj(anotherFakeHashedPwd);

        User unmodUser = getUnmodifiedUser();

        // The comparison on passwords should be performed, since they are both
        // hashed.
        Assert.assertFalse(unmodUser.equals(unmodUserWithDiffPwd));
    }

    @Test
    public void shouldCompareCorrectlyWhenPasswordsAreBothPlain() {
        Password anotherPlainPwd = Password.newInstance("Hi. I'm another plaintext password. But different.");
        User modUserWithDiffPwd = new User("joe_user");
        modUserWithDiffPwd.setPasswordObj(anotherPlainPwd);

        User modUser = getModifiedUser();

        // The comparison on password should be performed, since they are both
        // plain.
        Assert.assertFalse(modUser.equals(modUserWithDiffPwd));
    }

    @Test
    public void shouldReturnToString() {
        User user = getTestUser();

        Assert.assertNotNull(user.toString());
    }

    @Test
    public void shouldGetHashCode() {
        User user = getTestUser();
        int hashCode = user.hashCode();

        Assert.assertNotNull(hashCode);
    }

    @Test
    public void shouldReturnTrueForEquals() {
        User user1 = getTestUser();
        User user2 = getTestUser();

        Assert.assertTrue(user1.equals(user1));
        Assert.assertTrue(user1.equals(user2));

        user1.setApiKey(null);
        user1.setCountry(null);
        user1.setCustomerId(null);
        user1.setDisplayName(null);
        user1.setEmail(null);
        user1.setFirstname(null);
        user1.setLastname(null);
        user1.setMiddlename(null);
        user1.setPasswordObj(null);
        user1.setPersonId(null);
        user1.setLocale(null);
        user1.setSecretAnswer(null);
        user1.setSecretQuestion(null);
        user1.setTimeZoneObj(null);

        user2.setApiKey(null);
        user2.setCountry(null);
        user2.setCustomerId(null);
        user2.setDisplayName(null);
        user2.setEmail(null);
        user2.setFirstname(null);
        user2.setLastname(null);
        user2.setMiddlename(null);
        user2.setPasswordObj(null);
        user2.setPersonId(null);
        user2.setLocale(null);
        user2.setSecretAnswer(null);
        user2.setSecretQuestion(null);
        user2.setTimeZoneObj(null);

        Assert.assertTrue(user1.equals(user2));
    }

    @Test
    public void shouldReturnFalseForEquals() {
        User user1 = getTestUser();
        User user2 = getTestUser();

        Assert.assertFalse(user1.equals(null));
        Assert.assertFalse(user1.equals(1));

        user2.setApiKey("NewAPI");
        Assert.assertFalse(user1.equals(user2));
        user2.setApiKey(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setApiKey(user1.getApiKey());

        user2.setCountry("SomeOtherValue");
        Assert.assertFalse(user1.equals(user2));
        user2.setCountry(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setCountry(user1.getCountry());

        user2.setCustomerId("SomeOtherValue");
        Assert.assertFalse(user1.equals(user2));
        user2.setCustomerId(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setCustomerId(user1.getCustomerId());

        user2.setDisplayName("SomeOtherValue");
        Assert.assertFalse(user1.equals(user2));
        user2.setDisplayName(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setDisplayName(user1.getDisplayName());

        user2.setEmail("SomeOtherValue");
        Assert.assertFalse(user1.equals(user2));
        user2.setEmail(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setEmail(user1.getEmail());

        user2.setFirstname("SomeOtherValue");
        Assert.assertFalse(user1.equals(user2));
        user2.setFirstname(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setFirstname(user1.getFirstname());

        user2.setLastname("SomeOtherValue");
        Assert.assertFalse(user1.equals(user2));
        user2.setLastname(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setLastname(user1.getLastname());

        user2.setMiddlename("SomeOtherValue");
        Assert.assertFalse(user1.equals(user2));
        user2.setMiddlename(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setMiddlename(user1.getMiddlename());

        user2.setPasswordObj(Password.newInstance("SomeOtherValue"));
        Assert.assertFalse(user1.equals(user2));
        user2.setPasswordObj(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setPasswordObj(user1.getPasswordObj());

        user2.setPersonId("SomeOtherValue");
        Assert.assertFalse(user1.equals(user2));
        user2.setPersonId(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setPersonId(user1.getPersonId());

        user2.setLocale(Locale.CHINA);
        Assert.assertFalse(user1.equals(user2));
        user2.setLocale(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setLocale(user1.getLocale());

        user2.setSecretAnswer("SomeOtherValue");
        Assert.assertFalse(user1.equals(user2));
        user2.setSecretAnswer(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setSecretAnswer(user1.getSecretAnswer());

        user2.setSecretQuestion("SomeOtherValue");
        Assert.assertFalse(user1.equals(user2));
        user2.setSecretQuestion(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setSecretQuestion(user1.getSecretQuestion());

        user2.setTimeZoneObj(DateTimeZone.forID("America/Chicago"));
        Assert.assertFalse(user1.equals(user2));
        user2.setTimeZoneObj(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setTimeZoneObj(user1.getTimeZoneObj());
    }

    @Test
    public void shouldRunValidations() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<User>> violations = validator.validate(new User());
        Assert.assertEquals(1, violations.size());
        System.out.println(violations);
    }

    @Test
    public void shouldAllowValidUserName() {
        User user = getTestUser();
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    public void shouldNotAllowUserNameLongerThan32Chars() {
        User user = getTestUser();
        user.setUsername("o12345678901234567890123456789012"); // 33 chars
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        Assert.assertEquals(1, violations.size());
        Assert.assertEquals("length must be between 1 and 32", violations.iterator().next().getMessage());
        System.out.println(violations);
    }

    @Test
    public void shouldAllowUserNameLongerThatBeginsWithNumber() {
        User user = getTestUser();
        user.setUsername("4BCDEFG");
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        Assert.assertEquals(0, violations.size());

        System.out.println(violations);
    }

    @Test
    public void setUniqueId_uniqueIdIsNull_doesNotSet() throws Exception {
        user.setUniqueId("uniqueId");
        user.setUniqueId(null);
        String result = user.getUniqueId();
        assertThat("unique id", result, equalTo("uniqueId"));
    }

    @Test
    public void hasEmptyPassword_passwordObjIsNull_returnsTrue() throws Exception {
        Password password = new Password();
        password.setValue("password");
        User spy = spy(user);
        doReturn(null).doReturn(password).when(spy).getPasswordObj();
        boolean result = spy.hasEmptyPassword();
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void hasEmptyPassword_passwordObjValueIsBlank_returnsTrue() throws Exception {
        Password password = new Password();
        user.setPasswordObj(password);
        boolean result = user.hasEmptyPassword();
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void hasEmptyPassword_passwordObjNotNullAndValueNotBlank_returnsFalse() throws Exception {
        Password password = new Password();
        password.setValue("password");
        user.setPasswordObj(password);
        boolean result = user.hasEmptyPassword();
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void setDefaults_localeNotNullAndTimeZoneObjNotNull_setsEnabledToTrue() throws Exception {
        user.setPreferredLang(GlobalConstants.USER_PREFERRED_LANG_DEFAULT);
        user.setTimeZone(GlobalConstants.USER_TIME_ZONE_DEFAULT);
        user.setEnabled(false);
        user.setDefaults();
        Boolean result = user.isEnabled();
        assertThat("enabled", result, equalTo(true));
    }
    
    @Test
    public void copyChanges_modifiedUserAttributesNotNull_copyChanges() throws Exception {
        User modifiedUser = new User();
        modifiedUser.setCustomerId("newCustomerId");
        modifiedUser.setEnabled(true);
        modifiedUser.setPersonId("newPersonId");
        modifiedUser.setFirstname("newFirstName");
        modifiedUser.setMiddlename("newMiddleName");
        modifiedUser.setLastname("newLastName");
        modifiedUser.setDisplayName("newDisplayName");
        modifiedUser.setEmail("newEmail");
        modifiedUser.setPreferredLang("newPreferredLang");
        modifiedUser.setTimeZone(GlobalConstants.USER_TIME_ZONE_DEFAULT);
        modifiedUser.setCountry("newCountry");
        modifiedUser.setRegion("newRegion");
        modifiedUser.setPassword("newPassword");
        modifiedUser.setUsername("newUsername");
        
        user.copyChanges(modifiedUser);
        
        assertThat("customer id", user.getCustomerId(), equalTo("newCustomerId"));
        assertThat("enabled", user.isEnabled(), equalTo(true));
        assertThat("person id", user.getPersonId(), equalTo("newPersonId"));
        assertThat("first anme", user.getFirstname(), equalTo("newFirstName"));
        assertThat("middle name", user.getMiddlename(), equalTo("newMiddleName"));
        assertThat("last name", user.getLastname(), equalTo("newLastName"));
        assertThat("display name", user.getDisplayName(), equalTo("newDisplayName"));
        assertThat("email", user.getEmail(), equalTo("newEmail"));
        assertThat("pref lang", user.getPreferredLang(), equalTo("newpreferredlang"));
        assertThat("time zone", user.getTimeZone(), equalTo(GlobalConstants.USER_TIME_ZONE_DEFAULT));
        assertThat("country", user.getCountry(), equalTo("newCountry"));
        assertThat("region", user.getRegion(), equalTo("newRegion"));
        assertThat("password", user.getPassword(), equalTo("newPassword"));
        assertThat("username", user.getUsername(), equalTo("newUsername"));
    }
    
    @Test
    public void equals_allAttributesMatches_returnsTrue() throws Exception {
        DateTime dateTime = new DateTime();
        User object = new User();
        object.setCustomerId("customerId");
        object.setEnabled(true);
        object.setPersonId("personId");
        object.setFirstname("firstName");
        object.setMiddlename("middleName");
        object.setLastname("lastName");
        object.setDisplayName("displayName");
        object.setEmail("email");
        object.setPreferredLang("preferredLang");
        object.setTimeZone(GlobalConstants.USER_TIME_ZONE_DEFAULT);
        object.setCountry("country");
        object.setRegion("region");
        object.setPassword("password");
        object.setUsername("username");
        object.setApiKey("apiKey");
        object.setCreated(dateTime);
        object.setId("id");
        object.setMaxLoginFailuresExceded(true);
        object.setMossoId(1);
        object.setNastId("nastId");
        object.setSecureId("secureId");
        object.setSoftDeletedTimestamp(dateTime);
        object.setUpdated(dateTime);
        object.setUniqueId("uniqueId");


        user.setCustomerId("customerId");
        user.setEnabled(true);
        user.setPersonId("personId");
        user.setFirstname("firstName");
        user.setMiddlename("middleName");
        user.setLastname("lastName");
        user.setDisplayName("displayName");
        user.setEmail("email");
        user.setPreferredLang("preferredLang");
        user.setTimeZone(GlobalConstants.USER_TIME_ZONE_DEFAULT);
        user.setCountry("country");
        user.setRegion("region");
        user.setPassword("password");
        user.setUsername("username");
        user.setApiKey("apiKey");
        user.setCreated(dateTime);
        user.setId("id");
        user.setMaxLoginFailuresExceded(true);
        user.setMossoId(1);
        user.setNastId("nastId");
        user.setSecureId("secureId");
        user.setSoftDeletedTimestamp(dateTime);
        user.setUpdated(dateTime);
        user.setUniqueId("uniqueId");

        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void equals_apiKeyIsNullAndObjectApiKeyNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setApiKey("notNull");
        user.setApiKey(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_apiKeyNotNullAndNotEqualsObjectApiKey_returnsFalse() throws Exception {
        User object = new User();
        object.setApiKey("notNull");
        user.setApiKey("notSame");
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_countryIsNullAndObjectCountryNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setCountry("notNull");
        user.setCountry(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_countryNotNullAndNotEqualsObjectCountry_returnsFalse() throws Exception {
        User object = new User();
        object.setCountry("notNull");
        user.setCountry("notSame");
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_displayNameIsNullAndObjectDisplayNameNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setDisplayName("notNull");
        user.setDisplayName(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_displayNameNotNullAndNotEqualsObjectDisplayName_returnsFalse() throws Exception {
        User object = new User();
        object.setDisplayName("notNull");
        user.setDisplayName("notSame");
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_emailIsNullAndObjectEmailNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setEmail("notNull");
        user.setEmail(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_emailNotNullAndNotEqualsObjectEmail_returnsFalse() throws Exception {
        User object = new User();
        object.setEmail("notNull");
        user.setEmail("notSame");
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_createdIsNullAndObjectCreatedNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setCreated(new DateTime());
        user.setCreated(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_createdNotNullAndNotEqualsObjectCreated_returnsFalse() throws Exception {
        User object = new User();
        object.setCreated(new DateTime());
        user.setCreated(new DateTime(1));
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_credentialIsNullAndObjectCredentialNotNull_returnsFalse() throws Exception {
        Password password = new Password();
        password.setValue("notNull");
        User object = new User();
        object.setPasswordObj(password);
        object.setSecretAnswer("secretAnswer");
        object.setSecretQuestion("secretQuestion");
        user.setCredential(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_credentialNotNullAndNotEqualsObjectCredential_returnsFalse() throws Exception {
        Password password = new Password();
        password.setValue("notNull");
        Password userPassword = new Password();
        userPassword.setValue("notSame");
        User object = new User();
        object.setPasswordObj(password);
        object.setSecretAnswer("secretAnswer");
        object.setSecretQuestion("secretQuestion");
        user.setPasswordObj(userPassword);
        user.setSecretAnswer("secretAnswer");
        user.setSecretQuestion("secretQuestion");
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_credentialsNotNullAndSame_returnsTrue() throws Exception {
        Password password = new Password();
        password.setValue("notNull");
        User object = new User();
        object.setPasswordObj(password);
        object.setSecretAnswer("secretAnswer");
        object.setSecretQuestion("secretQuestion");
        user.setPasswordObj(password);
        user.setSecretAnswer("secretAnswer");
        user.setSecretQuestion("secretQuestion");
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void equals_idIsNullAndObjectIdNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setId("notNull");
        user.setId(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_idNotNullAndNotEqualsObjectId_returnsFalse() throws Exception {
        User object = new User();
        object.setId("notNull");
        user.setId("notSame");
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_maxLoginFailuresExcededIsNullAndObjectMaxLoginFailuresExcededNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setMaxLoginFailuresExceded(true);
        user.setMaxLoginFailuresExceded(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_maxLoginFailuresExcededNotNullAndNotEqualsObjectMaxLoginFailuresExceded_returnsFalse() throws Exception {
        User object = new User();
        object.setMaxLoginFailuresExceded(true);
        user.setMaxLoginFailuresExceded(false);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_mossoIdIsNullAndObjectMossoIdNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setMossoId(1);
        user.setMossoId(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_mossoIdNotNullAndNotEqualsObjectMossoId_returnsFalse() throws Exception {
        User object = new User();
        object.setMossoId(1);
        user.setMossoId(2);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_nameIsNullAndObjectNameNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setFirstname("notNull");
        object.setMiddlename("notNull");
        object.setLastname("notNull");
        user.setName(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_nameNotNullAndNotEqualsObjectName_returnsFalse() throws Exception {
        User object = new User();
        object.setFirstname("notNull");
        object.setMiddlename("notNull");
        object.setLastname("notNull");
        user.setFirstname("notSame");
        user.setLastname("notSame");
        user.setMiddlename("notSame");
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_nameNotNullAndSame_returnsTrue() throws Exception {
        User object = new User();
        object.setFirstname("Same");
        object.setMiddlename("Same");
        object.setLastname("Same");
        user.setFirstname("Same");
        user.setLastname("Same");
        user.setMiddlename("Same");
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void equals_nastIdIsNullAndObjectNastIdNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setNastId("notNull");
        user.setNastId(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_nastIdNotNullAndNotEqualsObjectNastId_returnsFalse() throws Exception {
        User object = new User();
        object.setNastId("notNull");
        user.setNastId("notSame");
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_personIdIsNullAndObjectPersonIdNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setPersonId("notNull");
        user.setPersonId(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_personIdNotNullAndNotEqualsObjectPersonId_returnsFalse() throws Exception {
        User object = new User();
        object.setPersonId("notNull");
        user.setPersonId("notSame");
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_preferenceIsNullAndObjectPreferenceNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setPreferredLang("notNull");
        object.setTimeZone(GlobalConstants.USER_TIME_ZONE_DEFAULT);
        user.setPreference(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_preferenceNotNullAndNotEqualsObjectPreference_returnsFalse() throws Exception {
        User object = new User();
        object.setPreferredLang("notNull");
        object.setTimeZone(GlobalConstants.USER_TIME_ZONE_DEFAULT);
        user.setPreferredLang("notSame");
        user.setTimeZone(GlobalConstants.USER_TIME_ZONE_DEFAULT);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_preferenceNotNullAndSame_returnsTrue() throws Exception {
        User object = new User();
        object.setPreferredLang("same");
        object.setTimeZone(GlobalConstants.USER_TIME_ZONE_DEFAULT);
        user.setPreferredLang("same");
        user.setTimeZone(GlobalConstants.USER_TIME_ZONE_DEFAULT);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void equals_regionIsNullAndObjectRegionNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setRegion("notNull");
        user.setRegion(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_regionNotNullAndNotEqualsObjectRegion_returnsFalse() throws Exception {
        User object = new User();
        object.setRegion("notNull");
        user.setRegion("notSame");
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_secureIdIsNullAndObjectSecureIdNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setSecureId("notNull");
        user.setSecureId(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_secureIdNotNullAndNotEqualsObjectSecureId_returnsFalse() throws Exception {
        User object = new User();
        object.setSecureId("notNull");
        user.setSecureId("notSame");
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_softDeletedTimeStampIsNullAndObjectSoftDeletedTimeStampNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setSoftDeletedTimestamp(new DateTime());
        user.setSoftDeletedTimestamp(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_softDeletedTimeStampNotNullAndNotEqualsObjectSoftDeletedTimeStamp_returnsFalse() throws Exception {
        User object = new User();
        object.setSoftDeletedTimestamp(new DateTime());
        user.setSoftDeletedTimestamp(new DateTime(1));
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_uniqueIdIsNullAndObjectUniqueIdNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setUniqueId("notNull");
        user.setUniqueId(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_uniqueIdNotNullAndNotEqualsObjectUniqueId_returnsFalse() throws Exception {
        User object = new User();
        object.setUniqueId("notNull");
        user.setUniqueId("notSame");
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_updatedIsNullAndObjectUpdatedNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setUpdated(new DateTime());
        user.setUpdated(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_updatedNotNullAndNotEqualsObjectUpdated_returnsFalse() throws Exception {
        User object = new User();
        object.setUpdated(new DateTime());
        user.setUpdated(new DateTime(1));
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_usernameIsNullAndObjectUsernameNotNull_returnsFalse() throws Exception {
        User object = new User();
        object.setUsername("notNull");
        user.setUsername(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_usernameNotNullAndNotEqualsObjectUsername_returnsFalse() throws Exception {
        User object = new User();
        object.setUsername("notNull");
        user.setUsername("notSame");
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_usernameIsNullAndObjectUsernameIsNull_returnsTrue() throws Exception {
        User object = new User();
        object.setUsername(null);
        object.setName(null);
        object.setCredential(null);
        object.setPreference(null);
        user.setUsername(null);
        user.setName(null);
        user.setCredential(null);
        user.setPreference(null);
        boolean result = user.equals(object);
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void hashCode_attributesNotNull_returnsHashCode() throws Exception {
        DateTime dateTime = new DateTime(1);
        user.setCustomerId("customerId");
        user.setEnabled(true);
        user.setPersonId("personId");
        user.setFirstname("firstName");
        user.setMiddlename("middleName");
        user.setLastname("lastName");
        user.setDisplayName("displayName");
        user.setEmail("email");
        user.setPreferredLang("preferredLang");
        user.setTimeZone(GlobalConstants.USER_TIME_ZONE_DEFAULT);
        user.setSecretQuestion("secretQuestion");
        user.setSecretAnswer("secretAnswer");
        user.setCountry("country");
        user.setRegion("region");
        user.setPassword("password");
        user.setUsername("username");
        user.setApiKey("apiKey");
        user.setCreated(dateTime);
        user.setId("id");
        user.setMaxLoginFailuresExceded(true);
        user.setMossoId(1);
        user.setNastId("nastId");
        user.setSecureId("secureId");
        user.setSoftDeletedTimestamp(dateTime);
        user.setUpdated(dateTime);
        user.setUniqueId("uniqueId");
        int result = user.hashCode();
        assertThat("hash code", result, notNullValue());
    }

    @Test
    public void hashCode_attributesIsNull_returnsHashCode() throws Exception {
        user.setCustomerId(null);
        user.setEnabled(null);
        user.setPersonId(null);
        user.setFirstname(null);
        user.setMiddlename(null);
        user.setLastname(null);
        user.setDisplayName(null);
        user.setEmail(null);
        user.setPreferredLang(null);
        user.setTimeZone(null);
        user.setCountry(null);
        user.setRegion(null);
        user.setPassword(null);
        user.setSecretQuestion(null);
        user.setSecretAnswer(null);
        user.setUsername(null);
        user.setApiKey(null);
        user.setCreated(null);
        user.setId(null);
        user.setMaxLoginFailuresExceded(null);
        user.setMossoId(null);
        user.setNastId(null);
        user.setSecureId(null);
        user.setSoftDeletedTimestamp(null);
        user.setUpdated(null);
        user.setUniqueId(null);
        user.setName(null);
        user.setCredential(null);
        user.setPreference(null);
        int result = user.hashCode();
        assertThat("hash code", result, equalTo(0));
    }

    @Test
    public void builder_build_returnsUser() throws Exception {
        User.Builder builder = new User.Builder();
        builder.setUsername("username");
        builder.setEmail("email");
        builder.setCisIds("customerId", "personId");
        builder.setUniqueIds("username", "inum", "iname", "uniqueId");
        builder.setNames("firstName", "middleName", "lastName", "displayName");
        builder.setLocale("preferredLang", GlobalConstants.USER_TIME_ZONE_DEFAULT, "country");
        builder.setApiKey("apiKey");
        builder.setSecurityInfo("password", "question", "answer");
        User result = builder.build();
        assertThat("customer id", result.getCustomerId(), equalTo("customerId"));
        assertThat("person id", result.getPersonId(), equalTo("personId"));
        assertThat("first anme", result.getFirstname(), equalTo("firstName"));
        assertThat("middle name", result.getMiddlename(), equalTo("middleName"));
        assertThat("last name", result.getLastname(), equalTo("lastName"));
        assertThat("display name", result.getDisplayName(), equalTo("displayName"));
        assertThat("email", result.getEmail(), equalTo("email"));
        assertThat("pref lang", result.getPreferredLang(), equalTo("preferredlang"));
        assertThat("time zone", result.getTimeZone(), equalTo(GlobalConstants.USER_TIME_ZONE_DEFAULT));
        assertThat("country", result.getCountry(), equalTo("country"));
        assertThat("password", result.getPassword(), equalTo("password"));
        assertThat("username", result.getUsername(), equalTo("username"));
        assertThat("apikey", result.getApiKey(), equalTo("apiKey"));
        assertThat("secret answer", result.getSecretAnswer(), equalTo("answer"));
        assertThat("secret question", result.getSecretQuestion(), equalTo("question"));
    }

    @Test (expected = IllegalStateException.class)
    public void builder_build_customerIdIsBlank_throwsIllegalStateExpcetion() throws Exception {
        User.Builder builder = new User.Builder();
        builder.setUsername("username");
        builder.setEmail("email");
        builder.setCisIds("", "personId");
        builder.build();
    }

    @Test (expected = IllegalStateException.class)
    public void builder_build_usernameIsBlank_throwsIllegalStateExpcetion() throws Exception {
        User.Builder builder = new User.Builder();
        builder.setUsername("");
        builder.setEmail("email");
        builder.setCisIds("customerId", "personId");
        builder.build();
    }

    @Test (expected = IllegalStateException.class)
    public void builder_build_emailIsBlank_throwsIllegalStateExpcetion() throws Exception {
        User.Builder builder = new User.Builder();
        builder.setUsername("username");
        builder.setEmail("");
        builder.setCisIds("customerId", "personId");
        builder.build();
    }
}
