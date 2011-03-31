package com.rackspace.idm.docs;

import java.io.IOException;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

import com.rackspace.idm.jaxb.Permission;
import com.rackspace.idm.jaxb.PermissionList;
import com.rackspace.idm.jaxb.Permissions;
import com.rackspace.idm.jaxb.Role;
import com.rackspace.idm.jaxb.Roles;
import com.rackspace.idm.jaxb.User;
import com.rackspace.idm.jaxb.UserApiKey;
import com.rackspace.idm.jaxb.UserCredentials;
import com.rackspace.idm.jaxb.UserPassword;
import com.rackspace.idm.jaxb.UserSecret;
import com.rackspace.idm.jaxb.UserStatus;
import com.rackspace.idm.jaxb.Users;

public class UserSampleGenerator extends SampleGenerator {
    private UserSampleGenerator() {
        super();
    }

    public static void main(String[] args) throws JAXBException, IOException {
        UserSampleGenerator sampleGen = new UserSampleGenerator();

        sampleGen.marshalToFiles(sampleGen.getUser(), "user");
        sampleGen.marshalToFiles(sampleGen.getUsers(), "users");
        sampleGen.marshalToFiles(sampleGen.getUserApiKey(), "user_api_key");
        sampleGen.marshalToFiles(sampleGen.getUserCredentials(),
            "user_credentials");
        sampleGen.marshalToFiles(sampleGen.getUserSecret(), "user_secret");
        sampleGen.marshalToFiles(sampleGen.getPassword(), "user_password");
    }

    private UserPassword getPassword() {
        UserPassword pass = of.createUserPassword();
        pass.setPassword("P@ssword1");
        return pass;
    }
    
    private User getUser() {
        User user = of.createUser();
        user.setCustomerId("RCN-000-000-000");
        user.setCustomerInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE");
        user.setDisplayName("John Smith");
        user.setEmail("john.smith@example.org");
        user.setFirstName("John");
        user.setIname("@Example.Smith*John");
        user.setInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!1111");
        user.setLocked(Boolean.FALSE);
        user.setSoftDeleted(Boolean.FALSE);
        user.setLastName("Smith");
        user.setMiddleName("Quincy");
        user.setPersonId("RPN-111-111-111");
        user.setPrefLanguage("US_en");
        user.setRegion("America/Chicago");
        user.setStatus(UserStatus.ACTIVE);
        UserApiKey uapikey = of.createUserApiKey();
        uapikey.setApiKey("10388a8497547f8w77e");
        user.setApiKey(uapikey);
        user.setUsername("jqsmith");
        UserPassword pwd = of.createUserPassword();
        pwd.setPassword("C@n+f001me!");
        user.setPassword(pwd);
        UserSecret secret = of.createUserSecret();
        secret
            .setSecretQuestion("What is the middle name of your best fried, spelled backward?");
        secret.setSecretAnswer("sicnarF");
        user.setSecret(secret);
        
        com.rackspace.idm.jaxb.PermissionList permissions = of.createPermissionList();
        
        com.rackspace.idm.jaxb.Permission permission = of.createPermission();
        permission.setPermissionId("addUser");
        permission.setClientId("IDM");
        
        permissions.getPermissions().add(permission);
        
        Role role = of.createRole();
        
        role.setName("Admin");
        role.setType("RackspaceDefined");
        
        Permission permission2 = of.createPermission();
        permission.setPermissionId("addUser");
        permission.setClientId("IDM");
        
        PermissionList permissions2 = of.createPermissionList();
        permissions.getPermissions().add(permission2);
        
        role.setPermissions(permissions2);
        
        Roles roles = of.createRoles();
        roles.getRoles().add(role);
        
//        user.setRoles(roles);
        
        return user;
    }

    private Users getUsers() {
        Users users = of.createUsers();
        User user = getUser();
        users.getUsers().add(user);
        User user2 = getUser2();
        users.getUsers().add(user2);
        
        users.setLimit(new Integer(10));
        users.setOffset(new Integer(0));
        users.setTotalRecords(new Integer(2));
        return users;
    }

    private User getUser2() {
        User user = of.createUser();
        user.setCustomerId("RCN-000-000-000");
        user.setCustomerInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE");
        user.setDisplayName("Bob Anderson");
        user.setEmail("bob.anderson@example.org");
        user.setFirstName("Bob");
        user.setIname("@Example.Anderson*Bob");
        user.setInum("@!FFFF.FFFF.FFFF.FFFF!EEEE.EEEE!2222");
        user.setLocked(Boolean.FALSE);
        user.setSoftDeleted(Boolean.FALSE);
        user.setLastName("Anderson");
        user.setMiddleName("Mark");
        user.setPersonId("RPN-111-111-222");
        user.setPrefLanguage("US_en");
        user.setRegion("America/Chicago");
        user.setStatus(UserStatus.ACTIVE);
        UserApiKey uapikey = of.createUserApiKey();
        uapikey.setApiKey("02398452835b0qe9rc0w934a");
        user.setApiKey(uapikey);
        user.setUsername("bmanderson");
        UserPassword pwd = of.createUserPassword();
        pwd.setPassword("thispasswordisnogood");
        user.setPassword(pwd);
        UserSecret secret = of.createUserSecret();
        secret
            .setSecretQuestion("Should a secret question be more rigorous than this?");
        secret.setSecretAnswer("Definitely");
        user.setSecret(secret);
        return user;
    }

    private UserApiKey getUserApiKey() {
        UserApiKey key = of.createUserApiKey();
        key.setApiKey("403958809d0809834e0809808a");
        return key;
    }

    private UserCredentials getUserCredentials() {
        UserCredentials cred = of.createUserCredentials();
        UserPassword opwd = of.createUserPassword();
        opwd.setPassword("oldpassword");
        UserPassword npwd = of.createUserPassword();
        npwd.setPassword("newpassword");
        cred.setCurrentPassword(opwd);
        cred.setNewPassword(npwd);
        return cred;
    }

    private UserSecret getUserSecret() {
        UserSecret secret = of.createUserSecret();
        secret.setSecretQuestion("Is this a secret question?");
        secret.setSecretAnswer("Not a very good one.");
        return secret;
    }
}
