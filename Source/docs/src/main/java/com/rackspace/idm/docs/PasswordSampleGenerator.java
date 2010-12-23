package com.rackspace.idm.docs;

import java.io.IOException;

import javax.xml.bind.JAXBException;

import com.rackspace.idm.jaxb.CustomParam;
import com.rackspace.idm.jaxb.CustomParamsList;
import com.rackspace.idm.jaxb.PasswordRecovery;
import com.rackspace.idm.jaxb.PasswordRule;
import com.rackspace.idm.jaxb.PasswordRuleResult;
import com.rackspace.idm.jaxb.PasswordRuleResults;
import com.rackspace.idm.jaxb.PasswordRules;
import com.rackspace.idm.jaxb.PasswordValidation;

public class PasswordSampleGenerator extends SampleGenerator {
    private PasswordSampleGenerator() {
        super();
    }

    public static void main(String[] args) throws JAXBException, IOException {
        PasswordSampleGenerator sampleGen = new PasswordSampleGenerator();

        sampleGen.marshalToFiles(sampleGen.getPasswordRule(), "password_rule");
        sampleGen.marshalToFiles(sampleGen.getPasswordRules(), "password_rules");
        sampleGen.marshalToFiles(sampleGen.getPasswordRuleResult(), "password_rule_result");
        sampleGen.marshalToFiles(sampleGen.getPasswordRuleResults(), "password_rule_results");
        sampleGen.marshalToFiles(sampleGen.getPasswordValidation(), "password_validation");
        sampleGen.marshalToFiles(sampleGen.getPasswordRecovery(), "password_recovery");
    }
    
    private PasswordRule getPasswordRule() {
        PasswordRule rule = of.createPasswordRule();
        rule.setId(1);
        rule.setName("Mininum Legth");
        rule.setMessage("Password must be at least 7 characters long.");
        return rule;
    }
    
    private PasswordRule getPasswordRule2() {
        PasswordRule rule = of.createPasswordRule();
        rule.setId(2);
        rule.setName("Lowercase Character");
        rule.setMessage("Password must contain a lowercase character.");
        return rule;
    }
    
    private PasswordRules getPasswordRules() {
        PasswordRules rules = of.createPasswordRules();
        PasswordRule rule = getPasswordRule();
        rules.getPasswordRules().add(rule);
        PasswordRule rule2 = getPasswordRule2();
        rules.getPasswordRules().add(rule2);
        return rules;
    }
    
    private PasswordRuleResult getPasswordRuleResult() {
        PasswordRuleResult result = of.createPasswordRuleResult();
        result.setPassed(true);
        result.setRuleId(1);
        result.setRuleMessage("Password must be at least 7 characters long.");
        result.setRuleName("Minimum Length");
        return result;
    }
    
    private PasswordRuleResult getPasswordRuleResult2() {
        PasswordRuleResult result = of.createPasswordRuleResult();
        result.setPassed(true);
        result.setRuleId(2);
        result.setRuleMessage("Password must contain a lowercase character.");
        result.setRuleName("Lowercase Character");
        return result;
    }
    
    private PasswordRuleResults getPasswordRuleResults() {
        PasswordRuleResults results = of.createPasswordRuleResults();
        PasswordRuleResult result = getPasswordRuleResult();
        results.getPasswordRuleResults().add(result);
        PasswordRuleResult result2 = getPasswordRuleResult2();
        results.getPasswordRuleResults().add(result2);
        return results;
    }
    
    private PasswordValidation getPasswordValidation() {
        PasswordValidation validation = of.createPasswordValidation();
        validation.setValidPassword(true);
        validation.setPasswordRuleResults(getPasswordRuleResults());
        return validation;
    }
    
    private PasswordRecovery getPasswordRecovery() {
        PasswordRecovery rec = of.createPasswordRecovery();
        rec.setCallbackUrl("http://www.someurl.com");
        rec.setFrom("email@email.com");
        rec.setReplyTo("replay@email.com");
        rec.setSubject("Password Recovery");
        rec.setTemplateUrl("http://www.someurl.com");
        
        CustomParam parm = of.createCustomParam();
        parm.setName("FirstName");
        parm.setValue("Steve");
        
        CustomParamsList parms = of.createCustomParamsList();
        parms.getParams().add(parm);
        
        rec.setCustomParams(parms);
        
        return rec;
    }
}
