package com.rackspace.idm.domain.entity;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "passwordValidation")
@XmlAccessorType(XmlAccessType.NONE)
public class PasswordComplexityResult {

    private List<PasswordRuleResult> passwordRuleResults = new ArrayList<PasswordRuleResult>();

    public PasswordComplexityResult() {
    }

    @XmlElement(name = "validPassword")
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

    @XmlElementWrapper(name = "ruleResults")
    @XmlElement(name = "ruleResult")
    public List<PasswordRuleResult> getPasswordRuleResults() {
        return passwordRuleResults;
    }
}
