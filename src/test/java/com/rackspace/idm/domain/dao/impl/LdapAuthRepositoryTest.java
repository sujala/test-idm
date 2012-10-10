package com.rackspace.idm.domain.dao.impl;

import org.mockito.InjectMocks;
import org.mockito.Mock;

import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

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
@RunWith(PowerMockRunner.class)          //takes forever use only for small classes when absolutely necessary.
@PrepareForTest(LDAPConnectionPool.class)
public class LdapAuthRepositoryTest {

    @InjectMocks
    LdapAuthRepository authRepo = new LdapAuthRepository();
    @Mock
    LDAPConnectionPool connPool;
    @Mock
    Configuration config;
    
    @Before
    public void setUp() throws Exception {
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
        when(ldapInterface.searchForEntry(anyString(), eq(SearchScope.ONE), any(Filter.class))).thenThrow(new LDAPSearchException(ResultCode.NOT_SUPPORTED, "ERROR"));
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
    public void authenticate_callsConnPool_bind_and_callsConfig_getStringBaseDn() throws Exception {
        when(connPool.bind(anyString(), anyString())).thenReturn(new BindResult(new LDAPResult(123, ResultCode.SUCCESS)));
        authRepo.authenticate("someUser", "somePass");
        verify(connPool).bind(anyString(), anyString());
        verify(config).getString(eq("auth.ldap.base.dn"), anyString());
    }

    @Test
    public void authenticate_withLdapException_returnsFalse() throws Exception {
        when(connPool.bind(anyString(), anyString())).thenThrow(new LDAPSearchException(new LDAPException(ResultCode.NOT_SUPPORTED)));
        boolean authenticated = authRepo.authenticate("someUser", "somePass");
        assertThat("authenticated", authenticated, equalTo(false));
    }

    @Test
    public void authenticate_withLdapException_withInvalidCredentialsResultCode_returnsFalse() throws Exception {
        when(connPool.bind(anyString(), anyString())).thenThrow(new LDAPSearchException(new LDAPException(ResultCode.INVALID_CREDENTIALS)));
        boolean authenticated = authRepo.authenticate("someUser", "somePass");
        assertThat("authenticated", authenticated, equalTo(false));
    }

    @Test(expected = IllegalStateException.class)
    public void authenticate_withNullResult_throwsIllegalStateException() throws Exception {
        when(connPool.bind(anyString(), anyString())).thenReturn(null);
        authRepo.authenticate("someUser", "somePass");
        verify(connPool).bind(anyString(), anyString());
    }
}
