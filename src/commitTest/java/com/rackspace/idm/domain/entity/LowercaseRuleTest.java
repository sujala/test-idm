package com.rackspace.idm.domain.entity;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/11/12
 * Time: 5:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class LowercaseRuleTest {

    LowercaseRule lowercaseRule;

    @Before
    public void setUp() throws Exception {
        lowercaseRule = new LowercaseRule();
    }

    @Test
    public void setRuleId_correctlySetsRuleId() throws Exception {
        assertThat("rule id",lowercaseRule.getRuleId(),equalTo(3));
        lowercaseRule.setRuleId(123);
        assertThat("rule id",lowercaseRule.getRuleId(),equalTo(123));
    }

    @Test
    public void setRuleName_correctlySetsRuleName() throws Exception {
        assertThat("rule name",lowercaseRule.getRuleName(),equalTo("Lowercase Rule"));
        lowercaseRule.setRuleName("New Rule");
        assertThat("rule name",lowercaseRule.getRuleName(),equalTo("New Rule"));
    }

    @Test
    public void setMessage_correctlySetsMessage() throws Exception {
        assertThat("message",lowercaseRule.getMessage(),equalTo("The password must contain a lowercase character"));
        lowercaseRule.setMessage("This is a new message");
        assertThat("message",lowercaseRule.getMessage(),equalTo("This is a new message"));
    }
}
