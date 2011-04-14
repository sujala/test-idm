package com.rackspace.idm.domain.dao.impl;

import java.util.List;
import java.util.Random;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientAuthenticationResult;
import com.rackspace.idm.domain.entity.ClientGroup;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ClientStatus;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.exception.DuplicateClientGroupException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.Modification;

public class LdapClientRepositoryTest {

    private LdapUserRepository userRepo;
    private LdapClientRepository repo;
    private LdapConnectionPools connPools;
    
    String userDN = "inum=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!1111,ou=people,o=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE,o=rackspace,dc=rackspace,dc=com";
    String testCustomerDN = "o=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE,o=rackspace,dc=rackspace,dc=com";
    
    @BeforeClass
    public static void cleanUpData() {
        final LdapConnectionPools pools = getConnPools();
        LdapClientRepository cleanUpRepo = getRepo(pools);
        Client deleteme = cleanUpRepo.getClientByClientId("DELETE_My_ClientId");
        if (deleteme != null) {
            cleanUpRepo.deleteClient("DELETE_My_ClientId");
        }
        pools.close();
    }

    private static LdapClientRepository getRepo(LdapConnectionPools connPools) {
        Configuration appConfig = new PropertyFileConfiguration().getConfigFromClasspath();
        return new LdapClientRepository(connPools, appConfig);
    }
    
    private static LdapUserRepository getUserRepo(LdapConnectionPools connPools) {
        Configuration appConfig = new PropertyFileConfiguration().getConfigFromClasspath();
        return new LdapUserRepository(connPools, appConfig);
    }

    private static LdapConnectionPools getConnPools() {
        return new LdapConfiguration(new PropertyFileConfiguration()
            .getConfigFromClasspath()).connectionPools();
    }

    @Before
    public void setUp() {
        connPools = getConnPools();
        repo = getRepo(connPools);
        userRepo = getUserRepo(connPools);
    }

