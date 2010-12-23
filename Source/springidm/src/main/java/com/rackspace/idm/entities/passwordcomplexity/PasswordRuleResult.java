package com.rackspace.idm.entities.passwordcomplexity;

public class PasswordRuleResult {
    private int ruleId = 0;
    private String ruleName = "";
    private String message = "";
    private boolean rulePassed = false;

    public PasswordRuleResult() {
    }

    public PasswordRuleResult(boolean rulePassed, int ruleId, String ruleName,
        String message) {
        this.rulePassed = rulePassed;
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

    public boolean isRulePassed() {
        return rulePassed;
    }

    public void setRulePassed(boolean rulePassed) {
        this.rulePassed = rulePassed;
    }
}
