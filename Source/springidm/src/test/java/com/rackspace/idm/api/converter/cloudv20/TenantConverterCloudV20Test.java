package com.rackspace.idm.api.converter.cloudv20;

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.identity.api.v2.Tenant;
import org.openstack.docs.identity.api.v2.Tenants;

import javax.xml.datatype.DatatypeFactory;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 6/15/12
 * Time: 10:09 AM
 * To change this template use File | Settings | File Templates.
 */
public class TenantConverterCloudV20Test {
    private TenantConverterCloudV20 tenantConverterCloudV20;
    private JAXBObjectFactories jaxbObjectFactories;

    @Before
    public void setUp() throws Exception {
        tenantConverterCloudV20 = new TenantConverterCloudV20();
        jaxbObjectFactories = new JAXBObjectFactories();
        tenantConverterCloudV20.setObjFactories(jaxbObjectFactories);
    }

    @Test
    public void toTenant_tenantCreatedIsNullAndUpdatedIsNull_returnsTenant() throws Exception {
        com.rackspace.idm.domain.entity.Tenant tenant = new com.rackspace.idm.domain.entity.Tenant();
        tenant.setDescription("description");
        tenant.setDisplayName("displayName");
        tenant.setEnabled(true);
        tenant.setTenantId("tenantId");
        tenant.setName("name");
        Tenant jaxbTenant = tenantConverterCloudV20.toTenant(tenant);
        assertThat("description", jaxbTenant.getDescription(), equalTo("description"));
        assertThat("display name", jaxbTenant.getDisplayName(), equalTo("displayName"));
        assertThat("enabled", jaxbTenant.isEnabled(), equalTo(true));
        assertThat("id", jaxbTenant.getId(), equalTo("tenantId"));
        assertThat("name", jaxbTenant.getName(), equalTo("name"));
    }

    @Test
    public void toTenant_tenantCreatedNotNullAndUpdatedIsNull_returnsTenant() throws Exception {
        com.rackspace.idm.domain.entity.Tenant tenant = new com.rackspace.idm.domain.entity.Tenant();
        tenant.setDescription("description");
        tenant.setDisplayName("displayName");
        tenant.setEnabled(true);
        tenant.setTenantId("tenantId");
        tenant.setName("name");
        Date date = new Date(3000, 1, 1);
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        tenant.setCreated(date);
        Tenant jaxbTenant = tenantConverterCloudV20.toTenant(tenant);
        assertThat("description", jaxbTenant.getDescription(), equalTo("description"));
        assertThat("display name", jaxbTenant.getDisplayName(), equalTo("displayName"));
        assertThat("enabled", jaxbTenant.isEnabled(), equalTo(true));
        assertThat("id", jaxbTenant.getId(), equalTo("tenantId"));
        assertThat("name", jaxbTenant.getName(), equalTo("name"));
        assertThat("created", jaxbTenant.getCreated(), equalTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(gc)));
    }

    @Test
    public void toTenant_tenantCreatedNullAndUpdateNotNull_returnsTenant() throws Exception {
        com.rackspace.idm.domain.entity.Tenant tenant = new com.rackspace.idm.domain.entity.Tenant();
        tenant.setDescription("description");
        tenant.setDisplayName("displayName");
        tenant.setEnabled(true);
        tenant.setTenantId("tenantId");
        tenant.setName("name");
        Date date = new Date(3000, 1, 1);
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        tenant.setUpdated(date);
        Tenant jaxbTenant = tenantConverterCloudV20.toTenant(tenant);
        assertThat("description", jaxbTenant.getDescription(), equalTo("description"));
        assertThat("display name", jaxbTenant.getDisplayName(), equalTo("displayName"));
        assertThat("enabled", jaxbTenant.isEnabled(), equalTo(true));
        assertThat("id", jaxbTenant.getId(), equalTo("tenantId"));
        assertThat("name", jaxbTenant.getName(), equalTo("name"));
        assertThat("updated", jaxbTenant.getUpdated(), equalTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(gc)));
    }

    @Test
    public void toTenant_tenantCreatedNotNullAndUpdateNotNulll_returnsTenant() throws Exception {
        com.rackspace.idm.domain.entity.Tenant tenant = new com.rackspace.idm.domain.entity.Tenant();
        tenant.setDescription("description");
        tenant.setDisplayName("displayName");
        tenant.setEnabled(true);
        tenant.setTenantId("tenantId");
        tenant.setName("name");
        Date date = new Date(3000, 1, 1);
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(date);
        tenant.setUpdated(date);
        tenant.setCreated(date);
        Tenant jaxbTenant = tenantConverterCloudV20.toTenant(tenant);
        assertThat("description", jaxbTenant.getDescription(), equalTo("description"));
        assertThat("display name", jaxbTenant.getDisplayName(), equalTo("displayName"));
        assertThat("enabled", jaxbTenant.isEnabled(), equalTo(true));
        assertThat("id", jaxbTenant.getId(), equalTo("tenantId"));
        assertThat("name", jaxbTenant.getName(), equalTo("name"));
        assertThat("created", jaxbTenant.getCreated(), equalTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(gc)));
        assertThat("updated", jaxbTenant.getUpdated(), equalTo(DatatypeFactory.newInstance().newXMLGregorianCalendar(gc)));
    }

    @Test
    public void toTenantList_tenantsSizeMoreThanZero_returnsCorrectInfo() throws Exception {
        List<com.rackspace.idm.domain.entity.Tenant> tenants = new ArrayList<com.rackspace.idm.domain.entity.Tenant>();
        com.rackspace.idm.domain.entity.Tenant tenant = new com.rackspace.idm.domain.entity.Tenant();
        tenant.setName("name");
        tenants.add(tenant);
        Tenants jaxbTenants = tenantConverterCloudV20.toTenantList(tenants);
        assertThat("name", jaxbTenants.getTenant().get(0).getName(), equalTo("name"));
    }

    @Test
    public void toTenantList_tenantSizeIsZero_returnsEmptyTenants() throws Exception {
        List<com.rackspace.idm.domain.entity.Tenant> tenants = new ArrayList<com.rackspace.idm.domain.entity.Tenant>();
        Tenants jaxbTenants = tenantConverterCloudV20.toTenantList(tenants);
        assertThat("size", jaxbTenants.getTenant().size(), equalTo(0));
    }

    @Test
    public void toTenantDO_createsTenant_returnsCorrectInfo() throws Exception {
        Tenant jaxbTenant = new Tenant();
        jaxbTenant.setDescription("description");
        jaxbTenant.setDisplayName("displayName");
        jaxbTenant.setEnabled(true);
        jaxbTenant.setName("name");
        jaxbTenant.setId("id");
        com.rackspace.idm.domain.entity.Tenant tenant = tenantConverterCloudV20.toTenantDO(jaxbTenant);
        assertThat("description", tenant.getDescription(), equalTo("description"));
        assertThat("display name", tenant.getDisplayName(), equalTo("displayName"));
        assertThat("enabled", tenant.isEnabled(), equalTo(true));
        assertThat("id", tenant.getTenantId(), equalTo("id"));
        assertThat("name", tenant.getName(), equalTo("name"));
    }
}