    @Test
    public void shouldNotAcceptNullOrBlankClientname() {
        try {
            repo.getClientByClientname(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.getClientByClientname("     ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.addClient(null, testCustomerDN);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.deleteClient("     ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.getClientByClientId("     ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.getClientByInum("    ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.updateClient(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.addDefinedPermission(null, null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.deleteDefinedPermission(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.updateDefinedPermission(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void shouldFindOneClientThatExists() {
        Client client = repo.getClientByClientId("ABCDEF");
        Assert.assertNotNull(client);
        Assert.assertEquals("ABCDEF", client.getClientId());
    }

    @Test
    public void shouldNotFindClientThatDoesNotExist() {
        Client client = repo.getClientByClientname("hi. i don't exist.");
        Assert.assertNull(client);
    }

    @Test
    public void shouldRetrieveAllClientsThatExist() {
        List<Client> clients = repo.getAllClients();
        Assert.assertTrue(clients.size() >= 2);
    }

    @Test
    public void shouldAddNewClient() {
        Client newClient = addNewTestClient();
        Client checkClient = repo.getClientByClientId(newClient.getClientId());
        Assert.assertNotNull(checkClient);
        Assert.assertEquals("DELETE_My_Name", checkClient.getName());
        repo.deleteClient(newClient.getClientId());
    }

    @Test
    public void shouldDeleteClient() {
        Client newClient = addNewTestClient();
        repo.deleteClient(newClient.getClientId());
        Client idontexist = repo.getClientByClientname(newClient.getName());
        Assert.assertNull(idontexist);
    }

    @Test
    public void shouldUpdateNonDnAttrOfClient() {
        Client newClient = addNewTestClient();
        String clientId = newClient.getClientId();

        // Update all non-DN attributes
        newClient.setClientSecretObj(ClientSecret.newInstance("My_New_Secret"));
        newClient.setStatus(ClientStatus.INACTIVE);

        try {
            repo.updateClient(newClient);
        } catch (IllegalStateException e) {
            repo.deleteClient(newClient.getClientId());
            Assert.fail("Could not save the record: " + e.getMessage());
        }

        Client changedClient = repo.getClientByClientId(clientId);
        Assert.assertTrue(changedClient.equals(newClient));

        repo.deleteClient(newClient.getClientId());
    }
     
    @Test
    public void shouldAuthenticateForCorrectCredentials() {
        ClientAuthenticationResult authenticated = repo.authenticate("ABCDEF", "password");
        Assert.assertTrue(authenticated.isAuthenticated());
    }

    @Test
    public void shouldGenerateModifications() {
        Client client = createTestClientInstance();
        Client cClient = createTestClientInstance();
        cClient.setName("changed_client_name");
        cClient.setClientSecretObj(ClientSecret
            .newInstance("changed_client_secret"));
        cClient.setStatus(ClientStatus.INACTIVE);

        List<Modification> mods = repo.getModifications(client, cClient);

        Assert.assertEquals(2, mods.size());
        Assert.assertEquals("changed_client_secret", mods.get(0).getAttribute()
            .getValue());
        Assert.assertEquals(ClientStatus.INACTIVE.toString(), mods.get(1)
            .getAttribute().getValue());
    }

    @Test
    public void shouldGetUnusedClientInum() {
        String inum = repo
            .getUnusedClientInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE");
        Assert.assertFalse(inum.equals("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!2222"));
    }

    @Test
    public void shouldSetAllClientLocked() {
        Client newClient = addNewTestClient();
        repo.setClientsLockedFlagByCustomerId(newClient.getCustomerId(), true);
        Client changedClient = repo.getClientByClientId(newClient.getClientId());
        Assert.assertEquals(changedClient.isLocked(), true);
        repo.deleteClient(newClient.getClientId());
    }

    @Test
    public void shouldAddAndDeleteNewPermisison() {
        Client testClient = addNewTestClient();
        Permission testPermission = addNewTestPermission(testClient.getUniqueId());
        Permission checkPermission = repo
            .getDefinedPermissionByClientIdAndPermissionId(
                "DELETE_My_ClientId", "DELETE_My_Permission");
        Assert.assertTrue(checkPermission.getValue().equals("Some Value"));
        repo.deleteDefinedPermission(checkPermission);
        repo.deleteClient(testClient.getClientId());
    }

    @Test
    public void shouldUpdatePermission() {
        Client testClient = addNewTestClient();
        Permission testPermission = addNewTestPermission(testClient.getUniqueId());
        Permission checkPermission = repo
            .getDefinedPermissionByClientIdAndPermissionId(
                "DELETE_My_ClientId", "DELETE_My_Permission");
        Assert.assertTrue(checkPermission.getValue().equals("Some Value"));
        testPermission.setValue("Some Other Value");
        repo.updateDefinedPermission(testPermission);
        checkPermission = repo.getDefinedPermissionByClientIdAndPermissionId(
            "DELETE_My_ClientId", "DELETE_My_Permission");
        Assert
            .assertTrue(checkPermission.getValue().equals("Some Other Value"));
        repo.deleteDefinedPermission(checkPermission);
        repo.deleteClient(testClient.getClientId());
    }

    @Test
    public void shouldGetPermissions() {
        Client testClient = addNewTestClient();
        Permission testPermission = addNewTestPermission(testClient.getUniqueId());
        List<Permission> resources = repo
            .getDefinedPermissionsByClientId("DELETE_My_ClientId");
        Assert.assertTrue(resources.size() >= 1);
        repo.deleteDefinedPermission(testPermission);
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldAddClientGroup() {
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group, testClient.getUniqueId());
        ClientGroup returnedGroup = repo.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        Assert.assertNotNull(returnedGroup);
        repo.deleteClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldUpdateClientGroup() {
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        group = repo.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        group.setType("My New Type");
        repo.updateClientGroup(group);
        ClientGroup returnedGroup = repo.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        repo.deleteClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        repo.deleteClient(testClient.getClientId());
        
        Assert.assertNotNull(returnedGroup);
        Assert.assertTrue(returnedGroup.getType().equalsIgnoreCase("My New Type"));
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void shouldNotUpdateClientGroup() {
        repo.updateClientGroup(null);
    }
    
    @Test
    public void shouldNotAddClientGroupForDuplicate() {
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        ClientGroup returnedGroup = repo.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        Assert.assertNotNull(returnedGroup);
        try {
            repo.addClientGroup(group,testClient.getUniqueId());
            Assert.fail("Shouldn't Have added Group");
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof DuplicateClientGroupException);
        }
        repo.deleteClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldDeleteClientGroup() {
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        ClientGroup returnedGroup = repo.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        Assert.assertNotNull(returnedGroup);
        repo.deleteClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        returnedGroup = repo.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        Assert.assertNull(returnedGroup);
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldThrowErrorForDeleteClientGroupForNonExistentClient() {
        
        try {
            repo.deleteClientGroup("CUSTOMERID", "BADCLIENTNAME", "GROUPNAME");
            Assert.fail("Shouldn't have Deleted Group");
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof NotFoundException);
        }
    }
    
    @Test
    public void shouldGetClientGroup() {
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        ClientGroup returnedGroup = repo.getClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        Assert.assertNotNull(returnedGroup);
        repo.deleteClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldGetClientGroups() {
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        group.setName("NEWGROUPNAME");
        repo.addClientGroup(group,testClient.getUniqueId());
        List<ClientGroup> groups = repo.getClientGroupsByClientId(testClient.getClientId());
        Assert.assertTrue(groups.size() == 2);
        repo.deleteClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        repo.deleteClientGroup(group.getCustomerId(), group.getClientId(), "New Group");
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test 
    public void shouldThowNotFoundErrorForNonExistentClient() {
        try {
        repo.getClientGroupsByClientId("SOMEBADCLIENTID");
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof NotFoundException);
        }
    }
    
    @Test 
    public void shouldNotGetClientGroupForNonExistenClient() {
        ClientGroup returnedGorup = repo.getClientGroup("RACKSPACE", "BADCLIENTNAME", "name");
        Assert.assertNull(returnedGorup);
    }
    
    @Test
    public void shouldReturnNullForGetClientGroupForNonExistentGroup() {
        Client testClient = addNewTestClient();
        ClientGroup returnedGroup = repo.getClientGroup(testClient.getCustomerId(), testClient.getClientId(), "SOMEBADNAME");
        Assert.assertNull(returnedGroup);
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldAddUserToClientGroup() {
        User user = userRepo.getUserByUsername("mkovacs");
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        repo.addUserToClientGroup(user.getUniqueId(), group);
        boolean inGroup = repo.isUserInClientGroup("mkovacs", group.getUniqueId());
        repo.deleteClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        repo.deleteClient(testClient.getClientId());
        Assert.assertTrue(inGroup);
    }
    
    @Test
    public void shouldNotAddUserToClientGroupIfAlreadyInGroup() {
        User user = userRepo.getUserByUsername("mkovacs");
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        repo.addUserToClientGroup(user.getUniqueId(), group);
        try {
            repo.addUserToClientGroup(user.getUniqueId(), group);
            Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof DuplicateException);
        }
        repo.deleteClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldNotAddUserToClientGroupForNullUser() {
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        try {
        repo.addUserToClientGroup(null, group);
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldNotAddUserToClientGroupForUserWithBlankUniqueID() {
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        try {
        repo.addUserToClientGroup(new User().getUniqueId(), group);
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldNotAddUserToClientGroupForNullGroup() {
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        try {
        repo.addUserToClientGroup(createTestUser().getUniqueId(), null);
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldNotAddUserToClientGroupForGroupWithBlankUniqueID() {
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        group.setUniqueId(null);
        try {
        repo.addUserToClientGroup(createTestUser().getUniqueId(), group);
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldRemoveUserFromClientGroup() {

        User user = userRepo.getUserByUsername("mkovacs");
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        repo.addUserToClientGroup(user.getUniqueId(), group);
        repo.removeUserFromGroup(user.getUniqueId(), group);
        repo.deleteClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldNotRemoveUserFromClientGroupIfUserNotInGroup() {

        User user = userRepo.getUserByUsername("mkovacs");
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        repo.addClientGroup(group,testClient.getUniqueId());
        try {
        repo.removeUserFromGroup(user.getUniqueId(), group);
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof NotFoundException);
        }
        repo.deleteClientGroup(group.getCustomerId(), group.getClientId(), group.getName());
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldNotRemoveUserFromClientGroupForNullUser() {
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        try {
        repo.removeUserFromGroup(null, group);
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldNotRemoveUserFromClientGroupForUserWithBlankUniqueID() {
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        try {
        repo.removeUserFromGroup(new User().getUniqueId(), group);
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldNotRemoveUserFromClientGroupForNullGroup() {
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        try {
        repo.removeUserFromGroup(createTestUser().getUniqueId(), null);
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldNotRemoveUserFromClientGroupForGroupWithBlankUniqueID() {
        Client testClient = addNewTestClient();
        ClientGroup group = createNewTestClientGroup(testClient);
        group.setUniqueId(null);
        try {
        repo.removeUserFromGroup(createTestUser().getUniqueId(), group);
        Assert.fail();
        }
        catch (Exception ex) {
            Assert.assertTrue(ex instanceof IllegalArgumentException);
        }
        
        repo.deleteClient(testClient.getClientId());
    }
    
    @Test
    public void shouldGrantAndRevokePermissionToClient() {
        Client testClient = addNewTestClient();
        Permission permission = createTestPermissionInstance();
        permission.setUniqueId("XXX");
        repo.grantPermissionToClient(permission, testClient);
        
        Client returned = repo.getClientByCustomerIdAndClientId(testClient.getCustomerId(), testClient.getClientId());
        
        Assert.assertNotNull(returned);
        Assert.assertNotNull(returned.getPermissions());
        Assert.assertTrue(returned.getPermissions().get(0).getPermissionId().equals("DELETE_My_Permission"));
        
        repo.revokePermissionFromClient(permission, testClient);
        returned = repo.getClientByCustomerIdAndClientId(testClient.getCustomerId(), testClient.getClientId());
        repo.deleteClient(testClient.getClientId());
        
        Assert.assertNotNull(returned);
        Assert.assertNull(returned.getPermissions());
    }

    @After
    public void tearDown() {
        connPools.close();
    }

    private Client addNewTestClient() {
        Client newClient = createTestClientInstance();
        repo.addClient(newClient, testCustomerDN);
        return newClient;
    }
    
    private ClientGroup createNewTestClientGroup(Client client) {
        return new ClientGroup (client.getClientId(), client.getCustomerId(), "New Group", "TYPE");
    }

    private Client createTestClientInstance() {
        Random ran = new Random();
        //int random = ran.nextInt();
        Client newClient = new Client("DELETE_My_ClientId", ClientSecret
            .newInstance("DELETE_My_Client_Secret"), "DELETE_My_Name", "inum",
            "iname", "RCN-123-456-789", ClientStatus.ACTIVE);
        newClient.setLocked(false);
        newClient.setSoftDeleted(false);
        return newClient;
    }

    private Permission addNewTestPermission(String clientUniqueId) {
        Permission res = createTestPermissionInstance();
        repo.addDefinedPermission(res,clientUniqueId);
        return res;
    }

    private Permission createTestPermissionInstance() {
        Permission res = new Permission();
        res.setClientId("DELETE_My_ClientId");
        res.setCustomerId("RCN-123-456-789");
        res.setPermissionId("DELETE_My_Permission");
        res.setValue("Some Value");
        return res;
    }
    
    private User createTestUser() {
        User user = new User();
        user.setUniqueId(userDN);
        return user;
    }
}
