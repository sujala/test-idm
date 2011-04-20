package com.rackspace.idm.api.converter;

import java.util.List;

import com.rackspace.idm.domain.entity.PasswordComplexityResult;
import com.rackspace.idm.domain.entity.PasswordRule;
import com.rackspace.idm.domain.entity.PasswordRuleResult;
import com.rackspace.idm.jaxb.ObjectFactory;

public class PasswordRulesConverter {

    private final ObjectFactory of = new ObjectFactory();

    public PasswordRulesConverter() {
    }

    public com.rackspace.idm.jaxb.PasswordRules toPaswordRulesJaxb(
        List<PasswordRule> rules) {
        
        if (rules == null || rules.size() < 1) {
            return null;
        }
        
        com.rackspace.idm.jaxb.PasswordRules jaxbRules = of
            .createPasswordRules();

        for (PasswordRule rule : rules) {
            jaxbRules.getPasswordRules().add(this.toPasswordRuleJaxb(rule));
        }

        return jaxbRules;
    }

    public com.rackspace.idm.jaxb.PasswordRule toPasswordRuleJaxb(
        PasswordRule rule) {
        com.rackspace.idm.jaxb.PasswordRule jaxbRule = of.createPasswordRule();

        jaxbRule.setId(rule.getRuleId());
        jaxbRule.setMessage(rule.getMessage());
        jaxbRule.setName(rule.getRuleName());

        return jaxbRule;
    }

    public com.rackspace.idm.jaxb.PasswordValidation toPasswordValidationJaxb(
        PasswordComplexityResult passwordComplexityResult) {

        com.rackspace.idm.jaxb.PasswordValidation jaxbpasswordValidation = of
            .createPasswordValidation();

        com.rackspace.idm.jaxb.PasswordRuleResults jaxbRulesResult = of
            .createPasswordRuleResults();

        List<PasswordRuleResult> ruleResults = passwordComplexityResult
            .getPasswordRuleResults();

        for (PasswordRuleResult ruleResult : ruleResults) {
            jaxbRulesResult.getPasswordRuleResults().add(
                this.toPasswordRuleResult(ruleResult));
        }

        jaxbpasswordValidation.setPasswordRuleResults(jaxbRulesResult);
        jaxbpasswordValidation.setValidPassword(passwordComplexityResult
            .isValidPassword());

        return jaxbpasswordValidation;
    }

    public com.rackspace.idm.jaxb.PasswordRuleResult toPasswordRuleResult(
        PasswordRuleResult result) {
        com.rackspace.idm.jaxb.PasswordRuleResult jaxbRuleResult = of
            .createPasswordRuleResult();
        jaxbRuleResult.setRuleId(result.getRuleId());
        jaxbRuleResult.setRuleName(result.getRuleName());
        jaxbRuleResult.setRuleMessage(result.getMessage());
        jaxbRuleResult.setPassed(result.isRulePassed());

        return jaxbRuleResult;
    }
}
