package com.rackspace.idm.domain.entity;

import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;

import org.junit.Assert;
import org.junit.Test;

public class ClientTests {

    private final String clientId = "Id";
    private final String clientPassword = "Secret";
    private final String name = "Name";

    private Application getTestClient() {
        ClientSecret clientSecret = ClientSecret.newInstance(clientPassword);
        Application application = new Application(clientId, name);
        application.setClientSecret(clientSecret.getValue());
        return  application;
    }

    @Test
    public void shouldReturnToString() {
        Application client = getTestClient();

        Assert.assertNotNull(client.toString());
    }

    @Test
    public void shouldReturnHashCode() {
        Application client = getTestClient();

        Assert.assertNotNull(client.hashCode());
    }

    @Test
    public void shouldReturnTrueForEquals() {
        Application client1 = getTestClient();
        Application client2 = getTestClient();

        Assert.assertTrue(client1.equals(client1));
        Assert.assertTrue(client1.equals(client2));

        client1.setClientId(null);
        client1.setClientSecretObj(null);
        client1.setName(null);

        client2.setClientId(null);
        client2.setClientSecretObj(null);
        client2.setName(null);

        Assert.assertTrue(client1.equals(client2));
    }

    @Test
    public void shouldReturnFalseForEquals() {
        Application client1 = getTestClient();
        Application client2 = getTestClient();

        Assert.assertFalse(client1.equals(null));
        Assert.assertFalse(client1.equals(1));

        client2.setClientId("SomeOtherValue");
        Assert.assertFalse(client1.equals(client2));
        client2.setClientId(null);
        Assert.assertFalse(client2.equals(client1));
        client2.setClientId(client1.getClientId());

        client2.setClientSecretObj(ClientSecret.newInstance("SomeOtherValue"));
        Assert.assertFalse(client1.equals(client2));
        client2.setClientSecretObj(null);
        Assert.assertFalse(client2.equals(client1));
        client2.setClientSecretObj(client1.getClientSecretObj());

        client2.setName("SomeOtherValue");
        Assert.assertFalse(client1.equals(client2));
        client2.setName(null);
        Assert.assertFalse(client2.equals(client1));
        client2.setName(client1.getName());
    }

     @Test
    public void shouldRunValidation() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<Application>> violations = validator.validate(new Application());
        Assert.assertEquals(1, violations.size());
        System.out.println(violations);
    }
}
