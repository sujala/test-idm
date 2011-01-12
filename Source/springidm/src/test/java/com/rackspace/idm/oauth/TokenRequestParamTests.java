package com.rackspace.idm.oauth;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.validation.BasicCredentialsCheck;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.groups.Default;
import java.util.Set;

public class TokenRequestParamTests {

    private Validator validator;
    private AuthCredentials param;

    @Before
    public void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        param = new AuthCredentials();
    }

    @Test
    public void shouldNotAcceptNullBasicCredentialParams() {
        Set<ConstraintViolation<AuthCredentials>> violations = validator
            .validate(param, Default.class, BasicCredentialsCheck.class);
        Assert.assertEquals(5, violations.size());
        System.out.println(violations);
    }

    @Test
    public void shouldNotAcceptNullNoneCredentialParams() {
        Set<ConstraintViolation<AuthCredentials>> violations = validator
            .validate(param);
        Assert.assertEquals(3, violations.size());
        System.out.println(violations);
    }

    @Test
    public void shouldNotAcceptBlankParams() {
        param.setClientId("");
        param.setClientSecret("");
        param.setPassword("");
        param.setUsername("");
        param.setGrantType("");
        Set<ConstraintViolation<AuthCredentials>> violations = validator
            .validate(param, Default.class, BasicCredentialsCheck.class);
        Assert.assertEquals(5, violations.size());
        System.out.println(violations);
    }

    @Test
    public void shouldAcceptParamsWithWhitespaces() {
        param.setClientId(" foo ");
        param.setClientSecret("  bar  ");
        param.setPassword("   baz");
        param.setUsername("eek ");
        param.setGrantType("woot");
        Set<ConstraintViolation<AuthCredentials>> violations = validator
            .validate(param, Default.class, BasicCredentialsCheck.class);
        Assert.assertEquals(0, violations.size());
        System.out.println(violations);
    }
}