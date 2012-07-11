package com.rackspace.idm.domain.entity;

import flex.messaging.io.SerializationProxy;
import org.junit.Before;
import org.junit.Test;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 7/11/12
 * Time: 3:45 PM
 * To change this template use File | Settings | File Templates.
 */
public class ClientGroupTest {

    ClientGroup clientGroup;

    @Before
    public void setUp() throws Exception {
        clientGroup = new ClientGroup();
    }

    @Test
    public void hashCode_allFieldsNull_returnsHashCode() throws Exception {
        assertThat("returns hash code",clientGroup.hashCode(),equalTo(28629151));
    }

    @Test
    public void hashCode_clientIdNotNull_returnsHashCode() throws Exception {
        clientGroup.setClientId("clientId");
        assertThat("returns hash code", clientGroup.hashCode(), equalTo(-1708557339));
    }

    @Test
    public void hashCode_clientIdAndCustomerIdNotNull_returnsHashCode() throws Exception {
        clientGroup.setClientId("clientId");
        clientGroup.setCustomerId("customerId");
        assertThat("returns hash code", clientGroup.hashCode(), equalTo(421879724));
    }

    @Test
    public void hashCode_typeAndUniqueIdNull_returnsHashCode() throws Exception {
        clientGroup.setClientId("clientId");
        clientGroup.setCustomerId("customerId");
        clientGroup.setName("name");
        assertThat("returns hash code", clientGroup.hashCode(), equalTo(-630955145));
    }

    @Test
    public void hashCode_uniqueIdNull_returnsHashCode() throws Exception {
        clientGroup.setClientId("clientId");
        clientGroup.setCustomerId("customerId");
        clientGroup.setName("name");
        clientGroup.setType("type");
        assertThat("returns hash code", clientGroup.hashCode(), equalTo(-520111235));
    }

    @Test
    public void hashCode_allFieldsPopulated_returnsHashCode() throws Exception {
        clientGroup.setClientId("clientId");
        clientGroup.setCustomerId("customerId");
        clientGroup.setName("name");
        clientGroup.setType("type");
        clientGroup.setUniqueId("uniqueId");
        assertThat("returns hash code", clientGroup.hashCode(), equalTo(-814571447));
    }

    @Test
    public void equals_nullObject_returnsFalse() throws Exception {
        assertThat("returns false",clientGroup.equals(null),equalTo(false));
    }

    @Test
    public void equals_objectsAreDifferentClasses_returnsFalse() throws Exception {
        assertThat("returns false",clientGroup.equals(new Group()),equalTo(false));
    }

    @Test
    public void equals_clientIdIsNullButOtherClientIdNotNull_returnsFalse() throws Exception {
        ClientGroup clientGroup1 = new ClientGroup();
        clientGroup1.setClientId("clientId");
        assertThat("returns false", clientGroup.equals(clientGroup1), equalTo(false));
    }

    @Test
    public void equals_bothClientIdsExistAndNotEqual_returnsFalse() throws Exception {
        ClientGroup clientGroup1 = new ClientGroup();
        clientGroup1.setClientId("clientId");
        clientGroup.setClientId("anotherClientId");
        assertThat("returns false", clientGroup.equals(clientGroup1), equalTo(false));
    }

    @Test
    public void equals_bothClientIdsExistAndEqual_returnsTrue() throws Exception {
        ClientGroup clientGroup1 = new ClientGroup();
        clientGroup1.setClientId("clientId");
        clientGroup.setClientId("clientId");
        assertThat("returns false",clientGroup.equals(clientGroup1),equalTo(true));
    }

    @Test
    public void equals_customerIdIsNullButOtherCustomerIdNotNull_returnsFalse() throws Exception {
        ClientGroup clientGroup1 = new ClientGroup();
        clientGroup1.setCustomerId("customerId");
        assertThat("returns false",clientGroup.equals(clientGroup1),equalTo(false));
    }

    @Test
    public void equals_bothCustomerIdsExistAndNotEqual_returnsFalse() throws Exception {
        ClientGroup clientGroup1 = new ClientGroup();
        clientGroup1.setCustomerId("customerId");
        clientGroup.setCustomerId("anotherCustomerId");
        assertThat("returns false",clientGroup.equals(clientGroup1),equalTo(false));
    }

