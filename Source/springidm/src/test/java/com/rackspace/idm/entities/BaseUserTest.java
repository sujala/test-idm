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
        Permission perm = new Permission("customerId", "clientId", "permissionId", "value");
        List<Permission> perms = new ArrayList<Permission>();
        perms.add(perm);
        Role role = new Role("uniqueId", "name", "customerId", "country", "inum", "iname", "orgInum", "owner", RoleStatus.ACTIVE, "seeAlso", "type");
        role.setPermissions(perms);
        List<Role> roles = new ArrayList<Role>();
        roles.add(role);
        BaseUser bu = new BaseUser("username", "customerId", roles);
        byte[] serialized = SerializationUtils.serialize(bu);
        BaseUser dsbu = (BaseUser) SerializationUtils.deserialize(serialized);
        Assert.assertEquals(bu, dsbu);
    }
}
