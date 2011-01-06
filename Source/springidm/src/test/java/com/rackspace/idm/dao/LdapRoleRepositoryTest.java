package com.rackspace.idm.dao;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.config.LdapConfiguration;
import com.rackspace.idm.config.PropertyFileConfiguration;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.test.stub.StubLogger;

public class LdapRoleRepositoryTest {
    private LdapRoleRepository repo;
    private LdapConnectionPools connPools;

    String customerNumber = "RACKSPACE";
    String adminRoleName = "Idm Admin";
    String adminRoleInum = "@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!AAAA";
    String badRoleInum = "@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!XXXX";
    String roleDN = "inum=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!AAAA,ou=groups,o=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE,o=rackspace,dc=rackspace,dc=com";
    String userDN = "inum=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!1111,ou=people,o=@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE,o=rackspace,dc=rackspace,dc=com";

    private static LdapRoleRepository getRepo(LdapConnectionPools connPools) {
        return new LdapRoleRepository(connPools, new StubLogger());
    }

    private static LdapConnectionPools getConnPools() {
        LdapConfiguration config = new LdapConfiguration(
            new PropertyFileConfiguration().getConfigFromClasspath(), new StubLogger());
        return config.connectionPools();
    }

    @Before
    public void setUp() {
        connPools = getConnPools();
        repo = getRepo(connPools);
    }

    @Test
    public void shouldNotAcceptNullOrBlankUsernameOrInum() {

        try {
            repo.findByInum(null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.findByInum("     ");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.findByRoleNameAndCustomerId("", "customerId");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.findByRoleNameAndCustomerId("roleName", "");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.addUserToRole("userDN", "");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.addUserToRole("", "RoleDN");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.deleteUserFromRole("userDN", "");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }

        try {
            repo.deleteUserFromRole("", "RoleDN");
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }

    @Test
    public void shouldFindOneRoleThatExistsByInum() {
        Role role = repo.findByInum(adminRoleInum);
        Assert.assertNotNull(role);
        Assert.assertEquals("Idm Admin", role.getName());
    }

    @Test
    public void shouldNotFindRoleThatDoesNotExistByInum() {
        Role role = repo.findByInum(badRoleInum);
        Assert.assertNull(role);
    }

    @Test
    public void shouldFindRoleThatExistsByNameAndCustomerNumber() {
        Role role = repo.findByRoleNameAndCustomerId(adminRoleName,
            customerNumber);
        Assert.assertNotNull(role);
        Assert.assertEquals("Idm Admin", role.getName());
    }

    @Test
    public void shouldDeleteAndAddUsertoRol() {
        repo.deleteUserFromRole(userDN, roleDN);
        repo.addUserToRole(userDN, roleDN);
    }
}
