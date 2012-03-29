package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.entity.*;
import junit.framework.Assert;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.junit.*;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.fail;

public class LdapScopeAccessPersistenceRepositoryTest {
    private LdapCustomerRepository customerRepo;
    private ScopeAccessDao repo;
    private LdapApplicationRepository clientRepo;
    private LdapConnectionPools connPools;

    static String customerId = "DELETE_My_CustomerId";
    String customerName = "DELETE_My_Name";
    String inum = "@!FFFF.FFFF.FFFF.FFFF!CCCC.CCCC";
    String iname = "@Rackspae.TESTING";
    String seeAlso = "inum=@!FFFF.FFFF.FFFF.FFFF!CCCC.CCCC";
    String owner = "inum=@!FFFF.FFFF.FFFF.FFFF!CCCC.CCCC";
    String country = "USA";
    boolean softDeleted = false;

    String clientId = "XXX";
    String clientId2 = "YYY";
    String RCN = "RACKSPACE";

    String permissionId = "PermissionName";

    String accessToken = "YYYYYYY";
    String refreshToken = "ZZZZZZZ";
    
    String userId = "userId";

    Application client = null;
    Application client2 = null;
    Customer customer = null;
    
    String id = "XXXX";
    String id2 = "YYYY";


    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final LdapConnectionPools pools = getConnPools();
        final LdapApplicationRepository cleanUpRepo = getClientRepo(pools);
        final Application deleteme = cleanUpRepo.getClientByClientId("XXX");
        final Application deleteme2 = cleanUpRepo.getClientByClientId("YYY");
        if (deleteme != null) {
            cleanUpRepo.deleteClient(deleteme);
        }
        if (deleteme2 != null) {
            cleanUpRepo.deleteClient(deleteme2);
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

        try {
            customer = addNewTestCustomer(customerId, customerName, inum, iname, seeAlso, owner, country);
            client = addNewTestClient(clientId);
            client2 = addNewTestClient2(clientId2);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @After
    public void tearDown() throws Exception {
        customerRepo.deleteCustomer(customer.getRCN());
        clientRepo.deleteClient(client);
        clientRepo.deleteClient(client2);
        connPools.close();
    }

    @Test
    public void testAddDuplicateScopeAccess() {
        ScopeAccess sa = new ScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getRCN());

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
        userScopeAccess.setUserRsId(userId);
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
        sa.setUserRsId(userId);
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
        sa.setUserRsId(userId);
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        DefinedPermission p = createDefinedPermissionInstance(client.getClientId(), client.getRCN(), permissionId);
        p.setDescription("description");
        p.setTitle("title");
        p.setEnabled(false);
        p.setGrantedByDefault(true);

        p = repo.definePermission(sa.getUniqueId(), p);
        Assert.assertNotNull(p);
        Assert.assertTrue(p.getGrantedByDefault());
        Assert.assertEquals("title", p.getTitle());
        Assert.assertEquals("description", p.getDescription());

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
        sa.setUserRsId(userId);
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        DefinedPermission p = createDefinedPermissionInstance(client.getClientId(), client.getRCN(), permissionId);
        p.setDescription("description");
        p.setTitle("title");
        p.setEnabled(false);
        p.setGrantedByDefault(true);

        p = repo.definePermission(sa.getUniqueId(), p);
        p = repo.definePermission(sa.getUniqueId(), p);
        Assert.assertNotNull(p);
        Assert.assertTrue(p.getGrantedByDefault());
        Assert.assertEquals("title", p.getTitle());
        Assert.assertEquals("description", p.getDescription());

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
        sa.setUserRsId(userId);
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        GrantedPermission p = createGrantedPermissionInstance(client.getClientId(), client.getRCN(), permissionId);
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
        sa.setUserRsId(userId);
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        GrantedPermission p = createGrantedPermissionInstance(client.getClientId(), client.getRCN(), permissionId);
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

        GrantedPermission p = createGrantedPermissionInstance(client.getClientId(), client.getRCN(), permissionId);
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
        sa.setUserRsId(userId);
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        final List<ScopeAccess> scopeAccessesByParent = repo.getScopeAccessesByParent(client.getUniqueId());

        for (final ScopeAccess scopeAccess : scopeAccessesByParent) {
            if (scopeAccess instanceof UserScopeAccess) {
                Assert.assertEquals(((UserScopeAccess) scopeAccess).getAccessTokenString(), accessToken);
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
        sa.setUserRsId(userId);

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
        sa.setUserRsId(userId);
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        final ScopeAccess scopeAccessByRefreshToken = repo.getScopeAccessByRefreshToken(refreshToken);

        Assert.assertNotNull(scopeAccessByRefreshToken);
        Assert.assertTrue(scopeAccessByRefreshToken instanceof UserScopeAccess);
        Assert.assertEquals(((UserScopeAccess) scopeAccessByRefreshToken).getAccessTokenString(), accessToken);
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
        sa.setUserRsId(userId);
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
        sa1.setUserRsId(userId);
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
        sa2.setUserRsId(userId);
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
        sa.setUserRsId(userId);
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        DefinedPermission p = createDefinedPermissionInstance(client.getClientId(), client.getRCN(), permissionId);
        p.setDescription("description");
        p.setTitle("title");
        p.setEnabled(false);
        p.setGrantedByDefault(true);

        p = repo.definePermission(sa.getUniqueId(), p);
        Assert.assertNotNull(p);
        Assert.assertTrue(p.getGrantedByDefault());
        Assert.assertEquals("title", p.getTitle());
        Assert.assertEquals("description", p.getDescription());

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
        sa.setUserRsId(userId);
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        GrantedPermission p = createGrantedPermissionInstance(client.getClientId(), client.getRCN(), permissionId);
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
        sa.setUserRsId(userId);
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(date);

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        final Date newDate = new Date();
        sa.setAccessTokenExp(newDate);
        repo.updateScopeAccess(sa);

        final ScopeAccess scopeAccess = repo.getScopeAccessByAccessToken(accessToken);

        Assert.assertTrue(scopeAccess instanceof UserScopeAccess);
        Assert.assertEquals(((UserScopeAccess) scopeAccess).getAccessTokenExp(), newDate);
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
        sa.setUserRsId(userId);
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());

        sa = (UserScopeAccess) repo.addDirectScopeAccess(client.getUniqueId(), sa);

        GrantedPermission p = createGrantedPermissionInstance(client.getClientId(), client.getRCN(), permissionId);
        p = repo.grantPermission(sa.getUniqueId(), p);
        Assert.assertNotNull(p);

        final Boolean doesAccessTokenHavePermission = repo.doesAccessTokenHavePermission(sa, p);
        Assert.assertTrue(doesAccessTokenHavePermission);

        GrantedPermission po = (GrantedPermission) repo.getPermissionByParentAndPermission(sa.getUniqueId(), p);
        Assert.assertNotNull(po);

        po.setResourceGroups(new String[]{"test"});
        final Boolean result = repo.updatePermissionForScopeAccess(po);
        Assert.assertTrue(result);

        po = (GrantedPermission) repo.getPermissionByParentAndPermission(sa.getUniqueId(), p);
        Assert.assertNotNull(po);
        Assert.assertNotNull(po.getResourceGroups());
    }

    @Test
    public void testScopeAccess_Success() {
        ScopeAccess sa = new ScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        ScopeAccess scopeAccess = repo.addScopeAccess(client.getUniqueId(), sa);

        Assert.assertEquals(sa.getClientId(), scopeAccess.getClientId());
    }

    @Test
    public void testAddDelegatedScopeAccess_Success() {
        DelegatedClientScopeAccess sa = new DelegatedClientScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());
        sa.setAuthCode("authcode");
        sa.setAuthCodeExp(new Date());
        sa.setUserRsId(client.getName());
        ScopeAccess scopeAccess = repo.addDelegateScopeAccess(client.getUniqueId(), sa);

        Assert.assertEquals(sa.getClientId(), scopeAccess.getClientId());
    }

    @Test(expected = IllegalStateException.class)
    public void testAddDelegatedScopeAccess_nullClientID_throwsLDapException() {
        DelegatedClientScopeAccess sa = new DelegatedClientScopeAccess();
        sa.setClientId(null);
        sa.setClientRCN(client.getName());
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        sa.setRefreshTokenString(refreshToken);
        sa.setRefreshTokenExp(new Date());
        sa.setAuthCode("authcode");
        sa.setAuthCodeExp(new Date());
        sa.setUserRsId(client.getName());
        ScopeAccess scopeAccess = repo.addDelegateScopeAccess(client.getUniqueId(), sa);
    }

    @Test
    public void testAddImpersonatedScopeAccess_Success() {
        ImpersonatedScopeAccess sa = new ImpersonatedScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setImpersonatingToken("1234567890");
        sa.setImpersonatingUsername("impersonateme");
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        ScopeAccess scopeAccess = repo.addImpersonatedScopeAccess(client.getUniqueId(), sa);

        Assert.assertEquals(sa.getClientId(), scopeAccess.getClientId());
    }

    @Test(expected = IllegalStateException.class)
    public void testAddImpersonatedScopeAccess_nullUsername_throwsLDapException() {
        ImpersonatedScopeAccess sa = new ImpersonatedScopeAccess();
        sa.setClientId(client.getClientId());
        sa.setImpersonatingToken("1234567890");
        sa.setImpersonatingUsername(null);
        sa.setAccessTokenString(accessToken);
        sa.setAccessTokenExp(new Date());
        ScopeAccess scopeAccess = repo.addImpersonatedScopeAccess(client.getUniqueId(), sa);
    }

    private static LdapApplicationRepository getClientRepo(LdapConnectionPools connPools) {
        Configuration appConfig = null;
        try {
            appConfig = new PropertiesConfiguration("config.properties");

        } catch (ConfigurationException e) {
            System.out.println(e);
        }
        return new LdapApplicationRepository(connPools, appConfig);
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
                                         String seeAlso, String owner, String country) {

        final Customer newCustomer = createTestCustomerInstance(customerId);
        newCustomer.setId(id);
        customerRepo.addCustomer(newCustomer);
        return newCustomer;
    }

    private Customer createTestCustomerInstance(String customerId) {

        final Customer newCustomer = new Customer();
        newCustomer.setRCN(customerId);
        return newCustomer;
    }

    private Application addNewTestClient(String clientId) {
        final Application newClient = createTestClientInstance();
        newClient.setClientId(clientId);
        clientRepo.addClient(newClient);
        return newClient;
    }
    
    private Application addNewTestClient2(String clientId) {
        final Application newClient = createTestClientInstance();
        newClient.setClientId(clientId2);
        clientRepo.addClient(newClient);
        return newClient;
    }

    private Application createTestClientInstance() {
        final Application newClient = new Application("DELETE_My_ClientId", ClientSecret.newInstance("DELETE_My_Client_Secret"),
                "DELETE_My_Name", "RCN-123-456-789", ClientStatus.ACTIVE);
        return newClient;
    }

    private DefinedPermission createDefinedPermissionInstance(String clientId, String RCN, String permissionId) {
        final DefinedPermission res = new DefinedPermission(RCN, clientId, permissionId);
        return res;
    }

    private GrantedPermission createGrantedPermissionInstance(String clientId, String RCN, String permissionId) {
        final GrantedPermission res = new GrantedPermission(RCN, clientId, permissionId);
        return res;
    }
}
