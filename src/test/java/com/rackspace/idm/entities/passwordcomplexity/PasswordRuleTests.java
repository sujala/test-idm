package com.rackspace.idm.entities.passwordcomplexity;

import junit.framework.Assert;

import org.junit.Test;

import com.rackspace.idm.domain.entity.LowercaseRule;
import com.rackspace.idm.domain.entity.MinimumLengthRule;
import com.rackspace.idm.domain.entity.NumberRule;
import com.rackspace.idm.domain.entity.PasswordRule;
import com.rackspace.idm.domain.entity.PasswordRuleResult;
import com.rackspace.idm.domain.entity.SpecialCharacterRule;
import com.rackspace.idm.domain.entity.UppercaseRule;

public class PasswordRuleTests {
    
    PasswordRule rule;
    PasswordRuleResult result;
    
    String goodLengthPassword = "1234567";
    String badLengthPassword = "123456";
    
    String goodUppercasePassword = "ABC";
    String badUppercasePassword = "abc";
    
    String goodLowercasePassword = "abc";
    String badLowercasePassword = "ABC";
    
    String goodNumberPassword = "XX1XX";
    String badNumberPassword = "XXXXX";
    
    String goodSpecialCharacterPassword = "abc%123";
    String badSpecialCharacterPassword = "abc123";
    
    
    @Test
    public void shouldPassLengthRule() {
        rule = new MinimumLengthRule();
        
        result = rule.validatePassword(goodLengthPassword);
        
        Assert.assertTrue(result.isRulePassed());
    }
    
    @Test
    public void shouldFailLengthRule() {
        rule = new MinimumLengthRule();
        
        result = rule.validatePassword(badLengthPassword);
        
        Assert.assertFalse(result.isRulePassed());
    }
    
    @Test
    public void shouldPassUppercaseRule() {
        rule = new UppercaseRule();
        
        result = rule.validatePassword(goodUppercasePassword);
        
        Assert.assertTrue(result.isRulePassed());
    }
    
    @Test
    public void shouldFailUppercaseRule() {
        rule = new UppercaseRule();
        
        result = rule.validatePassword(badUppercasePassword);
        
        Assert.assertFalse(result.isRulePassed());
    }
    
    @Test
    public void shouldPassLowercaseRule() {
        rule = new LowercaseRule();
        
        result = rule.validatePassword(goodLowercasePassword);
        
        Assert.assertTrue(result.isRulePassed());
    }
    
    @Test
    public void shouldFailLowercaseRule() {
        rule = new LowercaseRule();
        
        result = rule.validatePassword(badLowercasePassword);
        
        Assert.assertFalse(result.isRulePassed());
    }
    
    @Test
    public void shouldPassNumberRule() {
        rule = new NumberRule();
        
        result = rule.validatePassword(goodNumberPassword);
        
        Assert.assertTrue(result.isRulePassed());
    }
    
    @Test
    public void shouldFailNumberRule() {
        rule = new NumberRule();
        
        result = rule.validatePassword(badNumberPassword);
        
        Assert.assertFalse(result.isRulePassed());
    }

    @Test
    public void shouldSpecialCharacterRule() {
        rule = new SpecialCharacterRule();
        
        result = rule.validatePassword(goodSpecialCharacterPassword);
        
        Assert.assertTrue(result.isRulePassed());
    }
    
    @Test
    public void shouldFailSpecialCharacterRule() {
        rule = new SpecialCharacterRule();
        
        result = rule.validatePassword(badSpecialCharacterPassword);
        
        Assert.assertFalse(result.isRulePassed());
    }
}
