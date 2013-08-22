package com.rackspace.idm.api.resource.application;

import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.ClientSecret;
import com.rackspace.idm.domain.entity.ScopeAccess;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.unboundid.ldap.sdk.ReadOnlyEntry;
import com.unboundid.ldap.sdk.Entry;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: matt.kovacs
 * Date: 7/30/13
 * Time: 10:12 AM
 * To change this template use File | Settings | File Templates.
 */
class ProvisionedApplicationResourceTest {

    ScopeAccessService scopeAccessService;
    ApplicationService applicationService;
    AuthorizationService authorizationService;
    ProvisionedApplicationResource provisionedApplicationResource;

    ClientSecret clientSecret = ClientSecret.newInstance("Secret")
    String name = "name"
    String customerId = "customerId"
    String salt = "a1 b1"
    String version = "0"
    String id = "applicationId"
    String dn = "clientId=$id,ou=applications,o=rackspace,dc=rackspace,dc=com"

    @Before
    public void setUp() throws Exception {
        scopeAccessService = mock(ScopeAccessService.class);
        applicationService = mock(ApplicationService.class);
        authorizationService = mock(AuthorizationService.class);
        provisionedApplicationResource = new ProvisionedApplicationResource(scopeAccessService, applicationService, authorizationService, null);
    }

    @Test
    public void provisionApplicationForApplication_callsAuthorizedService_verifyIdmSuperAdminAccess() throws Exception {
        when(applicationService.loadApplication(anyString())).thenReturn(getFakeApp());
        provisionedApplicationResource.provisionApplicationForApplication(null, null, "provision");
        verify(authorizationService).verifyIdmSuperAdminAccess(anyString());
    }

    @Test
    public void provisionApplicationForApplication_callsApplicationService_loadApplication_withApplicationId() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.loadApplication(anyString())).thenReturn(getFakeApp());
        provisionedApplicationResource.provisionApplicationForApplication(null, id, null);
        verify(applicationService).loadApplication(id);
    }

    @Test
    public void provisionApplicationForApplication_callsApplicationService_loadApplication_withProvisionedApplicationId() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.loadApplication(anyString())).thenReturn(getFakeApp());
        provisionedApplicationResource.provisionApplicationForApplication(null, id, "provisionedApplicationId");
        verify(applicationService).loadApplication("provisionedApplicationId");
    }

    @Test
    public void provisionApplicationForApplication_callsScopeAccessService_addDirectScopeAccess_withApplicationId() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        Application application = getFakeApp();
        when(applicationService.loadApplication(anyString())).thenReturn(application);
        provisionedApplicationResource.provisionApplicationForApplication(null, null, null);
        verify(scopeAccessService).addApplicationScopeAccess(any(Application.class), any(ScopeAccess.class));
    }

    @Test
    public void provisionApplicationForApplication_returnsNoContentResponse() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.loadApplication(anyString())).thenReturn(getFakeApp());
        Response response = provisionedApplicationResource.provisionApplicationForApplication(null, null, null);
        assertThat("response status", response.getStatus(), equalTo(204));
    }

    @Test
    public void removeApplicationFromUser_callsAuthorizationService_verifyIdmSuperAdminAccess() throws Exception {
        when(applicationService.loadApplication(anyString())).thenReturn(getFakeApp());
        provisionedApplicationResource.removeApplicationFromUser(null, null, null);
        verify(authorizationService).verifyIdmSuperAdminAccess(anyString());
    }

    @Test
    public void removeApplicationFromUser_callsScopeAccessService_deleteScopeAccessesForParentByApplicationId() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        Application application = getFakeApp();
        when(applicationService.loadApplication(anyString())).thenReturn(application);
        provisionedApplicationResource.removeApplicationFromUser(null, null, null);
        verify(scopeAccessService).deleteScopeAccessesForApplication(application, "applicationId");
    }

    @Test
    public void removeApplicationFromUser_returnsNoContentResponse() throws Exception {
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationService.loadApplication(anyString())).thenReturn(getFakeApp());
        Response response = provisionedApplicationResource.removeApplicationFromUser(null, null, null);
        assertThat("response status", response.getStatus(), equalTo(204));
    }

    private Application getFakeApp() {
            Entry entry = new Entry(dn)
            Application app = new Application(id, clientSecret, name, customerId)
            app.ldapEntry = new ReadOnlyEntry(entry)
            app.salt = salt
            app.encryptionVersion = version
            return app
    }
}
