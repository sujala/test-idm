package com.rackspace.idm.api.resource.tenant;

import com.rackspace.api.idm.v1.Tenant;
import com.rackspace.idm.api.converter.TenantConverter;
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService;
import com.rackspace.idm.domain.service.impl.DefaultScopeAccessService;
import com.rackspace.idm.domain.service.impl.DefaultTenantService;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.InputValidator;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.WebApplicationException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: ryan5034
 * Date: 6/15/12
 * Time: 11:00 AM
 * To change this template use File | Settings | File Templates.
 */
public class TenantsResourceTest {

    TenantsResource tenantsResource;
    DefaultAuthorizationService authorizationService = mock(DefaultAuthorizationService.class);
    DefaultScopeAccessService scopeAccessService = mock(DefaultScopeAccessService.class);
    DefaultTenantService tenantService = mock(DefaultTenantService.class);
    TenantConverter tenantConverter = mock(TenantConverter.class);
    TenantsResource spy;

    @Before
    public void setUp() throws Exception {
        tenantsResource = new TenantsResource(new InputValidator(),authorizationService,scopeAccessService,tenantService,
                tenantConverter);
        spy = spy(tenantsResource);
    }

    @Test
    public void createTenant_callsScopeAccessMethod() throws Exception {
        Tenant tenant = new Tenant();
        doNothing().when(spy).validateTenantId(null);
        doNothing().when(spy).updateTenantFields(tenant, null);
        when(tenantService.getTenant(null)).thenReturn(new com.rackspace.idm.domain.entity.Tenant());
        spy.createTenant(null, null, null, tenant);
        verify(scopeAccessService).getAccessTokenByAuthHeader(null);

    }

    @Test
    public void createTenant_callsAuthorizationServiceMethod() throws Exception {
        Tenant tenant = new Tenant();
        doNothing().when(spy).validateTenantId(null);
        doNothing().when(spy).updateTenantFields(tenant, null);
        when(tenantService.getTenant(null)).thenReturn(new com.rackspace.idm.domain.entity.Tenant());
        spy.createTenant(null, null, null, tenant);
        verify(authorizationService).authorizeIdmSuperAdminOrRackspaceClient(null);

    }

    @Test
    public void createTenant_callsTenantConverterMethod() throws Exception {
        Tenant tenant = new Tenant();
        doNothing().when(spy).validateTenantId(null);
        doNothing().when(spy).updateTenantFields(tenant, null);
        when(tenantService.getTenant(null)).thenReturn(new com.rackspace.idm.domain.entity.Tenant());
        spy.createTenant(null, null, null, tenant);
        verify(tenantConverter).toTenantDO(tenant);

    }

    @Test
    public void createTenant_callsTenantServiceMethodAdd() throws Exception {
        Tenant tenant1 = new Tenant();
        com.rackspace.idm.domain.entity.Tenant tenant2 = new com.rackspace.idm.domain.entity.Tenant();
        doNothing().when(spy).validateTenantId(null);
        doNothing().when(spy).updateTenantFields(tenant1, null);
        when(tenantService.getTenant(null)).thenReturn(tenant2);
        spy.createTenant(null, null, null, tenant1);
        verify(tenantService).addTenant(null);

    }

    @Test
    public void createTenant_callsTenantServiceMethodGet() throws Exception {
        Tenant tenant1 = new Tenant();
        com.rackspace.idm.domain.entity.Tenant tenant2 = new com.rackspace.idm.domain.entity.Tenant();
        doNothing().when(spy).validateTenantId(null);
        doNothing().when(spy).updateTenantFields(tenant1, null);
        when(tenantService.getTenant(null)).thenReturn(tenant2);
        spy.createTenant(null, null, null, tenant1);
        verify(tenantService).getTenant(null);

    }

    @Test
    public void createTenant_callsTenantConverterMethodTo() throws Exception {
        Tenant tenant1 = new Tenant();
        com.rackspace.idm.domain.entity.Tenant tenant2 = new com.rackspace.idm.domain.entity.Tenant();
        doNothing().when(spy).validateTenantId(null);
        doNothing().when(spy).updateTenantFields(tenant1, null);
        when(tenantService.getTenant(null)).thenReturn(tenant2);
        spy.createTenant(null, null, null, tenant1);
        verify(tenantConverter).toTenant(tenant2);

    }

