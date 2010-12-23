package com.rackspace.idm.entities.passwordcomplexity;

public class LowercaseRule extends PasswordRule {
    private static final int RULE_ID = 3;
    private static final String RULE_NAME = "Lowercase Rule";
    private static final String RULE_MESSAGE = "The password must contain a lowercase character";

    public LowercaseRule() {
        super(RULE_ID, RULE_NAME, RULE_MESSAGE);
    }

    @Override
    public PasswordRuleResult validatePassword(String password) {
        PasswordRuleResult result = new PasswordRuleResult();
        result.setRuleId(this.getRuleId());
        result.setRuleName(this.getRuleName());
        result.setMessage(this.getMessage());

        String upperCase = password.toUpperCase();
        result.setRulePassed(!upperCase.equals(password));

        return result;
    }
}
