package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.*;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;
import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * User: alan.erwin
 * Date: 5/30/12
 * Time: 2:11 PM
 */
public class DefaultScopeAccessServiceTest {
    DefaultScopeAccessService defaultScopeAccessService;
    private UserDao userDao;
    private ScopeAccessDao scopeAccessDao;
    private TenantDao tenantDao;
    private EndpointDao endpointDao;
    private AuthHeaderHelper authHeaderHelper;
    private Configuration config;
    private ApplicationDao clientDao;

    @Before
    public void setUp() throws Exception {
        userDao = mock(UserDao.class);
        clientDao =  mock(ApplicationDao.class);
        scopeAccessDao = mock(ScopeAccessDao.class);
        tenantDao = mock(TenantDao.class);
        endpointDao = mock(EndpointDao.class);
        authHeaderHelper = mock(AuthHeaderHelper.class);
        config = mock(Configuration.class);
        when(config.getInt("token.cloudAuthExpirationSeconds")).thenReturn(86400);
        defaultScopeAccessService = new DefaultScopeAccessService(userDao, clientDao, scopeAccessDao,
                                                                  tenantDao, endpointDao, authHeaderHelper,
                                                                  config);

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void getOpenstackEndpointsForScopeAccess() throws Exception {
        //TODO getOpenstackEndpointsForScopeAccess

    }

    @Test
    public void addDelegateScopeAccess() throws Exception {
        //TODO addDelegateScopeAccess

    }

    @Test
    public void addImpersonatedScopeAccess() throws Exception {
        //TODO addImpersonatedScopeAccess

    }

    @Test
    public void addDirectScopeAccess() throws Exception {
        //TODO addDirectScopeAccess

    }

    @Test
    public void addScopeAccess() throws Exception {
        //TODO addScopeAccess

    }

    @Test
    public void authenticateAccessToken() throws Exception {
        //TODO authenticateAccessToken

    }

    @Test
    public void delegatePermission() throws Exception {
        //TODO delegatePermission

    }

    @Test
    public void deleteScopeAccess() throws Exception {
        //TODO deleteScopeAccess

    }

    @Test
    public void deleteDelegatedToken() throws Exception {
        //TODO deleteDelegatedToken

    }

    @Test
    public void doesAccessTokenHavePermission() throws Exception {
        //TODO doesAccessTokenHavePermission

    }

    @Test
    public void doesAccessTokenHaveService() throws Exception {
        //TODO doesAccessTokenHaveService

    }

    @Test
    public void doesUserHavePermissionForClient() throws Exception {
        //TODO doesUserHavePermissionForClient

    }

    @Test
    public void expireAccessToken() throws Exception {
        //TODO expireAccessToken

    }

    @Test
    public void expireAllTokensForClient() throws Exception {
        //TODO expireAllTokensForClient

    }

    @Test
    public void expireAllTokensForCustomer() throws Exception {
        //TODO expireAllTokensForCustomer

    }

    @Test
    public void expireAllTokensForUser() throws Exception {
        //TODO expireAllTokensForUser

    }

    @Test
    public void getAccessTokenByAuthHeader() throws Exception {
        //TODO getAccessTokenByAuthHeader

    }

    @Test
    public void getClientScopeAccessForClientId() throws Exception {
        //TODO getClientScopeAccessForClientId

    }

    @Test
    public void getDelegateScopeAccessesForParent() throws Exception {
        //TODO getDelegateScopeAccessesForParent

    }

    @Test
    public void getDelegateScopeAccessForParentByClientId() throws Exception {
        //TODO getDelegateScopeAccessForParentByClientId

    }

    @Test
    public void getDirectScopeAccessForParentByClientId() throws Exception {
        //TODO getDirectScopeAccessForParentByClientId

    }

    @Test
    public void getOrCreatePasswordResetScopeAccessForUser() throws Exception {
        //TODO getOrCreatePasswordResetScopeAccessForUser

    }

    @Test
    public void getPermissionForParent() throws Exception {
        //TODO getPermissionForParent

    }

    @Test
    public void getPermissionsForParent_withPermission() throws Exception {
        //TODO getPermissionsForParent

    }

    @Test
    public void getPermissionsForParent() throws Exception {
        //TODO getPermissionsForParent

    }

    @Test
    public void getRackerScopeAccessForClientId() throws Exception {
        //TODO getRackerScopeAccessForClientId

    }

    @Test
    public void getScopeAccessByAccessToken() throws Exception {
        //TODO getScopeAccessByAccessToken

    }

    @Test
    public void loadScopeAccessByAccessToken() throws Exception {
        //TODO loadScopeAccessByAccessToken

    }

    @Test
    public void getDelegatedScopeAccessByRefreshToken() throws Exception {
        //TODO getDelegatedScopeAccessByRefreshToken

    }

    @Test
    public void getScopeAccessByAuthCode() throws Exception {
        //TODO getScopeAccessByAuthCode

    }

    @Test
    public void getScopeAccessByRefreshToken() throws Exception {
        //TODO getScopeAccessByRefreshToken

    }

    @Test
    public void getScopeAccessesForParentByClientId() throws Exception {
        //TODO getScopeAccessesForParentByClientId

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
        UserScopeAccess uas = defaultScopeAccessService.getUserScopeAccessForClientId("12345", "12345");
        uas.isAccessTokenExpired(new DateTime());
        assertThat("newUserScopeAccess", uas.getAccessTokenString(), not("1234567890"));
    }

    @Test
    public void getUserScopeAccessForClientId_withNonExpiredScopeAccess_returnsSameToken(){
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(new DateTime().toDate());
        userScopeAccess.setAccessTokenString("1234567890");
        when(scopeAccessDao.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(userScopeAccess);
        UserScopeAccess uas = defaultScopeAccessService.getUserScopeAccessForClientId("12345", "12345");
        uas.isAccessTokenExpired(new DateTime());
        assertThat("newUserScopeAccess", uas.getAccessTokenString(), equalTo(userScopeAccess.getAccessTokenString()));
    }

    @Test
    public void getDelegatedUserScopeAccessForUsername() throws Exception {
        //TODO getDelegatedUserScopeAccessForUsername

    }

    @Test
    public void updateUserScopeAccessTokenForClientIdByUser() throws Exception {
        //TODO updateUserScopeAccessTokenForClientIdByUser

    }

    @Test
    public void getUserScopeAccessForClientIdByMossoIdAndApiCredentials() throws Exception {
        //TODO getUserScopeAccessForClientIdByMossoIdAndApiCredentials

    }

    @Test
    public void getUserScopeAccessForClientIdByNastIdAndApiCredentials() throws Exception {
        //TODO getUserScopeAccessForClientIdByNastIdAndApiCredentials

    }

    @Test
    public void getUserScopeAccessForClientIdByUsernameAndApiCredentials() throws Exception {
        //TODO getUserScopeAccessForClientIdByUsernameAndApiCredentials

    }

    @Test
    public void getUserScopeAccessForClientIdByUsernameAndPassword() throws Exception {
        //TODO getUserScopeAccessForClientIdByUsernameAndPassword

    }

    @Test
    public void grantPermissionToClient() throws Exception {
        //TODO grantPermissionToClient

    }

    @Test
    public void grantPermissionToUser() throws Exception {
        //TODO grantPermissionToUser

    }

    @Test
    public void removePermission() throws Exception {
        //TODO removePermission

    }

    @Test
    public void updatePermission() throws Exception {
        //TODO updatePermission

    }

    @Test
    public void updateScopeAccess() throws Exception {
        //TODO updateScopeAccess

    }

    @Test
    public void deleteScopeAccessesForParentByApplicationId() throws Exception {
        //TODO deleteScopeAccessesForParentByApplicationId

    }

    @Test
    public void updateExpiredUserScopeAccess() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExpired();
        when(scopeAccessDao.updateScopeAccess(userScopeAccess)).thenReturn(true);
        defaultScopeAccessService.updateExpiredUserScopeAccess(userScopeAccess);
        assertThat("updatesExpiredUserScopeAccess", userScopeAccess.isAccessTokenExpired(new DateTime()), equalTo(false));
    }
}
