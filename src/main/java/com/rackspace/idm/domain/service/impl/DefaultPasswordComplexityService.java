package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.PasswordComplexityService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
public class DefaultPasswordComplexityService implements
    PasswordComplexityService {

    private MinimumLengthRule minLengthRule = new MinimumLengthRule();
    private UppercaseRule uppercaseRule = new UppercaseRule();
    private LowercaseRule lowercaseRule = new LowercaseRule();
//    private SpecialCharacterRule specialCharacterRule = new SpecialCharacterRule();
    private NumberRule numberRule = new NumberRule();
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private List<PasswordRule> rules = new ArrayList<PasswordRule>();

    @PostConstruct
    public void initialize() {
        rules.add(minLengthRule);
        rules.add(uppercaseRule);
        rules.add(lowercaseRule);
        //rules.add(specialCharacterRule); //ToDo: Removed for migration since Cloud Auth doesn't check for special chars
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
