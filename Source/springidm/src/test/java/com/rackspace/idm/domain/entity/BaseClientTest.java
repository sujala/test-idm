package com.rackspace.idm.domain.entity;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * User: john.eo
 * Date: 1/11/11
 * Time: 4:14 PM
 */
public class BaseClientTest {
    @Test
    public void shouldSerializeAndDeserialize() {
        BaseClient bc = new BaseClient("clientId", "customerId");
        byte[] serialized = SerializationUtils.serialize(bc);
        BaseClient dsbc = (BaseClient) SerializationUtils.deserialize(serialized);
        Assert.assertEquals(bc, dsbc);
    }
}
