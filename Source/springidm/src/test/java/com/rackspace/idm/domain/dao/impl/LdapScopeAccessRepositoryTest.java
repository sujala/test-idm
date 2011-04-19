package com.rackspace.idm.domain.dao.impl;

import org.apache.commons.configuration.Configuration;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ClientStatus;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.CustomerStatus;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.exception.NotFoundException;

public class LdapScopeAccessRepositoryTest {

    private LdapCustomerRepository customerRepo;
    private LdapScopeAccessRepository repo;
    private LdapClientRepository clientRepo;
    private LdapConnectionPools connPools;
    
    static String customerId = "DELETE_My_CustomerId";
    String customerName = "DELETE_My_Name";
    String inum = "@!FFFF.FFFF.FFFF.FFFF!CCCC.CCCC";
    String iname = "@Rackspae.TESTING";
    CustomerStatus status = CustomerStatus.ACTIVE;
    String seeAlso = "inum=@!FFFF.FFFF.FFFF.FFFF!CCCC.CCCC";
    String owner = "inum=@!FFFF.FFFF.FFFF.FFFF!CCCC.CCCC";
    String country = "USA";
    boolean softDeleted = false;
    
    String clientId = "XXX";
    String RCN = "RACKSPACE";
    
    String permissionId = "PermissionName";
    
    String accessToken = "YYYYYYY";
    String refreshToken = "ZZZZZZZ";
    
    Client client = null;
    Customer customer = null;

    @BeforeClass
    public static void cleanUpData() {
        final LdapConnectionPools pools = getConnPools();
        LdapClientRepository cleanUpRepo = getClientRepo(pools);
        Client deleteme = cleanUpRepo.getClientByClientId("XXX");
        if (deleteme != null) {
            cleanUpRepo.deleteClient("XXX");
        }
        pools.close();
    }

    @Before
    public void setUp() {
        connPools = getConnPools();
        repo = getSaRepo(connPools);
        customerRepo = getCustomerRepo(connPools);
        clientRepo = getClientRepo(connPools);
        
        customer = addNewTestCustomer(customerId, customerName, inum, iname, status,
            seeAlso, owner, country);
        client = addNewTestClient(customer);
    }

    @After
    public void tearDown() {
        customerRepo.deleteCustomer(customer.getCustomerId());
        connPools.close();
    }
    
    @Test
    public void shouldGetScopeAccessByUsernameAndClientId() {
        ScopeAccess sa = createScopeAccessObject(client.getClientId(), client.getCustomerId());
        sa.setUsername("username");
        repo.addScopeAccess(client.getUniqueId(), sa);
        ScopeAccess returned = repo.getScopeAccessByUsernameAndClientId("username", client.getClientId());
        Assert.assertTrue(sa.equals(returned));
    }
    
