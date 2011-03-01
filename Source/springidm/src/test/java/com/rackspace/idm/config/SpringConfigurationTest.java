package com.rackspace.idm.config;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.rackspace.idm.api.resource.ApiKeyResource;
import com.rackspace.idm.api.resource.AuthResource;
import com.rackspace.idm.api.resource.BaseUrlsResource;
import com.rackspace.idm.api.resource.ClientGroupMembersResource;
import com.rackspace.idm.api.resource.ClientGroupResource;
import com.rackspace.idm.api.resource.ClientGroupsResource;
import com.rackspace.idm.api.resource.CustomerClientResource;
import com.rackspace.idm.api.resource.CustomerClientsResource;
import com.rackspace.idm.api.resource.CustomerLockResource;
import com.rackspace.idm.api.resource.CustomerResource;
import com.rackspace.idm.api.resource.CustomerUsersResource;
import com.rackspace.idm.api.resource.CustomersResource;
import com.rackspace.idm.api.resource.DefinedPermissionResource;
import com.rackspace.idm.api.resource.DefinedPermissionsResource;
import com.rackspace.idm.api.resource.GrantedPermissionsResource;
import com.rackspace.idm.api.resource.MossoUserResource;
import com.rackspace.idm.api.resource.NastUserResource;
import com.rackspace.idm.api.resource.PasswordRulesResource;
import com.rackspace.idm.api.resource.PermissionsResource;
import com.rackspace.idm.api.resource.RolesResource;
import com.rackspace.idm.api.resource.TokenResource;
import com.rackspace.idm.api.resource.UserGroupsResource;
import com.rackspace.idm.api.resource.UserLockResource;
import com.rackspace.idm.api.resource.UserPasswordResource;
import com.rackspace.idm.api.resource.UserResource;
import com.rackspace.idm.api.resource.UserRoleResource;
import com.rackspace.idm.api.resource.UserSecretResource;
import com.rackspace.idm.api.resource.UserSoftDeleteResource;
import com.rackspace.idm.api.resource.UserStatusResource;
import com.rackspace.idm.api.resource.UsersResource;
import com.rackspace.idm.api.resource.VersionResource;
import com.rackspace.idm.domain.service.HealthMonitoringService;

public class SpringConfigurationTest {

    @Test
    public void shouldConfigureBeansWithoutException() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.scan("com.rackspace.idm");
        ctx.refresh();
        
        ApiKeyResource apiKeyResource = ctx.getBean(ApiKeyResource.class);
        Assert.assertNotNull(apiKeyResource);
        
        AuthResource authResource = ctx.getBean(AuthResource.class);
        Assert.assertNotNull(authResource);
        
        BaseUrlsResource baseUrlsResource = ctx.getBean(BaseUrlsResource.class);
        Assert.assertNotNull(baseUrlsResource);
        
        ClientGroupMembersResource clientGroupMembersResource = ctx.getBean(ClientGroupMembersResource.class);
        Assert.assertNotNull(clientGroupMembersResource);
        
        ClientGroupResource clientGroupResource = ctx.getBean(ClientGroupResource.class);
        Assert.assertNotNull(clientGroupResource);
        
        ClientGroupsResource clientGroupsResource = ctx.getBean(ClientGroupsResource.class);
        Assert.assertNotNull(clientGroupsResource);
        
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
        
        MossoUserResource mossoUserResource = ctx.getBean(MossoUserResource.class);
        Assert.assertNotNull(mossoUserResource);
        
        NastUserResource nastUserResource = ctx.getBean(NastUserResource.class);
        Assert.assertNotNull(nastUserResource);
        
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
        
        UserGroupsResource userGroupsResource = ctx.getBean(UserGroupsResource.class);
        Assert.assertNotNull(userGroupsResource);
        
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
        
        HealthMonitoringService healthMonitoringService = ctx.getBean(HealthMonitoringService.class);
        Assert.assertNotNull(healthMonitoringService);
    }
}
