package com.rackspace.idm.domain.dao.impl;

import com.unboundid.ldap.sdk.*;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.Policy;
import junit.framework.TestCase;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import java.util.List;

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
@RunWith(MockitoJUnitRunner.class)
public class LdapPolicyRepositoryTest {
    @InjectMocks
    LdapPolicyRepository ldapPolicyRepository = new LdapPolicyRepository();
    @Mock
    LdapConnectionPools ldapConnectionPools;
    @Mock
    Configuration configuration;
    @Mock
    LDAPInterface ldapInterface;

    Policy policy;

    @Before
    public void setUp() throws Exception {
        policy = new Policy();
        policy.setLdapEntry(new ReadOnlyEntry("dn:rsId=id,ou=rackspace", "policy:policyId"));
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

}