    @Test
    public void createTenant_returns201Status() throws Exception {
        Tenant tenant = new Tenant();
        doNothing().when(spy).validateTenantId(null);
        doNothing().when(spy).updateTenantFields(tenant, null);
        when(tenantService.getTenant(null)).thenReturn(new com.rackspace.idm.domain.entity.Tenant());
        assertThat("response status", spy.createTenant(null, null, null, tenant).getStatus(),equalTo(201));

    }

    @Test (expected = IllegalArgumentException.class)
    public void createTenant_nullTenant_throwsIllegalArgumentException() throws Exception {

        tenantsResource.createTenant(null,null,null,null);
    }

    @Test (expected = WebApplicationException.class)
    public void getTenant_nullTenant_throwsWebApplicationException() throws Exception {
        tenantsResource.getTenant(null,null,null,null);
    }

    @Test
    public void getTenant_notNullTenant_returns201Status() throws Exception {
        when(tenantService.getTenant(null)).thenReturn(new com.rackspace.idm.domain.entity.Tenant());
        assertThat("response status", tenantsResource.getTenant(null, null, null, null).getStatus(), equalTo(200));
    }

    @Test
    public void getTenant_notNullTenant_callsScopeAccessServiceMethod() throws Exception {
        when(tenantService.getTenant(null)).thenReturn(new com.rackspace.idm.domain.entity.Tenant());
        tenantsResource.getTenant(null, null, null, null);
        verify(scopeAccessService).getAccessTokenByAuthHeader(null);
    }

    @Test
    public void getTenant_notNullTenant_callsAuthorizationServiceMethod() throws Exception {
        when(tenantService.getTenant(null)).thenReturn(new com.rackspace.idm.domain.entity.Tenant());
        tenantsResource.getTenant(null,null,null,null);
        verify(authorizationService).authorizeIdmSuperAdminOrRackspaceClient(null);
    }

    @Test
    public void getTenant_notNullTenant_callsTenantServiceMethod() throws Exception {
        when(tenantService.getTenant(null)).thenReturn(new com.rackspace.idm.domain.entity.Tenant());
        tenantsResource.getTenant(null,null,null,null);
        verify(tenantService).getTenant(null);
    }

    @Test
    public void getTenant_notNullTenant_callsTenantConverterMethod() throws Exception {
        com.rackspace.idm.domain.entity.Tenant tenant = new com.rackspace.idm.domain.entity.Tenant();
        when(tenantService.getTenant(null)).thenReturn(tenant);
        tenantsResource.getTenant(null,null,null,null);
        verify(tenantConverter).toTenant(tenant);
    }

    @Test
    public void deleteTenant_returns204() throws Exception {
        assertThat("response status", tenantsResource.deleteTenant(null, null, null, null).getStatus(), equalTo(204));
    }

    @Test
    public void deleteTenant_callsScopeAccessServiceMethod() throws Exception {
        tenantsResource.deleteTenant(null,null,null,null);
        verify(scopeAccessService).getAccessTokenByAuthHeader(null);

    }

    @Test
    public void deleteTenant_callsAuthorizationServiceMethod() throws Exception {
        tenantsResource.deleteTenant(null,null,null,null);
        verify(authorizationService).authorizeIdmSuperAdminOrRackspaceClient(null);

    }

    @Test
    public void deleteTenant_callsTenantServiceMethod() throws Exception {
        tenantsResource.deleteTenant(null,null,null,null);
        verify(tenantService).deleteTenant(null);

    }

    @Test (expected = IllegalArgumentException.class)
    public void updateTenant_nullTenant_throwsIllegalArgumentException() throws Exception {
        tenantsResource.updateTenant(null, null, null, null, null);
    }

    @Test
    public void updateTenant_tenantNotNull_returns200() throws Exception {
        doNothing().when(spy).updateTenantFields(null,null);
        doReturn(new com.rackspace.idm.domain.entity.Tenant()).when(spy).checkAndGetTenant(null);
        assertThat("response status", spy.updateTenant(null, null, null, null, new Tenant()).getStatus(), equalTo(200));
    }

