package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 7/24/12
 * Time: 1:32 PM
 * To change this template use File | Settings | File Templates.
 */
public class LdapAuthRepositoryTest {

    LdapAuthRepository authRepo;
    LDAPConnectionPool connPool;
    Configuration config;
    
    @Before
    public void setUp() throws Exception {
        authRepo = new LdapAuthRepository(connPool, config);
        authRepo = spy(authRepo);
    }

    @Test
    public void getRackerRoles_callsSearchForEntry() throws Exception {
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        when(authRepo.getLdapInterface()).thenReturn(ldapInterface);
        SearchResultEntry result = new SearchResultEntry("dn", new Attribute[]{new Attribute("groupMembership", new String[]{})});
        when(ldapInterface.searchForEntry(anyString(), eq(SearchScope.ONE), any(Filter.class))).thenReturn(result);
        authRepo.getRackerRoles("");
        verify(authRepo).getLdapInterface();
        verify(ldapInterface).searchForEntry(anyString(), eq(SearchScope.ONE), any(Filter.class));
    }

    @Test(expected = IllegalStateException.class)
    public void getRackerRoles_withLdapException_throwsIllegalStateException() throws Exception {
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        when(authRepo.getLdapInterface()).thenReturn(ldapInterface);
        when(ldapInterface.searchForEntry(anyString(), eq(SearchScope.ONE), any(Filter.class))).thenThrow(new IllegalStateException());
        authRepo.getRackerRoles("");
    }

    @Test(expected = NotFoundException.class)
    public void getRackerRoles_withNullSearchResult_throwsNotFoundException() throws Exception {
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        when(authRepo.getLdapInterface()).thenReturn(ldapInterface);
        when(ldapInterface.searchForEntry(anyString(), eq(SearchScope.ONE), any(Filter.class))).thenReturn(null);
        authRepo.getRackerRoles("");
    }

    @Test
    public void getRackerRoles_withNoGroupMemberships_returnsEmptyRoles() throws Exception {
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        when(authRepo.getLdapInterface()).thenReturn(ldapInterface);
        SearchResultEntry result = new SearchResultEntry("dn", new Attribute[]{new Attribute("groupMembership", new String[]{})});
        when(ldapInterface.searchForEntry(anyString(), eq(SearchScope.ONE), any(Filter.class))).thenReturn(result);
        List<String> rackerRoles = authRepo.getRackerRoles("");
        assertThat("racker roles size", rackerRoles.size(), equalTo(0));
    }

    @Test
    public void getRackerRoles_withGroupMemberships_returnsCorrectRoles() throws Exception {
        LDAPInterface ldapInterface = mock(LDAPInterface.class);
        when(authRepo.getLdapInterface()).thenReturn(ldapInterface);
        SearchResultEntry result = new SearchResultEntry("dn", new Attribute[]{new Attribute("groupMembership", new String[]{"somePart=otherPart,something?"})});
        when(ldapInterface.searchForEntry(anyString(), eq(SearchScope.ONE), any(Filter.class))).thenReturn(result);
        List<String> rackerRoles = authRepo.getRackerRoles("");
        assertThat("racker roles size", rackerRoles.size(), equalTo(1));
        assertThat("racker roles", rackerRoles.get(0), equalTo("otherPart"));
    }

    @Test
    public void getLdapInterface_returnsConnPool() throws Exception {
        LDAPInterface ldapInterface = authRepo.getLdapInterface();
        assertThat("ldap interface", ldapInterface, nullValue()); //Because we set it to null in the @Before
    }
}
