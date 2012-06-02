package com.rackspace.idm.domain.service.impl;

import com.rackspace.docs.identity.api.ext.rax_ga.v1.ImpersonationRequest;
import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 5/31/12
 * Time: 10:30 AM
 */
public class DefaultScopeAccessServiceTest {

    DefaultScopeAccessService defaultScopeAccessService;
    DefaultScopeAccessService spy;
    ScopeAccessDao scopeAccessDao;
    Configuration configuration;
    ImpersonationRequest impersonationRequest;
    private UserDao userDao;
    private ApplicationDao clientDao;
    private TenantDao tenantDao;
    private EndpointDao endpointDao;
    private AuthHeaderHelper authHeaderHelper;

    @Before
    public void setUp() throws Exception {
        impersonationRequest = new ImpersonationRequest();
        org.openstack.docs.identity.api.v2.User user = new org.openstack.docs.identity.api.v2.User();
        user.setUsername("impersonatedUser");
        impersonationRequest.setUser(user);
        userDao = mock(UserDao.class);
        clientDao =  mock(ApplicationDao.class);
        scopeAccessDao = mock(ScopeAccessDao.class);
        tenantDao = mock(TenantDao.class);
        endpointDao = mock(EndpointDao.class);
        authHeaderHelper = mock(AuthHeaderHelper.class);
        configuration = mock(Configuration.class);
        when(configuration.getInt("token.cloudAuthExpirationSeconds")).thenReturn(86400);
        when(configuration.getInt("token.cloudAuthExpirationSeconds")).thenReturn(86400);
        when(configuration.getInt("token.refreshWindowHours")).thenReturn(12);

        defaultScopeAccessService = new DefaultScopeAccessService(userDao, clientDao, scopeAccessDao,
                                                                  tenantDao, endpointDao, authHeaderHelper,
                                                                  configuration);
        spy = spy(defaultScopeAccessService);

    }

