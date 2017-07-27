package com.rackspace.idm.domain.dao.impl;

import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import org.hamcrest.Matchers;
import org.junit.Ignore;
import org.junit.runner.RunWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import org.mockito.runners.MockitoJUnitRunner;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPInterface;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: matt.colton
 * Date: 8/13/12
 * Time: 10:12 AM
 * To change this template use File | Settings | File Templates.
 */
@RunWith(MockitoJUnitRunner.class)
public class LdapDomainRepositoryTest {
    @InjectMocks
    LdapDomainRepository ldapDomainRepository = new LdapDomainRepository();
    @Mock
    LdapConnectionPools ldapConnectionPools;
    @Mock
    Configuration configuration;

    LDAPInterface ldapInterface;

    @Before
    public void setUp() throws Exception {
        ldapInterface = mock(LDAPInterface.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void addDomain_domainIsNull_throwsIllegalArgument() throws Exception {
        ldapDomainRepository.addDomain(null);
    }

    @Test (expected = IllegalArgumentException.class)
    public void deleteDomain_domainIdIsBlank_throwsIllegalArgument() throws Exception {
        ldapDomainRepository.deleteDomain("");
    }

    @Test
    public void getDomain_domainIdIsBlank_returnsNull() throws Exception {
        Domain result = ldapDomainRepository.getDomain("");
        assertThat("domain", result, equalTo(null));
    }

}
