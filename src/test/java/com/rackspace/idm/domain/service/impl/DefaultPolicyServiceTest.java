package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.PolicyDao;
import com.rackspace.idm.domain.dao.impl.LdapPolicyRepository;
import com.rackspace.idm.domain.entity.Policies;
import com.rackspace.idm.domain.entity.Policy;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/13/12
 * Time: 1:50 PM
 * To change this template use File | Settings | File Templates.
 */
public class DefaultPolicyServiceTest{

    PolicyDao policyDao = mock(PolicyDao.class);
    DefaultPolicyService defaultPolicyService = new DefaultPolicyService(policyDao);
    DefaultPolicyService spy;

    @Before
    public void setUp() throws Exception {
        defaultPolicyService = new DefaultPolicyService(policyDao);
        spy = spy(defaultPolicyService);
    }

    @Test
    public void testGetPolicies() throws Exception {
        Policy policy = new Policy();
        Policies policies = new Policies();
        policy.setName("name");
        policy.setPolicyId("12345");
        policies.getPolicy().add(policy);
        when(policyDao.getPolicies()).thenReturn(policies);
        Policies policiesList = defaultPolicyService.getPolicies();
        assertThat("Test Policies", policiesList.getPolicy().get(0).getName(),equalTo("name"));

    }

    @Test
    public void testAddPolicy() throws Exception {

    }

    @Test
    public void testGetPolicy() throws Exception {

    }

    @Test
    public void testUpdatePolicy() throws Exception {

    }

    @Test
    public void testDeletePolicy() throws Exception {

    }
}
