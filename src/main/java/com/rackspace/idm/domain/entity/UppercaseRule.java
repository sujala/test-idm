package com.rackspace.idm.domain.entity;


public class UppercaseRule extends PasswordRule{
    private static final int RULE_ID = 2;
    private static final String RULE_NAME = "Uppercase Rule";
    private static final String RULE_MESSAGE = "The password must contain an uppercase character";

    public UppercaseRule() {
        super(RULE_ID, RULE_NAME, RULE_MESSAGE);
    }
    
    @Override
    public PasswordRuleResult validatePassword(String password) {
        PasswordRuleResult result = new PasswordRuleResult();
        result.setRuleId(this.getRuleId());
        result.setRuleName(this.getRuleName());
        result.setMessage(this.getMessage());
        
        String lowerCase = password.toLowerCase();
        result.setRulePassed(!lowerCase.equals(password));
        
        return result;
    }
}