    @Test
    public void equals_bothCustomerIdsExistAndEqual_returnsTrue() throws Exception {
        ClientGroup clientGroup1 = new ClientGroup();
        clientGroup1.setCustomerId("customerId");
        clientGroup.setCustomerId("customerId");
        assertThat("returns false",clientGroup.equals(clientGroup1),equalTo(true));
    }

    @Test
    public void equals_nameIsNullButOtherNameNotNull_returnsFalse() throws Exception {
        ClientGroup clientGroup1 = new ClientGroup();
        clientGroup1.setName("name");
        assertThat("returns false",clientGroup.equals(clientGroup1),equalTo(false));
    }

    @Test
    public void equals_bothNamesExistAndNotEqual_returnsFalse() throws Exception {
        ClientGroup clientGroup1 = new ClientGroup();
        clientGroup1.setName("name");
        clientGroup.setName("anotherName");
        assertThat("returns false",clientGroup.equals(clientGroup1),equalTo(false));
    }

    @Test
    public void equals_bothNamesExistAndEqual_returnsTrue() throws Exception {
        ClientGroup clientGroup1 = new ClientGroup();
        clientGroup1.setName("name");
        clientGroup.setName("name");
        assertThat("returns false",clientGroup.equals(clientGroup1),equalTo(true));
    }

    @Test
    public void equals_typeIsNullButOtherTypeNotNull_returnsFalse() throws Exception {
        ClientGroup clientGroup1 = new ClientGroup();
        clientGroup1.setType("type");
        assertThat("returns false",clientGroup.equals(clientGroup1),equalTo(false));
    }

    @Test
    public void equals_bothTypesExistAndNotEqual_returnsFalse() throws Exception {
        ClientGroup clientGroup1 = new ClientGroup();
        clientGroup1.setType("type");
        clientGroup.setType("anotherType");
        assertThat("returns false",clientGroup.equals(clientGroup1),equalTo(false));
    }

    @Test
    public void equals_bothTypesExistAndEqual_returnsTrue() throws Exception {
        ClientGroup clientGroup1 = new ClientGroup();
        clientGroup1.setType("type");
        clientGroup.setType("type");
        assertThat("returns false",clientGroup.equals(clientGroup1),equalTo(true));
    }

    @Test
    public void equals_uniqueIdIsNullButOtherUniqueIdNotNull_returnsFalse() throws Exception {
        ClientGroup clientGroup1 = new ClientGroup();
        clientGroup1.setUniqueId("uniqueId");
        assertThat("returns false",clientGroup.equals(clientGroup1),equalTo(false));
    }

    @Test
    public void equals_bothUniqueIdsExistAndNotEqual_returnsFalse() throws Exception {
        ClientGroup clientGroup1 = new ClientGroup();
        clientGroup1.setUniqueId("uniqueId");
        clientGroup.setUniqueId("anotherUniqueId");
        assertThat("returns false",clientGroup.equals(clientGroup1),equalTo(false));
    }

    @Test
    public void equals_bothUniqueIdsExistAndEqual_returnsTrue() throws Exception {
        ClientGroup clientGroup1 = new ClientGroup();
        clientGroup1.setUniqueId("uniqueId");
        clientGroup.setUniqueId("uniqueId");
        assertThat("returns false",clientGroup.equals(clientGroup1),equalTo(true));
    }

    @Test
    public void writeReplace_returnsCorrectInstanceOfSerializationProxy() throws Exception {
        clientGroup.setClientId("clientId");
        clientGroup.setCustomerId("customerId");
        clientGroup.setName("name");
        clientGroup.setType("type");
        ClientGroup.SerializationProxy serializationProxy = (ClientGroup.SerializationProxy) clientGroup.writeReplace();
        assertThat("has correct values",clientGroup.equals(serializationProxy.readResolve()),equalTo(true));
    }

    @Test
    public void readObject_throwsInvalidObjectException() throws Exception {
        try{
            ObjectInputStream objectInputStream = mock(ObjectInputStream.class);
            clientGroup.readObject(objectInputStream);
            assertTrue("should throw exception",false);
        } catch (InvalidObjectException ex){
            assertThat("exception message",ex.getMessage(),equalTo("Serialization proxy is required."));
        }
    }
}
