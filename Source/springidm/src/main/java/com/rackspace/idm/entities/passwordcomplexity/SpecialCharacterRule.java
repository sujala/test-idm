package com.rackspace.idm.entities.passwordcomplexity;

import java.util.regex.Pattern;


public class SpecialCharacterRule extends PasswordRule {
    private static final int RULE_ID = 5;
    private static final String RULE_NAME = "Special Character Rule";
    private static final String RULE_MESSAGE = "The password must contain a special character";

    private static final String SPECIAL_CHARACTER_REGEX = ".*\\W.*";
    private static final Pattern SPECIAL_CHARACTER_REGEX_PATTERN = Pattern
        .compile(SPECIAL_CHARACTER_REGEX);
    
    public SpecialCharacterRule() {
        super(RULE_ID, RULE_NAME, RULE_MESSAGE);
    }
    
    @Override
    public PasswordRuleResult validatePassword(String password) {
        PasswordRuleResult result = new PasswordRuleResult();
        result.setRuleId(this.getRuleId());
        result.setRuleName(this.getRuleName());
        result.setMessage(this.getMessage());

        result.setRulePassed(SPECIAL_CHARACTER_REGEX_PATTERN.matcher(password)
            .matches());
        
        return result;
    }
}
