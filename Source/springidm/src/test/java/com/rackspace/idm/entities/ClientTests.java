package com.rackspace.idm.entities;

import com.rackspace.idm.domain.entity.Client;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ClientStatus;

import org.junit.Assert;

import org.junit.Test;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import java.util.Set;

public class ClientTests {

    private String clientId = "Id";
    private String clientPassword = "Secret";
    private String name = "Name";
    private String inum = "Inum";
    private String iname = "Iname";
    private String customerId = "CustomerId";
    private ClientStatus status = ClientStatus.ACTIVE;
    private String seeAlso = "SeeAlso";
    private String owner = "Owner";

    private Client getTestClient() {
        ClientSecret clientSecret = ClientSecret.newInstance(clientPassword);

        return new Client(clientId, clientSecret, name, inum, iname,
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
        client1.setCustomerId(null);
        client1.setIname(null);
        client1.setInum(null);
        client1.setName(null);
        client1.setStatus(null);

        client2.setClientId(null);
        client2.setClientSecretObj(null);
        client2.setCustomerId(null);
        client2.setIname(null);
        client2.setInum(null);
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

        client2.setCustomerId("SomeOtherValue");
        Assert.assertFalse(client1.equals(client2));
        client2.setCustomerId(null);
        Assert.assertFalse(client2.equals(client1));
        client2.setCustomerId(client1.getCustomerId());

        client2.setIname("SomeOtherValue");
        Assert.assertFalse(client1.equals(client2));
        client2.setIname(null);
        Assert.assertFalse(client2.equals(client1));
        client2.setIname(client1.getIname());

        client2.setInum("SomeOtherValue");
        Assert.assertFalse(client1.equals(client2));
        client2.setInum(null);
        Assert.assertFalse(client2.equals(client1));
        client2.setInum(client1.getInum());

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
