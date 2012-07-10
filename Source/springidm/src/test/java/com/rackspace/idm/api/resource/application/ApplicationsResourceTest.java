package com.rackspace.idm.api.resource.application;

import com.rackspace.api.idm.v1.Application;
import com.rackspace.idm.api.converter.ApplicationConverter;
import com.rackspace.idm.domain.entity.FilterParam;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.exception.ClientConflictException;
import com.rackspace.idm.exception.DuplicateClientException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.validation.InputValidator;
import com.sun.jersey.core.provider.EntityHolder;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.notNull;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: kurt
 * Date: 6/18/12
 * Time: 3:17 PM
 * To change this template use File | Settings | File Templates.
 */
public class ApplicationsResourceTest {
    ApplicationsResource applicationsResource;
    private ApplicationResource applicationResource;
    private ScopeAccessService scopeAccessService;
    private ApplicationConverter applicationConverter;
    private ApplicationService applicationService;
    private AuthorizationService authorizationService;
    InputValidator inputValidator;

    @Before
    public void setUp() throws Exception {
        applicationResource = mock(ApplicationResource.class);
        scopeAccessService = mock(ScopeAccessService.class);
        applicationConverter = mock(ApplicationConverter.class);
        applicationService = mock(ApplicationService.class);
        authorizationService = mock(AuthorizationService.class);
        inputValidator = mock(InputValidator.class);

        applicationsResource = new ApplicationsResource(applicationResource, applicationService, scopeAccessService, applicationConverter, authorizationService, inputValidator);
    }

    @Test
    public void getApplications_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        applicationsResource.getApplications(null, 1, 1, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(anyString());
    }

    @Test
    public void getApplications_callsApplicationService_getAllApplications() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        applicationsResource.getApplications(null, null, null, null);
        verify(applicationService).getAllApplications(null, -1, -1);
    }

    @Test
    public void getApplications_withName_callsApplicationService_getAllApplications_withNonNullFilter() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        applicationsResource.getApplications("name", null, null, null);
        verify(applicationService).getAllApplications((List<FilterParam>) notNull(), eq(-1), eq(-1));
    }

    @Test
    public void getApplications_returns200Status() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        Response response = applicationsResource.getApplications(null, null, null, null);
        assertThat("Response status", response.getStatus(), equalTo(200));
    }

    @Test
    public void addApplication_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        com.rackspace.idm.domain.entity.Application application = mock(com.rackspace.idm.domain.entity.Application.class);
        EntityHolder<Application> holder = mock(EntityHolder.class);
        when(holder.hasEntity()).thenReturn(true);
        when(applicationConverter.toClientDO(any(Application.class))).thenReturn(application);
        when(application.getClientId()).thenReturn("clientId");
        applicationsResource.addApplication(null, holder);
        verify(authorizationService).verifyIdmSuperAdminAccess(anyString());
    }

    @Test
    public void addApplication_callsApplicationService_add() throws Exception {
        com.rackspace.idm.domain.entity.Application application = mock(com.rackspace.idm.domain.entity.Application.class);
        EntityHolder<Application> holder = mock(EntityHolder.class);
        when(holder.hasEntity()).thenReturn(true);
        when(applicationConverter.toClientDO(any(Application.class))).thenReturn(application);
        when(application.getClientId()).thenReturn("clientId");
        applicationsResource.addApplication(null, holder);
        verify(applicationService).add(any(com.rackspace.idm.domain.entity.Application.class));
    }

    @Test
    public void addApplication_returns201status() throws Exception {
        com.rackspace.idm.domain.entity.Application application = mock(com.rackspace.idm.domain.entity.Application.class);
        EntityHolder<Application> holder = mock(EntityHolder.class);
        when(holder.hasEntity()).thenReturn(true);
        when(applicationConverter.toClientDO(any(Application.class))).thenReturn(application);
        when(application.getClientId()).thenReturn("clientId");
        Response response = applicationsResource.addApplication(null, holder);
        assertThat("response status", response.getStatus(), equalTo(201));
    }

    @Test(expected = ClientConflictException.class)
    public void addApplication_withDuplicateApplications_throwsClientConflictException() throws Exception {
        com.rackspace.idm.domain.entity.Application application = mock(com.rackspace.idm.domain.entity.Application.class);
        EntityHolder<Application> holder = mock(EntityHolder.class);
        when(holder.hasEntity()).thenReturn(true);
        when(applicationConverter.toClientDO(any(Application.class))).thenReturn(application);
        when(application.getClientId()).thenReturn("clientId");
        doThrow(new DuplicateException()).when(applicationService).add(any(com.rackspace.idm.domain.entity.Application.class));
        applicationsResource.addApplication(null, holder);
    }

    @Test
    public void getApplicationResource_returnsApplicationResource() throws Exception {
        ApplicationResource applicationResource1 = applicationsResource.getApplicationResource();
        assertThat("application Resource", applicationResource1, equalTo(applicationResource));
    }
}
