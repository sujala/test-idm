package com.rackspace.idm.domain.dao.impl;

import static org.junit.Assert.fail;

import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.configuration.Configuration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.dao.ScopeAccessObjectDao;
import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientScopeAccessObject;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ClientStatus;
import com.rackspace.idm.domain.entity.Customer;
import com.rackspace.idm.domain.entity.CustomerStatus;
import com.rackspace.idm.domain.entity.PasswordResetScopeAccessObject;
import com.rackspace.idm.domain.entity.PermissionObject;
import com.rackspace.idm.domain.entity.ScopeAccessObject;
import com.rackspace.idm.domain.entity.UserScopeAccessObject;

public class LdapScopeAccessRepositoryPersistenceTest {
    private LdapCustomerRepository customerRepo;
    private ScopeAccessObjectDao   repo;
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
    String                         RCN          = "RACKSPACE";

    String                         permissionId = "PermissionName";

    String                         accessToken  = "YYYYYYY";
    String                         refreshToken = "ZZZZZZZ";

    Client                         client       = null;
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
        client = addNewTestClient(customer);
    }

    @After
    public void tearDown() throws Exception {
        customerRepo.deleteCustomer(customer.getCustomerId());
        connPools.close();
    }

    @Test
    public void testAddScopeAccess() {
        ScopeAccessObject sa = new ScopeAccessObject();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());

        repo.addScopeAccess(client.getUniqueId(), sa);

        sa = repo.getScopeAccessForParentByClientId(client.getUniqueId(), client.getClientId());

        Assert.assertEquals(sa.getClientId(), client.getClientId());
    }

    @Test
    public void testAddUserScopeAccess() {
        final UserScopeAccessObject userScopeAccess = new UserScopeAccessObject();
        userScopeAccess.setClientId(client.getClientId());
        userScopeAccess.setClientRCN(client.getName());
        userScopeAccess.setAccessTokenString(accessToken);
        userScopeAccess.setAccessTokenExp(new Date());
        userScopeAccess.setUsername("username");
        userScopeAccess.setUserRCN("user RCN");
        userScopeAccess.setRefreshTokenString(refreshToken);
        userScopeAccess.setRefreshTokenExp(new Date());

        repo.addScopeAccess(client.getUniqueId(), userScopeAccess);

        final ScopeAccessObject scopeAccessObject = repo.getScopeAccessForParentByClientId(client.getUniqueId(),
                client.getClientId());

        Assert.assertTrue(scopeAccessObject instanceof UserScopeAccessObject);
        Assert.assertEquals(scopeAccessObject.getClientId(), client.getClientId());
    }

    @Test
    public void testAddPermissionToScopeAccess() {
        UserScopeAccessObject sa = new UserScopeAccessObject();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccessObject) repo.addScopeAccess(client.getUniqueId(), sa);

        final PermissionObject p = createPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        final Boolean result = repo.addPermissionToScopeAccess(sa.getUniqueId(), p);
        Assert.assertTrue(result);

        final Boolean doesAccessTokenHavePermission = repo.doesAccessTokenHavePermission(accessToken, p);
        Assert.assertTrue(doesAccessTokenHavePermission);
    }

    @Test
    public void testDeleteScopeAccess() {
        ScopeAccessObject sa = new ScopeAccessObject();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());

        repo.addScopeAccess(client.getUniqueId(), sa);

        sa = repo.getScopeAccessForParentByClientId(client.getUniqueId(), client.getClientId());

        Assert.assertEquals(sa.getClientId(), client.getClientId());

        repo.deleteScopeAccess(sa);

        sa = repo.getScopeAccessForParentByClientId(client.getUniqueId(), client.getClientId());

        Assert.assertNull(sa);
    }

    @Test
    public void testDoesAccessTokenHavePermission() {
        ClientScopeAccessObject sa = new ClientScopeAccessObject();
        sa.setTokenScope("testScope");
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());

        sa = (ClientScopeAccessObject) repo.addScopeAccess(client.getUniqueId(), sa);

        final PermissionObject p = createPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        final Boolean result = repo.addPermissionToScopeAccess(sa.getUniqueId(), p);
        Assert.assertTrue(result);

        final Boolean doesAccessTokenHavePermission = repo.doesAccessTokenHavePermission(accessToken, p);
        Assert.assertTrue(doesAccessTokenHavePermission);
    }

    @Test
    public void testGetScopeAccessesByParent() {
        UserScopeAccessObject sa = new UserScopeAccessObject();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccessObject) repo.addScopeAccess(client.getUniqueId(), sa);

        final List<ScopeAccessObject> scopeAccessesByParent = repo.getScopeAccessesByParent(client.getUniqueId());

        for (final ScopeAccessObject scopeAccess : scopeAccessesByParent) {
            if(scopeAccess instanceof UserScopeAccessObject) {
                Assert.assertEquals(((UserScopeAccessObject)scopeAccess).getAccessTokenString(), accessToken);
            } else {
                fail("wrong scope access");
            }
        }
    }

    @Test
    public void testGetScopeAccessByAccessToken() {
        PasswordResetScopeAccessObject sa = new PasswordResetScopeAccessObject();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");

        sa = (PasswordResetScopeAccessObject) repo.addScopeAccess(client.getUniqueId(), sa);

        Assert.assertNotNull(sa.getUniqueId());

        final ScopeAccessObject scopeAccessByAccessToken = repo.getScopeAccessByAccessToken(accessToken);

        Assert.assertNotNull(scopeAccessByAccessToken);
        Assert.assertTrue(scopeAccessByAccessToken instanceof PasswordResetScopeAccessObject);
    }

    @Test
    public void testGetScopeAccessByRefreshToken() {
        UserScopeAccessObject sa = new UserScopeAccessObject();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccessObject) repo.addScopeAccess(client.getUniqueId(), sa);

        final ScopeAccessObject scopeAccessByRefreshToken = repo.getScopeAccessByRefreshToken(refreshToken);

        Assert.assertNotNull(scopeAccessByRefreshToken);
        Assert.assertTrue(scopeAccessByRefreshToken instanceof UserScopeAccessObject);
        Assert.assertEquals(((UserScopeAccessObject)scopeAccessByRefreshToken).getAccessTokenString(), accessToken);
    }

    @Test
    public void testGetScopeAccessForParentByClientId() {
        ScopeAccessObject sa = new ScopeAccessObject();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());

        repo.addScopeAccess(client.getUniqueId(), sa);

        sa = repo.getScopeAccessForParentByClientId(client.getUniqueId(), client.getClientId());

        Assert.assertEquals(sa.getClientId(), client.getClientId());
    }

    @Test
    public void testGetScopeAccessByUsernameAndClientId() {
        UserScopeAccessObject sa = new UserScopeAccessObject();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccessObject) repo.addScopeAccess(client.getUniqueId(), sa);

        final ScopeAccessObject scopeAccess = repo.getScopeAccessByUsernameAndClientId("username", client.getClientId());

        Assert.assertNotNull(scopeAccess);
    }

    @Test
    public void testRemovePermissionFromScopeAccess() {
        UserScopeAccessObject sa = new UserScopeAccessObject();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccessObject) repo.addScopeAccess(client.getUniqueId(), sa);

        final PermissionObject p = createPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        Boolean result = repo.addPermissionToScopeAccess(sa.getUniqueId(), p);
        Assert.assertTrue(result);

        final Boolean doesAccessTokenHavePermission = repo.doesAccessTokenHavePermission(accessToken, p);
        Assert.assertTrue(doesAccessTokenHavePermission);

        PermissionObject po = repo.getPermissionByParentAndPermissionId(sa.getUniqueId(), permissionId);
        Assert.assertNotNull(po);

        result = repo.removePermissionFromScopeAccess(po);
        Assert.assertNotNull(result);

        po = repo.getPermissionByParentAndPermissionId(sa.getUniqueId(), permissionId);
        Assert.assertNull(po);
    }

    @Test
    public void testUpdateScopeAccess() {
        final Date date = new Date();
        UserScopeAccessObject sa = new UserScopeAccessObject();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(date);
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(date);

        sa = (UserScopeAccessObject) repo.addScopeAccess(client.getUniqueId(), sa);

        final Date newDate = new Date();
        sa.setAccessTokenExp(newDate);
        repo.updateScopeAccess(sa);

        final ScopeAccessObject scopeAccess = repo.getScopeAccessByAccessToken(accessToken);

        Assert.assertTrue(scopeAccess instanceof UserScopeAccessObject);
        Assert.assertEquals(((UserScopeAccessObject)scopeAccess).getAccessTokenExp(), newDate);
    }

    @Test
    public void testUpdatePermissionForScopeAccess() {
        UserScopeAccessObject sa = new UserScopeAccessObject();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setUsername("username");
        sa.setUserRCN("user RCN");
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccessObject) repo.addScopeAccess(client.getUniqueId(), sa);

        final PermissionObject p = createPermissionInstance(client.getClientId(), client.getCustomerId(), permissionId);
        Boolean result = repo.addPermissionToScopeAccess(sa.getUniqueId(), p);
        Assert.assertTrue(result);

        final Boolean doesAccessTokenHavePermission = repo.doesAccessTokenHavePermission(accessToken, p);
        Assert.assertTrue(doesAccessTokenHavePermission);

        PermissionObject po = repo.getPermissionByParentAndPermissionId(sa.getUniqueId(), permissionId);
        Assert.assertNotNull(po);

        po.setEnabled(false);
        result = repo.updatePermissionForScopeAccess(po);
        Assert.assertTrue(result);

        po = repo.getPermissionByParentAndPermissionId(sa.getUniqueId(), permissionId);
        Assert.assertNotNull(po);
        Assert.assertFalse(po.getEnabled());
    }

    private static LdapClientRepository getClientRepo(LdapConnectionPools connPools) {
        final Configuration appConfig = new PropertyFileConfiguration().getConfigFromClasspath();
        return new LdapClientRepository(connPools, appConfig);
    }

    private static ScopeAccessObjectDao getSaRepo(LdapConnectionPools connPools) {
        final Configuration appConfig = new PropertyFileConfiguration().getConfigFromClasspath();
        return new LdapScopeAccessPeristenceRepository(connPools, appConfig);
    }

    private static LdapCustomerRepository getCustomerRepo(LdapConnectionPools connPools) {
        final Configuration appConfig = new PropertyFileConfiguration().getConfigFromClasspath();
        return new LdapCustomerRepository(connPools, appConfig);
    }

    private static LdapConnectionPools getConnPools() {
        return new LdapConfiguration(new PropertyFileConfiguration().getConfigFromClasspath()).connectionPools();
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

    private Client addNewTestClient(Customer customer) {
        final Client newClient = createTestClientInstance();
        clientRepo.addClient(newClient, customer.getUniqueId());
        return newClient;
    }

    private Client createTestClientInstance() {
        final Client newClient = new Client("DELETE_My_ClientId", ClientSecret.newInstance("DELETE_My_Client_Secret"),
                "DELETE_My_Name", "inum", "iname", "RCN-123-456-789", ClientStatus.ACTIVE);
        newClient.setLocked(false);
        newClient.setSoftDeleted(false);
        return newClient;
    }

    private PermissionObject createPermissionInstance(String clientId, String RCN, String permissionId) {
        final PermissionObject res = new PermissionObject(RCN,clientId,permissionId,null);
        return res;
    }

}
