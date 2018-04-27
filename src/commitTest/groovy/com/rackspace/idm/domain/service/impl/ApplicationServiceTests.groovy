package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;

import junit.framework.Assert;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.ClientSecret;

import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.exception.NotFoundException;

public class ApplicationServiceTests {

    DefaultApplicationService service;
    ApplicationDao applicationDao;
    ApplicationRoleDao applicationRoleDao;
    ScopeAccessService scopeAccessService;
    TenantService tenantService;

    String clientId = "ClientId";
    ClientSecret clientSecret = ClientSecret.newInstance("Secret");
    String name = "Name";
    String customerId = "CustomerId";

    String groupName = "groupName";
    String groupType = "groupType";

    String userDN = "userDN";
    String groupDN = "groupDN";
    String username = "username";

    String uniqueId = "uniqueId";
    
    String id = "XXX";

    @Before
    public void setUp() throws Exception {
        service = new DefaultApplicationService();
        applicationDao = EasyMock.createMock(ApplicationDao.class);
        applicationRoleDao = EasyMock.createMock(ApplicationRoleDao.class);
        scopeAccessService = EasyMock.createMock(ScopeAccessService.class);
        tenantService = EasyMock.createMock(TenantService.class);

        service.applicationDao = applicationDao;
        service.tenantService = tenantService;
        service.applicationRoleDao = applicationRoleDao;
        service.scopeAccessService = scopeAccessService;
    }

    @Test
    public void shouldGetClientByName() {

        Application client = getFakeClient();
        EasyMock.expect(applicationDao.getApplicationByName(name)).andReturn(
            client);
        EasyMock.replay(applicationDao);

        Application retrievedClient = service.getByName(name);

        Assert.assertTrue(retrievedClient.getName().equals(name));
        EasyMock.verify(applicationDao);
    }

    @Test
    public void shouldGetClientById() {

        Application client = getFakeClient();
        EasyMock.expect(applicationDao.getApplicationByClientId(clientId)).andReturn(
            client);
        EasyMock.replay(applicationDao);

        Application retrievedClient = service.getById(clientId);

        Assert.assertTrue(retrievedClient.getName().equals(name));
        EasyMock.verify(applicationDao);
    }

    @Test
    public void shouldReturnNullForNonExistentClientByName() {
        EasyMock.expect(applicationDao.getApplicationByName(name)).andReturn(
            null);
        EasyMock.replay(applicationDao);

        Application retrievedClient = service.getByName(name);

        Assert.assertNull(retrievedClient);
        EasyMock.verify(applicationDao);
    }

    @Test
    public void shouldReturnNullForNonExistentClientById() {
        EasyMock.expect(applicationDao.getApplicationByClientId(clientId)).andReturn(
            null);
        EasyMock.replay(applicationDao);

        Application retrievedClient = service.getById(clientId);

        Assert.assertNull(retrievedClient);
        EasyMock.verify(applicationDao);
    }

    @Test
    public void shouldDeleteClient() {
        Application fakeClient = getFakeClient()
        applicationDao.deleteApplication(EasyMock.anyObject(Application.class));
        EasyMock.expect(applicationDao.getApplicationByClientId(clientId)).andReturn(
            fakeClient);

        EasyMock.expect(
            scopeAccessService.getMostRecentDirectScopeAccessForUserByClientId(EasyMock.anyObject(User.class), EasyMock.anyObject(String.class))).andReturn(getFakeScopeAccess());

        List<ClientRole> clientRoles = new ArrayList<ClientRole>();
        EasyMock.expect(applicationRoleDao.getClientRolesForApplication(fakeClient)).andReturn(clientRoles);

        EasyMock.replay(applicationDao, scopeAccessService, applicationRoleDao);

        service.delete(clientId);

        EasyMock.verify(applicationDao);
    }

    @Test(expected = NotFoundException.class)
    public void shouldThrowNotFoundExceptionForDeleteNonExistentClient() {
        EasyMock.expect(applicationDao.getApplicationByClientId(clientId)).andReturn(
            null);

        EasyMock.replay(applicationDao);

        service.delete(clientId);
    }

    @Test
    public void shouldSaveClient() {
        Application client = getFakeClient();
        applicationDao.updateApplication(client);
        EasyMock.replay(applicationDao);

        service.save(client);

        EasyMock.verify(applicationDao);
    }

    @Test
    public void shouldResetClientSecret() {

        Application client = getFakeClient();
        String oldSecret = client.getClientSecret();

        applicationDao.updateApplication(client);
        EasyMock.replay(applicationDao);

        ClientSecret clientSecret = service.resetClientSecret(client);

        Assert.assertNotSame(oldSecret, clientSecret.getValue());
        EasyMock.verify(applicationDao);
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotResetClientSecretIfNull() {
        Application client = null;
        service.resetClientSecret(client);
    }

    private Application getFakeClient() {
        Application client = new Application(clientId, name);
        return client;
    }

    private List<ClientGroup> getFakeClientGroupList() {
        List<ClientGroup> groups = new ArrayList<ClientGroup>();
        groups.add(getFakeClientGroup());
        return groups;
    }

    private ClientGroup getFakeClientGroup() {
        ClientGroup group = new ClientGroup(clientId, customerId, groupName,
            groupType);
        group.setUniqueId(groupDN);
        return group;
    }

    private User getFakeUser() {
        User user = new User();
        user.setUsername(username);
        user.setUniqueId(userDN);
        user.setEnabled(true);
        return user;
    }

    private ScopeAccess getFakeScopeAccess() {
        ScopeAccess sa = new ScopeAccess();
        sa.setClientId(clientId);
        sa.setClientRCN(customerId);
        return sa;
    }
}
