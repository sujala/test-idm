package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.exception.BadRequestException;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.BadRequestFault;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/15/12
 * Time: 3:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class PolicyValidatorTest{
    private PolicyValidator policyValidator;

    @Before
    public void setUp(){
        policyValidator = new PolicyValidator();
    }

    @Test(expected = BadRequestException.class)
    public void validatePolicyName_specialCharacters_returns400() throws Exception{
        policyValidator.validatePolicyName("!#AV ");
    }

    @Test
    public void validatePolicyName_nameWithSpaces_returns200(){
        policyValidator.validatePolicyName("some name");
    }

    @Test(expected = BadRequestException.class)
    public void validatePolicyName_startWithANumber_returns400(){
        policyValidator.validatePolicyName("1someName");
    }

    @Test
    public void validatePolicy_nameWithNumbers_passes(){
        policyValidator.validatePolicyName("some name4");
    }

    @Test(expected = BadRequestException.class)
    public void validatePolicyName_nullName_return400(){
        policyValidator.validatePolicyName(null);
    }

    @Test(expected = BadRequestException.class)
    public void validatePolicyName_emptyString_returns400(){
        policyValidator.validatePolicyName("");
    }
}

