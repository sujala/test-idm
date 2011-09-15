package com.rackspace.idm.domain.dao.impl;

import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientScopeAccess;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ClientStatus;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.CustomerStatus;
import com.rackspace.idm.domain.entity.DefinedPermission;
import com.rackspace.idm.domain.entity.DelegatedClientScopeAccess;
import com.rackspace.idm.domain.entity.GrantedPermission;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccess;
import com.rackspace.idm.domain.entity.Permission;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.entity.UserScopeAccess;

public class LdapScopeAccessPersistenceRepositoryTest {
    private LdapCustomerRepository customerRepo;
    private ScopeAccessDao   repo;
    private LdapClientRepository   clientRepo;
    private LdapConnectionPools    connPools;

    static String                  customerId   = "DELETE_My_CustomerId";
    String                         customerName = "DELETE_My_Name";
    String                         inum         = "@!FFFF.FFFF.FFFF.FFFF!CCCC.CCCC";
    String                         iname        = "@Rackspae.TESTING";
    CustomerStatus                 status       = CustomerStatus.ACTIVE;
    String                         seeAlso      = "inum=@!FFFF.FFFF.FFFF.FFFF!CCCC.CCCC";
    String                         owner        = "inum=@!FFFF.FFFF.FFFF.FFFF!CCCC.CCCC";
    String                         country      = "USA";
    boolean                        softDeleted  = false;

    String                         clientId     = "XXX";
    String                         clientId2    = "YYY";
    String                         RCN          = "RACKSPACE";

    String                         permissionId = "PermissionName";

    String                         accessToken  = "YYYYYYY";
    String                         refreshToken = "ZZZZZZZ";

    Client                         client       = null;
    Client                         client2      = null;
    Customer                       customer     = null;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final LdapConnectionPools pools = getConnPools();
        final LdapClientRepository cleanUpRepo = getClientRepo(pools);
        final Client deleteme = cleanUpRepo.getClientByClientId("XXX");
        if (deleteme != null) {
            cleanUpRepo.deleteClient(deleteme);
        }
        pools.close();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Before
    public void setUp() throws Exception {
        connPools = getConnPools();
        repo = getSaRepo(connPools);
        customerRepo = getCustomerRepo(connPools);
        clientRepo = getClientRepo(connPools);

        customer = addNewTestCustomer(customerId, customerName, inum, iname, status, seeAlso, owner, country);
        client = addNewTestClient(clientId);
        client2 = addNewTestClient(clientId2);
    }

    @After
    public void tearDown() throws Exception {
        customerRepo.deleteCustomer(customer.getCustomerId());
        clientRepo.deleteClient(client);
        clientRepo.deleteClient(client2);
        connPools.close();
    }

    @Test
    public void testAddDuplicateScopeAccess() {
        ScopeAccess sa = new ScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getCustomerId());

        sa = repo.addDirectScopeAccess(client.getUniqueId(), sa);
        sa = repo.addDirectScopeAccess(client.getUniqueId(), sa);
        sa = repo.getDirectScopeAccessForParentByClientId(client.getUniqueId(), client.getClientId());

