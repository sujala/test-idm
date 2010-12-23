package com.rackspace.idm.entities.passwordcomplexity;

import java.util.regex.Pattern;


public class NumberRule extends PasswordRule {
    private static final int RULE_ID = 4;
    private static final String RULE_NAME = "Number Rule";
    private static final String RULE_MESSAGE = "The password must contain a number";

    private static final String NUMBER_REGEX = ".*\\d.*";
    private static final Pattern NUMBER_REGEX_PATTERN = Pattern
        .compile(NUMBER_REGEX);
    
    public NumberRule() {
        super(RULE_ID, RULE_NAME, RULE_MESSAGE);
    }
    
    @Override
    public PasswordRuleResult validatePassword(String password) {
        PasswordRuleResult result = new PasswordRuleResult();
        result.setRuleId(this.getRuleId());
        result.setRuleName(this.getRuleName());
        result.setMessage(this.getMessage());

        result.setRulePassed(NUMBER_REGEX_PATTERN.matcher(password)
            .matches());
        
        return result;
    }
}
