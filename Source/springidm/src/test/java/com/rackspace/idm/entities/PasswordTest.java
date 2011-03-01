package com.rackspace.idm.entities;

import junit.framework.Assert;

import org.junit.Test;

import com.rackspace.idm.domain.entity.Password;

public class PasswordTest {

    @Test
    public void shouldGenerateRandomPassword() {
        Password randomPassword = Password.generateRandom();
        String passwordValue = randomPassword.getValue();

        String regexpPattern = "^.*(?=.{10,})(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^+=?:]).*$";

        Assert.assertNotNull(randomPassword);
        Assert.assertTrue(passwordValue.matches(regexpPattern));
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowNullValueForExistingPassword() {
        Password.existingInstance(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowBlankValueForExistingPassword() {
        Password.existingInstance(" ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowNullValueForNewPassword() {
        Password.newInstance(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowBlankValueForNewPassword() {
        Password.newInstance(" ");
    }

    @Test
    public void shouldReturnCorrectPassword() {
        Password exiPwd = Password
                .existingInstance("existingPasswordShouldBeAHash");
        Assert.assertEquals("existingPasswordShouldBeAHash", exiPwd.getValue());

        Password newPwd = Password.newInstance("newPassword");
        Assert.assertEquals("newPassword", newPwd.getValue());
    }

    @Test
    public void shouldReturnToString() {
        Password pwd = Password.newInstance("Password");
        Assert.assertEquals("Password [******]", pwd.toString());
    }
    
    @Test
    public void shouldReturnHashCode() {
        Password pwd = Password.newInstance("Password");
        Assert.assertEquals(1281669005, pwd.hashCode());
        
        pwd = Password.existingInstance("Password");
        Assert.assertEquals(1281669191, pwd.hashCode());
    }
    
    @Test
    public void ShouldReturnEquals() {
        Password pwd1 = Password.newInstance("Password");
        Password pwd2 = Password.newInstance("Password");
        
        Assert.assertTrue(pwd1.equals(pwd2));
        Assert.assertTrue(pwd1.equals(pwd1));
        
        pwd1 = Password.existingInstance("Password");
        pwd2 = Password.existingInstance("Password");
        
        Assert.assertTrue(pwd1.equals(pwd2));
        Assert.assertTrue(pwd1.equals(pwd1));
    }
    
    @Test
    public void ShouldReturnNotEquals() {
        Password pwd1 = Password.newInstance("Password");
        Password pwd2 = Password.newInstance("OtherPassword");
        
        Assert.assertFalse(pwd1.equals(null));
        Assert.assertFalse(pwd1.equals(11));
        Assert.assertFalse(pwd1.equals(pwd2));
    }
}