    @Test
    public void updateTenant_tenantNotNull_callsScopeAccessServiceMethod() throws Exception {
        doNothing().when(spy).updateTenantFields(null,null);
        doReturn(new com.rackspace.idm.domain.entity.Tenant()).when(spy).checkAndGetTenant(null);
        spy.updateTenant(null, null, null, null, new Tenant());
        verify(scopeAccessService).getAccessTokenByAuthHeader(null);
    }

    @Test
    public void updateTenant_tenantNotNull_callsAuthorizationServiceMethod() throws Exception {
        doNothing().when(spy).updateTenantFields(null,null);
        doReturn(new com.rackspace.idm.domain.entity.Tenant()).when(spy).checkAndGetTenant(null);
        spy.updateTenant(null, null, null, null, new Tenant());
        verify(authorizationService).authorizeIdmSuperAdminOrRackspaceClient(null);
    }

    @Test
    public void updateTenant_tenantNotNull_setsTenantFields() throws Exception {
        Tenant tenant1 = new Tenant();
        tenant1.setDescription("this is a description");
        tenant1.setDisplayName("john");
        tenant1.setEnabled(false);
        com.rackspace.idm.domain.entity.Tenant tenant = new com.rackspace.idm.domain.entity.Tenant();
        doNothing().when(spy).updateTenantFields(null,null);
        doReturn(tenant).when(spy).checkAndGetTenant(null);
        spy.updateTenant(null, null, null, null, tenant1);
        assertThat("tenantDescription",tenant.getDescription(),equalTo("this is a description"));
        assertThat("tenantDisplayName",tenant.getDisplayName(),equalTo("john"));
        assertThat("tenantEnabled",tenant.isEnabled(),equalTo(false));
    }

    @Test
    public void updateTenant_tenantNotNull_callsTenantServiceMethodUpdate() throws Exception {
        com.rackspace.idm.domain.entity.Tenant tenant = new com.rackspace.idm.domain.entity.Tenant();
        doNothing().when(spy).updateTenantFields(null,null);
        doReturn(tenant).when(spy).checkAndGetTenant(null);
        spy.updateTenant(null, null, null, null, new Tenant());
        verify(tenantService).updateTenant(tenant);
    }

    @Test
    public void updateTenant_tenantNotNull_callsTenantServiceMethodGet() throws Exception {
        com.rackspace.idm.domain.entity.Tenant tenant = new com.rackspace.idm.domain.entity.Tenant();
        doNothing().when(spy).updateTenantFields(null,null);
        doReturn(tenant).when(spy).checkAndGetTenant(null);
        spy.updateTenant(null, null, null, null, new Tenant());
        verify(tenantService).getTenant(null);
    }

    @Test
    public void updateTenant_tenantNotNull_callsTenantConverterMethod() throws Exception {
        com.rackspace.idm.domain.entity.Tenant tenant = new com.rackspace.idm.domain.entity.Tenant();
        doNothing().when(spy).updateTenantFields(null,null);
        doReturn(tenant).when(spy).checkAndGetTenant(null);
        spy.updateTenant(null, null, null, null, new Tenant());
        verify(tenantConverter).toTenant(null);
    }

    @Test (expected = WebApplicationException.class)
    public void validateTenantId_nullTenantId_throwsWebApplicationException() throws Exception {
        tenantsResource.validateTenantId(null);
    }

    @Test (expected = WebApplicationException.class)
    public void validateTenantId_indexDoesNotExistAndNamespaceNullAndIndexNull_throwsWebApplicationException() throws Exception {
        tenantsResource.validateTenantId("string");
    }

    @Test (expected = WebApplicationException.class)
    public void validateTenantId_indexExistsNamespaceEmptyIdEmpty_throwsWebApplicationException() throws Exception {
        tenantsResource.validateTenantId(":");
    }

    @Test (expected = WebApplicationException.class)
    public void validateTenantId_indexDoesExistAndNamespaceEmptyAndIdExists_throwsWebApplicationException() throws Exception {
        tenantsResource.validateTenantId(":string");
    }

