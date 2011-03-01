package com.rackspace.idm.domain.entity;


public class MinimumLengthRule extends PasswordRule {
    
    private static final int RULE_ID = 1;
    private static final String RULE_NAME = "Mininumn Length";
    private static final String RULE_MESSAGE = "The password must be at least 7 characters long";

    public MinimumLengthRule() {
        super(RULE_ID, RULE_NAME, RULE_MESSAGE);
    }
    
    @Override
    public PasswordRuleResult validatePassword(String password) {
        PasswordRuleResult result = new PasswordRuleResult();
        result.setRuleId(this.getRuleId());
        result.setRuleName(this.getRuleName());
        result.setMessage(this.getMessage());
        result.setRulePassed(password.length() >= 7);
        return result;
    }
}
