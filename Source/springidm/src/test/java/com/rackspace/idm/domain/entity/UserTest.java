package com.rackspace.idm.domain.entity;

import java.util.Locale;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import junit.framework.Assert;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.rackspace.idm.validation.MessageTexts;

public class UserTest {

    private User getTestUser() {
        Password pwd = Password.newInstance("delete_my_password");
        User newUser = new User("delete.me", "RCN-DELETE-ME_NOW", "bademail@example.com", new UserHumanName(
            "delete_my_firstname", "delete_my_middlename", "delete_my_lastname"), new UserLocale(
            Locale.KOREA, DateTimeZone.UTC), new UserCredential(pwd, "What is your favourite colur?",
            "Yellow. No, Blue! Arrrrgh!"), "USA", "MY DISPLAY NAME", "@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE.5556",
            "@Rackspace.TestCustomer*delete.me", "@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE", "XXX", UserStatus.ACTIVE,
            "RPN-111-222-333");
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
        user1.setIname(null);
        user1.setInum(null);
        user1.setLastname(null);
        user1.setMiddlename(null);
        user1.setOrgInum(null);
        user1.setPasswordObj(null);
        user1.setPersonId(null);
        user1.setLocale(null);
        user1.setSecretAnswer(null);
        user1.setSecretQuestion(null);
        user1.setStatus(null);
        user1.setTimeZoneObj(null);

        user2.setApiKey(null);
        user2.setCountry(null);
        user2.setCustomerId(null);
        user2.setDisplayName(null);
        user2.setEmail(null);
        user2.setFirstname(null);
        user2.setIname(null);
        user2.setInum(null);
        user2.setLastname(null);
        user2.setMiddlename(null);
        user2.setOrgInum(null);
        user2.setPasswordObj(null);
        user2.setPersonId(null);
        user2.setLocale(null);
        user2.setSecretAnswer(null);
        user2.setSecretQuestion(null);
        user2.setStatus(null);
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

        user2.setIname("SomeOtherValue");
        Assert.assertFalse(user1.equals(user2));
        user2.setIname(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setIname(user1.getIname());

        user2.setInum("SomeOtherValue");
        Assert.assertFalse(user1.equals(user2));
        user2.setInum(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setInum(user1.getInum());

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

        user2.setOrgInum("SomeOtherValue");
        Assert.assertFalse(user1.equals(user2));
        user2.setOrgInum(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setOrgInum(user1.getOrgInum());

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

        user2.setStatus(UserStatus.INACTIVE);
        Assert.assertFalse(user1.equals(user2));
        user2.setStatus(null);
        Assert.assertFalse(user2.equals(user1));
        user2.setStatus(user1.getStatus());

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
        Assert.assertEquals(3, violations.size());
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
    public void shouldGetPasswordNoPrefix() {

        String pwdStr = "secret";

        User user = getTestUser();
        user.setPassword("{CLEAR}" + pwdStr);

        String pwdNoPrefix = user.getPasswordNoPrefix();
        Assert.assertEquals(pwdStr, pwdNoPrefix);
    }

    @Test
    public void shouldAllowValidEmail() {
        User user = getTestUser();
        user.setEmail("valid-email-format@example.com");
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        Assert.assertTrue(violations.isEmpty());
    }

    @Test
    public void shouldNotAllowEmailNoDomain() {
        User user = getTestUser();
        user.setEmail("invalidemail");

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        Assert.assertEquals(1, violations.size());
        Assert.assertEquals(MessageTexts.EMAIL, violations.iterator().next().getMessage());
        System.out.println(violations);
    }

    @Test
    public void shouldNotAllowEmailInvalidDomain() {
        User user = getTestUser();
        user.setEmail("invalidemail@baddomaincom");

        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<User>> violations = validator.validate(user);
        Assert.assertEquals(1, violations.size());
        Assert.assertEquals(MessageTexts.EMAIL, violations.iterator().next().getMessage());
        System.out.println(violations);
    }
}
