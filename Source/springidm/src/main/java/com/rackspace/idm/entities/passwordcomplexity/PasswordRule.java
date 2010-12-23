package com.rackspace.idm.entities.passwordcomplexity;

public abstract class PasswordRule {
    private int ruleId = 0;
    private String ruleName = "";
    private String message = "";

    public PasswordRule() {
    }

    public PasswordRule(int ruleId, String ruleName, String message) {
        this.ruleId = ruleId;
        this.ruleName = ruleName;
        this.message = message;
    }

    public int getRuleId() {
        return ruleId;
    }

    public void setRuleId(int ruleId) {
        this.ruleId = ruleId;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public abstract PasswordRuleResult validatePassword(String password);
}
