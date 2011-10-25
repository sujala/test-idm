package com.rackspace.idm.api.converter;

import java.util.List;

import javax.xml.bind.JAXBElement;

import com.rackspace.api.idm.v1.ObjectFactory;
import com.rackspace.idm.domain.entity.PasswordComplexityResult;
import com.rackspace.idm.domain.entity.PasswordRule;
import com.rackspace.idm.domain.entity.PasswordRuleResult;

public class PasswordRulesConverter {

    private final ObjectFactory objectFactory = new ObjectFactory();

    public PasswordRulesConverter() {
    }

    public JAXBElement<com.rackspace.api.idm.v1.PasswordRuleList> toPaswordRulesJaxb(
        List<PasswordRule> rules) {
        
        if (rules == null || rules.size() < 1) {
            return null;
        }
        
        com.rackspace.api.idm.v1.PasswordRuleList jaxbRules = objectFactory
            .createPasswordRuleList();

        for (PasswordRule rule : rules) {
            jaxbRules.getPasswordRule().add(this.toPasswordRuleJaxb(rule).getValue());
        }

        return objectFactory.createPasswordRules(jaxbRules);
    }

    public JAXBElement<com.rackspace.api.idm.v1.PasswordRule> toPasswordRuleJaxb(
        PasswordRule rule) {
        com.rackspace.api.idm.v1.PasswordRule jaxbRule = objectFactory.createPasswordRule();

        jaxbRule.setId(rule.getRuleId());
        jaxbRule.setMessage(rule.getMessage());
        jaxbRule.setName(rule.getRuleName());

        return objectFactory.createPasswordRule(jaxbRule);
    }

    public JAXBElement<com.rackspace.api.idm.v1.PasswordValidation> toPasswordValidationJaxb(
        PasswordComplexityResult passwordComplexityResult) {

        com.rackspace.api.idm.v1.PasswordValidation jaxbpasswordValidation = objectFactory
            .createPasswordValidation();

        com.rackspace.api.idm.v1.PasswordRuleResultList jaxbRulesResult = objectFactory
            .createPasswordRuleResultList();

        List<PasswordRuleResult> ruleResults = passwordComplexityResult
            .getPasswordRuleResults();

        for (PasswordRuleResult ruleResult : ruleResults) {
            jaxbRulesResult.getPasswordRuleResults().add(
                this.toPasswordRuleResult(ruleResult));
        }

        jaxbpasswordValidation.setPasswordRuleResults(jaxbRulesResult);
        jaxbpasswordValidation.setValidPassword(passwordComplexityResult
            .isValidPassword());

        return objectFactory.createPasswordValidation(jaxbpasswordValidation);
    }

    public com.rackspace.api.idm.v1.PasswordRuleResult toPasswordRuleResult(
        PasswordRuleResult result) {
        com.rackspace.api.idm.v1.PasswordRuleResult jaxbRuleResult = objectFactory
            .createPasswordRuleResult();
        jaxbRuleResult.setRuleId(result.getRuleId());
        jaxbRuleResult.setRuleName(result.getRuleName());
        jaxbRuleResult.setRuleMessage(result.getMessage());
        jaxbRuleResult.setPassed(result.isRulePassed());

        return jaxbRuleResult;
    }
}
