package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.Role;
import com.rackspace.api.idm.v1.RoleList;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.TenantRole;
import org.junit.Before;
import org.junit.Test;

import javax.xml.bind.JAXBElement;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/12/12
 * Time: 9:20 AM
 * To change this template use File | Settings | File Templates.
 */
public class RolesConverterTest {

    RolesConverter rolesConverter;
    Role role;
    ClientRole clientRole;
    TenantRole tenantRole;


    @Before
    public void setUp() throws Exception {
        rolesConverter = new RolesConverter();
        role = new Role();
        clientRole = new ClientRole();
        tenantRole = new TenantRole();
    }

    @Test
    public void toRoleJaxbFromClientRole_withNullClientRolesList_returnsNewRoleList() throws Exception {
        JAXBElement<RoleList> roleListJAXBElement = rolesConverter.toRoleJaxbFromClientRole((List<ClientRole>) null);
        assertThat("roles list total records", roleListJAXBElement.getValue().getTotalRecords(), nullValue());
    }

    @Test
    public void toRoleJaxbFromClientRole_withEmptyClientRolesList_returnsEmptyRoleList() throws Exception {
        JAXBElement<RoleList> roleListJAXBElement = rolesConverter.toRoleJaxbFromClientRole(new ArrayList<ClientRole>());
        assertThat("roles list total records", roleListJAXBElement.getValue().getTotalRecords(), equalTo(0));
    }

    @Test
    public void toRoleJaxbFromClientRole_withClientRolesList_returnsRoleList_withCorrectSize() throws Exception {
        ArrayList<ClientRole> clientRoles = new ArrayList<ClientRole>();
        clientRoles.add(new ClientRole());
        clientRoles.add(new ClientRole());
        JAXBElement<RoleList> roleListJAXBElement = rolesConverter.toRoleJaxbFromClientRole(clientRoles);
        assertThat("roles list size", roleListJAXBElement.getValue().getRole().size(), equalTo(2));
        assertThat("roles list total records", roleListJAXBElement.getValue().getTotalRecords(), equalTo(2));
    }

    @Test
    public void toRoleJaxbFromTenantRole_withNullTenantRolesList_returnsNewRoleList() throws Exception {
        JAXBElement<RoleList> roleListJAXBElement = rolesConverter.toRoleJaxbFromTenantRole((List<TenantRole>) null);
        assertThat("role list total records", roleListJAXBElement.getValue().getTotalRecords(), nullValue());
    }

    @Test
    public void toRoleJaxbFromTenantRole_withEmptyTenantRolesList_returnsEmptyRoleList() throws Exception {
        JAXBElement<RoleList> roleListJAXBElement = rolesConverter.toRoleJaxbFromTenantRole(new ArrayList<TenantRole>());
        assertThat("role list total records", roleListJAXBElement.getValue().getTotalRecords(), equalTo(0));
    }

    @Test
    public void toRoleJaxbFromTenantRole_withTenantRolesList_returnsRoleList_withCorrectSize() throws Exception {
        ArrayList<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        tenantRoles.add(new TenantRole());
        tenantRoles.add(new TenantRole());
        JAXBElement<RoleList> roleListJAXBElement = rolesConverter.toRoleJaxbFromTenantRole(tenantRoles);
        assertThat("role list total records", roleListJAXBElement.getValue().getTotalRecords(), equalTo(2));
        assertThat("role list size", roleListJAXBElement.getValue().getRole().size(), equalTo(2));
    }

    @Test
    public void toRoleJaxbFromRoleString_withNullRoleStringList_returnsNewRoleList() throws Exception {
        JAXBElement<RoleList> roleListJAXBElement = rolesConverter.toRoleJaxbFromRoleString((List<String>) null);
        assertThat("role list total records", roleListJAXBElement.getValue().getTotalRecords(), nullValue());
    }

    @Test
    public void toRoleJaxbFromRoleString_withEmptyRoleStringList_returnsEmptyRoleList() throws Exception {
        JAXBElement<RoleList> roleListJAXBElement = rolesConverter.toRoleJaxbFromRoleString(new ArrayList<String>());
        assertThat("role list total records", roleListJAXBElement.getValue().getTotalRecords(), equalTo(0));
    }

    @Test
    public void toRoleJaxbFromRoleString_withRoleStringList_returnsRoleList_withCorrectSize() throws Exception {
        ArrayList<String> roleStrings = new ArrayList<String>();
        roleStrings.add(new String());
        roleStrings.add(new String());
        JAXBElement<RoleList> roleListJAXBElement = rolesConverter.toRoleJaxbFromRoleString(roleStrings);
        assertThat("role list total records", roleListJAXBElement.getValue().getTotalRecords(), equalTo(2));
        assertThat("role list size", roleListJAXBElement.getValue().getRole().size(), equalTo(2));
    }

