package com.rackspace.idm.config;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.rackspace.idm.controllers.ClientController;
import com.rackspace.idm.controllers.CustomerController;
import com.rackspace.idm.controllers.PasswordComplexityController;
import com.rackspace.idm.controllers.TokenController;
import com.rackspace.idm.controllers.UsersController;
import com.rackspace.idm.controllers.VersionController;

public class SpringConfigurationTest {

    @Test
    public void shouldConfigureBeansWithoutException() {
        AnnotationConfigApplicationContext ctx = new AnnotationConfigApplicationContext();
        ctx.scan("com.rackspace.idm");
        ctx.refresh();
        UsersController usersController = ctx.getBean(UsersController.class);
        Assert.assertNotNull(usersController);
        TokenController tokenController = ctx.getBean(TokenController.class);
        Assert.assertNotNull(tokenController);
        ClientController clientController = ctx.getBean(ClientController.class);
        Assert.assertNotNull(clientController);
        CustomerController customerController = ctx
            .getBean(CustomerController.class);
        Assert.assertNotNull(customerController);
        PasswordComplexityController pwdController = ctx
            .getBean(PasswordComplexityController.class);
        Assert.assertNotNull(pwdController);
        VersionController versionController = ctx
            .getBean(VersionController.class);
        Assert.assertNotNull(versionController);
    }
}
