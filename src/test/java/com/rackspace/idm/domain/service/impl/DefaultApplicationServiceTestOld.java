package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.ApplicationDao;
import com.rackspace.idm.domain.dao.ApplicationRoleDao;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientRole;
import com.rackspace.idm.domain.entity.PaginatorContext;
import com.rackspace.idm.domain.entity.TenantRole;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.TenantService;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 4/3/12
 * Time: 1:52 PM
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultApplicationServiceTestOld {

    @InjectMocks
    private DefaultApplicationService defaultApplicationService = new DefaultApplicationService();
    @Mock
    private ApplicationDao applicationDao;
    @Mock
    ApplicationRoleDao applicationRoleDao;
    @Mock
    private ScopeAccessService scopeAccessService;
    @Mock
    private TenantService tenantService;

    private DefaultApplicationService spy;

    @Before
    public void setUp() throws Exception {
        spy = spy(defaultApplicationService);
    }

    @Test
    public void getAllApplications_callsClientDao_getAllClients() throws Exception {
        defaultApplicationService.getAllApplications(null, 0, 0);
        verify(applicationDao).getAllApplications(null, 0, 0);
    }

    @Test
    public void checkAndGetApplication_applicationNotNull_returnsApplication() throws Exception {
        Application application = new Application();
        doReturn(application).when(spy).getById("applicationId");
        assertThat("application",spy.checkAndGetApplication("applicationId"),equalTo(application));
    }

    @Test
    public void checkAndGetApplication_applicationNull_throwsNotFoundException() throws Exception {
        try{
            doReturn(null).when(spy).getById("applicationId");
            spy.checkAndGetApplication("applicationId");
            assertTrue("should throw exception",false);
        } catch (NotFoundException ex){
            assertThat("exception message", ex.getMessage(), equalTo("Service applicationId not found"));
        }
    }

    @Test
    public void getClientByScope_callsClientDao_getClientByScope() throws Exception {
        defaultApplicationService.getClientByScope(null);
        verify(applicationDao).getApplicationByScope(null);
    }

    @Test
    public void updateClientRole_callsClientDao_updateClientRole() throws Exception {
        defaultApplicationService.updateClientRole(null);
        verify(applicationRoleDao).updateClientRole(null);
    }

    @Test
    public void getClientRolesByClientId_callsClientRoleDao_getClientRoles() throws Exception {
        Application application = new Application();
        when(applicationDao.getApplicationByClientId("clientId")).thenReturn(application);
        defaultApplicationService.getClientRolesByClientId("clientId");
        verify(applicationRoleDao).getClientRolesForApplication(application);
    }

    @Test (expected = NotFoundException.class)
    public void getClientRolesByClientId_throwsNotFoundException() throws Exception {
        when(applicationDao.getApplicationByClientId("blah")).thenReturn(null);
        defaultApplicationService.getClientRolesByClientId("blah");
    }

    @Test
    public void getOpenStackService_callsClientDao_getOpenStackService() throws Exception {
        when(applicationDao.getOpenStackServices(anyInt(), anyInt())).thenReturn(new PaginatorContext<Application>());
        defaultApplicationService.getOpenStackServices();
        verify(applicationDao).getOpenStackServices();
    }

    @Test
    public void updateClient_callsClientDao_updateClient() throws Exception {
        Application client = new Application();
        defaultApplicationService.updateClient(client);
        verify(applicationDao).updateApplication(client);
    }

    @Test(expected = NotFoundException.class)
    public void addClientRole_throwsNotFoundException_whenClientIsNotFound() throws Exception {
        ClientRole role = new ClientRole();
        role.setClientId("clientId");
        when(applicationDao.getApplicationByClientId("clientId")).thenReturn(null);
        defaultApplicationService.addClientRole(role);
    }

    @Test(expected = DuplicateException.class)
    public void addClientRole_throwsDuplicateException_whenRoleIsNotFound() throws Exception {
        ClientRole role = new ClientRole();
        Application application = new Application();
        role.setClientId("clientId");
        role.setName("role");
        when(applicationDao.getApplicationByClientId("clientId")).thenReturn(application);
        when(applicationRoleDao.getClientRoleByApplicationAndName(application, role)).thenReturn(new ClientRole());
        defaultApplicationService.addClientRole(role);
    }

    @Test
    public void addClientRole_callsClientDao_addClientRole() throws Exception {
        ClientRole role = new ClientRole();
        Application application = new Application();
        role.setClientId("clientId");
        role.setName("role");
        when(applicationDao.getApplicationByClientId("clientId")).thenReturn(application);
        when(applicationRoleDao.getClientRoleByApplicationAndName(application, role)).thenReturn(null);
        defaultApplicationService.addClientRole(role);
        verify(applicationRoleDao).addClientRole(any(Application.class), eq(role));
    }

    @Test (expected = NotFoundException.class)
    public void loadApplication_clientIsNull_throwsNotFoundException() throws Exception {
        when(applicationDao.getApplicationByClientId(null)).thenReturn(null);
        defaultApplicationService.loadApplication(null);
    }

    @Test
    public void loadApplication_clientFound_returnsApplicationClient() throws Exception {
        Application client = new Application();
        client.setClientId("correctClientId");
        when(applicationDao.getApplicationByClientId("clientId")).thenReturn(client);
        Application applicationClient = defaultApplicationService.loadApplication("clientId");
        assertThat("client id", applicationClient.getClientId(), equalTo("correctClientId"));
    }

    @Test
    public void deleteClientRole_callsTenantDao_deleteTenantRole() throws Exception {
        ClientRole role = new ClientRole();
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        TenantRole tenantRole = new TenantRole();
        tenantRoles.add(tenantRole);
        when(tenantService.getTenantRolesForClientRole(role)).thenReturn(tenantRoles);
        defaultApplicationService.deleteClientRole(role);
        verify(tenantService).deleteTenantRole(tenantRole);
    }

    @Test
    public void deleteClientRole_callsClientDao_deleteClientRole() throws Exception {
        ClientRole role = new ClientRole();
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        when(tenantService.getTenantRolesForClientRole(role)).thenReturn(tenantRoles);
        defaultApplicationService.deleteClientRole(role);
        verify(applicationRoleDao).deleteClientRole(role);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientServices_clientIsNull_throwsIllegalArgumentException() throws Exception {
        Application client = mock(Application.class);
        defaultApplicationService.getClientServices(client);
    }

    @Test (expected = IllegalArgumentException.class)
    public void getClientServices_clientUniqueIdIsNull_throwsIllegalArgumentException() throws Exception {
        defaultApplicationService.getClientServices(new Application());
    }

    @Test (expected = NotFoundException.class)
    public void delete_clientIdIsNull_throwsNotFoundException() throws Exception {
        defaultApplicationService.delete(null);
    }
}
