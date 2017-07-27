package com.rackspace.idm.domain.service.impl;

import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.context.ContextConfiguration;

import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.rackspace.idm.domain.entity.PasswordComplexityResult;
import com.rackspace.idm.domain.entity.PasswordRule;
import com.rackspace.idm.domain.service.PasswordComplexityService;
import junit.framework.Assert;
import org.junit.Test;

import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:app-config.xml")
public class PasswordComplexityServiceTests {

    @Autowired
    PasswordComplexityService service;

    String badPassword = "";
    String goodPassword = "Ab1$XXXXXX";

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