    @Test
    public void shouldAddScopeAccessToClient() {
        ScopeAccess sa = createScopeAccessObject(client.getClientId(), client.getCustomerId());
        repo.addScopeAccess(client.getUniqueId(), sa);
        ScopeAccess returned = repo.getScopeAccessForParentByClientId(client.getUniqueId(), client.getClientId());
        Assert.assertTrue(sa.equals(returned));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void shouldFailToAddScopeAccessToClientForNullSA() {
        repo.addScopeAccess(null, null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void shouldFailToAddScopeAccessToClientForBlankParentId() {
        ScopeAccess sa = createScopeAccessObject(client.getClientId(), client.getCustomerId());
        repo.addScopeAccess(null, sa);
    }
    
    @Test
    public void shouldAddPermissionToScopeAccess() {
        ScopeAccess sa = createScopeAccessObject(client.getClientId(), client.getCustomerId());
        repo.addScopeAccess(client.getUniqueId(), sa);
        Permission p = createPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        repo.addPermissionToScopeAccess(sa.getUniqueId(), p);
        Assert.assertNotNull(p.getUniqueId());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void shouldFailToAddPermissionToSAForNullPermission() {
        repo.addPermissionToScopeAccess(null, null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void shouldFailToAddPermissionToSAForBlankParentId() {
        Permission p = createPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        repo.addPermissionToScopeAccess(null, null);
    }
    
    @Test
    public void shoudlDeleteScopeAccessObject() {
        ScopeAccess sa = createScopeAccessObject(client.getClientId(), client.getCustomerId());
        repo.addScopeAccess(client.getUniqueId(), sa);
        ScopeAccess returned = repo.getScopeAccessForParentByClientId(client.getUniqueId(), client.getClientId());
        repo.deleteScopeAccess(sa);
        sa = repo.getScopeAccessForParentByClientId(client.getUniqueId(), client.getClientId());
        Assert.assertNotNull(returned);
        Assert.assertNull(sa);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void shoudlNotDeleteScopeAccessObjectForBlankUniqueId() {
        ScopeAccess sa = createScopeAccessObject(client.getClientId(), client.getCustomerId());
        repo.deleteScopeAccess(sa);
    }
    
    @Test
    public void shouldReturnTrueForDoesAccessTokenHavePermission() {
        ScopeAccess sa = createScopeAccessObject(client.getClientId(), client.getCustomerId());
        repo.addScopeAccess(client.getUniqueId(), sa);
        Permission p = createPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        repo.addPermissionToScopeAccess(sa.getUniqueId(), p);
        boolean hasPermission = repo.doesAccessTokenHavePermission(accessToken, p);
        Assert.assertTrue(hasPermission);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void shouldThrowErrorForBlankAccessToken() {
        boolean hasPermission = repo.doesAccessTokenHavePermission(null, null);
        Assert.assertTrue(hasPermission);
    }
    
    @Test(expected=NotFoundException.class)
    public void shouldThrowErrorForTokenNotFound() {
        ScopeAccess sa = createScopeAccessObject(client.getClientId(), client.getCustomerId());
        repo.addScopeAccess(client.getUniqueId(), sa);
        Permission p = createPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        repo.addPermissionToScopeAccess(sa.getUniqueId(), p);
        repo.doesAccessTokenHavePermission("Non_existant_token", p);
    }
    
    @Test
    public void shouldGetScopeAccessByAccessToken() {
        ScopeAccess sa = createScopeAccessObject(client.getClientId(), client.getCustomerId());
        repo.addScopeAccess(client.getUniqueId(), sa);
        ScopeAccess returned = repo.getScopeAccessByAccessToken(accessToken);
        Assert.assertTrue(sa.equals(returned));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void shouldThrowErrorForGetScopeAccessByAccessTokenForBlankAccessToken() {
        repo.getScopeAccessByAccessToken(null);
    }
    
    @Test
    public void shouldGetScopeAccessByRefreshToken() {
        ScopeAccess sa = createScopeAccessObject(client.getClientId(), client.getCustomerId());
        repo.addScopeAccess(client.getUniqueId(), sa);
        ScopeAccess returned = repo.getScopeAccessByRefreshToken(refreshToken);
        Assert.assertTrue(sa.equals(returned));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void shouldThrowErrorForGetScopeAccessByRefreshTokenForBlankRefreshToken() {
        repo.getScopeAccessByRefreshToken(null);
    }
    
    @Test
    public void shouldRemovePermissionFromScopeAccess() {
        ScopeAccess sa = createScopeAccessObject(client.getClientId(), client.getCustomerId());
        repo.addScopeAccess(client.getUniqueId(), sa);
        Permission p = createPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        repo.addPermissionToScopeAccess(sa.getUniqueId(), p);
        Assert.assertNotNull(p.getUniqueId());
        repo.removePermissionFromScopeAccess(p);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void shouldNotRemovePermissionFromScopeAccessForNullUniqueId() {
        Permission p = createPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        repo.removePermissionFromScopeAccess(p);
    }
    
    @Test
    public void shouldUpdateScopeAccess() {
        ScopeAccess sa = createScopeAccessObject(client.getClientId(), client.getCustomerId());
        repo.addScopeAccess(client.getUniqueId(), sa);
        sa.setAccessToken("NewAccessToken");
        sa.setRefreshToken("NewRefreshToken");
        repo.updateScopeAccess(sa);
        ScopeAccess returnedByAccessToken = repo.getScopeAccessByAccessToken("NewAccessToken");
        ScopeAccess returnedByRefreshToken = repo.getScopeAccessByRefreshToken("NewRefreshToken");
        Assert.assertNotNull(returnedByRefreshToken);
        Assert.assertNotNull(returnedByAccessToken);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void shouldFailUpdateForNullScopeAccess() {
        repo.updateScopeAccess(null);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void shouldFailUpdateForBlankUniqueIdScopeAccess() {
        ScopeAccess sa = createScopeAccessObject(client.getClientId(), client.getCustomerId());
        repo.updateScopeAccess(sa);
    }

    private static LdapClientRepository getClientRepo(
        LdapConnectionPools connPools) {
        Configuration appConfig = new PropertyFileConfiguration()
            .getConfigFromClasspath();
        return new LdapClientRepository(connPools, appConfig);
    }

    private static LdapScopeAccessRepository getSaRepo(
        LdapConnectionPools connPools) {
        Configuration appConfig = new PropertyFileConfiguration()
            .getConfigFromClasspath();
        return new LdapScopeAccessRepository(connPools, appConfig);
    }

    private static LdapCustomerRepository getCustomerRepo(
        LdapConnectionPools connPools) {
        Configuration appConfig = new PropertyFileConfiguration()
            .getConfigFromClasspath();
        return new LdapCustomerRepository(connPools, appConfig);
    }

    private static LdapConnectionPools getConnPools() {
        return new LdapConfiguration(
            new PropertyFileConfiguration().getConfigFromClasspath())
            .connectionPools();
    }
    
    private Customer addNewTestCustomer(String customerId, String name,
        String inum, String iname, CustomerStatus status, String seeAlso,
        String owner, String country) {

        Customer newCustomer = createTestCustomerInstance(customerId, inum,
            iname, status, seeAlso, owner);
        newCustomer.setSoftDeleted(softDeleted);
        customerRepo.addCustomer(newCustomer);
        return newCustomer;
    }

    private Customer createTestCustomerInstance(String customerId, String inum,
        String iname, CustomerStatus status, String seeAlso, String owner) {

        Customer newCustomer = new Customer(customerId, inum, iname, status,
            seeAlso, owner);
        newCustomer.setSoftDeleted(softDeleted);
        return newCustomer;
    }
    
    private Client addNewTestClient(Customer customer) {
        Client newClient = createTestClientInstance();
        clientRepo.addClient(newClient, customer.getUniqueId());
        return newClient;
    }

    private Client createTestClientInstance() {
        Client newClient = new Client("DELETE_My_ClientId", ClientSecret
            .newInstance("DELETE_My_Client_Secret"), "DELETE_My_Name", "inum",
            "iname", "RCN-123-456-789", ClientStatus.ACTIVE);
        newClient.setLocked(false);
        newClient.setSoftDeleted(false);
        return newClient;
    }

    private Permission createPermissionInstance(String clientId, String RCN, String permissionId) {
        Permission res = new Permission();
        res.setClientId(clientId);
        res.setCustomerId(RCN);
        res.setPermissionId(permissionId);
        return res;
    }
    
    private ScopeAccess createScopeAccessObject(String clientId, String RCN) {
        ScopeAccess sa = new ScopeAccess();
        sa.setAccessToken(accessToken);
        sa.setRefreshToken(refreshToken);
        sa.setClientId(clientId);
        sa.setClientRCN(RCN);
        sa.setAccessTokenExpiration(new DateTime());
        sa.setRefreshTokenExpiration(new DateTime());
        return sa;
    }
}
