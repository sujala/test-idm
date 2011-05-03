package com.rackspace.idm.services;

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
import com.rackspace.idm.domain.entity.PasswordResetScopeAccessObject;
import com.rackspace.idm.domain.entity.PermissionObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.entity.UserAuthenticationResult;
import com.rackspace.idm.domain.entity.UserScopeAccessObject;
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
            scopeAccessDao.getPermissionByParentAndPermissionId(client
                .getUniqueId(), perm)).andReturn(perm);

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
            scopeAccessDao.getPermissionByParentAndPermissionId(client
                .getUniqueId(), perm)).andReturn(null);

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
            scopeAccessDao.getScopeAccessForParentByClientId(client
                .getUniqueId(), client.getClientId())).andReturn(sa);
        EasyMock.expect(
            scopeAccessDao.getPermissionByParentAndPermissionId(sa
                .getUniqueId(), perm)).andReturn(perm);

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
            scopeAccessDao.getScopeAccessForParentByClientId(client
                .getUniqueId(), client.getClientId())).andReturn(
            getFakeScopeAccess());
        EasyMock.replay(scopeAccessDao);
        scopeAccessService.getScopeAccessForParentByClientId(client
            .getUniqueId(), client.getClientId());
        EasyMock.verify(scopeAccessDao);
    }

    @Test
    public void shouldGetPermissionForParent() {

        Client client = getFakeClient();
        PermissionObject permission = getFakePermission("permissionId");
        EasyMock.expect(
            scopeAccessDao.getPermissionByParentAndPermissionId(client
                .getUniqueId(), permission)).andReturn(permission);
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
    public void shouldGrantPermission() {
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
            scopeAccessDao.getPermissionByParentAndPermissionId(client
                .getUniqueId(), perm)).andReturn(perm);

        EasyMock.expect(
            scopeAccessDao.getPermissionByParentAndPermissionId(client
                .getUniqueId(), grantedPerm)).andReturn(grantedPerm);

        EasyMock.expect(
            scopeAccessDao.getScopeAccessForParentByClientId(client
                .getUniqueId(), client.getClientId())).andReturn(sa);

        EasyMock.expect(
            scopeAccessDao.grantPermission(sa.getUniqueId(), grantedPerm))
            .andReturn(grantedPerm);

        EasyMock.replay(scopeAccessDao, mockClientDao);

        scopeAccessService.grantPermissionToClient(client.getUniqueId(), perm);

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
            scopeAccessDao.getPermissionByParentAndPermissionId(client
                .getUniqueId(), perm)).andReturn(null);

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
            scopeAccessDao.getScopeAccessForParentByClientId(user.getUniqueId(),
                "PASSWORDRESET")).andReturn(prsao);
        EasyMock.expect(scopeAccessDao.updateScopeAccess(prsao)).andReturn(
            Boolean.TRUE);
        EasyMock.replay(scopeAccessDao);
        scopeAccessService
            .getOrCreatePasswordResetScopeAccessForUser(user);
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
}
