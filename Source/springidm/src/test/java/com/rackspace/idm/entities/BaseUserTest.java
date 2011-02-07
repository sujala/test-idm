package com.rackspace.idm.entities;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * User: john.eo
 * Date: 1/11/11
 * Time: 4:34 PM
 */
public class BaseUserTest {
    @Test
    public void shouldSerializeAndDeserialzie() {

        ClientGroup group = new ClientGroup("clientId", "customerId", "groupName");
        List<ClientGroup> groups = new ArrayList<ClientGroup>();
        groups.add(group);
        BaseUser bu = new BaseUser("username", "customerId", groups);
        byte[] serialized = SerializationUtils.serialize(bu);
        BaseUser dsbu = (BaseUser) SerializationUtils.deserialize(serialized);
        Assert.assertEquals(bu, dsbu);
    }
}
