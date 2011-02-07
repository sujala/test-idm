package com.rackspace.idm.config;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.rackspace.idm.rest.resources.ApiKeyResource;
import com.rackspace.idm.rest.resources.CustomerClientResource;
import com.rackspace.idm.rest.resources.CustomerClientsResource;
import com.rackspace.idm.rest.resources.CustomerLockResource;
import com.rackspace.idm.rest.resources.CustomerResource;
import com.rackspace.idm.rest.resources.CustomerUsersResource;
import com.rackspace.idm.rest.resources.CustomersResource;
import com.rackspace.idm.rest.resources.DefinedPermissionResource;
import com.rackspace.idm.rest.resources.DefinedPermissionsResource;
import com.rackspace.idm.rest.resources.GrantedPermissionsResource;
import com.rackspace.idm.rest.resources.PasswordRulesResource;
import com.rackspace.idm.rest.resources.PermissionsResource;
import com.rackspace.idm.rest.resources.RolesResource;
import com.rackspace.idm.rest.resources.TokenResource;
import com.rackspace.idm.rest.resources.UserGroupsResource;
import com.rackspace.idm.rest.resources.UserLockResource;
import com.rackspace.idm.rest.resources.UserPasswordResource;
import com.rackspace.idm.rest.resources.UserResource;
import com.rackspace.idm.rest.resources.UserRoleResource;
import com.rackspace.idm.rest.resources.UserSecretResource;
import com.rackspace.idm.rest.resources.UserSoftDeleteResource;
import com.rackspace.idm.rest.resources.UserStatusResource;
import com.rackspace.idm.rest.resources.UsersResource;
import com.rackspace.idm.rest.resources.VersionResource;

public class SpringConfigurationTest {

    @Test
    public void shouldConfigureBeansWithoutException() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.scan("com.rackspace.idm");
        ctx.refresh();
        
        ApiKeyResource apiKeyResource = ctx.getBean(ApiKeyResource.class);
        Assert.assertNotNull(apiKeyResource);
        
        CustomerClientResource clientResource = ctx.getBean(CustomerClientResource.class);
        Assert.assertNotNull(clientResource);
        
        CustomerClientsResource clientsResource = ctx.getBean(CustomerClientsResource.class);
        Assert.assertNotNull(clientsResource);
        
        CustomerLockResource customerLockResource = ctx.getBean(CustomerLockResource.class);
        Assert.assertNotNull(customerLockResource);
        
        CustomerResource customerResource = ctx.getBean(CustomerResource.class);
        Assert.assertNotNull(customerResource);
        
        CustomersResource customersResource = ctx.getBean(CustomersResource.class);
        Assert.assertNotNull(customersResource);
        
        DefinedPermissionResource definedPermissionResource = ctx.getBean(DefinedPermissionResource.class);
        Assert.assertNotNull(definedPermissionResource);
        
        DefinedPermissionsResource definedPermissionsResource = ctx.getBean(DefinedPermissionsResource.class);
        Assert.assertNotNull(definedPermissionsResource);
        
        UsersResource firstUserResource = ctx.getBean(UsersResource.class);
        Assert.assertNotNull(firstUserResource);
        
        GrantedPermissionsResource grantedPermissionsResource = ctx.getBean(GrantedPermissionsResource.class);
        Assert.assertNotNull(grantedPermissionsResource);
        
        PasswordRulesResource passwordRulesResource = ctx.getBean(PasswordRulesResource.class);
        Assert.assertNotNull(passwordRulesResource);
        
        PermissionsResource permissionsResource = ctx.getBean(PermissionsResource.class);
        Assert.assertNotNull(permissionsResource);
        
        RolesResource rolesResource = ctx.getBean(RolesResource.class);
        Assert.assertNotNull(rolesResource);
        
        TokenResource tokenResource = ctx.getBean(TokenResource.class);
        Assert.assertNotNull(tokenResource);
        
        UserLockResource userLockResource = ctx.getBean(UserLockResource.class);
        Assert.assertNotNull(userLockResource);
        
        UserPasswordResource userPasswordResource = ctx.getBean(UserPasswordResource.class);
        Assert.assertNotNull(userPasswordResource);
        
        UserResource userResource = ctx.getBean(UserResource.class);
        Assert.assertNotNull(userResource);
        
        UserRoleResource userRoleResource = ctx.getBean(UserRoleResource.class);
        Assert.assertNotNull(userRoleResource);
        
        UserGroupsResource userRolesResource = ctx.getBean(UserGroupsResource.class);
        Assert.assertNotNull(userRolesResource);
        
        UserSecretResource userSecretResource = ctx.getBean(UserSecretResource.class);
        Assert.assertNotNull(userSecretResource);
        
        UserSoftDeleteResource userSoftDeleteResource = ctx.getBean(UserSoftDeleteResource.class);
        Assert.assertNotNull(userSoftDeleteResource);
        
        CustomerUsersResource customerUsersResource = ctx.getBean(CustomerUsersResource.class);
        Assert.assertNotNull(customerUsersResource);
        
        UserStatusResource userStatusResource = ctx.getBean(UserStatusResource.class);
        Assert.assertNotNull(userStatusResource);
        
        VersionResource versionResource = ctx.getBean(VersionResource.class);
        Assert.assertNotNull(versionResource);
    }
}
