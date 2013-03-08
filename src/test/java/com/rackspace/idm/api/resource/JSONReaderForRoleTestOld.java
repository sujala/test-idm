package com.rackspace.idm.api.resource;

import com.rackspace.api.idm.v1.Role;
import com.rackspace.idm.exception.BadRequestException;
import org.junit.Test;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/7/12
 * Time: 4:03 PM
 * To change this template use File | Settings | File Templates.
 */
public class JSONReaderForRoleTestOld {

    private String roleJSON = "{" +
            "   \"role\" : {" +
            "       \"id\" : \"roleId\"," +
            "       \"name\" : \"roleName\"," +
            "       \"tenantId\" : \"tenantId\"," +
            "       \"description\" : \"roleDescription\"," +
            "       \"applicationId\" : \"applicationId\"" +
            "   }" +
            "}";
    private String emptyRoleJSON = "{" +
            "   \"role\" : {" +
            "   }" +
            "}";

    @Test
    public void isReadable_withValidClass_returnsTrue() throws Exception {
        JSONReaderForRole jsonReaderForRole = new JSONReaderForRole();
        boolean readable = jsonReaderForRole.isReadable(Role.class, null, null, null);
        assertThat("readable", readable, equalTo(true));
    }

    @Test
    public void isReadable_withInvalidClass_returnsFalse() throws Exception {
        JSONReaderForRole jsonReaderForRole = new JSONReaderForRole();
        boolean readable = jsonReaderForRole.isReadable(Object.class, null, null, null);
        assertThat("readable", readable, equalTo(false));
    }

    @Test
    public void readFrom_withValidJSON_returnRoleObject() throws Exception {
        JSONReaderForRole jsonReaderForRole = new JSONReaderForRole();
        InputStream inputStream = new BufferedInputStream(new ByteArrayInputStream(roleJSON.getBytes()));
        Role role = jsonReaderForRole.readFrom(Role.class, null, null, null, null, inputStream);
        assertThat("role", role, is(Role.class));
    }

    @Test
    public void getRoleFromJSONString_withValidJSON_setsRoleId() throws Exception {
        Role roleFromJSONString = JSONReaderForRole.getRoleFromJSONString(roleJSON);
        assertThat("role id", roleFromJSONString.getId(), equalTo("roleId"));
    }

    @Test
    public void getRoleFromJSONString_withValidJSON_setsRoleName() throws Exception {
        Role roleFromJSONString = JSONReaderForRole.getRoleFromJSONString(roleJSON);
        assertThat("role name", roleFromJSONString.getName(), equalTo("roleName"));
    }

    @Test
    public void getRoleFromJSONString_withValidJSON_setsTenantId() throws Exception {
        Role roleFromJSONString = JSONReaderForRole.getRoleFromJSONString(roleJSON);
        assertThat("role tenant id", roleFromJSONString.getTenantId(), equalTo("tenantId"));
    }

    @Test
    public void getRoleFromJSONString_withValidJSON_setsRoleDescription() throws Exception {
        Role roleFromJSONString = JSONReaderForRole.getRoleFromJSONString(roleJSON);
        assertThat("role description", roleFromJSONString.getDescription(), equalTo("roleDescription"));
    }

    @Test
    public void getRoleFromJSONString_withValidJSON_setsApplicationId() throws Exception {
        Role roleFromJSONString = JSONReaderForRole.getRoleFromJSONString(roleJSON);
        assertThat("role application id", roleFromJSONString.getApplicationId(), equalTo("applicationId"));
    }

    @Test
    public void getRoleFromJSONString_withValidEmptyRoleJSON_setsNullRoleId() throws Exception {
        Role roleFromJSONString = JSONReaderForRole.getRoleFromJSONString(emptyRoleJSON);
        assertThat("role id", roleFromJSONString.getId(), nullValue());
    }

    @Test
    public void getRoleFromJSONString_withValidEmptyRoleJSON_setsNullRoleName() throws Exception {
        Role roleFromJSONString = JSONReaderForRole.getRoleFromJSONString(emptyRoleJSON);
        assertThat("role name", roleFromJSONString.getName(), nullValue());
    }

    @Test
    public void getRoleFromJSONString_withValidEmptyRoleJSON_setsNullTenantId() throws Exception {
        Role roleFromJSONString = JSONReaderForRole.getRoleFromJSONString(emptyRoleJSON);
        assertThat("role tenant id", roleFromJSONString.getTenantId(), nullValue());
    }

    @Test
    public void getRoleFromJSONString_withValidEmptyRoleJSON_setsNullRoleDescription() throws Exception {
        Role roleFromJSONString = JSONReaderForRole.getRoleFromJSONString(emptyRoleJSON);
        assertThat("role description", roleFromJSONString.getDescription(), nullValue());
    }

    @Test
    public void getRoleFromJSONString_withValidEmptyRoleJSON_setsNullApplicationId() throws Exception {
        Role roleFromJSONString = JSONReaderForRole.getRoleFromJSONString(emptyRoleJSON);
        assertThat("role application id", roleFromJSONString.getApplicationId(), nullValue());
    }

    @Test
    public void getRoleFromJSONSString_withEmptyJSON_returnsNewRoleObject() throws Exception {
        Role roleFromJSONString = JSONReaderForRole.getRoleFromJSONString("{ }");
        assertThat("role", roleFromJSONString, is(Role.class));
        assertThat("role id", roleFromJSONString.getId(), nullValue());
    }

    @Test(expected = BadRequestException.class)
    public void getRoleFromJSONString_withInvalidJSON_throwsBadRequestException() throws Exception {
        JSONReaderForRole.getRoleFromJSONString("Invalid JSON");
    }
}
