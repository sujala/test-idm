package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationRequest;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotAuthenticatedException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class DefaultScopeAccessServiceTestOld {

    @InjectMocks
    DefaultScopeAccessService defaultScopeAccessService = new DefaultScopeAccessService();
    @Mock
    ScopeAccessDao scopeAccessDao;
    @Mock
    Configuration configuration;
    @Mock
    private UserService userService;
    @Mock
    private ApplicationService applicationService;
    @Mock
    private TenantService tenantService;
    @Mock
    private EndpointService endpointService;
    @Mock
    private AuthHeaderHelper authHeaderHelper;

    ImpersonationRequest impersonationRequest;

    @Before
    public void setUp() throws Exception {
        impersonationRequest = new ImpersonationRequest();
        org.openstack.docs.identity.api.v2.User user = new org.openstack.docs.identity.api.v2.User();
        user.setUsername("impersonatedUser");
        impersonationRequest.setUser(user);
        when(configuration.getInt("token.cloudAuthExpirationSeconds")).thenReturn(86400);
        when(configuration.getInt("token.cloudAuthExpirationSeconds")).thenReturn(86400);
        when(configuration.getInt("token.refreshWindowHours")).thenReturn(12);
    }

    @Test
    public void addDirectScopeAccess_scopeAccessIsNull_throwsIllegalArgumentException() throws Exception {
        try{
            defaultScopeAccessService.addUserScopeAccess(null, null);
            assertTrue("illegalArgumentException expected",false);
        }catch (IllegalArgumentException ex)
        {
            assertThat("exception message", ex.getMessage(),equalTo("Null argument passed in."));
        }
    }

    @Test
    public void authenticateAccessToken_scopeAccessInstanceOfHasAccessTokenAndTokenNotExpired_authenticatedIsTrue() throws Exception {
        ScopeAccess scopeAccess = new ImpersonatedScopeAccess();
        (scopeAccess).setAccessTokenString("foo");
        (scopeAccess).setAccessTokenExp(new DateTime().plusMinutes(5).toDate());
        when(scopeAccessDao.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        assertThat("boolean", defaultScopeAccessService.authenticateAccessToken(null), equalTo(true));
    }

    @Test
    public void authenticateAccessToken_scopeAccessInstanceOfHasAccessTokenAndTokenNotExpired_getsAuditContext() throws Exception {
        ScopeAccess scopeAccess = mock(ImpersonatedScopeAccess.class);
        when((scopeAccess).isAccessTokenExpired(any(DateTime.class))).thenReturn(false);
        when(scopeAccessDao.getScopeAccessByAccessToken(null)).thenReturn(scopeAccess);
        when(scopeAccess.getAuditContext()).thenReturn("foo");
        defaultScopeAccessService.authenticateAccessToken(null);
        verify(scopeAccess).getAuditContext();
    }

    @Test
    public void getScopeAccessByAccessToken_accessTokenNull_throwsNotFoundException() throws Exception {
        try{
            defaultScopeAccessService.getScopeAccessByAccessToken(null);
            assertTrue("should throw exception",false);
        } catch (Exception ex){
            assertThat("exception type",ex.getClass().getName(),equalTo("com.rackspace.idm.exception.NotFoundException"));
            assertThat("exception message", ex.getMessage(),equalTo("Invalid accessToken; Token cannot be null"));
        }
    }

    @Test (expected = NotAuthenticatedException.class)
    public void handleApiKeyUsernameAuthenticationFailure_notAuthenticated_throwsNotAuthenticated() throws Exception {
        UserAuthenticationResult result = new UserAuthenticationResult(new User(), false);
        defaultScopeAccessService.handleApiKeyUsernameAuthenticationFailure("username", result);
    }

    @Test
    public void handleApiKeyUsernameAuthenticationFailure_authenticated_doesNothing() throws Exception {
        UserAuthenticationResult result = new UserAuthenticationResult(new User(), true);
        defaultScopeAccessService.handleApiKeyUsernameAuthenticationFailure("username", result);
    }

    @Test
    public void handleAuthenticationFailure_notAuthenticated_throwsNotAuthenticatedException() throws Exception {
        try{
            UserAuthenticationResult result = new UserAuthenticationResult(null,false);
            result.isAuthenticated();
            defaultScopeAccessService.handleAuthenticationFailure(null,result);
            assertTrue("should throw exception",false);
        } catch (NotAuthenticatedException ex){
            assertThat("exception message", ex.getMessage(), equalTo("Unable to authenticate user with credentials provided."));
        }
    }

}
