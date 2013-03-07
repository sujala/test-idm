package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.TenantRole;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.RoleList;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/15/12
 * Time: 9:24 AM
 * To change this template use File | Settings | File Templates.
 */
public class RoleConverterCloudV20TestOld {

    RoleConverterCloudV20 roleConverterCloudV20;

    @Before
    public void setUp() throws Exception {
        roleConverterCloudV20 = new RoleConverterCloudV20();
        roleConverterCloudV20.setObjFactories(new JAXBObjectFactories());
    }

    @Test
    public void toRoleListJaxb_rolesIsNull_returnsEmptyRoleList() throws Exception {

        assertThat("roleList", (ArrayList<Role>) roleConverterCloudV20.toRoleListJaxb(null).getRole(),equalTo(new ArrayList<Role>()));
    }

    @Test
    public void toRoleListJaxb_rolesIsEmpty_returnsEmptyRoleList() throws Exception {
        List<TenantRole> list = new ArrayList<TenantRole>();
        assertThat("roleList", (ArrayList<Role>) roleConverterCloudV20.toRoleListJaxb(list).getRole(),equalTo(new ArrayList<Role>()));
    }

    @Test
    public void toRoleListJaxb_roleTenantIdsExist_returnsRoleList() throws Exception {
        String[] ids = {"id1","id2"};
        TenantRole tenantRole = new TenantRole();
        tenantRole.setDescription("this is a description");
        tenantRole.setName("John Smith");
        tenantRole.setRoleRsId("123");
        tenantRole.setClientId("456");
        tenantRole.setTenantIds(ids);
        List<TenantRole> list = new ArrayList<TenantRole>();
        list.add(tenantRole);
        RoleList roleList = roleConverterCloudV20.toRoleListJaxb(list);

        assertThat("size", roleList.getRole().size(), equalTo(2));
        assertThat("roleDescritpion",roleList.getRole().get(0).getDescription(),equalTo("this is a description"));
        assertThat("roleDescritpion",roleList.getRole().get(1).getDescription(),equalTo("this is a description"));
        assertThat("roleName",roleList.getRole().get(0).getName(),equalTo("John Smith"));
        assertThat("roleName",roleList.getRole().get(1).getName(),equalTo("John Smith"));
        assertThat("roleId",roleList.getRole().get(0).getId(),equalTo("123"));
        assertThat("roleId",roleList.getRole().get(1).getId(),equalTo("123"));
        assertThat("roleServiceId",roleList.getRole().get(0).getServiceId(),equalTo("456"));
        assertThat("roleServiceId",roleList.getRole().get(1).getServiceId(),equalTo("456"));
        assertThat("roleTenantId",roleList.getRole().get(0).getTenantId(),equalTo("id1"));
        assertThat("roleServiceId",roleList.getRole().get(1).getTenantId(),equalTo("id2"));

    }

    @Test
    public void toRoleListJaxb_roleTenantIdsListEmpty_returnsRoleList() throws Exception {
        String[] ids = {};
        TenantRole tenantRole = new TenantRole();
        tenantRole.setDescription("this is a description");
        tenantRole.setName("John Smith");
        tenantRole.setRoleRsId("123");
        tenantRole.setClientId("456");
        tenantRole.setTenantIds(ids);
        List<TenantRole> list = new ArrayList<TenantRole>();
        list.add(tenantRole);
        RoleList roleList = roleConverterCloudV20.toRoleListJaxb(list);

        assertThat("size", roleList.getRole().size(), equalTo(1));
        assertThat("roleDescritpion",roleList.getRole().get(0).getDescription(),equalTo("this is a description"));
        assertThat("roleName",roleList.getRole().get(0).getName(),equalTo("John Smith"));
        assertThat("roleId",roleList.getRole().get(0).getId(),equalTo("123"));
        assertThat("roleServiceId",roleList.getRole().get(0).getServiceId(),equalTo("456"));
        assertThat("roleTenantId",roleList.getRole().get(0).getTenantId(),nullValue());

    }

