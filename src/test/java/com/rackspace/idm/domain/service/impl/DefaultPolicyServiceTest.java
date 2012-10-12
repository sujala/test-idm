package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.EndpointDao;
import com.rackspace.idm.domain.dao.PolicyDao;
import com.rackspace.idm.domain.entity.CloudBaseUrl;
import com.rackspace.idm.domain.entity.Policies;
import com.rackspace.idm.domain.entity.Policy;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/13/12
 * Time: 1:50 PM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultPolicyServiceTest{

    @Mock
    PolicyDao policyDao;
    @InjectMocks
    DefaultPolicyService defaultPolicyService = new DefaultPolicyService();
    EndpointDao endpointDao = mock(EndpointDao.class);
    DefaultPolicyService spy;

    @Before
    public void setUp() throws Exception {
        defaultPolicyService.setEndpointDao(endpointDao);
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

    @Test(expected = BadRequestException.class)
    public void testAddPolicy_nullPolicy_returnBadRequestException() throws Exception {
        defaultPolicyService.addPolicy(null);
    }

    @Test(expected = BadRequestException.class)
    public void testAddPolicy_nullName_returnBadRequestException() throws Exception {
        Policy policy = new Policy();
        policy.setPolicyId("123");
        defaultPolicyService.addPolicy(policy);
    }

    @Test(expected = BadRequestException.class)
    public void testAddPolicy_nullBlob_returnBadRequestException() throws Exception {
        Policy policy = new Policy();
        policy.setName("name");
        policy.setPolicyId("123");
        defaultPolicyService.addPolicy(policy);
    }

    @Test(expected = BadRequestException.class)
    public void testAddPolicy_nullType_returnBadRequestException() throws Exception {
        Policy policy = new Policy();
        policy.setName("name");
        policy.setPolicyId("123");
        policy.setBlob("someBlob");
        defaultPolicyService.addPolicy(policy);
    }

    @Test
    public void testAddPolicy_validPolicy_callsAddPolicyDAO() throws Exception {
        Policy policy = new Policy();
        policy.setName("name");
        policy.setPolicyId("123");
        policy.setBlob("someBlob");
        policy.setPolicyType("someType");
        defaultPolicyService.addPolicy(policy);
        verify(policyDao).addPolicy(policy);
    }

    @Test
    public void testGetPolicy_validPolicyId() throws Exception {
        Policy policy = new Policy();
        policy.setPolicyId("1");
        policy.setName("name");
        policy.setBlob("someBlob");
        policy.setPolicyType("someType");
        when(policyDao.getPolicy(anyString())).thenReturn(policy);
        Policy getPolicy = defaultPolicyService.getPolicy("1");
        assertThat("Check return policy", getPolicy.getName(), equalTo("name"));
    }

    @Test(expected = NotFoundException.class)
    public void testGetPolicy_invalidPolicyId() throws Exception {
        when(policyDao.getPolicy(anyString())).thenReturn(null);
        Policy getPolicy = defaultPolicyService.getPolicy("1");
        assertThat("Check return policy", getPolicy.getName(), equalTo("name"));
    }

    @Test(expected = BadRequestException.class)
    public void testUpdatePolicy_nullPolicy() throws Exception {
        defaultPolicyService.updatePolicy(null,"1");
    }

    @Test(expected = BadRequestException.class)
    public void testUpdatePolicy_nullName() throws Exception {
        Policy policy = new Policy();
        policy.setPolicyId("1");
        policy.setBlob("someBlob");
        policy.setPolicyType("someType");
        defaultPolicyService.updatePolicy(policy,"1");
    }

    @Test
    public void testUpdatePolicy() throws Exception {
        Policy policy = new Policy();
        policy.setPolicyId("1");
        policy.setBlob("someBlob");
        policy.setPolicyType("someType");
        policy.setName("name");
        defaultPolicyService.updatePolicy(policy,"1");
        verify(policyDao).updatePolicy(policy,"1");
    }

    @Test(expected = NotFoundException.class)
    public void testDeletePolicy_nullId() throws Exception {
        defaultPolicyService.deletePolicy(null);

    }

    @Test
    public void testDeletePolicy() throws Exception {
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        Policy policy = new Policy();
        policy.setPolicyId("1");
        policy.setName("name");
        policy.setBlob("someBlob");
        policy.setPolicyType("someType");
        when(endpointDao.getBaseUrlsWithPolicyId(anyString())).thenReturn(cloudBaseUrlList);
        when(policyDao.getPolicy(anyString())).thenReturn(policy);
        defaultPolicyService.deletePolicy("1");
        verify(policyDao).deletePolicy(policy.getPolicyId());

    }
}
