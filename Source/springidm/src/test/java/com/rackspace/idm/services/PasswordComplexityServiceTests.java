package com.rackspace.idm.services;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.entity.PasswordComplexityResult;
import com.rackspace.idm.test.stub.StubLogger;

public class PasswordComplexityServiceTests {

    PasswordComplexityService service;

    String badPassword = "";
    String goodPassword = "Ab1$XXXXXX";

    @Before
    public void setUp() {
        service = new DefaultPasswordComplexityService();
    }

    @Test
    public void shouldFailAllPasswordChecks() {
        PasswordComplexityResult result = service.checkPassword(badPassword);

        Assert.assertFalse(result.isValidPassword());
        Assert.assertTrue(result.getPasswordRuleResults().size() == 5);
    }

    @Test
    public void shouldPassAllPasswordChecks() {
        PasswordComplexityResult result = service.checkPassword(goodPassword);

        Assert.assertTrue(result.isValidPassword());
        Assert.assertTrue(result.getPasswordRuleResults().size() == 5);
    }
}