    @Test
    public void toRoleListJaxb_roleTenantIdsNull_returnsRoleList() throws Exception {;
        TenantRole tenantRole = new TenantRole();
        tenantRole.setDescription("this is a description");
        tenantRole.setName("John Smith");
        tenantRole.setRoleRsId("123");
        tenantRole.setClientId("456");
        List<TenantRole> list = new ArrayList<TenantRole>();
        list.add(tenantRole);
        RoleList roleList = roleConverterCloudV20.toRoleListJaxb(list);

        assertThat("size", roleList.getRole().size(), equalTo(1));
        assertThat("roleDescritpion",roleList.getRole().get(0).getDescription(),equalTo("this is a description"));
        assertThat("roleName",roleList.getRole().get(0).getName(),equalTo("John Smith"));
        assertThat("roleId",roleList.getRole().get(0).getId(),equalTo("123"));
        assertThat("roleServiceId",roleList.getRole().get(0).getServiceId(),equalTo("456"));
        assertThat("roleTenantId",roleList.getRole().get(0).getTenantId(),nullValue());

    }

    @Test
    public void toRoleListFromClientRoles_listIsNull_returnsEmptyRoleList() throws Exception {
        assertThat("roleList", (ArrayList<Role>) roleConverterCloudV20.toRoleListFromClientRoles(null).getRole(),equalTo(new ArrayList<Role>()));
    }

    @Test
    public void toRoleListFromClientRoles_listIsEmpty_returnsEmptyRoleList() throws Exception {
        List<ClientRole> list = new ArrayList<ClientRole>();
        assertThat("roleList", (ArrayList<Role>) roleConverterCloudV20.toRoleListFromClientRoles(list).getRole(),equalTo(new ArrayList<Role>()));
    }

    @Test
    public void toRoleListFromClientRoles_listIsPopulated_returnsRoleList() throws Exception {
        ClientRole clientRole = new ClientRole();
        clientRole.setDescription("this is a description");
        clientRole.setName("John Smith");
        clientRole.setId("123");
        clientRole.setClientId("456");
        List<ClientRole> list = new ArrayList<ClientRole>();
        list.add(clientRole);
        RoleList roleList = roleConverterCloudV20.toRoleListFromClientRoles(list);

        assertThat("size", roleList.getRole().size(), equalTo(1));
        assertThat("roleDescription",roleList.getRole().get(0).getDescription(),equalTo("this is a description"));
        assertThat("roleName",roleList.getRole().get(0).getName(),equalTo("John Smith"));
        assertThat("roleId",roleList.getRole().get(0).getId(),equalTo("123"));
        assertThat("roleServiceId",roleList.getRole().get(0).getServiceId(),equalTo("456"));

    }

    @Test (expected = IllegalArgumentException.class)
    public void toRole_nullInput_throwsIllegalArgumentException() throws Exception {
        roleConverterCloudV20.toRole(null);
    }

    @Test
    public void toRole_TenantRoleExists_returnsRole() throws Exception {
        TenantRole tenantRole = new TenantRole();
        tenantRole.setDescription("this is a description");
        tenantRole.setRoleRsId("123");
        Role role = roleConverterCloudV20.toRole(tenantRole);

        assertThat("roleDescription",role.getDescription(),equalTo("this is a description"));
        assertThat("roleId",role.getId(),equalTo("123"));
    }

    @Test (expected = IllegalArgumentException.class)
    public void roleFromClientRole_nullInput_throwsIllegalArgumentException() throws Exception {
       roleConverterCloudV20.toRoleFromClientRole(null);
    }

    @Test
    public void roleFromClientRole_ClientRoleExists_returnsRole() throws Exception {
        ClientRole clientRole = new ClientRole();
        clientRole.setDescription("this is a description");
        clientRole.setId("123");
        Role role = roleConverterCloudV20.toRoleFromClientRole(clientRole);

        assertThat("roleDescription",role.getDescription(),equalTo("this is a description"));
        assertThat("roleId", role.getId(), equalTo("123"));
    }
}
