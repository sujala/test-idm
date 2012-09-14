package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.entity.Policies;
import com.rackspace.idm.domain.entity.Policy;
import org.junit.*;

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
        Assert.assertEquals("compare blob", policy.getBlob(), "someBlob");
        Assert.assertEquals("compare name",policy.getName(),"XXXX");
        Assert.assertEquals("compare Type",policy.getPolicyType(),"someType");
        Assert.assertEquals("compare Description",policy.getDescription(),"Description");
        Assert.assertEquals("compare enabled",policy.isEnabled(),true);
        Assert.assertEquals("compare global",policy.isGlobal(),false);
        Assert.assertEquals("compare id",policy.getPolicyId(),"XXXX");
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
        Assert.assertEquals("compare blob", policy.getBlob(), "someBlob");
        Assert.assertEquals("compare name",policy.getName(),"XXXX");
        Assert.assertEquals("compare type",policy.getPolicyType(),"someType");
        Assert.assertEquals("compare description",policy.getDescription(),"Description");
        Assert.assertEquals("compare enable",policy.isEnabled(),true);
        Assert.assertEquals("compare global",policy.isGlobal(),false);
        Assert.assertEquals("compare id",policy.getPolicyId(),"XXXX");
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
        Assert.assertEquals("compare blob", policy.getBlob(), "newBlob");
        Assert.assertEquals("compare name",policy.getName(),"XXXX");
        Assert.assertEquals("compare type",policy.getPolicyType(),"someType");
        Assert.assertEquals("compare description",policy.getDescription(),"newDescription");
        Assert.assertEquals("compare enable",policy.isEnabled(),true);
        Assert.assertEquals("compare global",policy.isGlobal(),false);
        Assert.assertEquals("compare id",policy.getPolicyId(),"XXXX");
    }

    @Test
    public void listPolicies_returnsAllPolicies() {
        this.repo.addPolicy(getTestPolicy());
        Policy policy = new Policy();
        policy.setDescription("otherDes");
        policy.setEnabled(true);
        policy.setGlobal(false);
        policy.setName("otherName");
        policy.setBlob("someblob");
        policy.setPolicyType("otherType");
        policy.setPolicyId("321");
        this.repo.addPolicy(policy);
        Policies policies = repo.getPolicies();
        Boolean result1 = false;
        Boolean result2 = false;
        for(Policy singlePolicy : policies.getPolicy()){
            if(singlePolicy.getName().equals(policy.getName())) {
                result1 = true;
            }
            if(singlePolicy.getName().equals("XXXX")){
                result2 = true;
            }
        }
        Assert.assertEquals("compare policies", result1, true);
        Assert.assertEquals("compare policies", result2, true);
        repo.deletePolicy(policy.getPolicyId());
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
