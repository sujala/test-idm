package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.dao.ClientDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientScopeAccess;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.DefinedPermission;
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.GrantedPermission;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserScopeAccess;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.impl.DefaultScopeAccessService;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;

public class ScopeAccessServiceTests extends ServiceTestsBase {

    UserDao mockUserDao;
    ScopeAccessDao scopeAccessDao;
    ClientDao mockClientDao;
    AuthHeaderHelper authHeaderHelper;
    ScopeAccessService scopeAccessService;

    @Before
    public void setUp() throws Exception {
        Configuration appConfig = new PropertyFileConfiguration()
            .getConfigFromClasspath();
        authHeaderHelper = new AuthHeaderHelper();

        mockUserDao = EasyMock.createMock(UserDao.class);
        mockClientDao = EasyMock.createMock(ClientDao.class);
        scopeAccessDao = EasyMock.createMock(ScopeAccessDao.class);
        scopeAccessService = new DefaultScopeAccessService(mockUserDao,
            mockClientDao, scopeAccessDao, authHeaderHelper, appConfig);
    }

    @Test
    public void shouldRemovePermission() {

        Permission perm = getFakePermission("permissionId");

        EasyMock.expect(scopeAccessDao.removePermissionFromScopeAccess(perm))
            .andReturn(true);
        EasyMock.replay(scopeAccessDao);

        scopeAccessService.removePermission(perm);
        EasyMock.verify(scopeAccessDao);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotRemovePermissionIfIllegalStateExceptionNull() {

        Permission perm = getFakePermission("permissionId");

        EasyMock.expect(scopeAccessDao.removePermissionFromScopeAccess(perm))
            .andThrow(new IllegalStateException());
        EasyMock.replay(scopeAccessDao);

        scopeAccessService.removePermission(perm);
    }

    @Test
    public void shouldUpdatePermission() {
        Permission permission = getFakePermission("permissionId");
        EasyMock.expect(
            scopeAccessDao.updatePermissionForScopeAccess(permission))
            .andReturn(Boolean.TRUE);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.updatePermission(permission);
        EasyMock.verify(scopeAccessDao);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdatePermissionIfIllegalStateException() {
        Permission permission = getFakePermission("permissionId");
        EasyMock.expect(
            scopeAccessDao.updatePermissionForScopeAccess(permission))
            .andThrow(new IllegalStateException());
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.updatePermission(permission);
    }

    @Test
    public void shouldGetScopeAccessForParentByClientId() {
        Client client = getFakeClient();
        EasyMock.expect(
            scopeAccessDao.getDirectScopeAccessForParentByClientId(
                client.getUniqueId(), client.getClientId())).andReturn(
            getFakeScopeAccess());
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.getDirectScopeAccessForParentByClientId(
            client.getUniqueId(), client.getClientId());
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldGetPermissionForParent() {

        Client client = getFakeClient();
        Permission permission = getFakePermission("permissionId");
        EasyMock.expect(
            scopeAccessDao.getPermissionByParentAndPermission(
                client.getUniqueId(), permission)).andReturn(permission);
        EasyMock.replay(scopeAccessDao);

        scopeAccessService.getPermissionForParent(client.getUniqueId(),
            permission);
        EasyMock.verify(scopeAccessDao);

    }

    @Test
    public void shouldUpdateScopeAccess() {
        ScopeAccess sa = getFakeScopeAccess();
        EasyMock.expect(scopeAccessDao.updateScopeAccess(sa)).andReturn(true);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.updateScopeAccess(sa);
        EasyMock.verify(scopeAccessDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotUpdateScopeAccessIfScopeAccessIsNull() {
        scopeAccessService.updateScopeAccess(null);
    }

    @Test
    public void shouldGrantPermissionToClient() {
        Client client = getFakeClient();
        ScopeAccess sa = getFakeScopeAccess();
        Permission perm = getFakePermission("fakePermissionObjectId");

        EasyMock.expect(
            mockClientDao.getClientByClientId(perm.getClientId())).andReturn(client);
        
        GrantedPermission grantedPerm = new GrantedPermission();
        grantedPerm.setClientId(perm.getClientId());
        grantedPerm.setCustomerId(perm.getCustomerId());
        grantedPerm.setPermissionId(perm.getPermissionId());
        
        DefinedPermission dp = new DefinedPermission();
        dp.setClientId(perm.getClientId());
        dp.setCustomerId(perm.getCustomerId());
        dp.setPermissionId(perm.getPermissionId());

        EasyMock.expect(
            scopeAccessDao.getPermissionByParentAndPermission(
                client.getUniqueId(), dp)).andReturn(dp);

        EasyMock.expect(
            scopeAccessDao.getDirectScopeAccessForParentByClientId(
                client.getUniqueId(), client.getClientId())).andReturn(sa);

        EasyMock.expect(
            scopeAccessDao.grantPermission(sa.getUniqueId(), grantedPerm))
            .andReturn(grantedPerm);

        EasyMock.replay(scopeAccessDao, mockClientDao);

        Permission returnedPerm = scopeAccessService.grantPermissionToClient(client.getUniqueId(), grantedPerm);
        
        Assert.assertEquals(grantedPerm, returnedPerm);
        Assert.assertEquals(grantedPerm.getClientId(), returnedPerm.getClientId());
        Assert.assertEquals(grantedPerm.getCustomerId(), returnedPerm.getCustomerId());
        Assert.assertEquals(grantedPerm.getPermissionId(), returnedPerm.getPermissionId());

        EasyMock.verify(scopeAccessDao, mockClientDao);
    }
    
    @Test
    public void shouldGrantPermissionToUser() {
        User user = getFakeUser();
        Client client = getFakeClient();
        UserScopeAccess sa = getFakeUserScopeAccess();
        Permission perm = getFakePermission("fakePermissionObjectId");

        EasyMock.expect(
            mockClientDao.getClientByCustomerIdAndClientId(
                perm.getCustomerId(), perm.getClientId())).andReturn(client);
        
        GrantedPermission grantedPerm = new GrantedPermission();
        grantedPerm.setClientId(perm.getClientId());
        grantedPerm.setCustomerId(perm.getCustomerId());
        grantedPerm.setPermissionId(perm.getPermissionId());
        
        DefinedPermission dp = new DefinedPermission();
        dp.setClientId(perm.getClientId());
        dp.setCustomerId(perm.getCustomerId());
        dp.setPermissionId(perm.getPermissionId());

        EasyMock.expect(
            scopeAccessDao.getPermissionByParentAndPermission(
                client.getUniqueId(), dp)).andReturn(dp);

        EasyMock.expect(
            scopeAccessDao.getDirectScopeAccessForParentByClientId(
                user.getUniqueId(), client.getClientId())).andReturn(sa);

        EasyMock.expect(
            scopeAccessDao.grantPermission(sa.getUniqueId(), grantedPerm))
            .andReturn(grantedPerm);

        EasyMock.replay(scopeAccessDao, mockClientDao);

        GrantedPermission returnedPerm = scopeAccessService.grantPermissionToUser(user, grantedPerm);
        
        Assert.assertEquals(grantedPerm, returnedPerm);
        Assert.assertEquals(grantedPerm.getClientId(), returnedPerm.getClientId());
        Assert.assertEquals(grantedPerm.getCustomerId(), returnedPerm.getCustomerId());
        Assert.assertEquals(grantedPerm.getPermissionId(), returnedPerm.getPermissionId());

        EasyMock.verify(scopeAccessDao, mockClientDao);
    }   

    @Test(expected = NotFoundException.class)
    public void shouldNotGrantPermissionIfClientIsNotFound() {
        Client client = getFakeClient();
        ScopeAccess sa = getFakeScopeAccess();
        Permission perm = getFakePermission("fakePermissionObjectId");
        
        GrantedPermission grantedPerm = new GrantedPermission();
        grantedPerm.setClientId(perm.getClientId());
        grantedPerm.setCustomerId(perm.getCustomerId());
        grantedPerm.setPermissionId(perm.getPermissionId());

        EasyMock.expect(
            mockClientDao.getClientByCustomerIdAndClientId(
                perm.getCustomerId(), perm.getClientId())).andReturn(null);

        scopeAccessService.grantPermissionToClient(client.getUniqueId(), grantedPerm);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotGrantPermissionIfPermissionIsNotFound() {
        Client client = getFakeClient();
        ScopeAccess sa = getFakeScopeAccess();
        Permission perm = getFakePermission("fakePermissionObjectId");
        
        GrantedPermission grantedPerm = new GrantedPermission();
        grantedPerm.setClientId(perm.getClientId());
        grantedPerm.setCustomerId(perm.getCustomerId());
        grantedPerm.setPermissionId(perm.getPermissionId());

        EasyMock.expect(
            mockClientDao.getClientByCustomerIdAndClientId(
                perm.getCustomerId(), perm.getClientId())).andReturn(client);

        EasyMock.expect(
            scopeAccessDao.getPermissionByParentAndPermission(
                client.getUniqueId(), perm)).andReturn(null);

        scopeAccessService.grantPermissionToClient(client.getUniqueId(), grantedPerm);
    }

    @Test
    public void shouldGetUserScopeAccessForClientIdByUsernameAndPassword() {

        User user = getFakeUser();
        Client client = getFakeClient();
        UserScopeAccess sa = getFakeUserScopeAccess();
        EasyMock.expect(mockUserDao.authenticate(username, "password"))
            .andReturn(new UserAuthenticationResult(user, true));

        EasyMock.expect(
            scopeAccessDao.getDirectScopeAccessForParentByClientId(
                user.getUniqueId(), client.getClientId())).andReturn(sa);

        EasyMock.expect(scopeAccessDao.updateScopeAccess(sa)).andReturn(true);

        EasyMock.replay(scopeAccessDao, mockUserDao);

        scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword(
            user.getUsername(), "password", client.getClientId());

        EasyMock.verify(scopeAccessDao, mockUserDao);

    }

    @Test
    public void shouldGetUserScopeAccessForClientIdByNastIdAndApiCredentials() {
        User user = getFakeUser();
        Client client = getFakeClient();
        UserScopeAccess sa = getFakeUserScopeAccess();

        String nastId = "fakeNastId";
        String apiKey = "fakeApiKey";
        EasyMock.expect(
            mockUserDao.authenticateByNastIdAndAPIKey(nastId, apiKey))
            .andReturn(new UserAuthenticationResult(user, true));

        EasyMock.expect(
            scopeAccessDao.getDirectScopeAccessForParentByClientId(
                user.getUniqueId(), client.getClientId())).andReturn(sa);

        EasyMock.expect(scopeAccessDao.updateScopeAccess(sa)).andReturn(true);

        EasyMock.replay(scopeAccessDao, mockUserDao);

        scopeAccessService
            .getUserScopeAccessForClientIdByNastIdAndApiCredentials(nastId,
                apiKey, clientId);

        EasyMock.verify(scopeAccessDao, mockUserDao);

    }

    @Test
    public void shouldGetUserScopeAccessForClientIdByMossoIdAndApiCredentials() {
        User user = getFakeUser();
        Client client = getFakeClient();
        UserScopeAccess sa = getFakeUserScopeAccess();

        int mossoId = 12345;
        String apiKey = "fakeApiKey";
        EasyMock.expect(
            mockUserDao.authenticateByMossoIdAndAPIKey(mossoId, apiKey))
            .andReturn(new UserAuthenticationResult(user, true));

        EasyMock.expect(
            scopeAccessDao.getDirectScopeAccessForParentByClientId(
                user.getUniqueId(), client.getClientId())).andReturn(sa);

        EasyMock.expect(scopeAccessDao.updateScopeAccess(sa)).andReturn(true);

        EasyMock.replay(scopeAccessDao, mockUserDao);

        scopeAccessService
            .getUserScopeAccessForClientIdByMossoIdAndApiCredentials(mossoId,
                apiKey, clientId);

        EasyMock.verify(scopeAccessDao, mockUserDao);

    }

    @Test
    public void shouldGetUserScopeAccessForClientIdByUsernameAndApiCredentials() {
        User user = getFakeUser();
        Client client = getFakeClient();
        UserScopeAccess sa = getFakeUserScopeAccess();

        String apiKey = "fakeApiKey";
        EasyMock.expect(mockUserDao.authenticateByAPIKey(username, apiKey))
            .andReturn(new UserAuthenticationResult(user, true));

        EasyMock.expect(
            scopeAccessDao.getDirectScopeAccessForParentByClientId(
                user.getUniqueId(), client.getClientId())).andReturn(sa);

        EasyMock.expect(scopeAccessDao.updateScopeAccess(sa)).andReturn(true);

        EasyMock.replay(scopeAccessDao, mockUserDao);

        scopeAccessService
            .getUserScopeAccessForClientIdByUsernameAndApiCredentials(username,
                apiKey, clientId);

        EasyMock.verify(scopeAccessDao, mockUserDao);
    }

    @Test
    public void shouldGetUserScopeAccessForClientId() {
        UserScopeAccess usao = getFakeUserScopeAccess();
        EasyMock.expect(
            scopeAccessDao.getDirectScopeAccessForParentByClientId("userUniqueId",
                "clientId")).andReturn(usao);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.getUserScopeAccessForClientId("userUniqueId",
            "clientId");
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldGetScopeAccessByRefreshToken() {
        UserScopeAccess usao = getFakeUserScopeAccess();
        EasyMock.expect(
            scopeAccessDao.getScopeAccessByRefreshToken("refreshToken"))
            .andReturn(usao);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.getScopeAccessByRefreshToken("refreshToken");
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldGetScopeAccessByAccessToken() {
        UserScopeAccess usao = getFakeUserScopeAccess();
        EasyMock.expect(
            scopeAccessDao.getScopeAccessByAccessToken("accessToken"))
            .andReturn(usao);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.getScopeAccessByAccessToken("accessToken");
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldgetRackerScopeAccessForClientId() {

        EasyMock.expect(
            scopeAccessDao.getDirectScopeAccessForParentByClientId("rackerUniqueId",
                clientId)).andReturn(getFakeRackerScopeAccess());
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.getRackerScopeAccessForClientId("rackerUniqueId",
            clientId);
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldGetasswordResetScopeAccessForUser() {
        User user = getFakeUser();
        PasswordResetScopeAccess prsao = getFakePasswordResetScopeAccessObject();
        DateTime pastTime = new DateTime();
        prsao.setAccessTokenExp(pastTime.minusSeconds(10).toDate());
        EasyMock.expect(
            scopeAccessDao.getDirectScopeAccessForParentByClientId(
                user.getUniqueId(), "PASSWORDRESET")).andReturn(prsao);
        EasyMock.expect(scopeAccessDao.updateScopeAccess(prsao)).andReturn(
            Boolean.TRUE);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.getOrCreatePasswordResetScopeAccessForUser(user);
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldGetClientScopeAccessForClientId() {
        EasyMock.expect(
            scopeAccessDao.getDirectScopeAccessForParentByClientId("clientUniqueId",
                clientId)).andReturn(getFakeClientScopeAccess());
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.getClientScopeAccessForClientId("clientUniqueId",
            clientId);
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldGetAccessTokenByAuthHeader() {
        ScopeAccess fakeScopeAccess = getFakeScopeAccess();
        EasyMock.expect(
            scopeAccessDao.getScopeAccessByAccessToken("accessToken"))
            .andReturn(fakeScopeAccess);
        EasyMock.replay(scopeAccessDao);
        ScopeAccess sa = scopeAccessService
            .getAccessTokenByAuthHeader("OAuth accessToken");
        EasyMock.verify(scopeAccessDao);
        Assert.assertEquals(fakeScopeAccess, sa);
    }

    @Test
    public void shouldExpireAllTokensForUser() {
        User fakeUser = getFakeUser();
        setUpUserTokenExiprationTest(true, fakeUser);
        EasyMock.verify(mockUserDao, scopeAccessDao);
    }

    private void setUpUserTokenExiprationTest(boolean doTest, User fakeUser) {
        EasyMock.expect(mockUserDao.getUserByUsername(username)).andReturn(
            fakeUser);
        List<ScopeAccess> sas = new ArrayList<ScopeAccess>();
        UserScopeAccess fakeUserScopeAccess0 = getFakeUserScopeAccess();
        sas.add(fakeUserScopeAccess0);
        UserScopeAccess fakeUserScopeAccess1 = getFakeUserScopeAccess();
        sas.add(fakeUserScopeAccess1);
        EasyMock.expect(
            scopeAccessDao.getScopeAccessesByParent(fakeUser.getUniqueId()))
            .andReturn(sas);
        fakeUserScopeAccess0.setAccessTokenExpired();
        fakeUserScopeAccess1.setAccessTokenExpired();
        EasyMock.expect(scopeAccessDao.updateScopeAccess(fakeUserScopeAccess0))
            .andReturn(Boolean.TRUE);
        EasyMock.expect(scopeAccessDao.updateScopeAccess(fakeUserScopeAccess1))
            .andReturn(Boolean.TRUE);
        if (doTest) {
            EasyMock.replay(mockUserDao, scopeAccessDao);
            scopeAccessService.expireAllTokensForUser(fakeUser.getUsername());
        }
    }

    @Test
    public void shouldExpireAllTokensForClient() {
        Client fakeClient = getFakeClient();
        setUpClientTokenExiprationTest(true, fakeClient);
        EasyMock.verify(mockClientDao, scopeAccessDao);
    }

    private void setUpClientTokenExiprationTest(boolean doTest,
        Client fakeClient) {
        EasyMock.expect(
            mockClientDao.getClientByClientId(fakeClient.getClientId()))
            .andReturn(fakeClient);
        List<ScopeAccess> sas = new ArrayList<ScopeAccess>();
        ClientScopeAccess fakeClientScopeAccess0 = getFakeClientScopeAccess();
        sas.add(fakeClientScopeAccess0);
        ClientScopeAccess fakeClientScopeAccess1 = getFakeClientScopeAccess();
        sas.add(fakeClientScopeAccess1);
        EasyMock.expect(
            scopeAccessDao.getScopeAccessesByParent(fakeClient.getUniqueId()))
            .andReturn(sas);
        fakeClientScopeAccess0.setAccessTokenExpired();
        fakeClientScopeAccess1.setAccessTokenExpired();
        EasyMock.expect(
            scopeAccessDao.updateScopeAccess(fakeClientScopeAccess0))
            .andReturn(Boolean.TRUE);
        EasyMock.expect(
            scopeAccessDao.updateScopeAccess(fakeClientScopeAccess1))
            .andReturn(Boolean.TRUE);
        if (doTest) {
            EasyMock.replay(mockClientDao, scopeAccessDao);
            scopeAccessService.expireAllTokensForClient(fakeClient
                .getClientId());
        }
    }

    @Test
    public void shouldExpireAllTokensForCustomer() {
        Configuration config = EasyMock.createMock(Configuration.class);
        EasyMock.expect(config.getInt("ldap.paging.limit.max")).andReturn(10);
        Client client0 = getFakeClient();
        List<Client> clientsList = new ArrayList<Client>();
        clientsList.add(client0);
        Clients clients = new Clients();
        clients.setClients(clientsList);
        EasyMock.expect(
            mockClientDao.getClientsByCustomerId(customerId, 0, 1000))
            .andReturn(clients);

        User user0 = getFakeUser();
        List<User> usersList = new ArrayList<User>();
        usersList.add(user0);
        Users users = new Users();
        users.setUsers(usersList);
        EasyMock.expect(mockUserDao.getUsersByCustomerId(customerId, 0, 1000))
            .andReturn(users);

        setUpClientTokenExiprationTest(false, client0);
        setUpUserTokenExiprationTest(false, user0);
        EasyMock.replay(mockClientDao, mockUserDao, scopeAccessDao);
        scopeAccessService.expireAllTokensForCustomer(customerId);
        EasyMock.verify(mockClientDao, mockUserDao, scopeAccessDao);
    }

    @Test
    public void shouldExpireAccessToken() {
        UserScopeAccess sa = getFakeUserScopeAccess();
        String tokenString = "tokenString";
        EasyMock
            .expect(scopeAccessDao.getScopeAccessByAccessToken(tokenString))
            .andReturn(sa);

        EasyMock.expect(scopeAccessDao.updateScopeAccess(sa)).andReturn(true);

        EasyMock.replay(scopeAccessDao);

        scopeAccessService.expireAccessToken(tokenString);

        EasyMock.verify(scopeAccessDao);

    }

    @Test
    public void shouldVerifyAccessTokenHavePermission() {
        UserScopeAccess token = new UserScopeAccess();
        token.setAccessTokenString("token");
        
        Permission fakePermission = getFakePermission("permId");
        EasyMock.expect(
            scopeAccessDao.doesAccessTokenHavePermission(token,
                fakePermission)).andReturn(true);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.doesAccessTokenHavePermission(token,
            fakePermission);
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldDeleteScopeAccess() {
        ScopeAccess sa = getFakeScopeAccess();
        EasyMock.expect(scopeAccessDao.deleteScopeAccess(sa)).andReturn(
            Boolean.TRUE);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.deleteScopeAccess(sa);
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldAuthenticateAccessToken() {
        UserScopeAccess sao = getFakeUserScopeAccess();
        String accessTokenStr = "accessTokenString";
        EasyMock.expect(
            scopeAccessDao.getScopeAccessByAccessToken(accessTokenStr))
            .andReturn(sao);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.authenticateAccessToken(accessTokenStr);
        EasyMock.verify(scopeAccessDao);
    }
    
    @Test
    public void shouldAddScopeAccess() {
        UserScopeAccess sao = getFakeUserScopeAccess();
        User user = getFakeUser();
        EasyMock.expect(scopeAccessDao.addDirectScopeAccess(
            user.getUniqueId(), sao)).andReturn(sao);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.addDirectScopeAccess(user.getUniqueId(), sao);
        EasyMock.verify(scopeAccessDao);
    
    }
    
    @Test
    public void shouldGetDelegatedScopeAccessByUsername() {
        User user = getFakeUser();
        String accessToken = "accessTokenString";
        List<DelegatedClientScopeAccess> list = new ArrayList<DelegatedClientScopeAccess>();
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setAccessTokenString(accessToken);
        list.add(delegatedClientScopeAccess);
        EasyMock.expect(scopeAccessDao.getDelegatedClientScopeAccessByUsername(user.getUsername())).andReturn(list);
        EasyMock.replay(scopeAccessDao);
        
        List<DelegatedClientScopeAccess> retVal = scopeAccessService.getDelegatedUserScopeAccessForUsername(user.getUsername());
        Assert.assertNotNull(retVal);
        Assert.assertEquals(delegatedClientScopeAccess.getAccessTokenString(), retVal.get(0).getAccessTokenString());
        
        EasyMock.verify(scopeAccessDao);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotGetDelegatedScopeAccessByUsername() {
        User user = getFakeUser();
        String accessToken = "accessTokenString";
        List<DelegatedClientScopeAccess> list = new ArrayList<DelegatedClientScopeAccess>();
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setAccessTokenString(accessToken);
        
        EasyMock.expect(scopeAccessDao.getDelegatedClientScopeAccessByUsername(user.getUsername())).andReturn(null);
        EasyMock.replay(scopeAccessDao);
        
        List<DelegatedClientScopeAccess> retVal = scopeAccessService.getDelegatedUserScopeAccessForUsername(user.getUsername());
    }   
    
    @Test
    public void shouldGetDelegatedScopeAccessByAccessToken() {
        User user = getFakeUser();
        String accessToken = "accessTokenString";
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setAccessTokenString(accessToken);
        delegatedClientScopeAccess.setUsername(user.getUsername());
        EasyMock.expect(scopeAccessDao.getScopeAccessByRefreshToken(accessToken)).andReturn(delegatedClientScopeAccess);
        EasyMock.replay(scopeAccessDao);
        
        DelegatedClientScopeAccess retVal = scopeAccessService.getDelegatedScopeAccessByRefreshToken(user, accessToken);
        Assert.assertNotNull(retVal);
        Assert.assertEquals(delegatedClientScopeAccess.getAccessTokenString(), retVal.getAccessTokenString());
        
        EasyMock.verify(scopeAccessDao);
    }
    
    @Test
    public void shouldReturnNullForNonExistentTokenGetDelegatedScopeAccessByAccessToken() {
        User user = getFakeUser();
        String accessToken = "accessTokenString";
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setAccessTokenString(accessToken);
        delegatedClientScopeAccess.setUsername(user.getUsername());
        EasyMock.expect(scopeAccessDao.getScopeAccessByRefreshToken(accessToken)).andReturn(null);
        EasyMock.replay(scopeAccessDao);
        
        DelegatedClientScopeAccess retVal = scopeAccessService.getDelegatedScopeAccessByRefreshToken(user, accessToken);
        Assert.assertNull(retVal);
        
        EasyMock.verify(scopeAccessDao);
    }
    
    @Test
    public void shouldReturnNullForNonDelegateTokenGetDelegatedScopeAccessByAccessToken() {
        User user = getFakeUser();
        String accessToken = "accessTokenString";
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString(accessToken);
        userScopeAccess.setUsername(user.getUsername());
        EasyMock.expect(scopeAccessDao.getScopeAccessByRefreshToken(accessToken)).andReturn(userScopeAccess);
        EasyMock.replay(scopeAccessDao);
        
        DelegatedClientScopeAccess retVal = scopeAccessService.getDelegatedScopeAccessByRefreshToken(user, accessToken);
        Assert.assertNull(retVal);
        
        EasyMock.verify(scopeAccessDao);
    }
    
    @Test
    public void shouldDeleteDelegatedToken() {
        User user = getFakeUser();
        String accessToken = "accessTokenString";
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setAccessTokenString(accessToken);
        DateTime tokenExpiryDate = new DateTime().plusDays(15);
        delegatedClientScopeAccess.setAccessTokenExp(tokenExpiryDate.toDate());
        List<DelegatedClientScopeAccess> list = new ArrayList<DelegatedClientScopeAccess>();
        list.add(delegatedClientScopeAccess);
        delegatedClientScopeAccess.setRefreshTokenString(accessToken);
        EasyMock.expect(scopeAccessDao.getDelegatedClientScopeAccessByUsername(user.getUsername())).andReturn(list);
        EasyMock.expect(scopeAccessDao.deleteScopeAccess(delegatedClientScopeAccess)).andReturn(true);
        EasyMock.replay(scopeAccessDao);

        scopeAccessService.deleteDelegatedToken(user, accessToken);

        EasyMock.verify(scopeAccessDao);
    }
    
    @Test(expected = NotFoundException.class)
    public void shouldNotDeleteDelegatedTokenBecauseTokenNotFound() {
        User user = getFakeUser();
        String accessToken = "accessTokenString";
        DelegatedClientScopeAccess delegatedClientScopeAccess = new DelegatedClientScopeAccess();
        delegatedClientScopeAccess.setAccessTokenString(accessToken);
        DateTime tokenExpiryDate = new DateTime().minusDays(15);
        delegatedClientScopeAccess.setAccessTokenExp(tokenExpiryDate.toDate());
        List<DelegatedClientScopeAccess> list = new ArrayList<DelegatedClientScopeAccess>();
        list.add(delegatedClientScopeAccess);
        delegatedClientScopeAccess.setRefreshTokenString(accessToken);
        EasyMock.expect(scopeAccessDao.getDelegatedClientScopeAccessByUsername(user.getUsername())).andReturn(null);
        EasyMock.expect(scopeAccessDao.deleteScopeAccess(delegatedClientScopeAccess)).andReturn(true);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.deleteDelegatedToken(user, accessToken);
    }
}
