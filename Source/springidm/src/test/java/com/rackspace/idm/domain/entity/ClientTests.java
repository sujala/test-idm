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
    private final String customerId = "CustomerId";
    private final ClientStatus status = ClientStatus.ACTIVE;

    private Client getTestClient() {
        ClientSecret clientSecret = ClientSecret.newInstance(clientPassword);

        return new Client(clientId, clientSecret, name,
            customerId, status);
    }

    @Test
    public void shouldReturnToString() {
        Client client = getTestClient();

        Assert.assertNotNull(client.toString());
    }

    @Test
    public void shouldReturnHashCode() {
        Client client = getTestClient();

        Assert.assertNotNull(client.hashCode());
    }

    @Test
    public void shouldReturnTrueForEquals() {
        Client client1 = getTestClient();
        Client client2 = getTestClient();

        Assert.assertTrue(client1.equals(client1));
        Assert.assertTrue(client1.equals(client2));

        client1.setClientId(null);
        client1.setClientSecretObj(null);
        client1.setRCN(null);
        client1.setName(null);
        client1.setStatus(null);

        client2.setClientId(null);
        client2.setClientSecretObj(null);
        client2.setRCN(null);
        client2.setName(null);
        client2.setStatus(null);

        Assert.assertTrue(client1.equals(client2));
    }

    @Test
    public void shouldReturnFalseForEquals() {
        Client client1 = getTestClient();
        Client client2 = getTestClient();

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

        client2.setRCN("SomeOtherValue");
        Assert.assertFalse(client1.equals(client2));
        client2.setRCN(null);
        Assert.assertFalse(client2.equals(client1));
        client2.setRCN(client1.getRCN());

        client2.setName("SomeOtherValue");
        Assert.assertFalse(client1.equals(client2));
        client2.setName(null);
        Assert.assertFalse(client2.equals(client1));
        client2.setName(client1.getName());

        client2.setStatus(ClientStatus.INACTIVE);
        Assert.assertFalse(client1.equals(client2));
        client2.setStatus(null);
        Assert.assertFalse(client2.equals(client1));
    }

     @Test
    public void shouldRunValidation() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        Set<ConstraintViolation<Client>> violations = validator.validate(new Client());
        Assert.assertEquals(3, violations.size());
        System.out.println(violations);
    }
}
