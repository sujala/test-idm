package com.rackspace.idm.entities;

import org.apache.commons.lang.SerializationUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * User: john.eo
 * Date: 1/11/11
 * Time: 4:14 PM
 */
public class BaseClientTest {
    @Test
    public void shouldSerializeAndDeserialize() {
        Permission perm = new Permission("customerId", "clientId", "permissionId", "value");
        List<Permission> perms = new ArrayList<Permission>();
        perms.add(perm);
        BaseClient bc = new BaseClient("clientId", "customerId", perms);
        byte[] serialized = SerializationUtils.serialize(bc);
        BaseClient dsbc = (BaseClient) SerializationUtils.deserialize(serialized);
        Assert.assertEquals(bc, dsbc);
    }
}
