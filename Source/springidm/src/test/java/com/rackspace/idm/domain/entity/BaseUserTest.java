package com.rackspace.idm.domain.entity;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

/**
 * User: john.eo
 * Date: 1/11/11
 * Time: 4:34 PM
 */
public class BaseUserTest {
    @Test
    public void shouldSerializeAndDeserialzie() {

        BaseUser bu = new BaseUser("username", "customerId");
        byte[] serialized = SerializationUtils.serialize(bu);
        BaseUser dsbu = (BaseUser) SerializationUtils.deserialize(serialized);
        Assert.assertEquals(bu, dsbu);
    }
}
