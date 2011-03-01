package com.rackspace.idm.domain.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rackspace.idm.domain.entity.LowercaseRule;
import com.rackspace.idm.domain.entity.MinimumLengthRule;
import com.rackspace.idm.domain.entity.NumberRule;
import com.rackspace.idm.domain.entity.PasswordComplexityResult;
import com.rackspace.idm.domain.entity.PasswordRule;
import com.rackspace.idm.domain.entity.SpecialCharacterRule;
import com.rackspace.idm.domain.entity.UppercaseRule;
import com.rackspace.idm.domain.service.PasswordComplexityService;

public class DefaultPasswordComplexityService implements
    PasswordComplexityService {

    private MinimumLengthRule minLengthRule = new MinimumLengthRule();
    private UppercaseRule uppercaseRule = new UppercaseRule();
    private LowercaseRule lowercaseRule = new LowercaseRule();
    private SpecialCharacterRule specialCharacterRule = new SpecialCharacterRule();
    private NumberRule numberRule = new NumberRule();
    final private Logger logger = LoggerFactory.getLogger(this.getClass());

    private List<PasswordRule> rules = new ArrayList<PasswordRule>();

    public DefaultPasswordComplexityService() {
        
        rules.add(minLengthRule);
        rules.add(uppercaseRule);
        rules.add(lowercaseRule);
        rules.add(specialCharacterRule);
        rules.add(numberRule);
    }

    public PasswordComplexityResult checkPassword(String password) {
        logger.debug("Validating password: {}", password);
        PasswordComplexityResult result = new PasswordComplexityResult();

        for (PasswordRule rule : rules) {
            result.addPasswordRuleResult(rule.validatePassword(password));
        }
        logger.debug("Validated password: {} - {}", password, result.isValidPassword());
        return result;
    }

    public List<PasswordRule> getRules() {
        logger.debug("Retrieving password complexity rules - {}", rules);
        return rules;
    }
}