    @Test (expected = WebApplicationException.class)
    public void validateTenantId_indexDoesExistAndNamespaceExistsAndIdEmpty_throwsWebApplicationException() throws Exception {
        tenantsResource.validateTenantId("string:");
    }

    @Test
    public void validateTenantId_indexDoesExistAndNamespaceExistsAndIdExists_doNothing() throws Exception {
        tenantsResource.validateTenantId("namespace:id");
    }

    @Test
    public void updateTenantFields_descriptionNullAndDisplayNameNull_returnsTenant() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId("id");
        tenant.setName("name");
        tenantsResource.updateTenantFields(tenant,null);

        assertThat("tenantId",tenant.getId(),nullValue());
        assertThat("tenantName",tenant.getName(),nullValue());
        assertThat("tenantDescription",tenant.getDescription(),nullValue());
        assertThat("tenantDisplayName",tenant.getDisplayName(),nullValue());
    }

    @Test
    public void updateTenantFields_descriptionEmptyAndDisplayNameNull_returnsTenant() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId("id");
        tenant.setName("name");
        tenant.setDescription("");
        tenantsResource.updateTenantFields(tenant,null);

        assertThat("tenantId",tenant.getId(),nullValue());
        assertThat("tenantName",tenant.getName(),nullValue());
        assertThat("tenantDescription",tenant.getDescription(),nullValue());
        assertThat("tenantDisplayName",tenant.getDisplayName(),nullValue());
    }

    @Test
    public void updateTenantFields_descriptionNullAndDisplayNameEmpty_returnsTenant() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId("id");
        tenant.setName("name");
        tenant.setDisplayName("");
        tenantsResource.updateTenantFields(tenant,null);

        assertThat("tenantId",tenant.getId(),nullValue());
        assertThat("tenantName",tenant.getName(),nullValue());
        assertThat("tenantDescription",tenant.getDescription(),nullValue());
        assertThat("tenantDisplayName",tenant.getDisplayName(),nullValue());
    }

    @Test
    public void updateTenantFields_descriptionExistsAndDisplayNameNull_returnsTenant() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId("id");
        tenant.setName("name");
        tenant.setDescription("this is a description");
        tenantsResource.updateTenantFields(tenant,null);

        assertThat("tenantId",tenant.getId(),nullValue());
        assertThat("tenantName",tenant.getName(),nullValue());
        assertThat("tenantDescription",tenant.getDescription(),equalTo("this is a description"));
        assertThat("tenantDisplayName",tenant.getDisplayName(),nullValue());
    }

    @Test
    public void updateTenantFields_descriptionNullAndDisplayNameExists_returnsTenant() throws Exception {
        Tenant tenant = new Tenant();
        tenant.setId("id");
        tenant.setName("name");
        tenant.setDisplayName("john");
        tenantsResource.updateTenantFields(tenant,null);

        assertThat("tenantId",tenant.getId(),nullValue());
        assertThat("tenantName",tenant.getName(),nullValue());
        assertThat("tenantDescription",tenant.getDescription(),nullValue());
        assertThat("tenantDisplayName",tenant.getDisplayName(),equalTo("john"));
    }

    @Test (expected = NotFoundException.class)
    public void checkAndGetTenant_nullTenant_throwsNotFoundException() throws Exception {
        tenantsResource.checkAndGetTenant(null);
    }

    @Test
    public void checkAndGetTenant_callsTenantServiceMethod() throws Exception {
        when(tenantService.getTenant(null)).thenReturn(new com.rackspace.idm.domain.entity.Tenant());
        tenantsResource.checkAndGetTenant(null);
        verify(tenantService).getTenant(null);
    }

    @Test
    public void checkAndGetTenant_tenantExists_returnsTenant() throws Exception {
        com.rackspace.idm.domain.entity.Tenant tenant = new com.rackspace.idm.domain.entity.Tenant();
        when(tenantService.getTenant(null)).thenReturn(tenant);
        assertThat("tenant",tenantsResource.checkAndGetTenant(null),equalTo(tenant));

    }
}
