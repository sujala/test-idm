package com.rackspace.idm.domain.entity;

import java.util.ArrayList;
import java.util.List;

public class PasswordComplexityResult {

    private final List<PasswordRuleResult> passwordRuleResults = new ArrayList<PasswordRuleResult>();

    public PasswordComplexityResult() {
    }

    public boolean isValidPassword() {
        boolean isValid = true;

        for (PasswordRuleResult result : passwordRuleResults) {
            if (!result.isRulePassed()) {
                isValid = false;
            }
        }
        return isValid;
    }

    public void addPasswordRuleResult(PasswordRuleResult result) {
        this.passwordRuleResults.add(result);
    }

    public List<PasswordRuleResult> getPasswordRuleResults() {
        return passwordRuleResults;
    }
}
