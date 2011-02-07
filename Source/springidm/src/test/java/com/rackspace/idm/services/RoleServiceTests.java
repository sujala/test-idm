package com.rackspace.idm.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.easymock.EasyMock;
import org.joda.time.DateTimeZone;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.dao.RoleDao;
import com.rackspace.idm.dao.UserDao;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.Password;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.entities.RoleStatus;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserCredential;
import com.rackspace.idm.entities.UserHumanName;
import com.rackspace.idm.entities.UserLocale;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.services.DefaultRoleService;
import com.rackspace.idm.services.RoleService;
import com.rackspace.idm.test.stub.StubLogger;

public class RoleServiceTests {
    RoleDao mockRoleDao;
    UserDao mockUserDao;
    RoleService roleService;
    
    Role testRole;
    User testUser;
    
    String uniqueId = "uniqueId";
    String customerId = "123456";
    String username = "testuser";
    String password = "secret";
    Password userpass = Password.newInstance(password);
    String firstname = "testfirstname";
    String lastname = "testlastname";
    String email = "test@example.com";
    String apiKey = "1234567890";

    String middlename = "middle";
    String secretQuestion = "question";
    String secretAnswer = "answer";
    String preferredLang = "en_US";
    String timeZone = "America/Chicago";
    
    String roleName = "roleName";
    String customerNumber = "customerNumber";
    String country = "USA";
    String inum = "inum";
    String iname = "iname";
    String orgInum = "orgInum";
    String owner = "owner";
    RoleStatus status = RoleStatus.ACTIVE;
    String seeAlso = "seeAlso";
    String type = "type";
    
    String roleDN = "inum=inum,o=rackspace";
    
    @Before
    public void setUp() throws Exception {

        mockUserDao = EasyMock.createMock(UserDao.class);
        mockRoleDao = EasyMock.createMock(RoleDao.class);

        roleService = new DefaultRoleService(mockRoleDao, mockUserDao, new StubLogger());
        
        testRole = new Role(uniqueId, roleName, customerId,
            country, inum, iname, orgInum,
            owner, status, seeAlso, type);
        
        testRole.setUniqueId(uniqueId);
        testRole.setName(roleName);
        testRole.setCustomerId(customerId);
        testRole.setCountry(country);
        testRole.setInum(inum);
        testRole.setIname(iname);
        testRole.setOrgInum(orgInum);
        testRole.setOwner(owner);
        testRole.setStatus(status);
        testRole.setSeeAlso(seeAlso);
        testRole.setType(type);
        
        UserHumanName name = new UserHumanName(firstname, middlename, lastname);
        UserLocale pref = new UserLocale(new Locale(preferredLang),
                DateTimeZone.forID(timeZone));
        UserCredential cred = new UserCredential(userpass, secretQuestion,
                secretAnswer);
        testUser = new User(username, customerId, email, name, pref, cred);
        testUser.setUniqueId("uniqueId");
    }
    
    @Test
    public void shouldThrowErrorForNullValues() {
        try {
            roleService.addUserToRole(null, testRole);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        
        try {
            roleService.addUserToRole(testUser, null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        
        try {
            roleService.deleteUserFromRole(null, testRole);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
        
        try {
            roleService.deleteUserFromRole(testUser, null);
            Assert.fail("Should have thrown an exception!");
        } catch (IllegalArgumentException e) {
            Assert.assertTrue(true);
        }
    }
    
    @Test
    public void shouldAddUserToRole() {
        mockRoleDao.addUserToRole(uniqueId, uniqueId);
        EasyMock.replay(mockRoleDao);
        
        roleService.addUserToRole(testUser, testRole);
        
        EasyMock.verify(mockRoleDao);
        Assert.assertTrue(true);
    }
    
    @Test
    public void shouldAddUserToRoleUserAlreadyHas() {
        mockRoleDao.addUserToRole(uniqueId, uniqueId);
        EasyMock.expectLastCall().andThrow(new DuplicateException());
        EasyMock.replay(mockRoleDao);
        
        roleService.addUserToRole(testUser, testRole);
        
        EasyMock.verify(mockRoleDao);
        Assert.assertTrue(true);
    }
    
    @Test
    public void shouldDeleteUserFromRole() {
        mockRoleDao.deleteUserFromRole(uniqueId, uniqueId);
        EasyMock.replay(mockRoleDao);
        
        roleService.deleteUserFromRole(testUser, testRole);
        
        EasyMock.verify(mockRoleDao);
        Assert.assertTrue(true);
    }
    
    @Test
    public void shouldDeleteUserFromRoleUserDoesntHave() {
        mockRoleDao.deleteUserFromRole(uniqueId, uniqueId);
        EasyMock.expectLastCall().andThrow(new NotFoundException());
        EasyMock.replay(mockRoleDao);
        
        roleService.deleteUserFromRole(testUser, testRole);
        
        EasyMock.verify(mockRoleDao);
        Assert.assertTrue(true);
    }
    
    @Test
    public void shouldGetRole() {
        EasyMock.expect(mockRoleDao.findByRoleNameAndCustomerId(roleName, customerId)).andReturn(testRole);
        EasyMock.replay(mockRoleDao);
        
        Role role = roleService.getRole(roleName, customerId);
        
        Assert.assertNotNull(role);
        Assert.assertTrue(role.getName().equals(roleName));
        Assert.assertTrue(role.getCountry().equals(country));
        Assert.assertTrue(role.getCustomerId().equals(customerId));
        Assert.assertTrue(role.getIname().equals(iname));
        Assert.assertTrue(role.getInum().equals(inum));
        Assert.assertTrue(role.getOrgInum().equals(orgInum));
        Assert.assertTrue(role.getOwner().equals(owner));
        Assert.assertTrue(role.getSeeAlso().equals(seeAlso));
        Assert.assertTrue(role.getType().equals(type));
        Assert.assertTrue(role.getUniqueId().equals(uniqueId));
        Assert.assertTrue(role.getStatus().equals(status));
        
        
        EasyMock.verify(mockRoleDao);
    }
    
    @Test
    public void shouldGetRolesForUser() {
        String[] roleIds = new String[] {roleDN};
        
        EasyMock.expect(mockUserDao.getGroupIdsForUser(testUser.getUsername())).andReturn(roleIds);
        EasyMock.replay(mockUserDao);
        
        EasyMock.expect(mockRoleDao.findByInum(inum)).andReturn(testRole);
        EasyMock.replay(mockRoleDao);
        
        List<Role> roles = roleService.getRolesForUser(testUser.getUsername());
        
        Assert.assertNotNull(roles);
        Assert.assertTrue(roles.size() == 1);
    }
    
    @Test
    public void shouldGetNullRolesForUser() {
        EasyMock.expect(mockUserDao.getGroupIdsForUser(testUser.getUsername())).andReturn(null);
        EasyMock.replay(mockUserDao);
        
        List<Role> roles = roleService.getRolesForUser(testUser.getUsername());
        
        Assert.assertNull(roles);
    }
    
    @Test
    public void shouldGetUsersByCustomerId() {
        List<Role> roles = new ArrayList<Role>();
        roles.add(testRole);
        roles.add(testRole);
        
        EasyMock.expect(mockRoleDao.findByCustomerId(customerId)).andReturn(roles);
        EasyMock.replay(mockRoleDao);
        
        List<Role> returned = roleService.getByCustomerId(customerId);
        
        Assert.assertTrue(returned.size() == 2);
        EasyMock.verify(mockRoleDao);
    }
}
