package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.PasswordComplexityResult;
import com.rackspace.idm.domain.entity.PasswordRule;
import com.rackspace.idm.domain.service.PasswordComplexityService;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

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
        Assert.assertTrue(result.getPasswordRuleResults().size() == 4);
    }

    @Test
    public void shouldPassAllPasswordChecks() {
        PasswordComplexityResult result = service.checkPassword(goodPassword);

        Assert.assertTrue(result.isValidPassword());
        Assert.assertTrue(result.getPasswordRuleResults().size() == 4);
    }

    @Test
    public void shouldReturnRules() {
        List<PasswordRule> rules = service.getRules();
        
        Assert.assertTrue(rules.size()==4);
    }
}
