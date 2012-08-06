package com.rackspace.idm.api.resource.cloud.migration;

import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/5/12
 * Time: 11:07 AM
 * To change this template use File | Settings | File Templates.
 */
public class ObjectFactoryTest {
    private ObjectFactory objectFactory;

    @Before
    public void setUp() throws Exception {
        objectFactory = new ObjectFactory();
    }

    @Test
    public void createMigrateUserResponseType_returnsNewCreatedObject() throws Exception {
        MigrateUserResponseType result = objectFactory.createMigrateUserResponseType();
        assertThat("list", result.getUsers().isEmpty(), equalTo(true));
    }

    @Test
    public void createUserType_returnsNewCreatedObject() throws Exception {
        UserType result = objectFactory.createUserType();
        List<RoleType> roles = result.getRoles();
        assertThat("id", result.getId(), equalTo(null));
        assertThat("username", result.getUsername(), equalTo(null));
        assertThat("email", result.getEmail(), equalTo(null));
        assertThat("apiKey", result.getApiKey(), equalTo(null));
        assertThat("password", result.getPassword(), equalTo(null));
        assertThat("secret question", result.getSecretQuestion(), equalTo(null));
        assertThat("secret answer", result.getSecretAnswer(), equalTo(null));
        assertThat("comment", result.getComment(), equalTo(null));
        assertThat("boolean", result.isValid(), equalTo(null));
        assertThat("list", result.getRoles().isEmpty(), equalTo(true));
    }

    @Test
    public void createRoleType_returnsNewCreatedObject() throws Exception {
        RoleType result = objectFactory.createRoleType();
        assertThat("id", result.getId(), equalTo(null));
        assertThat("name", result.getName(), equalTo(null));
        assertThat("comment", result.getComment(), equalTo(null));
        assertThat("boolean", result.isValid(), equalTo(null));
    }

    @Test
    public void createGroupType_returnsNewCreatedObject() throws Exception {
        GroupType result = objectFactory.createGroupType();
        assertThat("id", result.getId(), equalTo(null));
        assertThat("name", result.getName(), equalTo(null));
        assertThat("comment", result.getComment(), equalTo(null));
        assertThat("boolean", result.isValid(), equalTo(null));
    }

    @Test
    public void createEndpointType_returnsNewCreatedObject() throws Exception {
        EndpointType result = objectFactory.createEndpointType();
        result.setTenantId(null);
        result.setType(null);
        result.setRegion(null);
        assertThat("name", result.getName(), equalTo(null));
        assertThat("type", result.getType(), equalTo(null));
        assertThat("region", result.getRegion(), equalTo(null));
        assertThat("comment", result.getComment(), equalTo(null));
        assertThat("boolean", result.isValid(), equalTo(null));
        assertThat("tenant id", result.getTenantId(), equalTo(null));
    }

    @Test
    public void createResponse_returnsNewCreatedObject() throws Exception {
        JAXBElement<MigrateUserResponseType> result = objectFactory.createResponse(objectFactory.createMigrateUserResponseType());
        assertThat("list", result.isNil(), equalTo(false));
    }
}
