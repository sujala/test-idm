package com.rackspace.idm.api.converter;

import com.rackspace.api.idm.v1.Tenants;
import com.rackspace.idm.domain.entity.Tenant;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/12/12
 * Time: 11:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class TenantConverterTest {
    Tenant tenantDo;
    TenantConverter tenantConverter;

    @Before
    public void setUp() throws Exception {
        tenantDo = new Tenant();
        tenantConverter = new TenantConverter();
    }

    @Test
    public void toTenant_withTenant_setsDescription() throws Exception {
        tenantDo.setDescription("tenantDescription");
        com.rackspace.api.idm.v1.Tenant tenant = tenantConverter.toTenant(tenantDo);
        assertThat("tenant description", tenant.getDescription(), equalTo("tenantDescription"));
    }

    @Test
    public void toTenant_withTenant_setsDisplayName() throws Exception {
        tenantDo.setDisplayName("tenantDisplayName");
        com.rackspace.api.idm.v1.Tenant tenant = tenantConverter.toTenant(tenantDo);
        assertThat("tenant display name", tenant.getDisplayName(), equalTo("tenantDisplayName"));
    }

    @Test
    public void toTenant_withTenant_setsEnabled() throws Exception {
        tenantDo.setEnabled(true);
        com.rackspace.api.idm.v1.Tenant tenant = tenantConverter.toTenant(tenantDo);
        assertThat("tenant enabled", tenant.isEnabled(), equalTo(true));
    }

    @Test
    public void toTenant_withTenant_setsId() throws Exception {
        tenantDo.setTenantId("tenantId");
        com.rackspace.api.idm.v1.Tenant tenant = tenantConverter.toTenant(tenantDo);
        assertThat("tenant Id", tenant.getId(), equalTo("tenantId"));
    }

    @Test
    public void toTenant_withTenant_setsName() throws Exception {
        tenantDo.setName("tenantName");
        com.rackspace.api.idm.v1.Tenant tenant = tenantConverter.toTenant(tenantDo);
        assertThat("tenant name", tenant.getName(), equalTo("tenantName"));
    }

    @Test
    public void toTenant_withTenant_withNullCreated_doesNotSetCreated() throws Exception {
        com.rackspace.api.idm.v1.Tenant tenant = tenantConverter.toTenant(tenantDo);
        assertThat("tenant created", tenant.getCreated(), nullValue());
    }

    @Test
    public void toTenant_withTenant_withCreated_setsCreated() throws Exception {
        tenantDo = mock(Tenant.class);
        when(tenantDo.getCreated()).thenReturn(new DateTime(1).toDate());
        com.rackspace.api.idm.v1.Tenant tenant = tenantConverter.toTenant(tenantDo);
        assertThat("tenant created", tenant.getCreated().toGregorianCalendar().getTimeInMillis(), equalTo(1L));
    }

    @Test
    public void toTenant_withTenant_withNullUpdated_doesNotSetUpdated() throws Exception {
        com.rackspace.api.idm.v1.Tenant tenant = tenantConverter.toTenant(tenantDo);
        assertThat("tenant updated", tenant.getUpdated(), nullValue());
    }

    @Test
    public void toTenant_withTenant_withUpdated_setsUpdated() throws Exception {
        tenantDo = mock(Tenant.class);
        when(tenantDo.getUpdated()).thenReturn(new DateTime(1).toDate());
        com.rackspace.api.idm.v1.Tenant tenant = tenantConverter.toTenant(tenantDo);
        assertThat("tenant updated", tenant.getUpdated().toGregorianCalendar().getTimeInMillis(), equalTo(1L));
    }

    @Test
    public void toTenantList_withTenantsList_returnsCorrectSize() throws Exception {
        List<Tenant> tenantList = new ArrayList<Tenant>();
        tenantList.add(new Tenant());
        tenantList.add(new Tenant());
        Tenants tenants = tenantConverter.toTenantList(tenantList);
        assertThat("tenants size", tenants.getTenant().size(), equalTo(2));
    }

    @Test
    public void toTenantList_withEmptyTenantsList_returnsCorrectSize() throws Exception {
        List<Tenant> tenantList = new ArrayList<Tenant>();
        Tenants tenants = tenantConverter.toTenantList(tenantList);
        assertThat("tenants size", tenants.getTenant().size(), equalTo(0));
    }

    @Test
    public void toTenantDO_withTenant_setsDescription() throws Exception {
        com.rackspace.api.idm.v1.Tenant tenant = new com.rackspace.api.idm.v1.Tenant();
        tenant.setDescription("tenantDescription");
        Tenant returnedTenant = tenantConverter.toTenantDO(tenant);
        assertThat("tenant description", returnedTenant.getDescription(), equalTo("tenantDescription"));
    }

    @Test
    public void toTenantDO_withTenant_setsDisplayName() throws Exception {
        com.rackspace.api.idm.v1.Tenant tenant = new com.rackspace.api.idm.v1.Tenant();
        tenant.setDisplayName("displayName");
        Tenant returnedTenant = tenantConverter.toTenantDO(tenant);
        assertThat("tenant display name", returnedTenant.getDisplayName(), equalTo("displayName"));
    }

    @Test
    public void toTenantDO_withTenant_setsEnabled() throws Exception {
        com.rackspace.api.idm.v1.Tenant tenant = new com.rackspace.api.idm.v1.Tenant();
        tenant.setEnabled(true);
        Tenant returnedTenant = tenantConverter.toTenantDO(tenant);
        assertThat("tenant enabled", returnedTenant.isEnabled(), equalTo(true));
    }

    @Test
    public void toTenantDO_withTenant_setsName() throws Exception {
        com.rackspace.api.idm.v1.Tenant tenant = new com.rackspace.api.idm.v1.Tenant();
        tenant.setName("name");
        Tenant returnedTenant = tenantConverter.toTenantDO(tenant);
        assertThat("tenant name", returnedTenant.getName(), equalTo("name"));
    }

    @Test
    public void toTenantDO_withTenant_setsTenantId() throws Exception {
        com.rackspace.api.idm.v1.Tenant tenant = new com.rackspace.api.idm.v1.Tenant();
        tenant.setId("tenantId");
        Tenant returnedTenant = tenantConverter.toTenantDO(tenant);
        assertThat("tenant id", returnedTenant.getTenantId(), equalTo("tenantId"));
    }
}
