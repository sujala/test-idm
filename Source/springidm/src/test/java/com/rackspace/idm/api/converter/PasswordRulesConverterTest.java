package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.PasswordRule;
import com.rackspace.idm.domain.entity.PasswordRuleResult;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;

import java.util.ArrayList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/11/12
 * Time: 2:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class PasswordRulesConverterTest {

    PasswordRulesConverter passwordRulesConverter;

    @Before
    public void setUp() throws Exception {
        passwordRulesConverter = new PasswordRulesConverter();

    }

    @Test
    public void toPaswordRulesJaxb_withNull_returnsNull() throws Exception {
        JAXBElement<PasswordRuleList> passwordRuleListJAXBElement = passwordRulesConverter.toPaswordRulesJaxb(null);
        assertThat("password rule list", passwordRuleListJAXBElement, nullValue());
    }

    @Test
    public void toPaswordRulesJaxb_withZeroRules_returnsNull() throws Exception {
        JAXBElement<PasswordRuleList> passwordRuleListJAXBElement = passwordRulesConverter.toPaswordRulesJaxb(new ArrayList<PasswordRule>());
        assertThat("password rule list", passwordRuleListJAXBElement, nullValue());
    }

    @Test
    public void toPaswordRulesJaxb_withOneRule_returnsListSizeOne() throws Exception {
        ArrayList<PasswordRule> rules = new ArrayList<PasswordRule>();
        rules.add(new LowercaseRule());
        JAXBElement<PasswordRuleList> passwordRuleListJAXBElement = passwordRulesConverter.toPaswordRulesJaxb(rules);
        assertThat("password rule list size", passwordRuleListJAXBElement.getValue().getPasswordRule().size(), equalTo(1));
    }

    @Test
    public void toPaswordRulesJaxb_withLowercaseRule_returnsListWithLowercaseRule() throws Exception {
        ArrayList<PasswordRule> rules = new ArrayList<PasswordRule>();
        rules.add(new LowercaseRule());
        JAXBElement<PasswordRuleList> passwordRuleListJAXBElement = passwordRulesConverter.toPaswordRulesJaxb(rules);
        assertThat("list contains lowercaseRule", passwordRuleListJAXBElement.getValue().getPasswordRule().get(0).getId(), equalTo(3));
    }

    @Test
    public void toPasswordRuleJaxb_withPasswordRule_setsRuleId() throws Exception {
        JAXBElement<com.rackspace.api.idm.v1.PasswordRule> passwordRuleJAXBElement = passwordRulesConverter.toPasswordRuleJaxb(new MinimumLengthRule());
        assertThat("password rule id", passwordRuleJAXBElement.getValue().getId(), equalTo(1));
    }

    @Test
    public void toPasswordRuleJaxb_withPasswordRule_setsRuleName() throws Exception {
        JAXBElement<com.rackspace.api.idm.v1.PasswordRule> passwordRuleJAXBElement = passwordRulesConverter.toPasswordRuleJaxb(new MinimumLengthRule());
        assertThat("password rule name", passwordRuleJAXBElement.getValue().getName(), equalTo("Mininumn Length"));
    }

    @Test
    public void toPasswordRuleJaxb_withPasswordRule_setsRuleMessage() throws Exception {
        JAXBElement<com.rackspace.api.idm.v1.PasswordRule> passwordRuleJAXBElement = passwordRulesConverter.toPasswordRuleJaxb(new MinimumLengthRule());
        assertThat("password rule message", passwordRuleJAXBElement.getValue().getMessage(), equalTo("The password must be at least 7 characters long"));
    }

    @Test
    public void toPasswordValidationJaxb_withPasswordComplexityResult_withOneRuleResult_returnsJaxbPasswordValidation_withOnePasswordRuleResult() throws Exception {
        PasswordComplexityResult passwordComplexityResult = new PasswordComplexityResult();
        passwordComplexityResult.addPasswordRuleResult(new PasswordRuleResult(true, 1, "minimum length", "minimum length message"));
        JAXBElement<PasswordValidation> passwordValidationJAXBElement = passwordRulesConverter.toPasswordValidationJaxb(passwordComplexityResult);
        assertThat("password validation results size", passwordValidationJAXBElement.getValue().getPasswordRuleResults().getPasswordRuleResults().size(), equalTo(1));
    }

    @Test
    public void toPasswordValidationJaxb_withPasswordComplexityResult_withZeroRuleResult_returnsJaxbPasswordValidation_withZeroPasswordRuleResult() throws Exception {
        PasswordComplexityResult passwordComplexityResult = new PasswordComplexityResult();
        JAXBElement<PasswordValidation> passwordValidationJAXBElement = passwordRulesConverter.toPasswordValidationJaxb(passwordComplexityResult);
        assertThat("password validation results size", passwordValidationJAXBElement.getValue().getPasswordRuleResults().getPasswordRuleResults().size(), equalTo(0));
    }

    @Test
    public void toPasswordValidationJaxb_withPasswordComplexityResult_withMinimumLengthResult_returnsJaxbPasswordValidation_withMinimumLengthResult() throws Exception {
        PasswordComplexityResult passwordComplexityResult = new PasswordComplexityResult();
        passwordComplexityResult.addPasswordRuleResult(new PasswordRuleResult(true, 1, "minimum length", "minimum length message"));
        JAXBElement<PasswordValidation> passwordValidationJAXBElement = passwordRulesConverter.toPasswordValidationJaxb(passwordComplexityResult);
        assertThat("password validation results", passwordValidationJAXBElement.getValue().getPasswordRuleResults().getPasswordRuleResults().get(0).getRuleId(), equalTo(1));
    }

    @Test
    public void toPasswordValidationJaxb_withPasswordComplexity_withAllPassingTests_setsValidPasswordToTrue() throws Exception {
        PasswordComplexityResult passwordComplexityResult = new PasswordComplexityResult();
        passwordComplexityResult.addPasswordRuleResult(new PasswordRuleResult(true, 1, "minimum length", "minimum length message"));
        JAXBElement<PasswordValidation> passwordValidationJAXBElement = passwordRulesConverter.toPasswordValidationJaxb(passwordComplexityResult);
        assertThat("password validation results", passwordValidationJAXBElement.getValue().isValidPassword(), equalTo(true));
    }

    @Test
    public void toPasswordValidationJaxb_withPasswordComplexity_withFailingTest_setsValidPasswordToFalse() throws Exception {
        PasswordComplexityResult passwordComplexityResult = new PasswordComplexityResult();
        passwordComplexityResult.addPasswordRuleResult(new PasswordRuleResult(true, 1, "minimum length", "minimum length message"));
        passwordComplexityResult.addPasswordRuleResult(new PasswordRuleResult(false, 1, "minimum length", "minimum length message"));
        JAXBElement<PasswordValidation> passwordValidationJAXBElement = passwordRulesConverter.toPasswordValidationJaxb(passwordComplexityResult);
        assertThat("password validation results", passwordValidationJAXBElement.getValue().isValidPassword(), equalTo(false));
    }

    @Test
    public void toPasswordValidationJaxb_withPasswordComplexity_withNoTests_setsValidPasswordToTrue() throws Exception {
        PasswordComplexityResult passwordComplexityResult = new PasswordComplexityResult();
        JAXBElement<PasswordValidation> passwordValidationJAXBElement = passwordRulesConverter.toPasswordValidationJaxb(passwordComplexityResult);
        assertThat("password validation results", passwordValidationJAXBElement.getValue().isValidPassword(), equalTo(true));
    }

    @Test
    public void toApiPasswordRuleResult_withDomainPasswordRuleResult_setsRuleId() throws Exception {
        PasswordRuleResult passwordRuleResult = new PasswordRuleResult(true, 1, "minimumLength", "minimumLengthMessage");
        com.rackspace.api.idm.v1.PasswordRuleResult apiPasswordRuleResult = passwordRulesConverter.toPasswordRuleResult(passwordRuleResult);
        assertThat("password rule set id", apiPasswordRuleResult.getRuleId(), equalTo(1));
    }

    @Test
    public void toApiPasswordRuleResult_withDomainPasswordRuleResult_setsRuleName() throws Exception {
        PasswordRuleResult passwordRuleResult = new PasswordRuleResult(true, 1, "minimumLength", "minimumLengthMessage");
        com.rackspace.api.idm.v1.PasswordRuleResult apiPasswordRuleResult = passwordRulesConverter.toPasswordRuleResult(passwordRuleResult);
        assertThat("password rule set name", apiPasswordRuleResult.getRuleName(), equalTo("minimumLength"));
    }

    @Test
    public void toApiPasswordRuleResult_withDomainPasswordRuleResult_setsRuleMessage() throws Exception {
        PasswordRuleResult passwordRuleResult = new PasswordRuleResult(true, 1, "minimumLength", "minimumLengthMessage");
        com.rackspace.api.idm.v1.PasswordRuleResult apiPasswordRuleResult = passwordRulesConverter.toPasswordRuleResult(passwordRuleResult);
        assertThat("password rule set message", apiPasswordRuleResult.getRuleMessage(), equalTo("minimumLengthMessage"));
    }
}