        Assert.assertEquals(sa.getClientId(), client.getClientId());
    }
    
    @Test
    public void testAddScopeAccess() {
        ScopeAccess sa = new ScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());

        repo.addDirectScopeAccess(client.getUniqueId(), sa);

        sa = repo.getDirectScopeAccessForParentByClientId(client.getUniqueId(), client.getClientId());

        Assert.assertEquals(sa.getClientId(), client.getClientId());
    }

    @Test
    public void testAddUserScopeAccess() {
        final UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setClientId(client.getClientId());
        userScopeAccess.setClientRCN(client.getName());
        userScopeAccess.setAccessTokenString(accessToken);
        userScopeAccess.setAccessTokenExp(new Date());
        userScopeAccess.setUsername("username");
        userScopeAccess.setUserRCN("user RCN");
        userScopeAccess.setRefreshTokenString(refreshToken);
        userScopeAccess.setRefreshTokenExp(new Date());

        repo.addDirectScopeAccess(client.getUniqueId(), userScopeAccess);

        final ScopeAccess scopeAccessObject = repo.getDirectScopeAccessForParentByClientId(client.getUniqueId(),
                client.getClientId());

        Assert.assertTrue(scopeAccessObject instanceof UserScopeAccess);
        Assert.assertEquals(scopeAccessObject.getClientId(), client.getClientId());
    }

    @Test
    public void testAddClientScopeAccess() {
        final ClientScopeAccess clientScopeAccess = new ClientScopeAccess();
        clientScopeAccess.setClientId(client.getClientId());
        clientScopeAccess.setClientRCN(client.getName());
        clientScopeAccess.setAccessTokenString(accessToken);
        repo.addDirectScopeAccess(client.getUniqueId(), clientScopeAccess);

        final ScopeAccess scopeAccessObject = repo.getDirectScopeAccessForParentByClientId(client.getUniqueId(),
                client.getClientId());

        Assert.assertTrue(scopeAccessObject instanceof ClientScopeAccess);
        Assert.assertEquals(scopeAccessObject.getClientId(), client.getClientId());
    }

    @Test
    public void testAddPasswordResetScopeAccess() {
        final PasswordResetScopeAccess sa = new PasswordResetScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        repo.addDirectScopeAccess(client.getUniqueId(), sa);

        final ScopeAccess scopeAccessObject = repo.getDirectScopeAccessForParentByClientId(client.getUniqueId(),
                client.getClientId());

        Assert.assertTrue(scopeAccessObject instanceof PasswordResetScopeAccess);
        Assert.assertEquals(scopeAccessObject.getClientId(), client.getClientId());
    }

    @Test
    public void testDefinePermissionToScopeAccess() {
        UserScopeAccess sa = new UserScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        DefinedPermission p = createDefinedPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        p.setDescription("description");
        p.setTitle("title");
        p.setEnabled(false);
        p.setGrantedByDefault(true);

        p = repo.definePermission(sa.getUniqueId(), p);
        Assert.assertNotNull(p);
        Assert.assertTrue(p.getGrantedByDefault());
        Assert.assertEquals("title", p.getTitle());
        Assert.assertEquals("description",p.getDescription());

        final Boolean doesAccessTokenHavePermission = repo.doesAccessTokenHavePermission(sa, p);
        Assert.assertTrue(doesAccessTokenHavePermission);
    }
    
    @Test
    public void testDefineDuplicatePermissionToScopeAccess() {
        UserScopeAccess sa = new UserScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        DefinedPermission p = createDefinedPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        p.setDescription("description");
        p.setTitle("title");
        p.setEnabled(false);
        p.setGrantedByDefault(true);

        p = repo.definePermission(sa.getUniqueId(), p);
        p = repo.definePermission(sa.getUniqueId(), p);
        Assert.assertNotNull(p);
        Assert.assertTrue(p.getGrantedByDefault());
        Assert.assertEquals("title", p.getTitle());
        Assert.assertEquals("description",p.getDescription());

        final Boolean doesAccessTokenHavePermission = repo.doesAccessTokenHavePermission(sa, p);
        Assert.assertTrue(doesAccessTokenHavePermission);
    }

    @Test
    public void testGrantPermissionToScopeAccess() {
        UserScopeAccess sa = new UserScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        GrantedPermission p = createGrantedPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        p.setResourceGroups(new String[] { "test" });
        p = repo.grantPermission(sa.getUniqueId(), p);
        Assert.assertNotNull(p);
        Assert.assertNotNull(p.getResourceGroups());

        final Boolean doesAccessTokenHavePermission = repo.doesAccessTokenHavePermission(sa, p);
        Assert.assertTrue(doesAccessTokenHavePermission);
    }
    
    @Test
    public void testGrantDuplicatePermissionToScopeAccess() {
        UserScopeAccess sa = new UserScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        GrantedPermission p = createGrantedPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        p.setResourceGroups(new String[] { "test" });
        p = repo.grantPermission(sa.getUniqueId(), p);
        p = repo.grantPermission(sa.getUniqueId(), p);
        Assert.assertNotNull(p);
        Assert.assertNotNull(p.getResourceGroups());

        final Boolean doesAccessTokenHavePermission = repo.doesAccessTokenHavePermission(sa, p);
        Assert.assertTrue(doesAccessTokenHavePermission);
    }

    @Test
    public void testDeleteScopeAccess() {
        ScopeAccess sa = new ScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());

        repo.addDirectScopeAccess(client.getUniqueId(), sa);

        sa = repo.getDirectScopeAccessForParentByClientId(client.getUniqueId(), client.getClientId());

        Assert.assertEquals(sa.getClientId(), client.getClientId());

        repo.deleteScopeAccess(sa);

        sa = repo.getDirectScopeAccessForParentByClientId(client.getUniqueId(), client.getClientId());

        Assert.assertNull(sa);
    }

    @Test
    public void testDoesAccessTokenHavePermission() {
        ClientScopeAccess sa = new ClientScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());

        sa = (ClientScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        GrantedPermission p = createGrantedPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        p = repo.grantPermission(sa.getUniqueId(), p);
        Assert.assertNotNull(p);

        final Boolean doesAccessTokenHavePermission = repo.doesAccessTokenHavePermission(sa, p);
        Assert.assertTrue(doesAccessTokenHavePermission);
    }

    @Test
    public void testGetScopeAccessesByParent() {
        UserScopeAccess sa = new UserScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        final List<ScopeAccess> scopeAccessesByParent = repo.getScopeAccessesByParent(client.getUniqueId());

        for (final ScopeAccess scopeAccess : scopeAccessesByParent) {
            if(scopeAccess instanceof UserScopeAccess) {
                Assert.assertEquals(((UserScopeAccess)scopeAccess).getAccessTokenString(), accessToken);
            } else {
                fail("wrong scope access");
            }
        }
    }

    @Test
    public void testGetScopeAccessByAccessToken() {
        PasswordResetScopeAccess sa = new PasswordResetScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");

        sa = (PasswordResetScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        Assert.assertNotNull(sa.getUniqueId());

        final ScopeAccess scopeAccessByAccessToken = repo.getScopeAccessByAccessToken(accessToken);

        Assert.assertNotNull(scopeAccessByAccessToken);
        Assert.assertTrue(scopeAccessByAccessToken instanceof PasswordResetScopeAccess);
    }

    @Test
    public void testGetScopeAccessByRefreshToken() {
        UserScopeAccess sa = new UserScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        final ScopeAccess scopeAccessByRefreshToken = repo.getScopeAccessByRefreshToken(refreshToken);

        Assert.assertNotNull(scopeAccessByRefreshToken);
        Assert.assertTrue(scopeAccessByRefreshToken instanceof UserScopeAccess);
        Assert.assertEquals(((UserScopeAccess)scopeAccessByRefreshToken).getAccessTokenString(), accessToken);
    }

    @Test
    public void testGetScopeAccessForParentByClientId() {
        ScopeAccess sa = new ScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());

        repo.addDirectScopeAccess(client.getUniqueId(), sa);

        sa = repo.getDirectScopeAccessForParentByClientId(client.getUniqueId(), client.getClientId());

        Assert.assertEquals(sa.getClientId(), client.getClientId());
    }

    @Test
    public void testGetScopeAccessByUsernameAndClientId() {
        UserScopeAccess sa = new UserScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        final ScopeAccess scopeAccess = repo.getScopeAccessByUsernameAndClientId("username", client.getClientId());

        Assert.assertNotNull(scopeAccess);
    }
    
    @Test
    public void testGetScopeAccessByUsername() {
        DelegatedClientScopeAccess sa1 = new DelegatedClientScopeAccess();
        sa1.setClientId(client.getClientId());
        sa1.setClientRCN(client.getName());
        sa1.setAccessTokenString(accessToken);
        sa1.setAccessTokenExp(new Date());
        sa1.setUsername("username");
        sa1.setUserRCN("user RCN");
        sa1.setRefreshTokenString(refreshToken);
        sa1.setRefreshTokenExp(new Date());

        sa1 = (DelegatedClientScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa1);
        
        DelegatedClientScopeAccess sa2 = new DelegatedClientScopeAccess();
        sa2.setClientId(client2.getClientId());
        sa2.setClientRCN(client2.getName());
        sa2.setAccessTokenString(accessToken);
        sa2.setAccessTokenExp(new Date());
        sa2.setUsername("username");
        sa2.setUserRCN("user RCN");
        sa2.setRefreshTokenString(refreshToken);
        sa2.setRefreshTokenExp(new Date());
        
        sa2 = (DelegatedClientScopeAccess) repo.addDirectScopeAccess(client2.getUniqueId(), sa2);
       
        final List<DelegatedClientScopeAccess> scopeAccessList = repo.getDelegatedClientScopeAccessByUsername("username");  
        
        Assert.assertNotNull(scopeAccessList);
        Assert.assertEquals(2, scopeAccessList.size());
        
    }   

    @Test
    public void testGetPermissions() {
        UserScopeAccess sa = new UserScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        DefinedPermission p = createDefinedPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        p.setDescription("description");
        p.setTitle("title");
        p.setEnabled(false);
        p.setGrantedByDefault(true);

        p = repo.definePermission(sa.getUniqueId(), p);
        Assert.assertNotNull(p);
        Assert.assertTrue(p.getGrantedByDefault());
        Assert.assertEquals("title", p.getTitle());
        Assert.assertEquals("description",p.getDescription());
        
        Permission filter = new Permission(p.getCustomerId(), p.getClientId(), p.getPermissionId());

        List<Permission> list = repo.getPermissionsByPermission(filter);
        
        Assert.assertTrue(list.size() >= 1);
    }
    @Test
    public void testRemovePermissionFromScopeAccess() {
        UserScopeAccess sa = new UserScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        GrantedPermission p = createGrantedPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        p = repo.grantPermission(sa.getUniqueId(), p);
        Assert.assertNotNull(p);

        final Boolean doesAccessTokenHavePermission = repo.doesAccessTokenHavePermission(sa, p);
        Assert.assertTrue(doesAccessTokenHavePermission);

        Permission po = repo.getPermissionByParentAndPermission(sa.getUniqueId(), p);
        Assert.assertNotNull(po);

        final Boolean result = repo.removePermissionFromScopeAccess(po);
        Assert.assertNotNull(result);

        po = repo.getPermissionByParentAndPermission(sa.getUniqueId(), p);
        Assert.assertNull(po);
    }

    @Test
    public void testUpdateScopeAccess() {
        final Date date = new Date();
        UserScopeAccess sa = new UserScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(date);
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(date);

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        final Date newDate = new Date();
        sa.setAccessTokenExp(newDate);
        repo.updateScopeAccess(sa);

        final ScopeAccess scopeAccess = repo.getScopeAccessByAccessToken(accessToken);

        Assert.assertTrue(scopeAccess instanceof UserScopeAccess);
        Assert.assertEquals(((UserScopeAccess)scopeAccess).getAccessTokenExp(), newDate);
    }

    @Test
    public void testUpdatePermissionForScopeAccess() {
        UserScopeAccess sa = new UserScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        GrantedPermission p = createGrantedPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        p = repo.grantPermission(sa.getUniqueId(), p);
        Assert.assertNotNull(p);

        final Boolean doesAccessTokenHavePermission = repo.doesAccessTokenHavePermission(sa, p);
        Assert.assertTrue(doesAccessTokenHavePermission);

        GrantedPermission po = (GrantedPermission)repo.getPermissionByParentAndPermission(sa.getUniqueId(), p);
        Assert.assertNotNull(po);

        po.setResourceGroups(new String[] { "test" });
        final Boolean result = repo.updatePermissionForScopeAccess(po);
        Assert.assertTrue(result);

        po = (GrantedPermission) repo.getPermissionByParentAndPermission(sa.getUniqueId(), p);
        Assert.assertNotNull(po);
        Assert.assertNotNull(po.getResourceGroups());
    }

    private static LdapClientRepository getClientRepo(LdapConnectionPools connPools) {
        Configuration appConfig = null;
        try {
            appConfig = new PropertiesConfiguration("config.properties");

        } catch (ConfigurationException e) {
            System.out.println(e);
        }
        return new LdapClientRepository(connPools, appConfig);
    }

    private static ScopeAccessDao getSaRepo(LdapConnectionPools connPools) {
        Configuration appConfig = null;
        try {
            appConfig = new PropertiesConfiguration("config.properties");

        } catch (ConfigurationException e) {
            System.out.println(e);
        }
        return new LdapScopeAccessPeristenceRepository(connPools, appConfig);
    }

    private static LdapCustomerRepository getCustomerRepo(LdapConnectionPools connPools) {
        Configuration appConfig = null;
        try {
            appConfig = new PropertiesConfiguration("config.properties");

        } catch (ConfigurationException e) {
            System.out.println(e);
        }
        return new LdapCustomerRepository(connPools, appConfig);
    }

    private static LdapConnectionPools getConnPools() {
        Configuration appConfig = null;
        try {
            appConfig = new PropertiesConfiguration("config.properties");

        } catch (ConfigurationException e) {
            System.out.println(e);
        }
        return new LdapConfiguration(appConfig).connectionPools();
    }

    private Customer addNewTestCustomer(String customerId, String name, String inum, String iname,
            CustomerStatus status, String seeAlso, String owner, String country) {

        final Customer newCustomer = createTestCustomerInstance(customerId, inum, iname, status, seeAlso, owner);
        newCustomer.setSoftDeleted(softDeleted);
        customerRepo.addCustomer(newCustomer);
        return newCustomer;
    }

    private Customer createTestCustomerInstance(String customerId, String inum, String iname, CustomerStatus status,
            String seeAlso, String owner) {

        final Customer newCustomer = new Customer(customerId, inum, iname, status, seeAlso, owner);
        newCustomer.setSoftDeleted(softDeleted);
        return newCustomer;
    }

    private Client addNewTestClient(String clientId) {
        final Client newClient = createTestClientInstance();
        newClient.setClientId(clientId);
        newClient.setInum(clientId);
        clientRepo.addClient(newClient);
        return newClient;
    }

    private Client createTestClientInstance() {
        final Client newClient = new Client("DELETE_My_ClientId", ClientSecret.newInstance("DELETE_My_Client_Secret"),
                "DELETE_My_Name", "inum", "iname", "RCN-123-456-789", ClientStatus.ACTIVE);
        newClient.setLocked(false);
        newClient.setSoftDeleted(false);
        return newClient;
    }

    private DefinedPermission createDefinedPermissionInstance(String clientId, String RCN, String permissionId) {
        final DefinedPermission res = new DefinedPermission(RCN,clientId,permissionId);
        return res;
    }
    
    private GrantedPermission createGrantedPermissionInstance(String clientId, String RCN, String permissionId) {
        final GrantedPermission res = new GrantedPermission(RCN,clientId,permissionId);
        return res;
    }
}