    @Test
    public void addImpersonatedScopeAccess_TokenDoesNotExists_callsScopeAccessDao_addImpersonatedScopeAccess() throws Exception {
        when(scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(null);
        defaultScopeAccessService.addImpersonatedScopeAccess(new User(), "clientId", "impToken", impersonationRequest);
        verify(scopeAccessDao).addImpersonatedScopeAccess(anyString(), any(ScopeAccess.class));
    }

    @Test
    public void addImpersonatedScopeAccess_TokenExists_callsScopeAccessDao_updateScopeAccess() throws Exception {
        when(scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(new ImpersonatedScopeAccess());
        defaultScopeAccessService.addImpersonatedScopeAccess(new User(), "clientId", "impToken", impersonationRequest);
        verify(scopeAccessDao).updateScopeAccess(any(ScopeAccess.class));
    }

    @Test
        public void addImpersonatedScopeAccess_TokenExistsAndIsNotExpired_returnsSameAccessToken() throws Exception {
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setAccessTokenExp(new DateTime().plusSeconds(100).toDate());
        String token = "abc";
        String impToken = "imp";
        impersonatedScopeAccess.setAccessTokenString(token);
        impersonatedScopeAccess.setImpersonatingToken(impToken);
        when(scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(impersonatedScopeAccess);
        ImpersonatedScopeAccess returnedImpersonatedScopeAccess = defaultScopeAccessService.addImpersonatedScopeAccess(new User(), "clientId", "imp", impersonationRequest);
        assertThat("impersonated token", returnedImpersonatedScopeAccess.getAccessTokenString(), equalTo("abc"));
    }

    @Test
    public void setImpersonatedScopeAccess_callerIsRacker_setsRackerId() throws Exception {
        Racker racker = new Racker();
        racker.setRackerId("foo");
        ImpersonatedScopeAccess impersonatedScopeAccess = defaultScopeAccessService.setImpersonatedScopeAccess(racker, impersonationRequest, new ImpersonatedScopeAccess());
        assertThat("racker id", impersonatedScopeAccess.getRackerId(), equalTo("foo"));
    }

    @Test
    public void addImpersonatedScopeAccess_expireInIsNullAndCallerIsServiceUser_setsExpirationToDefault() throws Exception {
        ArgumentCaptor<ImpersonatedScopeAccess> argument = ArgumentCaptor.forClass(ImpersonatedScopeAccess.class);
        when(scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(null);
        when(scopeAccessDao.addImpersonatedScopeAccess(anyString(), argument.capture())).thenReturn(null);
        when(configuration.getInt("token.impersonatedByServiceDefaultSeconds")).thenReturn(3600);
        defaultScopeAccessService.addImpersonatedScopeAccess(new User(), "clientId", "impToken", impersonationRequest);
        DateTime dateTime = new DateTime().plusSeconds(3600);
        assertThat("expiration date", argument.getValue().getAccessTokenExp().getTime(), greaterThan(dateTime.getMillis() - 60000L));
        assertThat("expiration date", argument.getValue().getAccessTokenExp().getTime(), lessThan(dateTime.getMillis() + 60000L));
    }


    @Test
    public void setImpersonationScopeAccess_expireInIsNotNullAndCallerIsRacker_setsExpiration() throws Exception {
        DateTime expectedExpirationTime = new DateTime().plusSeconds(10000);
        impersonationRequest.setExpireInSeconds(10000);
        when(configuration.getInt("token.impersonatedByRackerMaxSeconds")).thenReturn(10800);
        ImpersonatedScopeAccess impersonatedScopeAccess = defaultScopeAccessService.setImpersonatedScopeAccess(new Racker(), impersonationRequest, new ImpersonatedScopeAccess());
        assertThat("expiration date", impersonatedScopeAccess.getAccessTokenExp().getTime(), greaterThan(expectedExpirationTime.getMillis() - 60000L));
        assertThat("expiration date", impersonatedScopeAccess.getAccessTokenExp().getTime(), lessThan(expectedExpirationTime.getMillis() + 60000L));
    }

    @Test
    public void setImpersonationScopeAccess_expireInIsNotNullAndCallerIsServiceUser_setsExpiration() throws Exception {
        DateTime expectedExpirationTime = new DateTime().plusSeconds(10000);
        impersonationRequest.setExpireInSeconds(10000);
        when(configuration.getInt("token.impersonatedByServiceMaxSeconds")).thenReturn(10800);
        ImpersonatedScopeAccess impersonatedScopeAccess = defaultScopeAccessService.setImpersonatedScopeAccess(new User(), impersonationRequest, new ImpersonatedScopeAccess());
        assertThat("expiration date", impersonatedScopeAccess.getAccessTokenExp().getTime(), greaterThan(expectedExpirationTime.getMillis() - 60000L));
        assertThat("expiration date", impersonatedScopeAccess.getAccessTokenExp().getTime(), lessThan(expectedExpirationTime.getMillis() + 60000L));
    }

    @Test(expected = BadRequestException.class)
    public void setImpersonationScopeAccess_expireInGreaterThanMaxAndCallerIsRacker_throwsBadRequestException() throws Exception {
        impersonationRequest.setExpireInSeconds(10800000);
        defaultScopeAccessService.setImpersonatedScopeAccess(new Racker(), impersonationRequest, new ImpersonatedScopeAccess());
    }

    @Test(expected = BadRequestException.class)
    public void setImpersonationScopeAccess_expireInGreaterThanMaxAndCallerIsServiceUser_throwsBadRequestException() throws Exception {
        impersonationRequest.setExpireInSeconds(108000000);
        defaultScopeAccessService.setImpersonatedScopeAccess(new User(), impersonationRequest, new ImpersonatedScopeAccess());
    }

    @Test(expected = BadRequestException.class)
    public void setImpersonationScopeAccess_expireInLessThan1AndCallerIsRacker_throwsBadRequestException() throws Exception {
        impersonationRequest.setExpireInSeconds(0);
        defaultScopeAccessService.setImpersonatedScopeAccess(new Racker(), impersonationRequest, new ImpersonatedScopeAccess());
    }

    @Test(expected = BadRequestException.class)
    public void setImpersonationScopeAccess_expireInLessThan1AndCallerIsServiceUser_throwsBadRequestException() throws Exception {
        impersonationRequest.setExpireInSeconds(0);
        defaultScopeAccessService.setImpersonatedScopeAccess(new User(), impersonationRequest, new ImpersonatedScopeAccess());
    }

    @Test
    public void setImpersonationScopeAccess_expireInIsNotNullAndCallerIsRacker_checksMaxTime() throws Exception {
        impersonationRequest.setExpireInSeconds(10800);
        when(configuration.getInt("token.impersonatedByRackerMaxSeconds")).thenReturn(10800);
        defaultScopeAccessService.setImpersonatedScopeAccess(new Racker(), impersonationRequest, new ImpersonatedScopeAccess());
        verify(configuration).getInt("token.impersonatedByRackerMaxSeconds");
    }

    @Test
    public void setImpersonationScopeAccess_expireInIsNotNullAndCallerIsServiceUser_checksMaxTime() throws Exception {
        impersonationRequest.setExpireInSeconds(10800);
        when(configuration.getInt("token.impersonatedByServiceMaxSeconds")).thenReturn(10800);
        defaultScopeAccessService.setImpersonatedScopeAccess(new User(), impersonationRequest, new ImpersonatedScopeAccess());
        verify(configuration).getInt("token.impersonatedByServiceMaxSeconds");
    }

    @Test
    public void setImpersonationScopeAccess_expireInIsNullAndCallerIsRacker_setsExpirationToRackerDefault() throws Exception {
        impersonationRequest.setExpireInSeconds(null);
        defaultScopeAccessService.setImpersonatedScopeAccess(new Racker(), impersonationRequest, new ImpersonatedScopeAccess());
        verify(configuration).getInt("token.impersonatedByRackerDefaultSeconds");
    }

    @Test
    public void setImpersonationScopeAccess_expireInIsNullAndCallerIsServiceUser_setsExpirationToServiceDefault() throws Exception {
        impersonationRequest.setExpireInSeconds(null);
        defaultScopeAccessService.setImpersonatedScopeAccess(new User(), impersonationRequest, new ImpersonatedScopeAccess());
        verify(configuration).getInt("token.impersonatedByServiceDefaultSeconds");
    }

    @Test
    public void setImpersonationScopeAccess_CallerIsRacker_setsExpirationToRackerDefault() throws Exception {
        defaultScopeAccessService.setImpersonatedScopeAccess(new Racker(), impersonationRequest, new ImpersonatedScopeAccess());
        verify(configuration).getInt("token.impersonatedByRackerDefaultSeconds");
    }

    @Test
    public void setImpersonationScopeAccess_CallerIsServiceUser_setsExpirationToServiceDefault() throws Exception {
        defaultScopeAccessService.setImpersonatedScopeAccess(new User(), impersonationRequest, new ImpersonatedScopeAccess());
        verify(configuration).getInt("token.impersonatedByServiceDefaultSeconds");
    }

    @Test
    public void addImpersonatedScopeAccess_whenScopeAccessDoesNotExist_callsSetImpersonatedScopeAccess() throws Exception {
        when(scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(null);
        User user = new User();
        spy.addImpersonatedScopeAccess(user, null, null, impersonationRequest);
        verify(spy).setImpersonatedScopeAccess(eq(user), eq(impersonationRequest), any(ImpersonatedScopeAccess.class));
    }

    @Test
    public void addImpersonatedScopeAccess_whenScopeAccessExists_callsSetImpersonatedScopeAccess() throws Exception {
        when(scopeAccessDao.getImpersonatedScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(new ImpersonatedScopeAccess());
        User user = new User();
        spy.addImpersonatedScopeAccess(user, null, null, impersonationRequest);
        verify(spy).setImpersonatedScopeAccess(eq(user), eq(impersonationRequest), any(ImpersonatedScopeAccess.class));
    }

    @Test
    public void updateExpiredUserScopeAccess() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExpired();
        when(scopeAccessDao.updateScopeAccess(userScopeAccess)).thenReturn(true);
        defaultScopeAccessService.updateExpiredUserScopeAccess(userScopeAccess);
        assertThat("updatesExpiredUserScopeAccess", userScopeAccess.isAccessTokenExpired(new DateTime()), equalTo(false));
    }

    @Test(expected = NotFoundException.class)
    public void getUserScopeAccessForClientId_withNonExistentUser_throwsNotFoundException(){
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(anyString(),anyString())).thenReturn(null);
        defaultScopeAccessService.getUserScopeAccessForClientId("12345", "12345");
    }

    @Test
    public void getUserScopeAccessForClientId_withExpiredScopeAccess_returnsNewToken(){
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(new DateTime().minusDays(2).toDate());
        userScopeAccess.setAccessTokenString("1234567890");
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(userScopeAccess);
        when(scopeAccessDao.updateScopeAccess(userScopeAccess)).thenReturn(true);
        UserScopeAccess uas = defaultScopeAccessService.getUserScopeAccessForClientId("12345", "12345");
        uas.isAccessTokenExpired(new DateTime());
        assertThat("newUserScopeAccess", uas.getAccessTokenString(), not("1234567890"));
    }

    @Test
    public void getUserScopeAccessForClientId_withinRefreshWindow_returnsNewToken(){
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(new DateTime().plusHours(5).toDate());
        userScopeAccess.setAccessTokenString("1234567890");
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(userScopeAccess);
        when(scopeAccessDao.updateScopeAccess(userScopeAccess)).thenReturn(true);
        UserScopeAccess uas = defaultScopeAccessService.getUserScopeAccessForClientId("12345", "12345");
        assertThat("newUserScopeAccessWithinWindow", uas.getAccessTokenString(), not("1234567890"));
    }

    @Test
    public void getUserScopeAccessForClientId_withNonExpiredScopeAccess_returnsSameToken(){
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(new DateTime().plusDays(1).toDate());
        userScopeAccess.setAccessTokenString("1234567890");
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(userScopeAccess);
        UserScopeAccess uas = defaultScopeAccessService.getUserScopeAccessForClientId("12345", "12345");
        assertThat("newUserScopeAccessNoneExpired", uas.getAccessTokenString(), equalTo("1234567890"));
    }




}
