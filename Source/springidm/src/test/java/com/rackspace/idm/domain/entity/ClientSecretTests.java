package com.rackspace.idm.domain.entity;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import junit.framework.Assert;

import org.junit.Test;

public class ClientSecretTests {

    @Test
    public void shouldReturnCorrectSecret() {
        ClientSecret exiPwd = ClientSecret
                .existingInstance("existingPasswordShouldBeAHash");
        Assert.assertEquals("existingPasswordShouldBeAHash", exiPwd.getValue());

        ClientSecret newPwd = ClientSecret.newInstance("newPassword");
        Assert.assertEquals("newPassword", newPwd.getValue());
    }

    @Test
    public void shouldReturnToString() {
        ClientSecret pwd = ClientSecret.newInstance("Password");
        Assert.assertEquals("ClientSecret [******]", pwd.toString());
    }
    
    @Test
    public void shouldReturnHashCode() {
        ClientSecret pwd = ClientSecret.newInstance("Password");
        Assert.assertEquals(1281669005, pwd.hashCode());
        
        pwd = ClientSecret.existingInstance("Password");
        Assert.assertEquals(1281669191, pwd.hashCode());
    }
    
    @Test
    public void ShouldReturnEquals() {
        ClientSecret pwd1 = ClientSecret.newInstance("Password");
        ClientSecret pwd2 = ClientSecret.newInstance("Password");
        
        Assert.assertTrue(pwd1.equals(pwd2));
        Assert.assertTrue(pwd1.equals(pwd1));
        
        pwd1 = ClientSecret.existingInstance("Password");
        pwd2 = ClientSecret.existingInstance("Password");
        
        Assert.assertTrue(pwd1.equals(pwd2));
        Assert.assertTrue(pwd1.equals(pwd1));
    }
    
    @Test
    public void ShouldReturnNotEquals() {
        ClientSecret pwd1 = ClientSecret.newInstance("Password");
        ClientSecret pwd2 = ClientSecret.newInstance("OtherPassword");
        
        Assert.assertFalse(pwd1.equals(null));
        Assert.assertFalse(pwd1.equals(11));
        Assert.assertFalse(pwd1.equals(pwd2));
    }

     @Test
    public void shouldRunValidation() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<ClientSecret>> violations = validator.validate(ClientSecret.newInstance("foo"));
        Assert.assertEquals(0, violations.size());
        System.out.println(violations);
    }
}
