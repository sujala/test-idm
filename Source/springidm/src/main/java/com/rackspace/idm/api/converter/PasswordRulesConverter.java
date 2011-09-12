package com.rackspace.idm.api.converter;

import java.util.List;

import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.idm.domain.entity.PasswordComplexityResult;
import com.rackspace.idm.domain.entity.PasswordRule;
import com.rackspace.idm.domain.entity.PasswordRuleResult;

public class PasswordRulesConverter {

    private final ObjectFactory of = new ObjectFactory();

    public PasswordRulesConverter() {
    }

    public com.rackspace.api.idm.v1.PasswordRules toPaswordRulesJaxb(
        List<PasswordRule> rules) {
        
        if (rules == null || rules.size() < 1) {
            return null;
        }
        
        com.rackspace.api.idm.v1.PasswordRules jaxbRules = of
            .createPasswordRules();

        for (PasswordRule rule : rules) {
            jaxbRules.getPasswordRules().add(this.toPasswordRuleJaxb(rule));
        }

        return jaxbRules;
    }

    public com.rackspace.api.idm.v1.PasswordRule toPasswordRuleJaxb(
        PasswordRule rule) {
        com.rackspace.api.idm.v1.PasswordRule jaxbRule = of.createPasswordRule();

        jaxbRule.setId(rule.getRuleId());
        jaxbRule.setMessage(rule.getMessage());
        jaxbRule.setName(rule.getRuleName());

        return jaxbRule;
    }

    public com.rackspace.api.idm.v1.PasswordValidation toPasswordValidationJaxb(
        PasswordComplexityResult passwordComplexityResult) {

        com.rackspace.api.idm.v1.PasswordValidation jaxbpasswordValidation = of
            .createPasswordValidation();

        com.rackspace.api.idm.v1.PasswordRuleResults jaxbRulesResult = of
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

    public com.rackspace.api.idm.v1.PasswordRuleResult toPasswordRuleResult(
        PasswordRuleResult result) {
        com.rackspace.api.idm.v1.PasswordRuleResult jaxbRuleResult = of
            .createPasswordRuleResult();
        jaxbRuleResult.setRuleId(result.getRuleId());
        jaxbRuleResult.setRuleName(result.getRuleName());
        jaxbRuleResult.setRuleMessage(result.getMessage());
        jaxbRuleResult.setPassed(result.isRulePassed());

        return jaxbRuleResult;
    }
}
