package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.Policy;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPInterface;
import junit.framework.TestCase;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/10/12
 * Time: 5:21 PM
 * To change this template use File | Settings | File Templates.
 */
public class LdapPolicyRepositoryTest{

    LdapPolicyRepository ldapPolicyRepository;
    LdapPolicyRepository spy;
    LDAPInterface ldapInterface;
    Policy policy;


    @Before
    public void setUp() throws Exception {
        ldapPolicyRepository = new LdapPolicyRepository(mock(LdapConnectionPools.class), mock(Configuration.class));
        ldapInterface = mock(LDAPInterface.class);
        spy = spy(ldapPolicyRepository);
        doReturn(ldapInterface).when(spy).getAppInterface();
        policy = new Policy();
        policy.setPolicyType("someType");
        policy.setBlob("someBlob");
        policy.setDescription("someDescription");
        policy.setEnabled(true);
        policy.setGlobal(false);
        policy.setName("somename");
        policy.setPolicyId("123");
    }

    @Test(expected = IllegalArgumentException.class)
    public void addPolicy_policyIsNull_throwsIllegalArgument() throws Exception {
        ldapPolicyRepository.addPolicy(null);
    }

    @Test
    public void addPolicy_validPolicy_created() throws Exception {
        spy.addPolicy(policy);
        verify(spy, times(2)).getLogger();
    }

    @Test(expected = IllegalStateException.class)
    public void addPolicy_callsLDAPPersister_throwsIllegalStateException()  throws Exception {
        spy.addPolicy(new Policy());
    }

    @Test
    public void getPolicy_getValidPolicy_returnsPolicy() throws Exception {
        doReturn(policy).when(spy).getSinglePolicy(Matchers.<Filter>any());
        Policy policy = spy.getPolicy("123");
        assertThat("policy",policy.getPolicyId(),equalTo("123"));
    }

   @Test
    public void getPolicy_emptyPolicyId_returnsNull() throws Exception {
        Policy policy = spy.getPolicy("");
        assertThat("policy",policy,equalTo(null));
    }

    @Test
    public void getPolicy_nullPolicyId_returnsNull() throws Exception {
        Policy policy = spy.getPolicy(null);
        assertThat("policy",policy,equalTo(null));
    }

    @Test
    public void getPolicyByName_getValidPolicy_returnsPolicy() throws Exception {
        doReturn(policy).when(spy).getSinglePolicy(Matchers.<Filter>any());
        Policy policy = spy.getPolicyByName("someName");
        assertThat("policy",policy.getPolicyId(),equalTo("123"));
    }

   @Test
    public void getPolicyByName_emptyPolicyName_returnsNull() throws Exception {
        Policy policy = ldapPolicyRepository.getPolicyByName("");
        assertThat("policy",policy,equalTo(null));
    }

    @Test
    public void getPolicyByName_nullPolicyName_returnsNull() throws Exception {
        doReturn(policy).when(spy).getSinglePolicy(Matchers.<Filter>any());
        Policy policy = ldapPolicyRepository.getPolicy(null);
        assertThat("policy",policy,equalTo(null));
    }

    @Test
    public void updatePolicy_noDifferences_returns() throws Exception {
        doReturn(policy).when(spy).getSinglePolicy(Matchers.<Filter>any());
        spy.updatePolicy(policy,"123");
    }

    @Test
    public void updatePolicy_Differences_returns() throws Exception {
        Policy oldPolicy = new Policy();
        oldPolicy.setPolicyId("123");
        doReturn(oldPolicy).when(spy).getSinglePolicy(Matchers.<Filter>any());
        spy.updatePolicy(policy,"123");
        verify(spy,times(4)).getLogger();
    }


}
