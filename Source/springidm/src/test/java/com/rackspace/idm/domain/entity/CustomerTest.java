package com.rackspace.idm.domain.entity;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class CustomerTest {

    private final String customerId = "123";
    Customer customer;

    @Before
    public void setUp() throws Exception {
        customer = new Customer();
    }

    private Customer getTestCustomer() {
        Customer customer = new Customer();
        customer.setRCN(customerId);
        return customer;
    }

    @Test
    public void shouldReturnToString() {
        Customer customer = getTestCustomer();
        Assert.assertNotNull(customer.toString());
    }

    @Test
    public void shouldReturnHashCode() {
        Customer customer = getTestCustomer();
        Assert.assertNotNull( customer.hashCode());
    }

    @Test
    public void shouldReturnTrueForEquals() {
        Customer customer1 = getTestCustomer();
        Customer customer2 = getTestCustomer();
        Assert.assertTrue(customer1.equals(customer2));
        Assert.assertTrue(customer1.equals(customer1));

        customer1.setRCN(null);

        customer2.setRCN(null);

        Assert.assertTrue(customer1.equals(customer2));
    }

    @Test
    public void shouldReturnFalseForEqual() {
        Customer customer1 = getTestCustomer();
        Customer customer2 = getTestCustomer();

        Assert.assertFalse(customer1.equals(null));
        Assert.assertFalse(customer1.equals(1));

        customer2.setRCN("NewId");
        Assert.assertFalse(customer1.equals(customer2));
        customer2.setRCN(null);
        Assert.assertFalse(customer2.equals(customer1));
    }

    @Test
    public void setUniqueId_uniqueIdIsNull_doesNotSet() throws Exception {
        customer.setUniqueId("uniqueId");
        customer.setUniqueId(null);
        String result = customer.getUniqueId();
        assertThat("unique id", result, equalTo("uniqueId"));
    }

    @Test
    public void hashCode_attributesNotNull_returnsHashCode() throws Exception {
        customer.setRCN("rcn");
        customer.setEnabled(true);
        customer.setId("id");
        customer.setPasswordRotationDuration(1);
        customer.setPasswordRotationEnabled(true);
        customer.setUniqueId("uniqueId");
        int result = customer.hashCode();
        assertThat("hash code", result, equalTo(-535441162));
    }

    @Test
    public void hashCode_attributesIsNull_returnsHashCode() throws Exception {
        customer.setRCN(null);
        customer.setEnabled(null);
        customer.setId(null);
        customer.setPasswordRotationDuration(null);
        customer.setPasswordRotationEnabled(null);
        customer.setUniqueId(null);
        int result = customer.hashCode();
        assertThat("hash code", result, equalTo(887503681));
    }

    @Test
    public void equals_rcnIsNullAndObjectRcnNotNull_returnsFalse() throws Exception {
        Customer object = new Customer();
        object.setRCN("notNull");
        customer.setRCN(null);
        boolean result = customer.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_enabledIsNullAndObjectEnabledNotNull_returnsFalse() throws Exception {
        Customer object = new Customer();
        object.setEnabled(true);
        customer.setEnabled(null);
        boolean result = customer.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_enabledNotNullAndNotEqualsObjectEnabled_returnsFalse() throws Exception {
        Customer object = new Customer();
        object.setEnabled(true);
        customer.setEnabled(false);
        boolean result = customer.equals(object);
        assertThat("boolean", result, equalTo(false));
    }
    
    @Test
    public void equals_idIsNullAndObjectIdNotNull_returnsFalse() throws Exception {
        Customer object = new Customer();
        object.setId("notNull");
        customer.setId(null);
        boolean result = customer.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_idNotNullAndNotEqualsObjectId_returnsFalse() throws Exception {
        Customer object = new Customer();
        object.setId("notNull");
        customer.setId("notSame");
        boolean result = customer.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_passwordRotationDurationIsNullAndObjectPasswordRotationDurationNotNull_returnsFalse() throws Exception {
        Customer object = new Customer();
        object.setPasswordRotationDuration(1);
        customer.setPasswordRotationDuration(null);
        boolean result = customer.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_passwordRotationDurationNotNullAndNotEqualsObjectPasswordRotationDuration_returnsFalse() throws Exception {
        Customer object = new Customer();
        object.setPasswordRotationDuration(1);
        customer.setPasswordRotationDuration(2);
        boolean result = customer.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_passwordRotationEnabledIsNullAndObjectPasswordRotationEnabledNotNull_returnsFalse() throws Exception {
        Customer object = new Customer();
        object.setPasswordRotationEnabled(true);
        customer.setPasswordRotationEnabled(null);
        boolean result = customer.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_passwordRotationEnabledNotNullAndNotEqualsObjectPasswordRotationEnabled_returnsFalse() throws Exception {
        Customer object = new Customer();
        object.setPasswordRotationEnabled(true);
        customer.setPasswordRotationEnabled(false);
        boolean result = customer.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_uniqueIdIsNullAndObjectUniqueIdNotNull_returnsFalse() throws Exception {
        Customer object = new Customer();
        object.setUniqueId("notNull");
        customer.setUniqueId(null);
        boolean result = customer.equals(object);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void equals_uniqueIdNotNullAndNotEqualsObjectUniqueId_returnsFalse() throws Exception {
        Customer object = new Customer();
        object.setUniqueId("notNull");
        customer.setUniqueId("notSame");
        boolean result = customer.equals(object);
        assertThat("boolean", result, equalTo(false));
    }
    
    @Test
    public void equals_returnsTrue() throws Exception {
        Customer object = new Customer();
        object.setRCN("rcn");
        object.setId("id");
        object.setPasswordRotationEnabled(true);
        object.setUniqueId("uniqueId");
        object.setEnabled(true);
        object.setPasswordRotationDuration(1);

        customer.setRCN("rcn");
        customer.setId("id");
        customer.setPasswordRotationEnabled(true);
        customer.setUniqueId("uniqueId");
        customer.setEnabled(true);
        customer.setPasswordRotationDuration(1);

        boolean result = customer.equals(object);
        assertThat("boolean", result, equalTo(true));
    }
}
