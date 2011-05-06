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
import com.rackspace.idm.domain.dao.ScopeAccessObjectDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.Clients;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccessObject;
import com.rackspace.idm.domain.entity.PermissionObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserScopeAccessObject;
import com.rackspace.idm.domain.entity.Users;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.impl.DefaultScopeAccessService;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.util.AuthHeaderHelper;

public class ScopeAccessServiceTests extends ServiceTestsBase {

    UserDao mockUserDao;
    ScopeAccessObjectDao scopeAccessDao;
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
        scopeAccessDao = EasyMock.createMock(ScopeAccessObjectDao.class);
        scopeAccessService = new DefaultScopeAccessService(mockUserDao,
            mockClientDao, scopeAccessDao, authHeaderHelper, appConfig);
    }

    @Test
    public void shouldAddPermission() {
        Client client = getFakeClient();
        ScopeAccessObject sa = getFakeScopeAccess();
        PermissionObject perm = getFakePermission("permissionId");

        EasyMock.expect(mockClientDao.getClientByClientId(perm.getClientId()))
            .andReturn(client);

        EasyMock.expect(
            scopeAccessDao.getPermissionByParentAndPermissionId(
                client.getUniqueId(), perm)).andReturn(perm);

        EasyMock.expect(scopeAccessDao.grantPermission(sa.getUniqueId(), perm))
            .andReturn(perm);

        EasyMock.replay(scopeAccessDao, mockClientDao);

        scopeAccessService.addPermissionToScopeAccess(sa.getUniqueId(), perm);

        EasyMock.verify(scopeAccessDao);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotAddPermissionIfItDoesNotExist() {
        Client client = getFakeClient();
        ScopeAccessObject sa = getFakeScopeAccess();
        PermissionObject perm = getFakePermission("permissionId");

        EasyMock.expect(mockClientDao.getClientByClientId(perm.getClientId()))
            .andReturn(client);

        EasyMock.expect(
            scopeAccessDao.getPermissionByParentAndPermissionId(
                client.getUniqueId(), perm)).andReturn(null);

        EasyMock.expect(scopeAccessDao.grantPermission(sa.getUniqueId(), perm))
            .andReturn(perm);

        EasyMock.replay(scopeAccessDao, mockClientDao);

        scopeAccessService.addPermissionToScopeAccess(sa.getUniqueId(), perm);

        EasyMock.verify(scopeAccessDao);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotAddPermissionIfClientDoesNotExist() {
        Client client = getFakeClient();
        ScopeAccessObject sa = getFakeScopeAccess();
        PermissionObject perm = getFakePermission("permissionId");

        EasyMock.expect(mockClientDao.getClientByClientId(perm.getClientId()))
            .andReturn(null);

        EasyMock.expect(
            scopeAccessDao.getScopeAccessForParentByClientId(
                client.getUniqueId(), client.getClientId())).andReturn(sa);
        EasyMock.expect(
            scopeAccessDao.getPermissionByParentAndPermissionId(
                sa.getUniqueId(), perm)).andReturn(perm);

        EasyMock.expect(scopeAccessDao.grantPermission(sa.getUniqueId(), perm))
            .andReturn(perm);

        EasyMock.replay(scopeAccessDao, mockClientDao);

        scopeAccessService.addPermissionToScopeAccess(sa.getUniqueId(), perm);

        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldRemovePermission() {

        PermissionObject perm = getFakePermission("permissionId");

        EasyMock.expect(scopeAccessDao.removePermissionFromScopeAccess(perm))
            .andReturn(true);
        EasyMock.replay(scopeAccessDao);

        scopeAccessService.removePermission(perm);
        EasyMock.verify(scopeAccessDao);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotRemovePermissionIfIllegalStateExceptionNull() {

        PermissionObject perm = getFakePermission("permissionId");

        EasyMock.expect(scopeAccessDao.removePermissionFromScopeAccess(perm))
            .andThrow(new IllegalStateException());
        EasyMock.replay(scopeAccessDao);

        scopeAccessService.removePermission(perm);
    }

    @Test
    public void shouldUpdatePermission() {
        PermissionObject permission = getFakePermission("permissionId");
        EasyMock.expect(
            scopeAccessDao.updatePermissionForScopeAccess(permission))
            .andReturn(Boolean.TRUE);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.updatePermission(permission);
        EasyMock.verify(scopeAccessDao);
    }

    @Test(expected = IllegalStateException.class)
    public void shouldNotUpdatePermissionIfIllegalStateException() {
        PermissionObject permission = getFakePermission("permissionId");
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
            scopeAccessDao.getScopeAccessForParentByClientId(
                client.getUniqueId(), client.getClientId())).andReturn(
            getFakeScopeAccess());
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.getScopeAccessForParentByClientId(
            client.getUniqueId(), client.getClientId());
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldGetPermissionForParent() {

        Client client = getFakeClient();
        PermissionObject permission = getFakePermission("permissionId");
        EasyMock.expect(
            scopeAccessDao.getPermissionByParentAndPermissionId(
                client.getUniqueId(), permission)).andReturn(permission);
        EasyMock.replay(scopeAccessDao);

        scopeAccessService.getPermissionForParent(client.getUniqueId(),
            permission);
        EasyMock.verify(scopeAccessDao);

    }

    @Test
    public void shouldUpdateScopeAccess() {
        ScopeAccessObject sa = getFakeScopeAccess();
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
        ScopeAccessObject sa = getFakeScopeAccess();
        PermissionObject perm = getFakePermission("fakePermissionObjectId");

        EasyMock.expect(
            mockClientDao.getClientByCustomerIdAndClientId(
                perm.getCustomerId(), perm.getClientId())).andReturn(client);

        EasyMock.expect(mockClientDao.getClientByClientId(perm.getClientId()))
            .andReturn(client);
        PermissionObject grantedPerm = new PermissionObject();
        grantedPerm.setClientId(perm.getClientId());
        grantedPerm.setCustomerId(perm.getCustomerId());
        grantedPerm.setPermissionId(perm.getPermissionId());

        EasyMock.expect(
            scopeAccessDao.getPermissionByParentAndPermissionId(
                client.getUniqueId(), perm)).andReturn(perm);

        EasyMock.expect(
            scopeAccessDao.getPermissionByParentAndPermissionId(
                client.getUniqueId(), grantedPerm)).andReturn(grantedPerm);

        EasyMock.expect(
            scopeAccessDao.getScopeAccessForParentByClientId(
                client.getUniqueId(), client.getClientId())).andReturn(sa);

        EasyMock.expect(
            scopeAccessDao.grantPermission(sa.getUniqueId(), grantedPerm))
            .andReturn(grantedPerm);

        EasyMock.replay(scopeAccessDao, mockClientDao);

        PermissionObject returnedPerm = scopeAccessService.grantPermissionToClient(client.getUniqueId(), perm);
        
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
        UserScopeAccessObject sa = getFakeUserScopeAccess();
        PermissionObject perm = getFakePermission("fakePermissionObjectId");

        EasyMock.expect(
            mockClientDao.getClientByCustomerIdAndClientId(
                perm.getCustomerId(), perm.getClientId())).andReturn(client);

        EasyMock.expect(mockClientDao.getClientByClientId(perm.getClientId()))
            .andReturn(client);
        
        PermissionObject grantedPerm = new PermissionObject();
        grantedPerm.setClientId(perm.getClientId());
        grantedPerm.setCustomerId(perm.getCustomerId());
        grantedPerm.setPermissionId(perm.getPermissionId());

        EasyMock.expect(
            scopeAccessDao.getPermissionByParentAndPermissionId(
                client.getUniqueId(), perm)).andReturn(perm);

        EasyMock.expect(
            scopeAccessDao.getPermissionByParentAndPermissionId(
                client.getUniqueId(), grantedPerm)).andReturn(grantedPerm);

        EasyMock.expect(
            scopeAccessDao.getScopeAccessForParentByClientId(
                user.getUniqueId(), client.getClientId())).andReturn(sa);

        EasyMock.expect(
            scopeAccessDao.grantPermission(sa.getUniqueId(), grantedPerm))
            .andReturn(grantedPerm);

        EasyMock.replay(scopeAccessDao, mockClientDao);

        PermissionObject returnedPerm = scopeAccessService.grantPermissionToUser(user, perm);
        
        Assert.assertEquals(grantedPerm, returnedPerm);
        Assert.assertEquals(grantedPerm.getClientId(), returnedPerm.getClientId());
        Assert.assertEquals(grantedPerm.getCustomerId(), returnedPerm.getCustomerId());
        Assert.assertEquals(grantedPerm.getPermissionId(), returnedPerm.getPermissionId());

        EasyMock.verify(scopeAccessDao, mockClientDao);
    }   

    @Test(expected = NotFoundException.class)
    public void shouldNotGrantPermissionIfClientIsNotFound() {
        Client client = getFakeClient();
        ScopeAccessObject sa = getFakeScopeAccess();
        PermissionObject perm = getFakePermission("fakePermissionObjectId");

        EasyMock.expect(
            mockClientDao.getClientByCustomerIdAndClientId(
                perm.getCustomerId(), perm.getClientId())).andReturn(client);

        EasyMock.expect(mockClientDao.getClientByClientId(perm.getClientId()))
            .andReturn(null);

        scopeAccessService.grantPermissionToClient(client.getUniqueId(), perm);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotGrantPermissionIfPermissionIsNotFound() {
        Client client = getFakeClient();
        ScopeAccessObject sa = getFakeScopeAccess();
        PermissionObject perm = getFakePermission("fakePermissionObjectId");

        EasyMock.expect(
            mockClientDao.getClientByCustomerIdAndClientId(
                perm.getCustomerId(), perm.getClientId())).andReturn(client);

        EasyMock.expect(mockClientDao.getClientByClientId(perm.getClientId()))
            .andReturn(client);

        EasyMock.expect(
            scopeAccessDao.getPermissionByParentAndPermissionId(
                client.getUniqueId(), perm)).andReturn(null);

        scopeAccessService.grantPermissionToClient(client.getUniqueId(), perm);
    }

    @Test
    public void shouldGetUserScopeAccessForClientIdByUsernameAndPassword() {

        User user = getFakeUser();
        Client client = getFakeClient();
        UserScopeAccessObject sa = getFakeUserScopeAccess();
        EasyMock.expect(mockUserDao.authenticate(username, "password"))
            .andReturn(new UserAuthenticationResult(user, true));

        EasyMock.expect(
            scopeAccessDao.getScopeAccessForParentByClientId(
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
        UserScopeAccessObject sa = getFakeUserScopeAccess();

        String nastId = "fakeNastId";
        String apiKey = "fakeApiKey";
        EasyMock.expect(
            mockUserDao.authenticateByNastIdAndAPIKey(nastId, apiKey))
            .andReturn(new UserAuthenticationResult(user, true));

        EasyMock.expect(
            scopeAccessDao.getScopeAccessForParentByClientId(
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
        UserScopeAccessObject sa = getFakeUserScopeAccess();

        int mossoId = 12345;
        String apiKey = "fakeApiKey";
        EasyMock.expect(
            mockUserDao.authenticateByMossoIdAndAPIKey(mossoId, apiKey))
            .andReturn(new UserAuthenticationResult(user, true));

        EasyMock.expect(
            scopeAccessDao.getScopeAccessForParentByClientId(
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
        UserScopeAccessObject sa = getFakeUserScopeAccess();

        String apiKey = "fakeApiKey";
        EasyMock.expect(mockUserDao.authenticateByAPIKey(username, apiKey))
            .andReturn(new UserAuthenticationResult(user, true));

        EasyMock.expect(
            scopeAccessDao.getScopeAccessForParentByClientId(
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
        UserScopeAccessObject usao = getFakeUserScopeAccess();
        EasyMock.expect(
            scopeAccessDao.getScopeAccessForParentByClientId("userUniqueId",
                "clientId")).andReturn(usao);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.getUserScopeAccessForClientId("userUniqueId",
            "clientId");
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldGetScopeAccessByRefreshToken() {
        UserScopeAccessObject usao = getFakeUserScopeAccess();
        EasyMock.expect(
            scopeAccessDao.getScopeAccessByRefreshToken("refreshToken"))
            .andReturn(usao);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.getScopeAccessByRefreshToken("refreshToken");
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldGetScopeAccessByAccessToken() {
        UserScopeAccessObject usao = getFakeUserScopeAccess();
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
            scopeAccessDao.getScopeAccessForParentByClientId("rackerUniqueId",
                clientId)).andReturn(getFakeRackerScopeAccess());
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.getRackerScopeAccessForClientId("rackerUniqueId",
            clientId);
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldGetasswordResetScopeAccessForUser() {
        User user = getFakeUser();
        PasswordResetScopeAccessObject prsao = getFakePasswordResetScopeAccessObject();
        DateTime pastTime = new DateTime();
        prsao.setAccessTokenExp(pastTime.minusSeconds(10).toDate());
        EasyMock.expect(
            scopeAccessDao.getScopeAccessForParentByClientId(
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
            scopeAccessDao.getScopeAccessForParentByClientId("clientUniqueId",
                clientId)).andReturn(getFakeClientScopeAccess());
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.getClientScopeAccessForClientId("clientUniqueId",
            clientId);
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldGetAccessTokenByAuthHeader() {
        ScopeAccessObject fakeScopeAccess = getFakeScopeAccess();
        EasyMock.expect(
            scopeAccessDao.getScopeAccessByAccessToken("accessToken"))
            .andReturn(fakeScopeAccess);
        EasyMock.replay(scopeAccessDao);
        ScopeAccessObject sa = scopeAccessService
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
        List<ScopeAccessObject> sas = new ArrayList<ScopeAccessObject>();
        UserScopeAccessObject fakeUserScopeAccess0 = getFakeUserScopeAccess();
        sas.add(fakeUserScopeAccess0);
        UserScopeAccessObject fakeUserScopeAccess1 = getFakeUserScopeAccess();
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
        List<ScopeAccessObject> sas = new ArrayList<ScopeAccessObject>();
        ClientScopeAccessObject fakeClientScopeAccess0 = getFakeClientScopeAccess();
        sas.add(fakeClientScopeAccess0);
        ClientScopeAccessObject fakeClientScopeAccess1 = getFakeClientScopeAccess();
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
        UserScopeAccessObject sa = getFakeUserScopeAccess();
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
        String accessTokenString = "tokenString";
        PermissionObject fakePermission = getFakePermission("permId");
        EasyMock.expect(
            scopeAccessDao.doesAccessTokenHavePermission(accessTokenString,
                fakePermission)).andReturn(Boolean.TRUE);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.doesAccessTokenHavePermission(accessTokenString,
            fakePermission);
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldDeleteScopeAccess() {
        ScopeAccessObject sa = getFakeScopeAccess();
        EasyMock.expect(scopeAccessDao.deleteScopeAccess(sa)).andReturn(
            Boolean.TRUE);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.deleteScopeAccess(sa);
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldAuthenticateAccessToken() {
        UserScopeAccessObject sao = getFakeUserScopeAccess();
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
        UserScopeAccessObject sao = getFakeUserScopeAccess();
        User user = getFakeUser();
        EasyMock.expect(scopeAccessDao.addScopeAccess(
            user.getUniqueId(), sao)).andReturn(sao);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.addScopeAccess(user.getUniqueId(), sao);
        EasyMock.verify(scopeAccessDao);
    
    }
}