    @Test
    public void toClientRole_withJaxbRole_setsName() throws Exception {
        role.setName("roleName");
        ClientRole clientRole = rolesConverter.toClientRole(role);
        assertThat("client role name", clientRole.getName(), equalTo("roleName"));
    }

    @Test
    public void toClientRole_withJaxbRole_setsClientId() throws Exception {
        role.setApplicationId("applicationId");
        ClientRole clientRole = rolesConverter.toClientRole(role);
        assertThat("client role client id", clientRole.getClientId(), equalTo("applicationId"));
    }

    @Test
    public void toClientRole_withJaxbRole_setsId() throws Exception {
        role.setId("roleId");
        ClientRole clientRole = rolesConverter.toClientRole(role);
        assertThat("client role id", clientRole.getId(), equalTo("roleId"));
    }

    @Test
    public void toClientRole_withJaxbRole_setsDescription() throws Exception {
        role.setDescription("roleDescription");
        ClientRole clientRole = rolesConverter.toClientRole(role);
        assertThat("client role description", clientRole.getDescription(), equalTo("roleDescription"));
    }

    @Test
    public void toRoleJaxb_withClientRole_setsId() throws Exception {
        clientRole.setId("clientRoleId");
        Role jaxbRole = rolesConverter.toRoleJaxb(clientRole);
        assertThat("role id", jaxbRole.getId(), equalTo("clientRoleId"));
    }

    @Test
    public void toRoleJaxb_withClientRole_setsApplicationId() throws Exception {
        clientRole.setClientId("applicationId");
        Role jaxbRole = rolesConverter.toRoleJaxb(clientRole);
        assertThat("role application id", jaxbRole.getApplicationId(), equalTo("applicationId"));
    }

    @Test
    public void toRoleJaxb_withClientRole_setsName() throws Exception {
        clientRole.setName("clientRoleName");
        Role jaxbRole = rolesConverter.toRoleJaxb(clientRole);
        assertThat("role Name", jaxbRole.getName(), equalTo("clientRoleName"));
    }

    @Test
    public void toRoleJaxb_withClientRole_setsDescription() throws Exception {
        clientRole.setDescription("clientRoleDescription");
        Role jaxbRole = rolesConverter.toRoleJaxb(clientRole);
        assertThat("role description", jaxbRole.getDescription(), equalTo("clientRoleDescription"));
    }

    @Test
    public void toRoleJaxbFromTenantRole_withTenantIds_AddsJaxbRoleWithTenantId() throws Exception {
        tenantRole.addTenantId("tenantId");
        List<Role> roles = rolesConverter.toRoleJaxbFromTenantRole(tenantRole);
        assertThat("roles size", roles.size(), equalTo(1));
        assertThat("roles tenant id", roles.get(0).getTenantId(), equalTo("tenantId"));
    }

    @Test
    public void toRoleJaxbFromTenantRole_withZeroTenantIds_returnsEmptyRoleList() throws Exception { //Is this the correct behavior?
        tenantRole.setTenantIds(new String[0]);
        List<Role> roles = rolesConverter.toRoleJaxbFromTenantRole(tenantRole);
        assertThat("roles size", roles.size(), equalTo(0));
    }

    @Test
    public void toRoleJaxbFromTenantRole_withNoTenantIds_AddsJaxbRoleWithOutTenantId() throws Exception {
        tenantRole.setClientId("clientId");
        List<Role> roles = rolesConverter.toRoleJaxbFromTenantRole(tenantRole);
        assertThat("roles size", roles.size(), equalTo(1));
        assertThat("roles application id", roles.get(0).getApplicationId(), equalTo("clientId"));
    }

    @Test
    public void createJaxbRole_withTenantRole_setsName() throws Exception {
        tenantRole.setName("tenantRoleName");
        Role jaxbRole = rolesConverter.createJaxbRole(tenantRole);
        assertThat("jaxbRole name", jaxbRole.getName(), equalTo("tenantRoleName"));
    }

    @Test
    public void createJaxbRole_withTenantRole_setsId() throws Exception {
        tenantRole.setRoleRsId("roleId");
        Role jaxbRole = rolesConverter.createJaxbRole(tenantRole);
        assertThat("jaxbRole id", jaxbRole.getId(), equalTo("roleId"));
    }

    @Test
    public void createJaxbRole_withTenantRole_setsApplicationId() throws Exception {
        tenantRole.setClientId("clientId");
        Role jaxbRole = rolesConverter.createJaxbRole(tenantRole);
        assertThat("jaxbRole application id", jaxbRole.getApplicationId(), equalTo("clientId"));
    }
}
