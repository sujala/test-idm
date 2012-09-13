package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.entity.Policy;
import junit.framework.TestCase;
import org.junit.*;

import java.util.List;

import static org.hamcrest.EasyMock2Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 9/11/12
 * Time: 10:17 AM
 * To change this template use File | Settings | File Templates.
 */
public class LdapPolicyRepositoryIntegrationTest extends InMemoryLdapIntegrationTest{

    private static LdapPolicyRepository repo;
    private static LdapConnectionPools connPools;

    private final String policyId = "XXXX";
    private final String description = "Description";
    private final boolean enabled = true;
    private final boolean global = false;
    private final String name = "XXXX";
    private final String blob = "someBlob";
    private final String type = "someType";
    private final String dn = LdapRepository.BASE_DN;



    private static LdapPolicyRepository getRepo(LdapConnectionPools connPools) {
        return new LdapPolicyRepository(connPools, new PropertyFileConfiguration().getConfig());
    }

    private static LdapConnectionPools getConnPools() {
        LdapConfiguration config = new LdapConfiguration(new PropertyFileConfiguration().getConfig());
        return config.connectionPools();
    }

    @BeforeClass
    public static void setUp() {
        connPools = getConnPools();
        repo = getRepo(connPools);
    }

    @Before
    public void preTestSetUp() throws Exception {
        //cleanup before test
        try{
            repo.deletePolicy(policyId);
        }catch (Exception e){
            System.out.println("failed to delete policy");
        }
    }

    @AfterClass
    public static void tearDown() {
        connPools.close();
    }

    @Test
    public void shouldAddGetDeletePolicy() {
        this.repo.addPolicy(getTestPolicy());
        Policy policy = repo.getPolicy(policyId);
        Assert.assertEquals("compair blob", policy.getBlob(), "someBlob");
        Assert.assertEquals("compair blob",policy.getName(),"XXXX");
        Assert.assertEquals("compair blob",policy.getPolicyType(),"someType");
        Assert.assertEquals("compair blob",policy.getDescription(),"Description");
        Assert.assertEquals("compair blob",policy.isEnabled(),true);
        Assert.assertEquals("compair blob",policy.isGlobal(),false);
        Assert.assertEquals("compair blob",policy.getPolicyId(),"XXXX");
    }

    @Test(expected = IllegalArgumentException.class)
    public void addPolicy_passingNull_throwsIllegalArgumentException() throws Exception {
        this.repo.addPolicy(null);
    }

    @Test
    public void getPolicy_nullValue() throws Exception {
        Policy policy = this.repo.getPolicy(null);
        Assert.assertEquals(policy,null);
    }

    @Test
    public void getPolicy_emptyStringValue() throws Exception {
        Policy policy = this.repo.getPolicy("");
        Assert.assertEquals(policy,null);
    }

    @Test
    public void getPolicyByName_validPolicy() throws Exception {
        this.repo.addPolicy(getTestPolicy());
        Policy policy = this.repo.getPolicyByName(name);
        Assert.assertEquals("compair blob", policy.getBlob(), "someBlob");
        Assert.assertEquals("compair blob",policy.getName(),"XXXX");
        Assert.assertEquals("compair blob",policy.getPolicyType(),"someType");
        Assert.assertEquals("compair blob",policy.getDescription(),"Description");
        Assert.assertEquals("compair blob",policy.isEnabled(),true);
        Assert.assertEquals("compair blob",policy.isGlobal(),false);
        Assert.assertEquals("compair blob",policy.getPolicyId(),"XXXX");
    }

    @Test
    public void getPolicyByName_inValidPolicy() throws Exception {
        Policy policy = this.repo.getPolicyByName(null);
        Assert.assertEquals(policy,null);
    }

    @Test
    public void updateGetPolicy() {
        this.repo.addPolicy(getTestPolicy());
        Policy update = new Policy();
        update.setBlob("newBlob");
        update.setDescription("newDescription");
        this.repo.updatePolicy(update,policyId);
        Policy policy = repo.getPolicy(policyId);
        Assert.assertEquals("compair blob", policy.getBlob(), "newBlob");
        Assert.assertEquals("compair blob",policy.getName(),"XXXX");
        Assert.assertEquals("compair blob",policy.getPolicyType(),"someType");
        Assert.assertEquals("compair blob",policy.getDescription(),"newDescription");
        Assert.assertEquals("compair blob",policy.isEnabled(),true);
        Assert.assertEquals("compair blob",policy.isGlobal(),false);
        Assert.assertEquals("compair blob",policy.getPolicyId(),"XXXX");
    }

    private Policy getTestPolicy() {
        Policy policy = new Policy();
        policy.setDescription(description);
        policy.setEnabled(enabled);
        policy.setGlobal(global);
        policy.setName(name);
        policy.setBlob(blob);
        policy.setPolicyType(type);
        policy.setPolicyId(policyId);
        return policy;
    }


}
