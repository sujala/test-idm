package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.api.converter.cloudv20.*;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.dao.impl.LdapRepository;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Group;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Control;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.SearchResultEntry;
import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.common.api.v1.Extensions;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.ext.os_ksec2.v1.Ec2CredentialsType;
import org.openstack.docs.identity.api.v2.*;
import org.openstack.docs.identity.api.v2.BadRequestFault;
import org.openstack.docs.identity.api.v2.ItemNotFoundFault;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.openstack.docs.identity.api.v2.Token;
import org.openstack.docs.identity.api.v2.UserDisabledFault;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.ws.rs.core.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/12/11
 * Time: 3:58 PM
 */


public class DefaultCloud20ServiceTest {

    private DefaultCloud20Service defaultCloud20Service;
    private DefaultCloud20Service spy;
    private Configuration config;
    private ExceptionHandler exceptionHandler;
    private UserService userService;
    private GroupService userGroupService;
    private JAXBObjectFactories jaxbObjectFactories;
    private ScopeAccessService scopeAccessService;
    private AuthorizationService authorizationService;
    private TenantService tenantService;
    private EndpointService endpointService;
    private ApplicationService clientService;
    private UserConverterCloudV20 userConverterCloudV20;
    private TenantConverterCloudV20 tenantConverterCloudV20;
    private TokenConverterCloudV20 tokenConverterCloudV20;
    private EndpointConverterCloudV20 endpointConverterCloudV20;
    private RoleConverterCloudV20 roleConverterCloudV20;
    private String authToken = "token";
    private EndpointTemplate endpointTemplate;
    private String userId = "id";
    private User user;
    private Users users;
    private Tenant tenant;
    private String tenantId = "tenantId";
    private CloudBaseUrl baseUrl;
    private Role role;
    private TenantRole tenantRole;
    private ClientRole clientRole;
    private Service service;
    private UserScopeAccess userScopeAccess;
    private org.openstack.docs.identity.api.v2.Tenant tenantOS;
    private UserForCreate userOS;
    private CloudBaseUrl cloudBaseUrl;
    private Application application;
    private String roleId = "roleId";
    private com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group groupKs;
    private Group group;
    private UriInfo uriInfo;
    private CloudKsGroupBuilder cloudKsGroupBuilder;
    private AtomHopperClient atomHopperClient;
    private HttpHeaders httpHeaders;
    private String jsonBody = "{\"passwordCredentials\":{\"username\":\"test_user\",\"password\":\"resetpass\"}}";
    private AuthConverterCloudV20 authConverterCloudV20;
    private HashMap<String, JAXBElement<Extension>> extensionMap;
    private JAXBElement<Extensions> currentExtensions;
    private ServiceConverterCloudV20 serviceConverterCloudV20;
    private String passwordCredentials = "passwordCredentials";
    private String apiKeyCredentials = "RAX-KSKEY:apiKeyCredentials";
    private DelegateCloud20Service delegateCloud20Service;
    private SecretQA secretQA;

    @Before
    public void setUp() throws Exception {
        defaultCloud20Service = new DefaultCloud20Service();

        //mocks
        userService = mock(UserService.class);
        userGroupService = mock(GroupService.class);
        jaxbObjectFactories = mock(JAXBObjectFactories.class);
        scopeAccessService = mock(ScopeAccessService.class);
        authorizationService = mock(AuthorizationService.class);
        userConverterCloudV20 = mock(UserConverterCloudV20.class);
        tenantConverterCloudV20 = mock(TenantConverterCloudV20.class);
        tokenConverterCloudV20 = mock(TokenConverterCloudV20.class);
        endpointConverterCloudV20 = mock(EndpointConverterCloudV20.class);
        roleConverterCloudV20 = mock(RoleConverterCloudV20.class);
        tenantService = mock(TenantService.class);
        endpointService = mock(EndpointService.class);
        clientService = mock(ApplicationService.class);
        config = mock(Configuration.class);
        cloudKsGroupBuilder = mock(CloudKsGroupBuilder.class);
        atomHopperClient = mock(AtomHopperClient.class);
        httpHeaders = mock(HttpHeaders.class);
        authConverterCloudV20 = mock(AuthConverterCloudV20.class);
        serviceConverterCloudV20 = mock(ServiceConverterCloudV20.class);
        delegateCloud20Service = mock(DelegateCloud20Service.class);
        exceptionHandler = mock(ExceptionHandler.class);


        //setting mocks
        defaultCloud20Service.setUserService(userService);
        defaultCloud20Service.setUserGroupService(userGroupService);
        defaultCloud20Service.setOBJ_FACTORIES(jaxbObjectFactories);
        defaultCloud20Service.setScopeAccessService(scopeAccessService);
        defaultCloud20Service.setAuthorizationService(authorizationService);
        defaultCloud20Service.setUserConverterCloudV20(userConverterCloudV20);
        defaultCloud20Service.setTenantConverterCloudV20(tenantConverterCloudV20);
        defaultCloud20Service.setEndpointConverterCloudV20(endpointConverterCloudV20);
        defaultCloud20Service.setTenantService(tenantService);
        defaultCloud20Service.setTokenConverterCloudV20(tokenConverterCloudV20);
        defaultCloud20Service.setEndpointService(endpointService);
        defaultCloud20Service.setClientService(clientService);
        defaultCloud20Service.setConfig(config);
        defaultCloud20Service.setCloudKsGroupBuilder(cloudKsGroupBuilder);
        defaultCloud20Service.setAtomHopperClient(atomHopperClient);
        defaultCloud20Service.setRoleConverterCloudV20(roleConverterCloudV20);
        defaultCloud20Service.setAuthConverterCloudV20(authConverterCloudV20);
        defaultCloud20Service.setServiceConverterCloudV20(serviceConverterCloudV20);
        defaultCloud20Service.setDelegateCloud20Service(delegateCloud20Service);
        defaultCloud20Service.setExceptionHandler(exceptionHandler);

        //fields
        user = new User();
        user.setUsername(userId);
        user.setId(userId);
        user.setMossoId(123);
        role = new Role();
        role.setId("roleId");
        role.setName("roleName");
        role.setServiceId("role-ServiceId");
        tenantRole = new TenantRole();
        tenantRole.setClientId("clientId");
        tenantRole.setUserId(userId);
        tenantRole.setRoleRsId("tenantRoleId");
        userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString("access");
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setBaseUrlIds(new String[]{});
        clientRole = new ClientRole();
        clientRole.setClientId("clientId");
        clientRole.setId("clientRoleId");
        endpointTemplate = new EndpointTemplate();
        endpointTemplate.setId(101);
        service = new Service();
        service.setName("serviceName");
        service.setId("serviceId");
        service.setType("serviceType");
        tenantOS = new org.openstack.docs.identity.api.v2.Tenant();
        tenantOS.setId("tenantName");
        tenantOS.setName("tenantName");
        userOS = new UserForCreate();
        userOS.setId("userName");
        userOS.setUsername("username");
        userOS.setEmail("foo@bar.com");
        userOS.setEnabled(true);
        cloudBaseUrl = new CloudBaseUrl();
        cloudBaseUrl.setBaseUrlId(101);
        cloudBaseUrl.setGlobal(false);
        application = new Application();
        application.setClientId("clientId");
        group = new Group();
        group.setName("Group1");
        group.setDescription("Group Description");
        group.setGroupId(1);
        groupKs = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        groupKs.setId("1");
        groupKs.setName("Group1");
        groupKs.setDescription("Group Description");
        uriInfo = mock(UriInfo.class);
        secretQA = new SecretQA();
        secretQA.setUsername("username");
        secretQA.setAnswer("answer");
        secretQA.setQuestion("question");

        //stubbing
        when(jaxbObjectFactories.getRackspaceIdentityExtKsgrpV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory());
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(new org.openstack.docs.identity.api.v2.ObjectFactory());
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(userScopeAccess);
        when(authorizationService.authorizeCloudIdentityAdmin(userScopeAccess)).thenReturn(true);
        when(endpointService.getBaseUrlById(101)).thenReturn(cloudBaseUrl);
        when(clientService.getById(role.getServiceId())).thenReturn(application);
        when(clientService.getById("clientId")).thenReturn(application);
        when(clientService.getClientRoleById(role.getId())).thenReturn(clientRole);
        when(clientService.getClientRoleById(tenantRole.getRoleRsId())).thenReturn(clientRole);
        when(tenantService.getTenant(tenantId)).thenReturn(tenant);
        when(userService.getUserById(userId)).thenReturn(user);
        when(config.getString("rackspace.customerId")).thenReturn(null);
        when(userConverterCloudV20.toUserDO(userOS)).thenReturn(user);
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_XML_TYPE);

        spy = spy(defaultCloud20Service);
        doNothing().when(spy).checkXAUTHTOKEN(eq(authToken), anyBoolean(), any(String.class));
        doNothing().when(spy).checkXAUTHTOKEN(eq(authToken), anyBoolean(), eq(tenantId));
    }

    @Test(expected = BadRequestException.class)
    public void setDefaultRegionService_returns400WhenServiceIsNotFound() throws Exception {
        ArrayList<Application> applications = new ArrayList<Application>();
        Application application1 = new Application();
        applications.add(application1);
        Application application2 = new Application("cloudFiles", ClientSecret.newInstance("foo"), "cloudFiles", "rcn");
        applications.add(application2);
        Application application3 = new Application("cloudFilesCDN", ClientSecret.newInstance("foo"), "cloudFilesCDN", "rcn");
        application3.setUseForDefaultRegion(true);
        applications.add(application3);
        when(clientService.getOpenStackServices()).thenReturn(applications);
        DefaultRegionServices defaultRegionServices = new DefaultRegionServices();
        defaultRegionServices.getServiceName().add("Foo");
        defaultCloud20Service.setDefaultRegionServices("token", defaultRegionServices);
    }

    @Test
    public void setDefaultRegionService_serviceFound_returns204() throws Exception {
        ArrayList<Application> applications = new ArrayList<Application>();
        Application application1 = new Application();
        applications.add(application1);
        Application application2 = new Application("cloudFiles", ClientSecret.newInstance("foo"), "cloudFiles", "rcn");
        applications.add(application2);
        Application application3 = new Application("cloudFilesCDN", ClientSecret.newInstance("foo"), "cloudFilesCDN", "rcn");
        application3.setUseForDefaultRegion(true);
        applications.add(application3);
        when(clientService.getOpenStackServices()).thenReturn(applications);
        when(clientService.getByName("cloudFiles")).thenReturn(application2);
        when(clientService.getByName("cloudFilesCDN")).thenReturn(application3);
        DefaultRegionServices defaultRegionServices = new DefaultRegionServices();
        defaultRegionServices.getServiceName().add("cloudFiles");
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.setDefaultRegionServices("token", defaultRegionServices);
        assertThat("code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void setDefaultRegionService_forEachService_callsClientService_getByName() throws Exception {
        ArrayList<Application> applications = new ArrayList<Application>();
        Application application1 = new Application();
        applications.add(application1);
        Application application2 = new Application("cloudFiles", ClientSecret.newInstance("foo"), "cloudFiles", "rcn");
        applications.add(application2);
        Application application3 = new Application("cloudFilesCDN", ClientSecret.newInstance("foo"), "cloudFilesCDN", "rcn");
        application3.setUseForDefaultRegion(true);
        applications.add(application3);
        when(clientService.getOpenStackServices()).thenReturn(applications);
        when(clientService.getByName("cloudFiles")).thenReturn(application2);
        when(clientService.getByName("cloudFilesCDN")).thenReturn(application3);
        DefaultRegionServices defaultRegionServices = new DefaultRegionServices();
        defaultRegionServices.getServiceName().add("cloudFiles");
        defaultRegionServices.getServiceName().add("cloudFilesCDN");
        defaultCloud20Service.setDefaultRegionServices("token", defaultRegionServices);
        verify(clientService,times(2)).getByName(anyString());
    }

    @Test
    public void setDefaultRegionService_forEachService_callsClientService_updateClient() throws Exception {
        ArrayList<Application> applications = new ArrayList<Application>();
        Application application1 = new Application();
        applications.add(application1);
        Application application2 = new Application("cloudFiles", ClientSecret.newInstance("foo"), "cloudFiles", "rcn");
        applications.add(application2);
        Application application3 = new Application("cloudFilesCDN", ClientSecret.newInstance("foo"), "cloudFilesCDN", "rcn");
        application3.setUseForDefaultRegion(true);
        applications.add(application3);
        when(clientService.getOpenStackServices()).thenReturn(applications);
        when(clientService.getByName("cloudFiles")).thenReturn(application2);
        when(clientService.getByName("cloudFilesCDN")).thenReturn(application3);
        DefaultRegionServices defaultRegionServices = new DefaultRegionServices();
        defaultRegionServices.getServiceName().add("cloudFiles");
        defaultRegionServices.getServiceName().add("cloudFilesCDN");
        defaultCloud20Service.setDefaultRegionServices("token", defaultRegionServices);
        verify(clientService,times(2)).updateClient(any(Application.class));
    }

    @Test
    public void setDefaultRegionServices_callsVerifyServiceAdminAccess() throws Exception {
        spy.setDefaultRegionServices(authToken, new DefaultRegionServices());
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void setDefaultRegionServices_returnsNotNullResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.setDefaultRegionServices(authToken, new DefaultRegionServices());
        assertThat("response builder", responseBuilder, Matchers.<Response.ResponseBuilder>notNullValue());
    }

    @Test
    public void setDefaultRegionServices_callsApplicationService_getOSServices() throws Exception {
        defaultCloud20Service.setDefaultRegionServices(authToken, new DefaultRegionServices());
        verify(clientService).getOpenStackServices();
    }

    @Test
    public void listDefaultRegionServices_callsVerifyServiceAdminAccess() throws Exception {
        spy.listDefaultRegionServices(authToken);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listDefaultRegionServices_callsApplicationService_getOSServices() throws Exception {
        defaultCloud20Service.listDefaultRegionServices(authToken);
        verify(clientService).getOpenStackServices();
    }

    @Test
    public void listDefaultRegionServices_returnsNotNullResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.listDefaultRegionServices(authToken);
        assertThat("response builder", responseBuilder, Matchers.<Response.ResponseBuilder>notNullValue());
    }

    @Test
    public void listDefaultRegionServices_returnsNotNullEntity() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.listDefaultRegionServices(authToken);
        assertThat("response builder", responseBuilder.build().getEntity(), Matchers.notNullValue());
    }

    @Test
    public void listDefaultRegionServices_returnsDefaultRegionServicesType() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.listDefaultRegionServices(authToken);
        assertThat("response builder", responseBuilder.build().getEntity(), instanceOf(DefaultRegionServices.class));
    }

    @Test
    public void listDefaultRegionServices_filtersByUseForDefaultRegionFlag() throws Exception {
        ArrayList<Application> applications = new ArrayList<Application>();
        Application application1 = new Application();
        applications.add(application1);
        Application application2 = new Application("cloudFiles", ClientSecret.newInstance("foo"), "cloudFiles", "rcn");
        applications.add(application2);
        Application application3 = new Application("cloudFilesCDN", ClientSecret.newInstance("foo"), "cloudFilesCDN", "rcn");
        application3.setUseForDefaultRegion(true);
        applications.add(application3);
        when(clientService.getOpenStackServices()).thenReturn(applications);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.listDefaultRegionServices(authToken);
        assertThat("response builder", ((DefaultRegionServices)responseBuilder.build().getEntity()).getServiceName().size(), equalTo(1));
    }

    @Test
    public void listDefaultRegionServices_handlesNullValueForUseForDefaultRegion_filtersByUseForDefaultRegionFlag() throws Exception {
        ArrayList<Application> applications = new ArrayList<Application>();
        Application application1 = new Application();
        application1.setUseForDefaultRegion(null);
        applications.add(application1);
        Application application2 = new Application("cloudFiles", ClientSecret.newInstance("foo"), "cloudFiles", "rcn");
        applications.add(application2);
        Application application3 = new Application("cloudFilesCDN", ClientSecret.newInstance("foo"), "cloudFilesCDN", "rcn");
        application3.setUseForDefaultRegion(true);
        applications.add(application3);
        when(clientService.getOpenStackServices()).thenReturn(applications);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.listDefaultRegionServices(authToken);
        assertThat("response builder", ((DefaultRegionServices)responseBuilder.build().getEntity()).getServiceName().size(), equalTo(1));
    }

    @Test
    public void addUser_withUserAdminCaller_callsTenantService_addTenantRolesToUser() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doNothing().when(spy).setDomainId(scopeAccess,user);
        doNothing().when(spy).assignProperRole(httpHeaders,authToken,scopeAccess,user);
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess) ;
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(true);
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        when(userService.getAllUsers(org.mockito.Matchers.<FilterParam[]>anyObject())).thenReturn(new Users());
        spy.addUser(httpHeaders,uriInfo,authToken,userOS);
        verify(tenantService).addTenantRolesToUser(scopeAccess,user);
    }

    @Test
    public void deleteUser_userIsUserAdmin_callsUserService_hasSubUsers() throws Exception {
        when(authorizationService.hasUserAdminRole(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(true);
        spy.deleteUser(httpHeaders, authToken, userId);
        verify(userService).hasSubUsers(userId);
    }

    @Test
    public void deleteUser_userServiceHasSubUsersWithUserId_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ScopeAccess scopeAccess = new ScopeAccess();

        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doReturn(new User()).when(spy).checkAndGetUser("userId");
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(false);
        when(scopeAccessService.getScopeAccessByUserId("userId")).thenReturn(scopeAccess);
        when(authorizationService.hasUserAdminRole(scopeAccess)).thenReturn(true);
        when(userService.hasSubUsers("userId")).thenReturn(true);
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);

        assertThat("response builder", spy.deleteUser(null, authToken, "userId"), equalTo(responseBuilder));
    }

    @Test
    public void deleteUser_callsScopeAccessService_getScopeAccessByUserId() throws Exception {
        when(scopeAccessService.getScopeAccessByUserId(userId)).thenReturn(new ScopeAccess());
        spy.deleteUser(httpHeaders, authToken, userId);
        verify(scopeAccessService).getScopeAccessByUserId(userId);
    }

    @Test
    public void deleteUser_callsAuthService_hasUserAdminRole() throws Exception {
        when(scopeAccessService.getScopeAccessByUserId(userId)).thenReturn(new ScopeAccess());
        spy.deleteUser(httpHeaders, authToken, userId);
        verify(authorizationService).hasUserAdminRole(org.mockito.Matchers.any(ScopeAccess.class));
    }

    @Test
    public void addUserCredential_returns200() throws Exception {
        ApiKeyCredentials apiKeyCredentials1 = new ApiKeyCredentials();
        apiKeyCredentials1.setUsername(userId);
        apiKeyCredentials1.setApiKey("bar");
        doReturn(new JAXBElement<CredentialType>(new QName(""), CredentialType.class, apiKeyCredentials1)).when(spy).getXMLCredentials(anyString());
        Response.ResponseBuilder responseBuilder = spy.addUserCredential(httpHeaders, authToken, userId, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void validatePassword_containsHyphen_succeeds() throws Exception {
        defaultCloud20Service.validateEmail("foo-bar@test.com");
    }

    @Test
    public void getUserByName_callsAuthorizationService_authenticateCloudUserAdmin() throws Exception {
        when(userService.getUser("userName")).thenReturn(new User("username"));
        defaultCloud20Service.getUserByName(null, authToken, "userName");
        ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByAccessToken(authToken);
        verify(authorizationService).authorizeCloudUserAdmin(scopeAccess);
    }

    @Test
    public void getUserByName_callsAuthorizationService_authenticateCloudUser() throws Exception {
        when(userService.getUser("userName")).thenReturn(new User("username"));
        defaultCloud20Service.getUserByName(null, authToken, "userName");
        ScopeAccess scopeAccess = scopeAccessService.getScopeAccessByAccessToken(authToken);
        verify(authorizationService).authorizeCloudUser(scopeAccess);
    }

    @Test(expected = BadRequestException.class)
    public void validateImpersonationRequest_expireInIsLessThan1_throwsBadRequestException() throws Exception {
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        impersonationRequest.setUser(user1);
        impersonationRequest.setExpireInSeconds(0);
        defaultCloud20Service.validateImpersonationRequest(impersonationRequest);
    }

    @Test
    public void validateImpersonationRequest_expireInNull_succeeds() throws Exception {
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        impersonationRequest.setUser(user1);
        impersonationRequest.setExpireInSeconds(null);
        defaultCloud20Service.validateImpersonationRequest(impersonationRequest);
    }

    @Test(expected = BadRequestException.class)
    public void validateImpersonationRequest_userIsNull_throwsBadRequestException() throws Exception {
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        org.openstack.docs.identity.api.v2.User impersonateUser = null;
        impersonationRequest.setUser(impersonateUser);
        defaultCloud20Service.validateImpersonationRequest(impersonationRequest);
    }

    @Test(expected = BadRequestException.class)
    public void validateImpersonationRequest_userNameIsNull_throwsBadRequestException() throws Exception {
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonationRequest.setUser(impersonateUser);
        defaultCloud20Service.validateImpersonationRequest(impersonationRequest);
    }

    @Test(expected = BadRequestException.class)
    public void validateImpersonationRequest_userNameIsEmpty_throwsBadRequestException() throws Exception {
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername(" ");
        impersonationRequest.setUser(impersonateUser);
        defaultCloud20Service.validateImpersonationRequest(impersonationRequest);
    }

    @Test(expected = BadRequestException.class)
    public void validateImpersonationRequest_userNameIsBlankString_throwsBadRequestException() throws Exception {
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("");
        impersonationRequest.setUser(impersonateUser);
        defaultCloud20Service.validateImpersonationRequest(impersonationRequest);
    }

    @Test
    public void isValidImpersonatee_returnsFalse() throws Exception {
        boolean validImpersonatee = spy.isValidImpersonatee(user);
        assertThat("boolean value", validImpersonatee, equalTo(false));
    }

    @Test
    public void isValidImpersonatee_roleIsDefault_returnsTrue() throws Exception {
        List<TenantRole> tenantRoleList = new ArrayList<TenantRole>();
        tenantRole.setName("identity:default");
        tenantRoleList.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(user, null)).thenReturn(tenantRoleList);
        boolean validImpersonatee = spy.isValidImpersonatee(user);
        assertThat("boolean value", validImpersonatee, equalTo(true));
    }

    @Test
    public void isValidImpersonatee_roleIsUserAdmin_returnsTrue() throws Exception {
        List<TenantRole> tenantRoleList = new ArrayList<TenantRole>();
        tenantRole.setName("identity:user-admin");
        tenantRoleList.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(user, null)).thenReturn(tenantRoleList);
        boolean validImpersonatee = spy.isValidImpersonatee(user);
        assertThat("boolean value", validImpersonatee, equalTo(true));
    }

    @Test
    public void isValidImpersonatee_roleIsNotDefaultOrUserAdmin_returnsFalse() throws Exception {
        List<TenantRole> tenantRoleList = new ArrayList<TenantRole>();
        tenantRole.setName("identity:neither");
        tenantRoleList.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(user, null)).thenReturn(tenantRoleList);
        boolean validImpersonatee = spy.isValidImpersonatee(user);
        assertThat("boolean value", validImpersonatee, equalTo(false));
    }

    @Test
    public void validateToken_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.validateToken(null, null, null, null);
        verify(spy).verifyServiceAdminLevelAccess(null);
    }

    @Test
    public void validateToken_whenExpiredToken_returns404() throws Exception {
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        rackerScopeAccess.setAccessTokenString("rackerToken");
        rackerScopeAccess.setAccessTokenExpired();
        doReturn(rackerScopeAccess).when(spy).checkAndGetToken("token");
        Response.ResponseBuilder responseBuilder = spy.validateToken(null, authToken, "token", null);
        assertThat("Reponse Code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void validateToken_whenRackerScopeAccess_callsUserConverter_toUserForAuthenticateResponse() throws Exception {
        Racker racker = new Racker();
        racker.setRackerId("rackerId");
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        rackerScopeAccess.setRackerId("rackerId");
        rackerScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        rackerScopeAccess.setAccessTokenString("rackerToken");
        when(scopeAccessService.getScopeAccessByAccessToken("rackerToken")).thenReturn(rackerScopeAccess);
        when(userService.getRackerByRackerId(any(String.class))).thenReturn(racker);
        Token token = new Token();
        token.setId("rackerToken");
        when(tokenConverterCloudV20.toToken(rackerScopeAccess)).thenReturn(token);
        spy.validateToken(null, authToken, "rackerToken", null);
        verify(userConverterCloudV20).toUserForAuthenticateResponse(org.mockito.Matchers.any(Racker.class), org.mockito.Matchers.any(List.class));
    }

    @Test
    public void validateToken_whenRackerScopeAccess_callsTenantService_getTenantRolesForScopeAccess() throws Exception {
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        rackerScopeAccess.setRackerId("rackerId");
        rackerScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        rackerScopeAccess.setAccessTokenString("rackerToken");
        when(scopeAccessService.getScopeAccessByAccessToken("rackerToken")).thenReturn(rackerScopeAccess);
        Token token = new Token();
        token.setId("rackerToken");
        when(tokenConverterCloudV20.toToken(rackerScopeAccess)).thenReturn(token);
        spy.validateToken(null, authToken, "rackerToken", null);
        verify(tenantService).getTenantRolesForScopeAccess(rackerScopeAccess);
    }

    @Test
    public void validateToken_whenRackerScopeAccess_callsUserService_getRackerByRackerId() throws Exception {
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        rackerScopeAccess.setRackerId("rackerId");
        rackerScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        rackerScopeAccess.setAccessTokenString("rackerToken");
        when(scopeAccessService.getScopeAccessByAccessToken("rackerToken")).thenReturn(rackerScopeAccess);
        Token token = new Token();
        token.setId("rackerToken");
        when(tokenConverterCloudV20.toToken(rackerScopeAccess)).thenReturn(token);
        spy.validateToken(null, authToken, "rackerToken", null);
        verify(userService).getRackerByRackerId("rackerId");
    }

    @Test
    public void validateToken_callsScopeAccessService_getScopeAccessByAccessToken() throws Exception {
        spy.validateToken(null, authToken, "rackerToken", null);
        verify(scopeAccessService).getScopeAccessByAccessToken("rackerToken");
    }

    @Test
    public void validateToken_whenRackerScopeAccessResponseOk_returns200() throws Exception {
        Racker racker = new Racker();
        racker.setRackerId("rackerId");
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        rackerScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        rackerScopeAccess.setAccessTokenString("rackerToken");
        rackerScopeAccess.setRackerId("rackerId");
        List<String> rackerRoles = new ArrayList<String>();
        String rackerRole = "racker";
        rackerRoles.add(rackerRole);
        doReturn(rackerScopeAccess).when(spy).checkAndGetToken("rackerToken");
        when(userService.getRackerByRackerId("rackerId")).thenReturn(racker);
        when(userService.getRackerRoles(anyString())).thenReturn(rackerRoles);
        Response.ResponseBuilder responseBuilder = spy.validateToken(httpHeaders, authToken, "rackerToken", "belongsTo");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void validateToken_whenUserScopeAccess_callsValidateBelongsTo() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        userScopeAccess.setAccessTokenString("userToken");
        doReturn(userScopeAccess).when(spy).checkAndGetToken("token");
        spy.validateToken(httpHeaders, authToken, "token", "belongsTo");
        verify(spy).validateBelongsTo(eq("belongsTo"), any(List.class));
    }

    @Test
    public void validateToken_whenUserScopeAccessResponseOk_returns200() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        userScopeAccess.setAccessTokenString("userToken");
        doReturn(userScopeAccess).when(spy).checkAndGetToken("token");
        doNothing().when(spy).validateBelongsTo(eq("belongsTo"), any(List.class));
        Response.ResponseBuilder responseBuilder = spy.validateToken(httpHeaders, authToken, "token", "belongsTo");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void validateToken_whenImpersonatedScopeAccess_callsValidateBelongsTo() throws Exception {
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        impersonatedScopeAccess.setAccessTokenString("impToken");
        doReturn(impersonatedScopeAccess).when(spy).checkAndGetToken("token");
        spy.validateToken(httpHeaders, authToken, "token", "belongsTo");
        verify(spy).validateBelongsTo(eq("belongsTo"), any(List.class));
    }

    @Test
    public void validateToken_whenImpersonatedScopeAccessResponseOk_returns200() throws Exception {
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        impersonatedScopeAccess.setAccessTokenString("impToken");
        doReturn(impersonatedScopeAccess).when(spy).checkAndGetToken("token");
        doNothing().when(spy).validateBelongsTo(eq("belongsTo"), any(List.class));
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.validateToken(httpHeaders, authToken, "token", "belongsTo");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void validateBelongsTo_success() throws Exception {
        spy.validateBelongsTo("", null);
    }

    @Test(expected = NotFoundException.class)
    public void validateBelongsTo_throwsNotFoundException() throws Exception {
        spy.validateBelongsTo("belong", null);
    }

    @Test
    public void getRolesForScopeAccess_whenUserScopeAccess_callsTenantServiceGetTenantRolesForScopeAccess() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        spy.getRolesForScopeAccess(userScopeAccess);
        verify(tenantService).getTenantRolesForScopeAccess(userScopeAccess);
    }

    @Test
    public void getRolesForScopeAccess_whenImpersonatedScopeAccess_callsTenantServiceGetTenantRolesForScopeAccess() throws Exception {
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        spy.getRolesForScopeAccess(impersonatedScopeAccess);
        verify(tenantService).getTenantRolesForScopeAccess(impersonatedScopeAccess);
    }

    @Test
    public void exceptionResponse_whenClientConflictExceptionResponse_returns409() throws Exception {
        ClientConflictException clientConflictException = new ClientConflictException();
        Response.ResponseBuilder responseBuilder = spy.exceptionResponse(clientConflictException);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void exceptionResponse_whenUserDisabledException_returns403() throws Exception {
        UserDisabledException userDisabledException = new UserDisabledException();
        Response.ResponseBuilder responseBuilder = spy.exceptionResponse(userDisabledException);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void exceptionResponse_whenUserDisabledException_detailsNotSet() throws Exception {
        UserDisabledFault userDisabledFault = mock(UserDisabledFault.class);
        JAXBElement<UserDisabledFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "userDisabled"), UserDisabledFault.class, null, userDisabledFault);
        ObjectFactory objectFactory = mock(ObjectFactory.class);
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(objectFactory);
        when(objectFactory.createUserDisabled(userDisabledFault)).thenReturn(someFault);
        when(objectFactory.createUserDisabledFault()).thenReturn(userDisabledFault);
        UserDisabledException userDisabledException = new UserDisabledException("User is Disabled");
        spy.exceptionResponse(userDisabledException);
        verify(userDisabledFault, never()).setDetails(anyString());
    }

    @Test
    public void exceptionResponse_whenNotFoundException_detailsNotSet() throws Exception {
        ItemNotFoundFault itemNotFoundFault = mock(ItemNotFoundFault.class);
        JAXBElement<ItemNotFoundFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "itemNotFound"), ItemNotFoundFault.class, null, itemNotFoundFault);
        ObjectFactory objectFactory = mock(ObjectFactory.class);
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(objectFactory);
        when(objectFactory.createItemNotFound(itemNotFoundFault)).thenReturn(someFault);
        when(objectFactory.createItemNotFoundFault()).thenReturn(itemNotFoundFault);
        spy.exceptionResponse(new NotFoundException());
        verify(itemNotFoundFault, never()).setDetails(anyString());
    }

    @Test
    public void exceptionResponse_whenStalePasswordException_returns400() throws Exception {
        StalePasswordException stalePasswordException = new StalePasswordException("Wrong Password");
        Response.ResponseBuilder responseBuilder = spy.exceptionResponse(stalePasswordException);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void belongsTo_stringBelongsToIsBlank_returnsTrue() throws Exception {
        Boolean belong = spy.belongsTo("", null);
        assertThat("Boolean Value", belong, equalTo(true));
    }

    @Test
    public void belongsTo_rolesIsNull_returnsFalse() throws Exception {
        Boolean belong = spy.belongsTo("belong", null);
        assertThat("Boolean Value", belong, equalTo(false));
    }

    @Test
    public void belongsTo_rolesSizeIsZero_returnsFalse() throws Exception {
        List<TenantRole> roles = new ArrayList<TenantRole>();
        Boolean belong = spy.belongsTo("belong", roles);
        assertThat("Boolean Value", belong, equalTo(false));
    }

    @Test
    public void belongsTo_tenantIdMatchesBelongsTo_returnsTrue() throws Exception {
        List<TenantRole> roles = new ArrayList<TenantRole>();
        String[] tenantId = {"belong"};
        tenantRole.setTenantIds(tenantId);
        roles.add(tenantRole);
        Boolean belong = spy.belongsTo("belong", roles);
        assertThat("Boolean Value", belong, equalTo(true));
    }

    @Test
    public void belongsTo_tenantIdDoesNotMatcheBelongsTo_returnsFalse() throws Exception {
        List<TenantRole> roles = new ArrayList<TenantRole>();
        String[] tenantId = {"NotMatch"};
        tenantRole.setTenantIds(tenantId);
        roles.add(tenantRole);
        Boolean belong = spy.belongsTo("belong", roles);
        assertThat("Boolean Value", belong, equalTo(false));
    }

    @Test(expected = NotFoundException.class)
    public void checkAndGetEndpointTemplate_baseUrlIsNull_throwsNotFoundException() throws Exception {
        spy.checkAndGetEndpointTemplate(1);
    }

    @Test(expected = NotFoundException.class)
    public void checkAndGetUserByName_userIsNull_throwsNotFoundException() throws Exception {
        spy.checkAndGetUserByName(null);
    }

    @Test
    public void checkAndGetUserByName_returnsCorrectUser() throws Exception {
        when(userService.getUser("user")).thenReturn(user);
        User checkUser = spy.checkAndGetUserByName("user");
        assertThat("user name", checkUser.getUsername(), equalTo("id"));
    }

    @Test(expected = NotFoundException.class)
    public void checkAndGetSoftDeletedUser_userIsNull_throwsNotFoundException() throws Exception {
        spy.checkAndGetSoftDeletedUser(null);
    }

    @Test
    public void checkAndGetSoftDeletedUser_returnsCorrectUser() throws Exception {
        when(userService.getSoftDeletedUser("user")).thenReturn(user);
        User checkUser = spy.checkAndGetSoftDeletedUser("user");
        assertThat("user name", checkUser.getUsername(), equalTo("id"));
    }

    @Test
    public void authenticate_withTenantId_callsTenantService_hasTenantAccess() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        authenticationRequest.setTenantId("tenantId");
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("uuuuuuuuuu");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        spy.authenticate(null, authenticationRequest);
        verify(tenantService).hasTenantAccess(org.mockito.Matchers.any(UserScopeAccess.class), eq("tenantId"));
    }

    @Test
    public void authenticate_withTenantIdAndNoTenantAccess_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();

        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");

        authenticationRequest.setTenantId("tenantId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);

        UserScopeAccess scopeAccess = new UserScopeAccess();

        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");
        scopeAccess.setUserRsId("userRsId");

        when(scopeAccessService.getScopeAccessByAccessToken("tokenId")).thenReturn(scopeAccess);
        when(tenantService.hasTenantAccess(any(ScopeAccess.class), eq("tenantId"))).thenReturn(false);
        doReturn(new User()).when(spy).getUserByIdForAuthentication("userRsId");
        when(exceptionHandler.exceptionResponse(any(NotAuthenticatedException.class))).thenReturn(responseBuilder);

        assertThat("response code", spy.authenticate(null, authenticationRequest), equalTo(responseBuilder));
    }

    @Test
    public void authenticate_withTenantNameAndNoTenantAccess_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();

        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");

        authenticationRequest.setTenantName("tenantName");
        authenticationRequest.setToken(tokenForAuthenticationRequest);

        UserScopeAccess scopeAccess = new UserScopeAccess();

        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");
        scopeAccess.setUserRsId("userRsId");

        when(scopeAccessService.getScopeAccessByAccessToken("tokenId")).thenReturn(scopeAccess);
        when(tenantService.hasTenantAccess(scopeAccess, "tenantName")).thenReturn(false);
        doReturn(new User()).when(spy).getUserByIdForAuthentication("userRsId");
        when(exceptionHandler.exceptionResponse(any(NotAuthenticatedException.class))).thenReturn(responseBuilder);

        assertThat("response code",spy.authenticate(null, authenticationRequest), equalTo(responseBuilder));
    }

    @Test
    public void authenticate_endpoints_callsScopeAccessService() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        spy.authenticate(null, authenticationRequest);
        verify(scopeAccessService).getOpenstackEndpointsForScopeAccess(any(ScopeAccess.class));
    }

    @Test
    public void authenticate_notCloudIdentityAdmin_callsStripEndpoints() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        spy.authenticate(null, authenticationRequest);
        verify(spy).stripEndpoints(any(List.class));
    }

    @Test
    public void authenticate_callsAuthConverterCloud() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        spy.authenticate(null, authenticationRequest);
        verify(authConverterCloudV20).toAuthenticationResponse(any(User.class), any(ScopeAccess.class), any(List.class), any(List.class));
    }

    @Test
    public void authenticate_withBothTenantIdAndTenantName_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();

        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setUsername("test_user");
        passwordCredentialsRequiredUsername.setPassword("123");

        JAXBElement<? extends PasswordCredentialsRequiredUsername> credentialType = new JAXBElement(QName.valueOf("foo"), PasswordCredentialsRequiredUsername.class, passwordCredentialsRequiredUsername);

        authenticationRequest.setToken(null);
        authenticationRequest.setCredential(credentialType);
        authenticationRequest.setTenantName("1");
        authenticationRequest.setTenantId("id");

        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);

        Response.ResponseBuilder authenticate = spy.authenticate(httpHeaders, authenticationRequest);

        assertThat("response builder", authenticate, notNullValue());
        assertThat("message",argumentCaptor.getValue().getMessage(),equalTo("Invalid request. Specify tenantId OR tenantName, not both."));
    }

    @Test
    public void authenticate_responseOk_returns200() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        AuthenticateResponse authenticateResponse = new AuthenticateResponse();
        Token token = new Token();
        token.setId("token");
        UserForAuthenticateResponse userForAuthenticateResponse = new UserForAuthenticateResponse();
        RoleList roleList = mock(RoleList.class);
        userForAuthenticateResponse.setRoles(roleList);
        authenticateResponse.setToken(token);
        authenticateResponse.setUser(userForAuthenticateResponse);
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setTenantId("tenantId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        when(tokenConverterCloudV20.toToken(any(ScopeAccess.class))).thenReturn(token);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(tenantService.hasTenantAccess(any(ScopeAccess.class), eq("tenantId"))).thenReturn(true);
        when(authConverterCloudV20.toAuthenticationResponse(any(User.class), any(ScopeAccess.class), any(List.class), any(List.class))).thenReturn(authenticateResponse);
        doNothing().when(spy).verifyTokenHasTenantAccessForAuthenticate(anyString(), anyString());
        Response.ResponseBuilder responseBuilder = spy.authenticate(null, authenticationRequest);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void authenticate_withTenantId_returnsOnlyTenantEndpoints() throws Exception {
        Token token = new Token();
        token.setId("token");
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        AuthenticateResponse authenticateResponse = new AuthenticateResponse();
        UserForAuthenticateResponse userForAuthenticateResponse = new UserForAuthenticateResponse();
        RoleList roleList = mock(RoleList.class);
        userForAuthenticateResponse.setRoles(roleList);
        authenticateResponse.setUser(userForAuthenticateResponse);
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setTenantId("tenantId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");
        when(tokenConverterCloudV20.toToken(any(ScopeAccess.class))).thenReturn(token);
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(tenantService.hasTenantAccess(any(ScopeAccess.class), eq("tenantId"))).thenReturn(true);
        ArrayList<OpenstackEndpoint> openstackEndpoints = new ArrayList<OpenstackEndpoint>();
        OpenstackEndpoint endpoint = new OpenstackEndpoint();
        endpoint.setTenantId("tenantId");
        openstackEndpoints.add(endpoint);
        OpenstackEndpoint endpoint2 = new OpenstackEndpoint();
        openstackEndpoints.add(endpoint2);
        when(scopeAccessService.getOpenstackEndpointsForScopeAccess(scopeAccess)).thenReturn(openstackEndpoints);
        ArgumentCaptor<List> argument = ArgumentCaptor.forClass(List.class);
        when(authConverterCloudV20.toAuthenticationResponse(any(User.class), any(ScopeAccess.class), any(List.class), any(ArrayList.class))).thenReturn(authenticateResponse);
        spy.authenticate(null, authenticationRequest);
        verify(authConverterCloudV20).toAuthenticationResponse(any(User.class), any(ScopeAccess.class), any(List.class), argument.capture());
        assertThat("response code", argument.getValue().size(), equalTo(1));
    }

    @Test
    public void authenticate_withPasswordCredentialsWithInvalidTenant_returnsResponseBuilder() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();

        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setUsername("test_user");
        passwordCredentialsRequiredUsername.setPassword("123");

        JAXBElement<? extends PasswordCredentialsRequiredUsername> credentialType = new JAXBElement(QName.valueOf("foo"), PasswordCredentialsRequiredUsername.class, passwordCredentialsRequiredUsername);

        authenticationRequest.setToken(null);
        authenticationRequest.setCredential(credentialType);
        authenticationRequest.setTenantId("tenantId");

        Token token = new Token();
        token.setId("token");

        RoleList roleList = mock(RoleList.class);
        UserForAuthenticateResponse userForAuthenticateResponse = new UserForAuthenticateResponse();
        userForAuthenticateResponse.setRoles(roleList);

        AuthenticateResponse authenticateResponse = new AuthenticateResponse();
        authenticateResponse.setToken(token);
        authenticateResponse.setUser(userForAuthenticateResponse);

        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");

        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");

        doNothing().when(spy).validatePasswordCredentials(passwordCredentialsRequiredUsername);
        when(userService.getUser("test_user")).thenReturn(user);
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).getUserByUsernameForAuthentication("test_user");
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(tenantService.hasTenantAccess(null, "tenantId")).thenReturn(false);
        when(authConverterCloudV20.toAuthenticationResponse(any(User.class), any(ScopeAccess.class), any(List.class), any(List.class))).thenReturn(authenticateResponse);
        doNothing().when(spy).verifyTokenHasTenantAccessForAuthenticate(anyString(), anyString());
        when(exceptionHandler.exceptionResponse(any(NotAuthenticatedException.class))).thenReturn(responseBuilder);

        assertThat("response builder", spy.authenticate(null, authenticationRequest), equalTo(responseBuilder));
    }

    @Test
    public void authenticate_withApiKeyCredentialsWithInvalidTenant_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();


        ApiKeyCredentials keyCredentials = new ApiKeyCredentials();
        keyCredentials.setUsername("test_user");
        keyCredentials.setApiKey("123");

        JAXBElement<? extends PasswordCredentialsRequiredUsername> credentialType = new JAXBElement(QName.valueOf("foo"), ApiKeyCredentials.class, keyCredentials);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setToken(null);
        authenticationRequest.setCredential(credentialType);
        authenticationRequest.setTenantId("tenantId");

        Token token = new Token();
        token.setId("token");

        UserForAuthenticateResponse userForAuthenticateResponse = new UserForAuthenticateResponse();

        RoleList roleList = mock(RoleList.class);
        userForAuthenticateResponse.setRoles(roleList);

        AuthenticateResponse authenticateResponse = new AuthenticateResponse();
        authenticateResponse.setToken(token);
        authenticateResponse.setUser(userForAuthenticateResponse);

        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");

        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("foo");

        doNothing().when(spy).validateApiKeyCredentials(keyCredentials);
        when(userService.getUser("test_user")).thenReturn(user);
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).getUserByUsernameForAuthentication("test_user");
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(tenantService.hasTenantAccess(null, "tenantId")).thenReturn(false);
        when(authConverterCloudV20.toAuthenticationResponse(any(User.class), any(ScopeAccess.class), any(List.class), any(List.class))).thenReturn(authenticateResponse);
        doNothing().when(spy).verifyTokenHasTenantAccessForAuthenticate(anyString(), anyString());
        when(exceptionHandler.exceptionResponse(any(NotAuthenticatedException.class))).thenReturn(responseBuilder);

        assertThat("response code", spy.authenticate(null, authenticationRequest), equalTo(responseBuilder));
    }

    @Test
    public void authenticate_withTenantName_callsTenantService_hasTenantAccess() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        authenticationRequest.setTenantName("tenantId");
        UserScopeAccess scopeAccess = new UserScopeAccess();
        scopeAccess.setAccessTokenExp(new Date(5000, 1, 1));
        scopeAccess.setAccessTokenString("uuuuuuuuuu");
        when(scopeAccessService.getScopeAccessByAccessToken(anyString())).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).checkAndGetUser(anyString());
        spy.authenticate(null, authenticationRequest);
        verify(tenantService).hasTenantAccess(org.mockito.Matchers.any(UserScopeAccess.class), eq("tenantId"));
    }

    @Test
    public void authenticate_tokenNotNullAndIdNotBlankScopeAccessIsNull_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        when(exceptionHandler.exceptionResponse(any(NotAuthenticatedException.class))).thenReturn(responseBuilder);
        assertThat("response code", spy.authenticate(null, authenticationRequest), equalTo(responseBuilder));
    }

    @Test
    public void authenticate_tokenNotNullAndIsNotBlankTokenExpired_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        UserScopeAccess sa = new UserScopeAccess();
        sa.setAccessTokenExp(new Date(1000, 1, 1));
        tokenForAuthenticationRequest.setId("tokenId");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        when(scopeAccessService.getScopeAccessByAccessToken("tokenId")).thenReturn(sa);
        when(exceptionHandler.exceptionResponse(any(NotAuthenticatedException.class))).thenReturn(responseBuilder);

        assertThat("response code", spy.authenticate(null, authenticationRequest), equalTo(responseBuilder));
    }

    @Test
    public void authenticate_tokenNotNullAndIsNotBlankScopeAccessNotInstanceOfUserScopeAccess_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder= new ResponseBuilderImpl();

        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setToken(tokenForAuthenticationRequest);

        RackerScopeAccess sa = new RackerScopeAccess();
        sa.setAccessTokenExp(new Date(1000, 1, 1));

        when(scopeAccessService.getScopeAccessByAccessToken("tokenId")).thenReturn(sa);
        when(exceptionHandler.exceptionResponse(any(NotAuthenticatedException.class))).thenReturn(responseBuilder);

        assertThat("response code", spy.authenticate(null, authenticationRequest), equalTo(responseBuilder));
    }

    @Test
    public void authenticate_tokenIsNullAndIsPasswordCredentials_callsScopeAccessService() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setUsername("test_user");
        passwordCredentialsRequiredUsername.setPassword("123");
        JAXBElement<? extends PasswordCredentialsRequiredUsername> credentialType = new JAXBElement(QName.valueOf("foo"), PasswordCredentialsRequiredUsername.class, passwordCredentialsRequiredUsername);
        authenticationRequest.setToken(null);
        authenticationRequest.setCredential(credentialType);
        doReturn(new User()).when(spy).checkAndGetUserByName("test_user");
        spy.authenticate(null, authenticationRequest);
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndPassword("test_user", "123", null);
    }

    @Test
    public void authenticate_tokenIsNullAndIsAPICredentials_callsScopeAccessService() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        ApiKeyCredentials apiKeyCredentials = new ApiKeyCredentials();
        apiKeyCredentials.setUsername("test_user");
        apiKeyCredentials.setApiKey("123");
        JAXBElement<? extends ApiKeyCredentials> credentialType = new JAXBElement(QName.valueOf("foo"), ApiKeyCredentials.class, apiKeyCredentials);
        authenticationRequest.setToken(null);
        authenticationRequest.setCredential(credentialType);
        doReturn(new User()).when(spy).checkAndGetUserByName("test_user");
        spy.authenticate(null, authenticationRequest);
        verify(scopeAccessService).getUserScopeAccessForClientIdByUsernameAndApiCredentials("test_user", "123", null);
    }

    @Test
    public void authenticate_withTenantIdAndNullToken_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setTenantId("tenantId");
        authenticationRequest.setToken(null);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);

        assertThat("response builder",  spy.authenticate(null, authenticationRequest), equalTo(responseBuilder));
        assertThat("response message", argumentCaptor.getValue().getMessage(), equalTo("Invalid request body: unable to parse Auth data. Please review XML or JSON formatting."));
    }

    @Test
    public void authenticate_withTenantIdAndBlankTokenId_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setTenantId("tenantId");
        TokenForAuthenticationRequest token = new TokenForAuthenticationRequest();
        token.setId(" ");
        authenticationRequest.setToken(token);
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        assertThat("response status", spy.authenticate(null, authenticationRequest), equalTo(responseBuilder));
    }

    @Test
    public void authenticate_withTenantIdAndNullTokenId_returnsBadRequestResponse() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setTenantId("tenantId");
        TokenForAuthenticationRequest token = new TokenForAuthenticationRequest();
        token.setId(null);
        authenticationRequest.setToken(token);
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        assertThat("response status", spy.authenticate(null, authenticationRequest), equalTo(responseBuilder));
    }

    @Test(expected = ForbiddenException.class)
    public void verifyRackerOrServiceAdminAccess_notRackerAndNotCloudServiceAdmin_throwsForbidden() throws Exception {
        when(authorizationService.authorizeRacker(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(false);
        spy.verifyRackerOrServiceAdminAccess(authToken);
    }

    @Test
    public void verifyRackerOrServiceAdminAccess_isRackerAndNotCloudServiceAdmin_succeeds() throws Exception {
        when(authorizationService.authorizeRacker(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(false);
        spy.verifyRackerOrServiceAdminAccess(authToken);
    }

    @Test
    public void verifyRackerOrServiceAdminAccess_callsAuthorizationService_authorizeCloudServiceAdmin() throws Exception {
        when(authorizationService.authorizeRacker(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(true);
        spy.verifyRackerOrServiceAdminAccess(authToken);
        verify(authorizationService).authorizeCloudServiceAdmin(org.mockito.Matchers.any(ScopeAccess.class));
    }

    @Test
    public void verifyRackerOrServiceAdminAccess_withServiceAdmin_succeeds() throws Exception {
        when(authorizationService.authorizeCloudServiceAdmin(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(true);
        spy.verifyRackerOrServiceAdminAccess(authToken);
        verify(authorizationService).authorizeCloudServiceAdmin(org.mockito.Matchers.any(ScopeAccess.class));
    }

    @Test(expected = ForbiddenException.class)
    public void verifyUserLevelAccess_notValidUserLevelAccess_throwsForbidden() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(false);
        spy.verifyUserLevelAccess(authToken);
    }

    @Test(expected = ForbiddenException.class)
    public void verifyUserAdminLevelAccess_notAuthorized_throwsForbidden() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(false);
        spy.verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void assignDefaultRegionToDomainUser_withNullRegion_assignsDefaultRegion() throws Exception {
        final User userDO = new User();
        defaultCloud20Service.assignDefaultRegionToDomainUser(userDO);
        assertThat("default region", userDO.getRegion(), equalTo("default"));
    }

    @Test
    public void assignDefaultRegionToDomainUser_withRegion_assignsRegion() throws Exception {
        final User userDO = new User();
        userDO.setRegion("foo");
        defaultCloud20Service.assignDefaultRegionToDomainUser(userDO);
        assertThat("default region", userDO.getRegion(), equalTo("foo"));
    }

    @Test
    public void updateUser_userIdDoesNotMatchUriId_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        UserForCreate userForCreate = new UserForCreate();
        userForCreate.setId("notSameId");
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        doReturn(user).when(spy).checkAndGetUser("123");
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        assertThat("response code", spy.updateUser(httpHeaders, authToken, "123", userForCreate), equalTo(responseBuilder));
    }

    @Test
    public void updateUser_userCannotDisableOwnAccount_throwsBadRequest() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        UserForCreate userForCreate = new UserForCreate();
        userForCreate.setEnabled(false);
        User user = new User();
        user.setEnabled(true);
        user.setId(userId);
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        doReturn(user).when(spy).checkAndGetUser(userId);
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        assertThat("response code", spy.updateUser(httpHeaders, authToken, userId, userForCreate), equalTo(responseBuilder));
    }

    @Test
    public void updateUser_withNoRegionAndPreviousRegionsExists_previousRegionRemains() throws Exception {
        UserForCreate userNoRegion = new UserForCreate();
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doNothing().when(spy).validateUser(org.mockito.Matchers.any(org.openstack.docs.identity.api.v2.User.class));
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(new User());
        User retrievedUser = new User("testUser");
        retrievedUser.setId("id");
        retrievedUser.setRegion("US of A");
        doReturn(retrievedUser).when(spy).checkAndGetUser("id");
        ArgumentCaptor<User> argumentCaptor = ArgumentCaptor.forClass(User.class);
        doNothing().when(userService).updateUserById(argumentCaptor.capture(), anyBoolean());
        spy.updateUser(httpHeaders, authToken, userId, userNoRegion);
        verify(userService).updateUserById(argumentCaptor.capture(), anyBoolean());
        assertThat("default region", argumentCaptor.getValue().getRegion(), equalTo("US of A"));
    }

    @Test
    public void updateUser_withRegionAndPreviousRegionsExists_newRegionGetsSaved() throws Exception {
        UserForCreate userWithRegion = new UserForCreate();
        userWithRegion.setId(userId);
        userWithRegion.getOtherAttributes().put(new QName("http://docs.rackspace.com/identity/api/ext/RAX-AUTH/v1.0", "defaultRegion"), "foo");
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doNothing().when(spy).validateUser(org.mockito.Matchers.any(org.openstack.docs.identity.api.v2.User.class));
        spy.setUserConverterCloudV20(new UserConverterCloudV20());
        User retrievedUser = new User("testUser");
        retrievedUser.setId("id");
        retrievedUser.setRegion("US of A");
        doReturn(retrievedUser).when(spy).checkAndGetUser("id");
        ArgumentCaptor<User> argumentCaptor = ArgumentCaptor.forClass(User.class);
        doNothing().when(userService).updateUserById(argumentCaptor.capture(), anyBoolean());
        spy.updateUser(httpHeaders, authToken, userId, userWithRegion);
        verify(userService).updateUserById(argumentCaptor.capture(), anyBoolean());
        assertThat("default region", argumentCaptor.getValue().getRegion(), equalTo("foo"));
    }

    @Test
    public void addUser_userPasswordIsNull_generateRandomPassword() throws Exception {
        UriBuilder uriBuilder = mock(UriBuilder.class);
        UserForCreate userNullPassword = new UserForCreate();
        userNullPassword.setUsername("testUser");
        User user = new User();
        ArgumentCaptor<UserForCreate> argumentCaptor = ArgumentCaptor.forClass(UserForCreate.class);
        doNothing().when(spy).assignProperRole(any(HttpHeaders.class), anyString(), any(ScopeAccess.class), any(User.class));
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doNothing().when(spy).validateUser(org.mockito.Matchers.any(org.openstack.docs.identity.api.v2.User.class));
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(new URI("uri"));
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(new ObjectFactory());
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(user);
        when(userConverterCloudV20.toUser(any(User.class))).thenReturn(new org.openstack.docs.identity.api.v2.User());
        spy.addUser(httpHeaders, uriInfo, authToken, userNullPassword);
        verify(userConverterCloudV20).toUserDO(argumentCaptor.capture());
        assertThat("user password", argumentCaptor.getValue().getPassword(), notNullValue());
    }

    @Test
    public void addUser_withNoRegion_RegionIsNull() throws Exception {
        UserForCreate userNoRegion = new UserForCreate();
        userNoRegion.setUsername("testUser");
        ArgumentCaptor<User> argumentCaptor = ArgumentCaptor.forClass(User.class);
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doNothing().when(spy).validateUser(org.mockito.Matchers.any(org.openstack.docs.identity.api.v2.User.class));
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(new User());
        spy.addUser(httpHeaders, uriInfo, authToken, userNoRegion);
        verify(userService).addUser(argumentCaptor.capture());
        assertThat("user region", argumentCaptor.getValue().getRegion(), equalTo(null));
    }

    @Test
    public void addUser_callerIsUserAdmin_setsMossoAndNastId() throws Exception {
        ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        User caller = new User();
        caller.setMossoId(123);
        caller.setNastId("nastId");
        users = mock(Users.class);
        List<User> usersList = new ArrayList();
        User tempUser = new User();
        tempUser.setId("1");
        tempUser.setUsername("tempUser");
        usersList.add(tempUser);
        users.setUsers(usersList);
        when(userService.getUserByAuthToken(authToken)).thenReturn(caller);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(new User());
        when(userService.getAllUsers(org.mockito.Matchers.<FilterParam[]>any())).thenReturn(users);
        when(config.getInt("numberOfSubUsers")).thenReturn(100);
        doNothing().when(spy).setDomainId(any(ScopeAccess.class), any(User.class));
        doNothing().when(spy).validatePassword("password");
        UserForCreate userForCreate = new UserForCreate();
        userForCreate.setUsername("userforcreate");
        userForCreate.setEmail("user@rackspace.com");
        spy.addUser(null, null, authToken, userForCreate);
        verify(userService).addUser(argument.capture());
        assertThat("nast id", argument.getValue().getNastId(), equalTo("nastId"));
        assertThat("mosso id", argument.getValue().getMossoId(), equalTo(123));
    }

    @Test
    public void addUser_callerIsNotUserAdmin_doesNotSetMossoAndNastId() throws Exception {
        ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        User caller = new User();
        caller.setMossoId(123);
        caller.setNastId("nastId");
        when(userService.getUserByAuthToken(authToken)).thenReturn(caller);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(new User());
        doNothing().when(spy).setDomainId(any(ScopeAccess.class), any(User.class));
        UserForCreate userForCreate = new UserForCreate();
        userForCreate.setUsername("userforcreate");
        userForCreate.setEmail("user@rackspace.com");
        spy.addUser(null, null, authToken, userForCreate);
        verify(userService).addUser(argument.capture());
        assertThat("nast id", argument.getValue().getNastId(), not(equalTo("nastId")));
        assertThat("mosso id", argument.getValue().getMossoId(), not(equalTo(123)));
    }

    @Test
    public void addUser_callsVerifyUserAdminLevelAccess() throws Exception {
        spy.addUser(null, null, authToken, userOS);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void addUser_callsAddUserRole_whenCallerHasIdentityAdminRole() throws Exception {
        when(clientService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(clientRole);
        when(authorizationService.authorizeCloudIdentityAdmin(null)).thenReturn(true);
        when(config.getString("cloudAuth.userAdminRole")).thenReturn(clientRole.getId());
        spy.addUser(null, null, authToken, userOS);
        verify(spy).addUserRole(null, authToken, user.getId(), clientRole.getId());
    }

    @Test
    public void addUser_callsClientServiceGetClientRoleByClientIdAndName_whenCallerHasIdentityAdminRole() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(null)).thenReturn(true);
        when(config.getString("cloudAuth.userAdminRole")).thenReturn("roleId");
        spy.addUser(null, null, authToken, userOS);
        verify(clientService).getClientRoleByClientIdAndRoleName(anyString(), anyString());
    }

    @Test
    public void addUser_withUserMissingUsername_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        BadRequestException badRequestException = new BadRequestException("missing username");
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doThrow(badRequestException).when(spy).validateUser(userOS);
        when(exceptionHandler.exceptionResponse(badRequestException)).thenReturn(responseBuilder);
        userOS.setUsername(null);
        assertThat("response code", spy.addUser(null, null, authToken, userOS), equalTo(responseBuilder));
    }

    @Test
    public void addUser_callsUserConverter_toUserDOMethod() throws Exception {
        spy.addUser(null, null, authToken, userOS);
        verify(userConverterCloudV20).toUserDO(userOS);
    }

    @Test
    public void addUser_callsUserService_addUserMethod() throws Exception {
        spy.addUser(null, null, authToken, userOS);
        verify(userService).addUser(user);
    }

    @Test
    public void addUser_callsSetDomainId() throws Exception {
        spy.addUser(null, null, authToken, userOS);
        verify(spy).setDomainId(any(ScopeAccess.class), any(User.class));
    }

    @Test
    public void addUser_userPasswordNotNull_callsValidatePassword() throws Exception {
        userOS.setPassword("password");
        spy.addUser(null, null, authToken, userOS);
        verify(spy).validatePassword("password");
    }

    @Test
    public void addUser_adminRoleUserSizeGreaterThanNumSubUsersThrowsBadRequest_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ScopeAccess scopeAccess = new ScopeAccess();
        User caller = new User();
        users = mock(Users.class);
        List<User> userList = new ArrayList();
        User tempUser = new User();
        tempUser.setId("1");
        tempUser.setUsername("tempUser");
        userList.add(tempUser);
        users.setUsers(userList);

        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doNothing().when(spy).validateUser(userOS);
        doNothing().when(spy).validateUsernameForUpdateOrCreate(userOS.getUsername());
        when(userConverterCloudV20.toUserDO(userOS)).thenReturn(new User());
        when(userService.getUserByAuthToken(authToken)).thenReturn(caller);
        when(userService.getAllUsers(org.mockito.Matchers.<FilterParam[]>any())).thenReturn(users);
        when(config.getInt("numberOfSubUsers")).thenReturn(-1);
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);

        assertThat("response builder", spy.addUser(null, null, authToken, userOS), equalTo(responseBuilder));
    }

    @Test
    public void addUser_adminRoleUserSizeNotGreaterThanNumSubUsersThrowsBadRequest_returns201() throws Exception {
        userOS.setPassword("password");
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI uri = new URI("");
        User caller = new User();
        users = mock(Users.class);
        List<User> userList = new ArrayList();
        User tempUser = new User();
        tempUser.setId("1");
        tempUser.setUsername("tempUser");
        userList.add(tempUser);
        users.setUsers(userList);
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(new User());
        when(userConverterCloudV20.toUser(any(User.class))).thenReturn(new org.openstack.docs.identity.api.v2.User());
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUserByAuthToken(authToken)).thenReturn(caller);
        when(userService.getAllUsers(org.mockito.Matchers.<FilterParam[]>any())).thenReturn(users);
        when(config.getInt("numberOfSubUsers")).thenReturn(2);
        doNothing().when(spy).setDomainId(any(ScopeAccess.class), any(User.class));
        doNothing().when(spy).assignProperRole(eq(httpHeaders), eq(authToken), any(ScopeAccess.class), any(User.class));
        doNothing().when(spy).validatePassword("password");
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        doReturn(uriBuilder).when(uriBuilder).path(anyString());
        doReturn(uri).when(uriBuilder).build();
        Response.ResponseBuilder responseBuilder = spy.addUser(httpHeaders, uriInfo, authToken, userOS);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addUser_responseCreated_returns201() throws Exception {
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI uri = new URI("");
        User user = new User();
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(user);
        when(userConverterCloudV20.toUser(any(User.class))).thenReturn(new org.openstack.docs.identity.api.v2.User());
        doNothing().when(spy).assignProperRole(eq(httpHeaders), eq(authToken), any(ScopeAccess.class), any(User.class));
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        doReturn(uriBuilder).when(uriBuilder).path(anyString());
        doReturn(uri).when(uriBuilder).build();
        Response.ResponseBuilder responseBuilder = spy.addUser(httpHeaders, uriInfo, authToken, userOS);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addUser_userServiceDuplicateException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doNothing().when(spy).validateUser(userOS);
        doNothing().when(spy).validateUsernameForUpdateOrCreate(userOS.getUsername());
        when(userConverterCloudV20.toUserDO(userOS)).thenReturn(new User());
        doThrow(new DuplicateException("duplicate")).when(userService).addUser(any(User.class));
        when(exceptionHandler.conflictExceptionResponse("duplicate")).thenReturn(responseBuilder);
        assertThat("response code", spy.addUser(null, null, authToken, userOS), equalTo(responseBuilder));
    }

    @Test
    public void addUser_userServiceDuplicateUserNameException_returnsResponseBuilder() throws Exception {
        User caller = new User();
        DuplicateUsernameException duplicateUsernameException = new DuplicateUsernameException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doNothing().when(spy).validateUser(userOS);
        doNothing().when(spy).validateUsernameForUpdateOrCreate(userOS.getUsername());
        when(userConverterCloudV20.toUserDO(userOS)).thenReturn(caller);
        when(userService.getUserByAuthToken(authToken)).thenReturn(caller);
        when(userService.getAllUsers(any(FilterParam[].class))).thenReturn(null);
        doNothing().when(spy).setDomainId(userScopeAccess,caller);
        doThrow(duplicateUsernameException).when(userService).addUser(caller);
        when(exceptionHandler.exceptionResponse(duplicateUsernameException)).thenReturn(responseBuilder);

        assertThat("response code", spy.addUser(null, null, authToken, userOS), equalTo(responseBuilder));
    }

    @Test
    public void deleteUser_callsUserService_softDeleteUserMethod() throws Exception {
        User user1 = new User();
        User caller = new User();
        user1.setDomainId("domainId");
        caller.setId("id");
        caller.setDomainId("domainId");
        when(userService.getUserById(userId)).thenReturn(user1);
        when(userService.getUserByAuthToken(authToken)).thenReturn(caller);
        spy.deleteUser(null, authToken, userId);
        verify(userService).softDeleteUser(any(User.class));
    }

    @Test
    public void userDisabledExceptionResponse_setsMessage() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.userDisabledExceptionResponse("userName");
        UserDisabledFault object = (UserDisabledFault)responseBuilder.build().getEntity();
        assertThat("message", object.getMessage(), equalTo("userName"));
    }

    @Test
    public void userDisabledExceptionResponse_returns403() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.userDisabledExceptionResponse("responseMessage");
        assertThat("message", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void tenantConflictExceptionResponse_setsMessage() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.tenantConflictExceptionResponse("responseMessage");
        TenantConflictFault object = (TenantConflictFault)responseBuilder.build().getEntity();
        assertThat("message", object.getMessage(), equalTo("responseMessage"));
    }

    @Test
    public void tenantConflictExceptionResponse_returns409() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.tenantConflictExceptionResponse("responseMessage");
        assertThat("message", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void userConflictExceptionResponse_setsMessage() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.userConflictExceptionResponse("responseMessage");
        BadRequestFault object = (BadRequestFault)responseBuilder.build().getEntity();
        assertThat("message", object.getMessage(), equalTo("responseMessage"));
    }

    @Test
    public void userConflictExceptionResponse_returns409() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.userConflictExceptionResponse("responseMessage");
        assertThat("message", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void exceptionResponse_withForbiddenException_403() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.exceptionResponse(new ForbiddenException());
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void exceptionResponse_withNotAuthenticatedException_401() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.exceptionResponse(new NotAuthenticatedException());
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void deleteService_callsClientService_deleteMethod() throws Exception {
        spy.deleteService(null, authToken, "clientId");
        verify(clientService).delete("clientId");
    }

    @Test
    public void deleteRoleFromUserOnTenant_callsTenantService_deleteTenantRoleMethod() throws Exception {
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doNothing().when(spy).verifyTokenHasTenantAccess(authToken, tenantId);
        spy.deleteRoleFromUserOnTenant(null, authToken, tenantId, userId, role.getId());
        verify(tenantService).deleteTenantRole(anyString(), any(TenantRole.class));
    }

    @Test
    public void deleteRole_callsClientService_deleteClientRoleMethod() throws Exception {
        doNothing().when(spy).verifyServiceAdminLevelAccess(anyString());
        spy.deleteRole(null, authToken, role.getId());
        verify(clientService).deleteClientRole(any(ClientRole.class));
    }

    @Test
    public void deleteRole_throwsForbiddenException_returnsResponseBuilder() throws Exception {
        ForbiddenException forbiddenException = new ForbiddenException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doThrow(forbiddenException).when(spy).verifyIdentityAdminLevelAccess(authToken);
        when(exceptionHandler.exceptionResponse(forbiddenException)).thenReturn(responseBuilder);
        assertThat("code", spy.deleteRole(null, authToken, "roleId"), equalTo(responseBuilder));
    }

    @Test
    public void deleteRole_withNullRole_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyIdentityAdminLevelAccess(authToken);
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        assertThat("code", spy.deleteRole(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void deleteEndpointTemplate_callsEndpointService_deleteBaseUrlMethod() throws Exception {
        spy.deleteEndpointTemplate(null, authToken, "101");
        verify(endpointService).deleteBaseUrl(101);
    }

    @Test
    public void deleteEndpoint_callsTenantService_updateTenantMethod() throws Exception {
        spy.deleteEndpoint(null, authToken, tenantId, "101");
        verify(tenantService).updateTenant(any(Tenant.class));
    }

    @Test
    public void addUserRole_callsTenantService_addTenantRoleToUserMethod() throws Exception {
        spy.addUserRole(null, authToken, userId, tenantRole.getRoleRsId());
        verify(tenantService).addTenantRoleToUser(any(User.class), any(TenantRole.class));
    }

    @Test
    public void addUserRole_callsCheckForMultipleIdentityRoles() throws Exception {
        spy.addUserRole(null, authToken, userId, tenantRole.getRoleRsId());
        verify(spy).checkForMultipleIdentityRoles(any(User.class), any(ClientRole.class));
    }

    @Test
    public void checkForMultipleIdentityRoles_callsTenantService_getGlobalRoles() throws Exception {
        spy.checkForMultipleIdentityRoles(new User(), null);
        verify(tenantService).getGlobalRolesForUser(any(User.class));
    }

    @Test
    public void checkForMultipleIdentityRoles_userGetRolesIsNull_returns() throws Exception {
        ClientRole clientRole = new ClientRole();
        clientRole.setName("identity:admin");
        when(tenantService.getGlobalRolesForUser(user)).thenReturn(null);
        spy.checkForMultipleIdentityRoles(user, clientRole);
        assertTrue("method threw no errors", true);
    }

    @Test
    public void checkForMultipleIdentityRoles_roleToAddIsNull_returns() throws Exception {
        when(tenantService.getGlobalRolesForUser(user)).thenReturn(new ArrayList<TenantRole>());
        spy.checkForMultipleIdentityRoles(user, null);
        assertTrue("method threw no errors", true);
    }

    @Test
    public void checkForMultipleIdentityRoles_roleToAddNameDoesNotStartWithIdentity_returns() throws Exception {
        ClientRole clientRole = new ClientRole();
        clientRole.setName("admin");
        when(tenantService.getGlobalRolesForUser(user)).thenReturn(new ArrayList<TenantRole>());
        spy.checkForMultipleIdentityRoles(user, clientRole);
        assertTrue("method threw no errors", true);
    }

    @Test(expected = BadRequestException.class)
    public void checkForMultipleIdentityRoles_throwsBadRequestException_withIdentityRoleAddedToUserWithIdentityRole() throws Exception {
        User user1 = new User();
        ArrayList<TenantRole> roles = new ArrayList<TenantRole>();
        TenantRole tenantRole1 = new TenantRole();
        tenantRole1.setName("identity:role");
        roles.add(tenantRole1);
        ClientRole roleToAdd = new ClientRole();
        roleToAdd.setName("Identity:role");
        when(tenantService.getGlobalRolesForUser(user1)).thenReturn(roles);
        spy.checkForMultipleIdentityRoles(user1, roleToAdd);
    }

    @Test
    public void addTenant_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.addTenant(null, null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addTenant_callsTenantService_addTenant() throws Exception {
        spy.addTenant(null, null, authToken, tenantOS);
        verify(tenantService).addTenant(any(Tenant.class));
    }

    @Test
    public void addTenant_callsTenantConverterCloudV20_toTenantDO() throws Exception {
        spy.addTenant(null, null, authToken, tenantOS);
        verify(tenantConverterCloudV20).toTenantDO(tenantOS);
    }

    @Test
    public void addTenant_withNullTenantName_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        tenantOS.setName(null);
        assertThat("response builder", spy.addTenant(null, null, authToken, tenantOS), equalTo(responseBuilder));
    }

    @Test
    public void addTenant_responseCreated_returns201() throws Exception {
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI uri = new URI("");
        Tenant domainTenant = mock(Tenant.class);
        org.openstack.docs.identity.api.v2.Tenant tenant = new org.openstack.docs.identity.api.v2.Tenant();
        JAXBElement<org.openstack.docs.identity.api.v2.Tenant> someValue = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "tenant"), org.openstack.docs.identity.api.v2.Tenant.class, null, tenant);
        org.openstack.docs.identity.api.v2.ObjectFactory objectFactory = mock(org.openstack.docs.identity.api.v2.ObjectFactory.class);
        doReturn(domainTenant).when(tenantConverterCloudV20).toTenantDO(any(org.openstack.docs.identity.api.v2.Tenant.class));
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        doReturn(uriBuilder).when(uriBuilder).path(anyString());
        doReturn(uri).when(uriBuilder).build();
        doReturn(tenantId).when(domainTenant).getTenantId();
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(objectFactory);
        when(tenantConverterCloudV20.toTenant(any(Tenant.class))).thenReturn(tenantOS);
        when(objectFactory.createTenant(tenantOS)).thenReturn(someValue);
        Response.ResponseBuilder responseBuilder = spy.addTenant(httpHeaders, uriInfo, authToken, tenantOS);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addTenant_tenantServiceDuplicateException_returns409() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doThrow(new DuplicateException("duplicate")).when(tenantService).addTenant(any(Tenant.class));
        when(exceptionHandler.tenantConflictExceptionResponse("duplicate")).thenReturn(responseBuilder);
        assertThat("response code", spy.addTenant(null, null, authToken, tenantOS), equalTo(responseBuilder));
    }

    @Test
    public void addService_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        spy.addService(null, null, authToken, null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void addService_callsClientService_add() throws Exception {
        spy.addService(null, null, authToken, service);
        verify(clientService).add(any(Application.class));
    }

    @Test
    public void addService_withNullService_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).checkXAUTHTOKEN(authToken, true, null);
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.addService(null, null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void addService_withNullServiceType_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).checkXAUTHTOKEN(authToken,true,null);
        service.setType(null);
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.addService(null, null, authToken, service), equalTo(responseBuilder));
    }

    @Test
    public void addService_withNullName_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).checkXAUTHTOKEN(authToken,true,null);
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        service.setName(null);
        assertThat("response builder", spy.addService(null, null, authToken, service), equalTo(responseBuilder));
    }

    @Test
    public void addService_responseCreated_returns201() throws Exception {
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI uri = new URI("");
        org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory objectFactory = mock(org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory.class);
        JAXBElement<Service> someValue = new JAXBElement(new QName("http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0", "service"), Service.class, null, service);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        doReturn(uriBuilder).when(uriBuilder).path(anyString());
        doReturn(uri).when(uriBuilder).build();
        when(jaxbObjectFactories.getOpenStackIdentityExtKsadmnV1Factory()).thenReturn(objectFactory);
        when(objectFactory.createService(service)).thenReturn(someValue);
        Response.ResponseBuilder responseBuilder = spy.addService(httpHeaders, uriInfo, authToken, service);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addService_clientServiceDuplicateException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doThrow(new DuplicateException("duplicate")).when(clientService).add(any(Application.class));
        when(exceptionHandler.conflictExceptionResponse("duplicate")).thenReturn(responseBuilder);
        assertThat("response code", spy.addService(null, null, authToken, service), equalTo(responseBuilder));
    }

    @Test
    public void addRole_isAdminCall_callsCheckAuthTokenMethod() throws Exception {
        Role role1 = new Role();
        role1.setServiceId("id");
        spy.addRole(null, null, authToken, role1);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addRole_callsClientService_addClientRole() throws Exception {
        doNothing().when(spy).verifyServiceAdminLevelAccess(anyString());
        spy.addRole(null, null, authToken, role);
        verify(clientService).addClientRole(any(ClientRole.class));
    }

    @Test
    public void addRole_roleWithNullName_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        Role role1 = new Role();
        role1.setName(null);
        assertThat("response builder", spy.addRole(null, null, authToken, role1), equalTo(responseBuilder));
    }

    @Test
    public void addRole_nullRole_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        assertThat("response bulider", spy.addRole(null, null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void addRole_roleWithNullServiceId_returnsDefaults() throws Exception {
        Role role1 = new Role();
        role1.setName("roleName");
        role1.setServiceId(null);
        when(clientService.getById(anyString())).thenReturn(application);
        spy.addRole(null, null, authToken, role1);
        verify(clientService).addClientRole(any(ClientRole.class));
    }

    @Test
    public void addRole_roleWithIdentityNameWithNotIdenityAdmin_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        Role role1 = new Role();
        role1.setName("Identity:role");
        role1.setServiceId("serviceId");
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doThrow(new ForbiddenException()).when(spy).verifyIdentityAdminLevelAccess(authToken);
        when(exceptionHandler.exceptionResponse(any(ForbiddenException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.addRole(null, null, authToken, role1), equalTo(responseBuilder));
    }

    @Test
    public void addRole_roleWithIdentityNameWithIdentityAdmin_returns201Status() throws Exception {
        Role role1 = new Role();
        role1.setName("Identity:role");
        JAXBElement<Role> someValue = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "role"), Role.class, null, role);
        doNothing().when(spy).verifyIdentityAdminLevelAccess(authToken);
        doReturn(application).when(spy).checkAndGetApplication(anyString());
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI uri = new URI("");
        org.openstack.docs.identity.api.v2.ObjectFactory objectFactory = mock(org.openstack.docs.identity.api.v2.ObjectFactory.class);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        doReturn(uriBuilder).when(uriBuilder).path(anyString());
        doReturn(uri).when(uriBuilder).build();
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(objectFactory);
        when(objectFactory.createRole(any(Role.class))).thenReturn(someValue);
        when(roleConverterCloudV20.toRoleFromClientRole(any(ClientRole.class))).thenReturn(role1);
        Response response = spy.addRole(httpHeaders, uriInfo, authToken, role1).build();
        assertThat("response status", response.getStatus(), equalTo(201));
    }

    @Test
    public void addRole_responseCreated() throws Exception {
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI uri = new URI("");
        org.openstack.docs.identity.api.v2.ObjectFactory objectFactory = mock(org.openstack.docs.identity.api.v2.ObjectFactory.class);
        JAXBElement<Role> someValue = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "role"), Role.class, null, role);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        doReturn(uriBuilder).when(uriBuilder).path(anyString());
        doReturn(uri).when(uriBuilder).build();
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(objectFactory);
        when(roleConverterCloudV20.toRoleFromClientRole(any(ClientRole.class))).thenReturn(role);
        when(objectFactory.createRole(role)).thenReturn(someValue);
        Response.ResponseBuilder responseBuilder = spy.addRole(httpHeaders, uriInfo, authToken, role);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addRole_roleConflictThrowsDuplicateException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doReturn(new Application()).when(spy).checkAndGetApplication(role.getServiceId());
        doThrow(new DuplicateException("role conflict")).when(clientService).addClientRole(any(ClientRole.class));
        when(exceptionHandler.conflictExceptionResponse("role conflict")).thenReturn(responseBuilder);
        assertThat("response builder", spy.addRole(null, null, authToken, role), equalTo(responseBuilder));
    }

    @Test
    public void addRolesToUserOnTenant_callsTenantService_addTenantRoleToUser() throws Exception {
        clientRole.setName("name");
        doNothing().when(spy).verifyUserAdminLevelAccess(anyString());
        doNothing().when(spy).verifyTokenHasTenantAccess(authToken, tenantId);
        doReturn(clientRole).when(spy).checkAndGetClientRole(role.getId());
        when(config.getString("cloudAuth.serviceAdminRole")).thenReturn("identity:service-admin");
        when(config.getString("cloudAuth.adminRole")).thenReturn("identity:admin");
        when(config.getString("cloudAuth.userAdminRole")).thenReturn("identity:user-admin");
        spy.addRolesToUserOnTenant(null, authToken, tenantId, userId, role.getId());
        verify(tenantService).addTenantRoleToUser(any(User.class), any(TenantRole.class));
    }

    @Test
    public void addRolesToUserOnTenant_roleNameEqualsCloudServiceAdmin_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        clientRole.setName("identity:service-admin");
        doNothing().when(spy).verifyUserAdminLevelAccess(null);
        doNothing().when(spy).verifyTokenHasTenantAccess(null, null);
        doReturn(new Tenant()).when(spy).checkAndGetTenant(null);
        doReturn(new User()).when(spy).checkAndGetUser(null);
        doReturn(clientRole).when(spy).checkAndGetClientRole(null);
        when(config.getString("cloudAuth.serviceAdminRole")).thenReturn("identity:service-admin");
        when(config.getString("cloudAuth.adminRole")).thenReturn("identity:admin");
        when(config.getString("cloudAuth.userAdminRole")).thenReturn("identity:user-admin");
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.addRolesToUserOnTenant(null, null, null, null, null), equalTo(responseBuilder));
    }

    @Test
    public void addRolesToUserOnTenant_roleNameEqualsUserAdmin_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        clientRole.setName("identity:user-admin");
        doNothing().when(spy).verifyUserAdminLevelAccess(null);
        doNothing().when(spy).verifyTokenHasTenantAccess(null, null);
        doReturn(new Tenant()).when(spy).checkAndGetTenant(null);
        doReturn(new User()).when(spy).checkAndGetUser(null);
        doReturn(clientRole).when(spy).checkAndGetClientRole(null);
        when(config.getString("cloudAuth.serviceAdminRole")).thenReturn("identity:service-admin");
        when(config.getString("cloudAuth.adminRole")).thenReturn("identity:admin");
        when(config.getString("cloudAuth.userAdminRole")).thenReturn("identity:user-admin");
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        assertThat("resonse builder", spy.addRolesToUserOnTenant(null, null, null, null, null), equalTo(responseBuilder));
    }

    @Test
    public void addRolesToUserOnTenant_roleNameEqualsAdmin_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        clientRole.setName("identity:admin");
        doNothing().when(spy).verifyUserAdminLevelAccess(null);
        doNothing().when(spy).verifyTokenHasTenantAccess(null, null);
        doReturn(new Tenant()).when(spy).checkAndGetTenant(null);
        doReturn(new User()).when(spy).checkAndGetUser(null);
        doReturn(clientRole).when(spy).checkAndGetClientRole(null);
        when(config.getString("cloudAuth.serviceAdminRole")).thenReturn("identity:service-admin");
        when(config.getString("cloudAuth.adminRole")).thenReturn("identity:admin");
        when(config.getString("cloudAuth.userAdminRole")).thenReturn("identity:user-admin");
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.addRolesToUserOnTenant(null, null, null, null, null), equalTo(responseBuilder));
    }

    @Test
    public void addEndpoint_callsTenantService_updateTenant() throws Exception {
        spy.addEndpoint(null, authToken, tenantId, endpointTemplate);
        verify(tenantService).updateTenant(tenant);
    }

    @Test
    public void addEndpoint_Global_throwBadRequestExceptionAndReturnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doReturn(tenant).when(spy).checkAndGetTenant(tenantId);
        doReturn(cloudBaseUrl).when(spy).checkAndGetEndpointTemplate(endpointTemplate.getId());
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        cloudBaseUrl.setGlobal(true);
        assertThat("response builder", spy.addEndpoint(null, authToken, tenantId, endpointTemplate), equalTo(responseBuilder));
    }

    @Test
    public void listUserGroups_noGroup_ReturnDefaultGroup() throws Exception {
        when(userGroupService.getGroupById(config.getInt(org.mockito.Matchers.<String>any()))).thenReturn(group);
        when(cloudKsGroupBuilder.build(org.mockito.Matchers.<Group>any())).thenReturn(groupKs);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.listUserGroups(null, authToken, userId);
        assertThat("Default Group added", ((com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups)responseBuilder.build().getEntity()).getGroup().get(0).getName(), equalTo("Group1"));
    }

    @Test
    public void listUserGroups_badRequestException_returns400() throws Exception {
        doThrow(new BadRequestException()).when(spy).verifyServiceAdminLevelAccess(authToken);
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null, authToken, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void listUserGroups_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listUserGroups(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listUserGroups_withValidUser_returns200() throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null, authToken, userId);
        assertThat("code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUserGroups_withValidUser_returnsNonNullEntity() throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null, authToken, userId);
        assertThat("code", responseBuilder.build().getEntity(), Matchers.<Object>notNullValue());
    }

    @Test
    public void listUserGroups_withValidUser_returnsGroups() throws Exception {
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(null, authToken, userId);
        assertThat("code", responseBuilder.build().getEntity(), instanceOf(com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups.class));
    }

    @Test
    public void getGroupById_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.getGroupById(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getGroupById_callsValidateGroupId() throws Exception {
        spy.getGroupById(null, authToken, "1");
        verify(spy).validateGroupId("1");
    }

    @Test
    public void getGroupById_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getGroupById(httpHeaders, authToken, "1");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listTenants_invalidToken_returns401() throws Exception {
        doNothing().when(spy).verifyUserLevelAccess("bad");
        when(scopeAccessService.getAccessTokenByAuthHeader("bad")).thenReturn(null);
        when(tenantConverterCloudV20.toTenantList(org.mockito.Matchers.<List<Tenant>>any())).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.listTenants(null, "bad", null, 1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void listTenants_callsUserLevelAccess() throws Exception {
        spy.listTenants(null, null, null, null);
        verify(spy).verifyUserLevelAccess(null);
    }

    @Test
    public void listTenants_scopeAccessSaNotNullResponseOk_returns200() throws Exception {
        when(scopeAccessService.getAccessTokenByAuthHeader(authToken)).thenReturn(new ScopeAccess());
        Response.ResponseBuilder responseBuilder = spy.listTenants(httpHeaders, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void addEndpoint_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.addEndpoint(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addEndpointTemplate_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.addEndpointTemplate(null, null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addEndpointTemplate_callsEndpointService_addBaseUrl() throws Exception {
        when(endpointConverterCloudV20.toCloudBaseUrl(endpointTemplate)).thenReturn(baseUrl);
        spy.addEndpointTemplate(null, null, authToken, endpointTemplate);
        verify(endpointService).addBaseUrl(baseUrl);
    }

    @Test
    public void addEndpointTemplate_returnsResponseCreated() throws Exception {
        UriBuilder uriBuilder = mock(UriBuilder.class);
        URI uri = new URI("101");
        org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory objectFactory = mock(org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory.class);
        JAXBElement<EndpointTemplate> someValue = new JAXBElement(new QName("http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0", "endpointTemplate"), EndpointTemplate.class, null, endpointTemplate);
        when(endpointConverterCloudV20.toCloudBaseUrl(endpointTemplate)).thenReturn(cloudBaseUrl);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        doReturn(uriBuilder).when(uriBuilder).path("101");
        doReturn(uri).when(uriBuilder).build();
        when(endpointConverterCloudV20.toEndpointTemplate(cloudBaseUrl)).thenReturn(endpointTemplate);
        when(jaxbObjectFactories.getOpenStackIdentityExtKscatalogV1Factory()).thenReturn(objectFactory);
        when(objectFactory.createEndpointTemplate(endpointTemplate)).thenReturn(someValue);
        Response.ResponseBuilder responseBuilder = spy.addEndpointTemplate(httpHeaders, uriInfo, authToken, endpointTemplate);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addEndpointTemplate_endPointServiceThrowsBaseUrlConflictException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        when(endpointConverterCloudV20.toCloudBaseUrl(endpointTemplate)).thenReturn(cloudBaseUrl);
        doThrow(new BaseUrlConflictException()).when(endpointService).addBaseUrl(cloudBaseUrl);
        when(exceptionHandler.exceptionResponse(any(BaseUrlConflictException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.addEndpointTemplate(null, null, authToken, endpointTemplate), equalTo(responseBuilder));
    }

    @Test
    public void addEndpointTemplate_endPointServiceThrowsDuplicateException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        when(endpointConverterCloudV20.toCloudBaseUrl(endpointTemplate)).thenReturn(cloudBaseUrl);
        when(exceptionHandler.conflictExceptionResponse("duplicate exception")).thenReturn(responseBuilder);
        doThrow(new DuplicateException("duplicate exception")).when(endpointService).addBaseUrl(cloudBaseUrl);
        assertThat("response builder", spy.addEndpointTemplate(null, null, authToken, endpointTemplate), equalTo(responseBuilder));
    }

    @Test
    public void addRoleToUserOnTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.addRolesToUserOnTenant(null, authToken, null, null, null);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void addUserCredential_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.addUserCredential(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addUserCredential_mediaTypeXML_callsGetXMLCredentials() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE)).thenReturn(true);
        spy.addUserCredential(httpHeaders, authToken, null, null);
        verify(spy).getXMLCredentials(null);
    }

    @Test
    public void addUserCredential_mediaTypeJSON_callsGetJSONCredentials() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)).thenReturn(true);
        spy.addUserCredential(httpHeaders, authToken, null, null);
        verify(spy).getJSONCredentials(null);
    }

    @Test
    public void addUserCredential_passwordCredentials_callsUserServiceUpdateUser() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        user.setUsername("test_user");
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)).thenReturn(true);
        doNothing().when(spy).validatePasswordCredentials(any(PasswordCredentialsRequiredUsername.class));
        doNothing().when(spy).validatePassword(anyString());
        doReturn(user).when(spy).checkAndGetUser(anyString());
        spy.addUserCredential(httpHeaders, authToken, userId, jsonBody);
        verify(userService).updateUser(user, false);
    }

    @Test
    public void addUserCredential_passwordCredentialsUserCredentialNotMatchUserName_returnsResponseBuilder() throws Exception {
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setUsername("username");
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        MediaType mediaType = mock(MediaType.class);
        user.setUsername("wrong_user");
        JAXBElement<PasswordCredentialsRequiredUsername> jaxbElement = new JAXBElement<PasswordCredentialsRequiredUsername>(QName.valueOf("credentials"),PasswordCredentialsRequiredUsername.class,passwordCredentialsRequiredUsername);
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doReturn(jaxbElement).when(spy).getJSONCredentials(jsonBody);
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)).thenReturn(true);
        doNothing().when(spy).validatePasswordCredentials(passwordCredentialsRequiredUsername);
        doNothing().when(spy).validatePassword(anyString());
        doReturn(user).when(spy).checkAndGetUser(userId);
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        assertThat("response builder",spy.addUserCredential(httpHeaders, authToken, userId, jsonBody), equalTo(responseBuilder));
    }

    @Test
    public void addUserCredential_passwordCredentialOkResponseCreated_returns200() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        user.setUsername("test_user");
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)).thenReturn(true);
        doNothing().when(spy).validatePasswordCredentials(any(PasswordCredentialsRequiredUsername.class));
        doNothing().when(spy).validatePassword(anyString());
        doReturn(user).when(spy).checkAndGetUser(anyString());
        Response.ResponseBuilder responseBuilder = spy.addUserCredential(httpHeaders, authToken, userId, jsonBody);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void addUserCredential_apiKeyCredentialsUserCredentialNotMatchUserName_returnsBodyBuilder() throws Exception {
        ApiKeyCredentials apiCredentials = new ApiKeyCredentials();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        apiCredentials.setUsername("user");
        String jsonBody = "body";
        MediaType mediaType = mock(MediaType.class);
        user.setUsername("wrong_user");
        JAXBElement<ApiKeyCredentials> jaxbElement = new JAXBElement<ApiKeyCredentials>(QName.valueOf("credentials"),ApiKeyCredentials.class,apiCredentials);
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doReturn(jaxbElement).when(spy).getXMLCredentials(jsonBody);
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE)).thenReturn(true);
        doNothing().when(spy).validateApiKeyCredentials(apiCredentials);
        doReturn(user).when(spy).checkAndGetUser(anyString());
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.addUserCredential(httpHeaders, authToken, userId, jsonBody), equalTo(responseBuilder));
    }

    @Test
    public void addUserCredential_apiKeyCredentialsOkResponse_returns200() throws Exception {
        String jsonBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<apiKeyCredentials\n" +
                "    xmlns=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\"\n" +
                "    username=\"id\"\n" +
                "    apiKey=\"aaaaa-bbbbb-ccccc-12345678\"/>";
        MediaType mediaType = mock(MediaType.class);
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE)).thenReturn(true);
        doReturn(user).when(spy).checkAndGetUser(anyString());
        Response.ResponseBuilder responseBuilder = spy.addUserCredential(httpHeaders, authToken, userId, jsonBody);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void addUserCredentials_notApiKeyCredentialsAndNotPasswordCredentials_returns200() throws Exception {
        JAXBElement<Ec2CredentialsType> credentials = new JAXBElement<Ec2CredentialsType>(new QName("ec2"), Ec2CredentialsType.class, new Ec2CredentialsType());
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_XML_TYPE);
        doReturn(credentials).when(spy).getXMLCredentials("body");
        Response.ResponseBuilder responseBuilder = spy.addUserCredential(httpHeaders, authToken, "userId", "body");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void addUserRole_callsVerifyUserAdminLevelAccess() throws Exception {
        spy.addUserRole(null, authToken, authToken, null);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void addUserRole_userNotIdentityAdminAndRoleIsIdentityAdmin_returnsResponseBuilder() throws Exception {
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        ScopeAccess scopeAccess = new ScopeAccess();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(false);
        when(config.getString("cloudAuth.adminRole")).thenReturn("admin");
        ClientRole clientRole1 = new ClientRole();
        doNothing().when(spy).checkForMultipleIdentityRoles(user,clientRole1);
        clientRole1.setName("admin");
        doReturn(clientRole1).when(spy).checkAndGetClientRole(roleId);
        doReturn(user).when(spy).checkAndGetUser(null);
        when(exceptionHandler.exceptionResponse(any(ForbiddenException.class))).thenReturn(responseBuilder);

        assertThat("response code",spy.addUserRole(null, authToken, null, roleId), equalTo(responseBuilder));
    }

    @Test
    public void addUserRole_userNotIdentityAdminAndRoleNotIdentityAdmin_returns200() throws Exception {
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(false);
        when(config.getString("cloudAuth.adminRole")).thenReturn("admin");
        ClientRole clientRole1 = new ClientRole();
        clientRole1.setName("notAdmin");
        doReturn(clientRole1).when(spy).checkAndGetClientRole(roleId);
        doReturn(new User()).when(spy).checkAndGetUser(null);
        Response.ResponseBuilder responseBuilder = spy.addUserRole(null, authToken, null, roleId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void addUserRole_userIsIdentityAdminAndRoleNotIdentityAdmin_returns200() throws Exception {
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(true);
        when(config.getString("cloudAuth.adminRole")).thenReturn("admin");
        ClientRole clientRole1 = new ClientRole();
        clientRole1.setName("notAdmin");
        doReturn(clientRole1).when(spy).checkAndGetClientRole(roleId);
        doReturn(new User()).when(spy).checkAndGetUser(null);
        Response.ResponseBuilder responseBuilder = spy.addUserRole(null, authToken, null, roleId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void addUserRole_userIsIdentityAdminAndRoleIsIdentityAdmin_returns200() throws Exception {
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(true);
        when(config.getString("cloudAuth.adminRole")).thenReturn("admin");
        ClientRole clientRole1 = new ClientRole();
        clientRole1.setName("admin");
        doReturn(clientRole1).when(spy).checkAndGetClientRole(roleId);
        doReturn(new User()).when(spy).checkAndGetUser(null);
        Response.ResponseBuilder responseBuilder = spy.addUserRole(null, authToken, null, roleId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void checkToken_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.checkToken(null, authToken, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void checkToken_belongsToNotBlank_callsTenantServiceGetTenantRolesForScopeAccess() throws Exception {
        doReturn(null).when(spy).checkAndGetToken("tokenId");
        spy.checkToken(null, authToken, "tokenId", "belongsTo");
        verify(tenantService).getTenantRolesForScopeAccess(any(ScopeAccess.class));
    }

    @Test
    public void checkToken_belongsToNotBlankThrowsNotFoundException_returns404() throws Exception {
        doReturn(null).when(spy).checkAndGetToken("tokenId");
        when(tenantService.getTenantRolesForScopeAccess(any(ScopeAccess.class))).thenReturn(null);
        doReturn(false).when(spy).belongsTo("belongsTo", null);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, "tokenId", "belongsTo");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void checkToken_belongsToBlank_returns200() throws Exception {
        doReturn(null).when(spy).checkAndGetToken("tokenId");
        when(tenantService.getTenantRolesForScopeAccess(any(ScopeAccess.class))).thenReturn(null);
        doReturn(true).when(spy).belongsTo("belongsTo", null);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, "tokenId", "belongsTo");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void checkToken_responseOk_returns200() throws Exception {
        doReturn(null).when(spy).checkAndGetToken("tokenId");
        when(tenantService.getTenantRolesForScopeAccess(any(ScopeAccess.class))).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, "tokenId", "");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void checkToken_throwsBadRequestException_returns400() throws Exception {
        doThrow(new BadRequestException()).when(spy).verifyServiceAdminLevelAccess(authToken);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void checkToken_throwsNotAuthorizedException_returns401() throws Exception {
        doThrow(new NotAuthorizedException()).when(spy).verifyServiceAdminLevelAccess(authToken);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void checkToken_throwsForbiddenException_returns403() throws Exception {
        doThrow(new ForbiddenException()).when(spy).verifyServiceAdminLevelAccess(authToken);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void checkToken_throwsNotFoundException_returns404() throws Exception {
        doThrow(new NotFoundException()).when(spy).verifyServiceAdminLevelAccess(authToken);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void checkToken_throwsException_returns500() throws Exception {
        doThrow(new NullPointerException()).when(spy).verifyServiceAdminLevelAccess(authToken);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(500));
    }

    @Test
    public void deleteEndpoint_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.deleteEndpoint(null, authToken, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteEndpoint_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doThrow(new NotFoundException()).when(spy).checkAndGetTenant(authToken);
        when(exceptionHandler.exceptionResponse(any(NotFoundException.class))).thenReturn(responseBuilder);
        assertThat("response bulider", spy.deleteEndpoint(null, authToken, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void deleteEndpointTemplate_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.deleteEndpointTemplate(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteEndpointTemplate_throwsException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doThrow(new NotFoundException()).when(spy).checkAndGetEndpointTemplate(null);
        when(exceptionHandler.exceptionResponse(any(NotFoundException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.deleteEndpointTemplate(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void deleteRole_callsVerifyIdentityAdminLevelAccess() throws Exception {
        doNothing().when(spy).verifyServiceAdminLevelAccess(anyString());
        spy.deleteRole(null, authToken, roleId);
        verify(spy).verifyIdentityAdminLevelAccess(anyString());
    }

    @Test
    public void deleteRoleFromUserOnTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.deleteRoleFromUserOnTenant(null, authToken, tenantId, null, null);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void deleteRoleFromUserOnTenant_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        NotFoundException notFoundException = new NotFoundException();
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doNothing().when(spy).verifyTokenHasTenantAccess(authToken, tenantId);
        doReturn(new Tenant()).when(spy).checkAndGetTenant(tenantId);
        doThrow(notFoundException).when(spy).checkAndGetUser(null);
        when(exceptionHandler.exceptionResponse(any(NotFoundException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.deleteRoleFromUserOnTenant(null, authToken, tenantId, null, null), equalTo(responseBuilder));
    }

    @Test
    public void deleteService_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.deleteService(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteService_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doThrow(notFoundException).when(spy).checkAndGetApplication(null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.deleteService(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void deleteTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.deleteTenant(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteUser_callsVerifyUserAdminLevelAccess() throws Exception {
        spy.deleteUser(null, authToken, null);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void deleteUser_userAdmin_differentDomain_throwsForbiddenException() throws Exception {
        ForbiddenException forbiddenException = new ForbiddenException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doReturn(user).when(spy).checkAndGetUser("dude");
        when(userService.getUserById("dude")).thenReturn(new User());
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        doThrow(forbiddenException).when(spy).verifyDomain(user, user);
        when(exceptionHandler.exceptionResponse(forbiddenException)).thenReturn(responseBuilder);
        assertThat("response code", spy.deleteUser(null, authToken, "dude"), equalTo(responseBuilder));
    }

    @Test
    public void deleteUserCredential_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.deleteUserCredential(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteUserCredential_passwordCredentials_returns501() throws Exception {
        String credentialType = "passwordCredentials";
        Response.ResponseBuilder responseBuilder = spy.deleteUserCredential(null, authToken, null, credentialType);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(501));
    }

    @Test
    public void deleteUserCredential_notPasswordCredentialAndNotAPIKEYCredential_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        String credentialType = "";
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        assertThat("response builder",spy.deleteUserCredential(null, authToken, null, credentialType) , equalTo(responseBuilder));
    }

    @Test
    public void deleteUserCredential_APIKEYCredential_callsCheckAndGetUser() throws Exception {
        String credentialType = "RAX-KSKEY:apiKeyCredentials";
        spy.deleteUserCredential(null, authToken, "userId", credentialType);
        verify(spy).checkAndGetUser("userId");
    }

    @Test
    public void deleteUserCredential_APIKeyCredential_callsUserServiceUpdateUser() throws Exception {
        String credentialType = "RAX-KSKEY:apiKeyCredentials";
        User user = new User();
        user.setApiKey("123");
        doReturn(user).when(spy).checkAndGetUser("userId");
        spy.deleteUserCredential(null, authToken, "userId", credentialType);
        verify(userService).updateUser(any(User.class), eq(false));
    }

    @Test
    public void deleteUserCredential_APIKeyCredentialResponseNoContent_returns204() throws Exception {
        String credentialType = "RAX-KSKEY:apiKeyCredentials";
        User user = new User();
        user.setApiKey("123");
        doReturn(user).when(spy).checkAndGetUser("userId");
        doNothing().when(userService).updateUser(any(User.class), eq(false));
        Response.ResponseBuilder responseBuilder = spy.deleteUserCredential(null, authToken, "userId", credentialType);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void deleteUserCredential_APIKeyCredentialNull_returnsResponseBuilder() throws Exception {
        String credentialType = "RAX-KSKEY:apiKeyCredentials";
        User user = new User();
        user.setId("123");
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doReturn(user).when(spy).checkAndGetUser("userId");
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.deleteUserCredential(null, authToken, "userId", credentialType), equalTo(responseBuilder));
        assertThat("message", (argumentCaptor.getValue().getMessage()),
                equalTo("Credential type RAX-KSKEY:apiKeyCredentials was not found for User with Id: 123"));
    }

    @Test
    public void deleteUserRole_callsVerifyUserAdminLevelAccess() throws Exception {
        spy.deleteUserRole(null, authToken, null, null);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void deleteUserRole_callsTenantServiceGetGlobalRolesForUser() throws Exception {
        spy.deleteUserRole(null, authToken, userId, null);
        verify(tenantService).getGlobalRolesForUser(any(User.class));
    }

    @Test
    public void deleteUserRole_roleIsNull_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doReturn(user).when(spy).checkAndGetUser(userId);
        when(exceptionHandler.exceptionResponse(any(NotFoundException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.deleteUserRole(null, authToken, userId, null), equalTo(responseBuilder));
    }

    @Test
    public void deleteUserRole_callsScopeAccessService() throws Exception {
        tenantRole.setRoleRsId("id");
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        globalRoles.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(any(User.class))).thenReturn(globalRoles);
        spy.deleteUserRole(null, authToken, userId, "tenantRoleId");
        verify(scopeAccessService).getScopeAccessByAccessToken(authToken);
    }

    @Test
    public void deleteUserRole_notCloudIdentityAdminAndAdminRoleThrowsForbiddenException_returns403() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        tenantRole.setName("identity:admin");
        globalRoles.add(tenantRole);
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doReturn(user).when(spy).checkAndGetUser(userId);
        when(tenantService.getGlobalRolesForUser(user)).thenReturn(globalRoles);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(config.getString("cloudAuth.adminRole")).thenReturn("identity:admin");
        when(exceptionHandler.exceptionResponse(any(ForbiddenException.class))).thenReturn(responseBuilder);
        assertThat("response code", spy.deleteUserRole(httpHeaders, authToken, userId, "tenantRoleId"), equalTo(responseBuilder));
    }

    @Test
    public void deleteUserRole_notCloudIdentityAdminAndNotAdminRole_returns204() throws Exception {
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        tenantRole.setName("notAdmin");
        globalRoles.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(any(User.class))).thenReturn(globalRoles);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(config.getString("cloudAuth.adminRole")).thenReturn("identity:admin");
        Response.ResponseBuilder responseBuilder = spy.deleteUserRole(httpHeaders, authToken, userId, "tenantRoleId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void deleteUserRole_isCloudIdentityAdminAndNotAdminRole_returns204() throws Exception {
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        tenantRole.setName("notAdmin");
        globalRoles.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(any(User.class))).thenReturn(globalRoles);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(config.getString("cloudAuth.adminRole")).thenReturn("identity:admin");
        Response.ResponseBuilder responseBuilder = spy.deleteUserRole(httpHeaders, authToken, userId, "tenantRoleId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void deleteUserRole_isCloudIdentityAdminAndIsAdminRole_returns204() throws Exception {
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        tenantRole.setName("identity:admin");
        globalRoles.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(any(User.class))).thenReturn(globalRoles);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(config.getString("cloudAuth.adminRole")).thenReturn("identity:admin");
        Response.ResponseBuilder responseBuilder = spy.deleteUserRole(httpHeaders, authToken, userId, "tenantRoleId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void deleteUserRole_callsTenantServiceDeleteGlobalRole() throws Exception {
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        globalRoles.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(any(User.class))).thenReturn(globalRoles);
        spy.deleteUserRole(httpHeaders, authToken, userId, "tenantRoleId");
        verify(tenantService).deleteGlobalRole(any(TenantRole.class));
    }

    @Test
    public void deleteUserRole_responseNoContent_returns204() throws Exception {
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        globalRoles.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(any(User.class))).thenReturn(globalRoles);
        doNothing().when(tenantService).deleteGlobalRole(any(TenantRole.class));
        Response.ResponseBuilder responseBuilder = spy.deleteUserRole(httpHeaders, authToken, userId, "tenantRoleId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void getEndpoint_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.getEndpoint(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getEndpoint_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doReturn(tenant).when(spy).checkAndGetTenant(tenantId);
        when(exceptionHandler.exceptionResponse(any(NotFoundException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.getEndpoint(null, authToken, tenantId, null), equalTo(responseBuilder));
    }

    @Test
    public void getEndpoint_callsCheckAndGetEndPointTemplate() throws Exception {
        tenant.addBaseUrlId("endpointId");
        doReturn(tenant).when(spy).checkAndGetTenant(tenantId);
        spy.getEndpoint(null, authToken, tenantId, "endpointId");
        verify(spy).checkAndGetEndpointTemplate("endpointId");
    }

    @Test
    public void getEndpoint_responseOk_returns200() throws Exception {
        tenant.addBaseUrlId("endpointId");
        doReturn(tenant).when(spy).checkAndGetTenant(tenantId);
        doReturn(cloudBaseUrl).when(spy).checkAndGetEndpointTemplate("endpointId");
        Response.ResponseBuilder responseBuilder = spy.getEndpoint(httpHeaders, authToken, tenantId, "endpointId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getEndpointTemplate_callsVerifyServiceAdminLevelAccess() throws Exception {
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        spy.getEndpointTemplate(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getEndpointTemplate_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        NotFoundException notFoundException = new NotFoundException();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doThrow(notFoundException).when(spy).checkAndGetEndpointTemplate(null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.getEndpointTemplate(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void getEndpointTemplate_responseOk_returns200() throws Exception {
        doReturn(cloudBaseUrl).when(spy).checkAndGetEndpointTemplate("endpointTemplateId");
        when(endpointConverterCloudV20.toEndpointTemplate(cloudBaseUrl)).thenReturn(endpointTemplate);
        when(jaxbObjectFactories.getOpenStackIdentityExtKscatalogV1Factory()).thenReturn(new org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.getEndpointTemplate(httpHeaders, authToken, "endpointTemplateId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getRole_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.getRole(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getRole_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doThrow(notFoundException).when(spy).checkAndGetClientRole(null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.getRole(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void getRole_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getRole(httpHeaders, authToken, roleId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getSecretQA_doesNotSetUsername() throws Exception {
        User user = new User();
        user.setSecretAnswer("secret");
        user.setSecretQuestion("question");
        user.setUsername("username");
        SecretQA secretQA2 = new SecretQA();
        doNothing().when(spy).verifyServiceAdminLevelAccess("authToken");
        doReturn(user).when(spy).checkAndGetUser("userId");
        com.rackspace.docs.identity.api.ext.rax_ksqa.v1.ObjectFactory objectFactory = mock(com.rackspace.docs.identity.api.ext.rax_ksqa.v1.ObjectFactory.class);
        when(jaxbObjectFactories.getRackspaceIdentityExtKsqaV1Factory()).thenReturn(objectFactory);
        when(objectFactory.createSecretQA()).thenReturn(secretQA2);
        when(objectFactory.createSecretQA(secretQA2)).thenReturn(new JAXBElement<SecretQA>(QName.valueOf("foo"),SecretQA.class,secretQA2));
        Response result = spy.getSecretQA(null, "authToken", "userId").build();
        assertThat("username", ( (SecretQA) result.getEntity()).getUsername(), equalTo(null));
    }

    @Test
    public void getSecretQA_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.getSecretQA(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getSecretQA_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doThrow(notFoundException).when(spy).checkAndGetUser(null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.getSecretQA(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void getSecretQA_responseOk_returns200() throws Exception {
        when(jaxbObjectFactories.getRackspaceIdentityExtKsqaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_ksqa.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.getSecretQA(httpHeaders, authToken, userId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getService_callsverifyServiceAdminLevelAccess() throws Exception {
        spy.getService(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getService_throwsNotFoundException_returnResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doThrow(notFoundException).when(spy).checkAndGetApplication(null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.getService(null, authToken, null), equalTo(responseBuilder));
    }


    @Test
    public void getService_responseOk_returns200() throws Exception {
        doReturn(new Application()).when(spy).checkAndGetApplication("serviceId");
        when(jaxbObjectFactories.getOpenStackIdentityExtKsadmnV1Factory()).thenReturn(new org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory());
        when(serviceConverterCloudV20.toService(any(Application.class))).thenReturn(new Service());
        Response.ResponseBuilder responseBuilder = spy.getService(httpHeaders, authToken, "serviceId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getTenantById_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.getTenantById(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getTenantById_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doThrow(notFoundException).when(spy).checkAndGetTenant(null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.getTenantById(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void getTenantById_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getTenantById(httpHeaders, authToken, tenantId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getTenantByName_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.getTenantByName(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getTenantByName_tenantIsNullThrowsNotFoundException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        when(exceptionHandler.exceptionResponse(any(NotFoundException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.getTenantByName(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void getTenantByName_responseOk_returns200() throws Exception {
        when(tenantService.getTenantByName(tenantId)).thenReturn(tenant);
        Response.ResponseBuilder responseBuilder = spy.getTenantByName(httpHeaders, authToken, tenantId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserById_callerDoesNotHaveDefaultUserRole_callsVerifyUserAdminLevelAccess() throws Exception {
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        spy.getUserById(null, authToken, null);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void getUserById_callerDoesNotHaveDefaultUserRole_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(new User()).when(spy).getUser(scopeAccess);
        when(authorizationService.authorizeCloudUser(scopeAccess)).thenReturn(false);
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        when(exceptionHandler.exceptionResponse(any(NotFoundException.class))).thenReturn(responseBuilder);
        assertThat("response builder",spy.getUserById(null, authToken, null),equalTo(responseBuilder));
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void getUserById_callerHasUserAdminRole_callsVerifyDomain() throws Exception {
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(this.userService.getUserById(any(String.class))).thenReturn(user);
        spy.getUserById(null, authToken, null);
        verify(spy).verifyDomain(any(User.class), any(User.class));
    }

    @Test
    public void getUserById_isCloudUserAndIdMatchesResponseOk_returns200() throws Exception {
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        Response.ResponseBuilder responseBuilder = spy.getUserById(httpHeaders, authToken, userId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserById_isCloudUserAndIdDoesNotMatchThrowForbiddenException_returnsResponseBuilder() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(authorizationService.authorizeCloudUser(scopeAccess)).thenReturn(true);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        when(exceptionHandler.exceptionResponse(any(ForbiddenException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserById(httpHeaders, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void getUserById_responseOk_returns200() throws Exception {
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        Response.ResponseBuilder responseBuilder = spy.getUserById(httpHeaders, authToken, userId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserByName_callsVerifyUserLevelAccess() throws Exception {
        spy.getUserByName(null, authToken, null);
        verify(spy).verifyUserLevelAccess(authToken);
    }

    @Test
    public void getUserByName_userIsNullThrowsNotFoundException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        when(exceptionHandler.exceptionResponse(any(NotFoundException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserByName(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void getUserByName_callsScopeAccessServiceGetScopeAccessByAccessToken() throws Exception {
        when(userService.getUser("userName")).thenReturn(user);
        spy.getUserByName(null, authToken, "userName");
        verify(scopeAccessService, times(2)).getScopeAccessByAccessToken(authToken);
    }

    @Test
    public void getUserByName_adminUser_callsVerifyDomain() throws Exception {
        when(userService.getUser("userName")).thenReturn(user);
        doReturn(true).when(spy).isUserAdmin(any(ScopeAccess.class), any(List.class));
        when(authorizationService.authorizeCloudUserAdmin(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        spy.getUserByName(null, authToken, "userName");
        verify(spy).verifyDomain(user, user);
    }

    @Test
    public void getUserByName_defaultUser_callsVerifySelf() throws Exception {
        when(userService.getUser("userName")).thenReturn(user);
        when(authorizationService.authorizeCloudUser(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        spy.getUserByName(null, authToken, "userName");
        verify(spy).verifySelf(authToken, user);
    }

    @Test
    public void getUserByName_responseOk_returns200() throws Exception {
        when(userService.getUser("userName")).thenReturn(user);
        doReturn(false).when(spy).isUserAdmin(any(ScopeAccess.class), any(List.class));
        doReturn(false).when(spy).isDefaultUser(any(ScopeAccess.class), any(List.class));
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.getUserByName(httpHeaders, authToken, "userName");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserCredential_callsVerifyUserLevelAccess() throws Exception {
        spy.getUserCredential(null, authToken, null, null);
        verify(spy).verifyUserLevelAccess(authToken);
    }

    @Test
    public void getUserCredential_notPasswordCredentialOrAPIKeyCredentialThrowsBadRequest_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserCredential(null, authToken, null, ""), equalTo(responseBuilder));
    }

    @Test
    public void getUserCredential_cloudUser_callsGetUser() throws Exception {
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        spy.getUserCredential(null, authToken, userId, apiKeyCredentials);
        verify(spy).getUser(any(ScopeAccess.class));
    }

    @Test
    public void getUserCredential_cloudUserIdNotMatchThrowsForbiddenException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        when(exceptionHandler.exceptionResponse(any(ForbiddenException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserCredential(null, authToken, "", apiKeyCredentials), equalTo(responseBuilder));
    }

    @Test
    public void getUserCredential_userIsNull_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        when(userService.getUserById("id")).thenReturn(null);
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        when(exceptionHandler.exceptionResponse(any(NotFoundException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserCredential(null, authToken, "id", apiKeyCredentials), equalTo(responseBuilder));
    }

    @Test
    public void getUserCredential_cloudAdminUser_callsGetUser() throws Exception {
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        spy.getUserCredential(null, authToken, userId, passwordCredentials);
        verify(spy).getUser(any(ScopeAccess.class));
    }

    @Test
    public void getUserCredential_cloudAdminUserIdNotMatchThrowsForbiddenException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        when(exceptionHandler.exceptionResponse(any(ForbiddenException.class))).thenReturn(responseBuilder);
        assertThat("response code",spy.getUserCredential(null, authToken, "", passwordCredentials), equalTo(responseBuilder));
    }

    @Test
    public void getUserCredential_userIsNullThrowsNotFoundException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        when(exceptionHandler.exceptionResponse(any(NotFoundException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserCredential(null, authToken, "", apiKeyCredentials), equalTo(responseBuilder));
    }

    @Test
    public void getUserCredential_passwordCredentialUserPasswordIsBlank_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        when(userService.getUserById(userId)).thenReturn(new User());
        when(exceptionHandler.exceptionResponse(any(NotFoundException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserCredential(null, authToken, userId, passwordCredentials), equalTo(responseBuilder));
    }

    @Test
    public void getUserCredential_passwordCredentialResponseOk_returns200() throws Exception {
        user.setPassword("123");
        when(userService.getUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.getUserCredential(httpHeaders, authToken, userId, passwordCredentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserCredential_apiKeyCredentialResponseOk_returns200() throws Exception {
        user.setApiKey("123");
        when(userService.getUserById(userId)).thenReturn(user);
        when(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.getUserCredential(httpHeaders, authToken, userId, apiKeyCredentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserCredential_apiKeyCredentialAPIKeyIsBlank_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        when(userService.getUserById(userId)).thenReturn(user);
        when(exceptionHandler.exceptionResponse(any(NotFoundException.class))).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserCredential(httpHeaders, authToken, userId, apiKeyCredentials), equalTo(responseBuilder));
    }

    @Test
    public void getUserRole_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.getUserRole(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getUserRole_roleIsNullThrowsNotFoundException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        User user = new User();
        TenantRole tenantRole = new TenantRole();
        tenantRole.setRoleRsId("different");
        List<TenantRole> tenantRoleList = new ArrayList<TenantRole>();
        tenantRoleList.add(tenantRole);
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doReturn(user).when(spy).checkAndGetUser("userId");
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        when(tenantService.getGlobalRolesForUser(user)).thenReturn(tenantRoleList);
        assertThat("response builder", spy.getUserRole(null, authToken, "userId", "roleId"), equalTo(responseBuilder));
        assertThat("correct exception type",argumentCaptor.getValue(),instanceOf(NotFoundException.class));
    }

    @Test
    public void getUserRole_responseOk_returns200() throws Exception {
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        globalRoles.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(any(User.class))).thenReturn(globalRoles);
        Response.ResponseBuilder responseBuilder = spy.getUserRole(httpHeaders, authToken, userId, "tenantRoleId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listCredentials_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listCredentials(null, authToken, null, null, 0);
        verify(spy).verifyUserLevelAccess(authToken);
    }

    @Test
    public void listCredentials_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        doThrow(notFoundException).when(spy).checkAndGetUser(null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder",spy.listCredentials(null, authToken, null, null, 0),equalTo(responseBuilder));
    }

    @Test
    public void listCredentials_userAdmin_callsVerifySelf() throws Exception {
        doReturn(true).when(spy).isUserAdmin(any(ScopeAccess.class), any(List.class));
        spy.listCredentials(null, authToken, userId, null, null);
        verify(spy).verifySelf(eq(authToken), any(User.class));
    }

    @Test
    public void listCredential_defaultUser_callsVerifySelf() throws Exception {
        doReturn(true).when(spy).isDefaultUser(any(ScopeAccess.class), any(List.class));
        spy.listCredentials(null, authToken, userId, null, null);
        verify(spy).verifySelf(eq(authToken), any(User.class));
    }

    @Test
    public void listCredential_userPasswordIsNotBlankResponseOk_returns200() throws Exception {
        user.setPassword("123");
        doReturn(user).when(spy).checkAndGetUser(userId);
        Response.ResponseBuilder responseBuilder = spy.listCredentials(httpHeaders, authToken, userId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listCredential_userApiKeyIsNotBlankResponseOk_returns200() throws Exception {
        user.setApiKey("123");
        doReturn(user).when(spy).checkAndGetUser(userId);
        when(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.listCredentials(httpHeaders, authToken, userId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listCredential_userPasswordIsBlankAndApiKeyIsBlankResponseOk_returns200() throws Exception {
        doReturn(user).when(spy).checkAndGetUser(userId);
        Response.ResponseBuilder responseBuilder = spy.listCredentials(httpHeaders, authToken, userId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listCredential_userPasswordIsNotBlankAndApiKeyIsNotBlank_returns200() throws Exception {
        user.setApiKey("123");
        user.setPassword("123");
        doReturn(user).when(spy).checkAndGetUser(userId);
        when(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.listCredentials(httpHeaders, authToken, userId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void isUserAdmin_tenantRolesIsNull_callsTenantService() throws Exception {
        spy.isUserAdmin(null, null);
        verify(tenantService).getTenantRolesForScopeAccess(null);
    }

    @Test
    public void isUserAdmin_returnsFalse() throws Exception {
        Boolean hasRole = spy.isUserAdmin(null, null);
        assertThat("boolean value", hasRole, equalTo(false));
    }

    @Test
    public void isUserAdmin_returnsTrue() throws Exception {
        tenantRole.setName("identity:user-admin");
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        globalRoles.add(tenantRole);
        Boolean hasRole = spy.isUserAdmin(null, globalRoles);
        assertThat("boolean value", hasRole, equalTo(true));
    }

    @Test
    public void isUserAdmin_nameNotIdentityUserAdmin_returnsFalse() throws Exception {
        tenantRole.setName("notAdmin");
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        globalRoles.add(tenantRole);
        Boolean hasRole = spy.isUserAdmin(null, globalRoles);
        assertThat("boolean value", hasRole, equalTo(false));
    }

    @Test
    public void isDefaultUser_tenantRolesIsNull_callsTenantService() throws Exception {
        spy.isDefaultUser(null, null);
        verify(tenantService).getTenantRolesForScopeAccess(null);
    }

    @Test
    public void isDefaultUser_returnsFalse() throws Exception {
        Boolean hasRole = spy.isUserAdmin(null, null);
        assertThat("boolean value", hasRole, equalTo(false));
    }

    @Test
    public void isDefaultUser_returnsTrue() throws Exception {
        tenantRole.setName("identity:default");
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        globalRoles.add(tenantRole);
        Boolean hasRole = spy.isDefaultUser(null, globalRoles);
        assertThat("boolean value", hasRole, equalTo(true));
    }

    @Test
    public void isDefaultUser_nameNotIdentityDefault_returnsFalse() throws Exception {
        tenantRole.setName("notDefault");
        List<TenantRole> globalRoles = new ArrayList<TenantRole>();
        globalRoles.add(tenantRole);
        Boolean hasRole = spy.isDefaultUser(null, globalRoles);
        assertThat("boolean value", hasRole, equalTo(false));
    }

    @Test
    public void listEndpoints_verifyServiceAdminLevelAccess() throws Exception {
        spy.listEndpoints(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listEndpoints_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doNothing().when(spy).verifyTokenHasTenantAccess(authToken, null);
        doThrow(notFoundException).when(spy).checkAndGetTenant(null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.listEndpoints(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void listEndpoints_baseUrlIdsIsNotNullResponseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listEndpoints(httpHeaders, authToken, tenantId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listEndpoints_baseUrlIdsIsNotNullCallsEndpointService_getBaseUrlById() throws Exception {
        String[] ids = {"1"};
        tenant.setBaseUrlIds(ids);
        ArrayList<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        cloudBaseUrlList.add(cloudBaseUrl);
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doNothing().when(spy).verifyTokenHasTenantAccess(authToken, tenantId);
        doReturn(tenant).when(spy).checkAndGetTenant(tenantId);
        when(endpointService.getGlobalBaseUrls()).thenReturn(cloudBaseUrlList);
        when(endpointService.getBaseUrlById(1)).thenReturn(cloudBaseUrl);
        spy.listEndpoints(httpHeaders, authToken, tenantId);
        verify(endpointService).getBaseUrlById(1);
    }

    @Test
    public void listEndpoints_baseUrlIdsIsNullResponseOk_returns200() throws Exception {
        tenant.setBaseUrlIds(null);
        doReturn(tenant).when(spy).checkAndGetTenant(tenantId);
        Response.ResponseBuilder responseBuilder = spy.listEndpoints(httpHeaders, authToken, tenantId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listEndpointTemplates_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listEndpointTemplates(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listEndpointTemplates_throwsNotAuthorizedException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        NotAuthorizedException notAuthorizedException = new NotAuthorizedException();
        doThrow(notAuthorizedException).when(spy).verifyServiceAdminLevelAccess(authToken);
        when(exceptionHandler.exceptionResponse(notAuthorizedException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.listEndpointTemplates(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void listEndpointTemplates_serviceIdIsBlankResponseOk_returns200() throws Exception {
        when(jaxbObjectFactories.getOpenStackIdentityExtKscatalogV1Factory()).thenReturn(new org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.listEndpointTemplates(httpHeaders, authToken, "");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listEndpointTemplates_serviceIdNotBlankResponseOk_returns200() throws Exception {
        List<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        when(jaxbObjectFactories.getOpenStackIdentityExtKscatalogV1Factory()).thenReturn(new org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory());
        doReturn(new Application()).when(spy).checkAndGetApplication("serviceId");
        when(endpointService.getBaseUrlsByServiceType(anyString())).thenReturn(cloudBaseUrlList);
        Response.ResponseBuilder responseBuilder = spy.listEndpointTemplates(httpHeaders, authToken, "serviceId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listRoles_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listRoles(null, authToken, null, null, 0);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listRoles_serviceIdIsBlankResponseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listRoles(httpHeaders, authToken, "", null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listRoles_serviceIdNotBlankResponseOk_returns200() throws Exception {
        List<ClientRole> roles = new ArrayList<ClientRole>();
        when(clientService.getClientRolesByClientId("serviceId")).thenReturn(roles);
        Response.ResponseBuilder responseBuilder = spy.listRoles(httpHeaders, authToken, "serviceId", null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listRolesForTenant_verifyServiceAdminLevelAccess() throws Exception {
        spy.listRolesForTenant(null, authToken, null, null, 0);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listRolesForTenant_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        NotFoundException notFoundException = new NotFoundException();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doNothing().when(spy).verifyTokenHasTenantAccess(authToken, null);
        doThrow(notFoundException).when(spy).checkAndGetTenant(null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.listRolesForTenant(null, authToken, null, null, 0), equalTo(responseBuilder));
    }

    @Test
    public void listRolesForTenant_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listRolesForTenant(httpHeaders, authToken, tenantId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listRolesForUserOnTenant_verifyServiceAdminLevelAccess() throws Exception {
        spy.listRolesForUserOnTenant(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listRolesForUserOnTenant_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doNothing().when(spy).verifyTokenHasTenantAccess(authToken,null);
        doThrow(notFoundException).when(spy).checkAndGetTenant(null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder",spy.listRolesForUserOnTenant(null, authToken, null, null),equalTo(responseBuilder));
    }

    @Test
    public void listRolesForUserOnTenant_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listRolesForUserOnTenant(httpHeaders, authToken, tenantId, userId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listServices_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listServices(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listServices_responseOk_returns200() throws Exception {
        when(jaxbObjectFactories.getOpenStackIdentityExtKsadmnV1Factory()).thenReturn(new org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.listServices(httpHeaders, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUserGlobalRoles_isUserAdmin_callsVerifyDomain() throws Exception {
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        doReturn(user).when(spy).getUser(org.mockito.Matchers.any(ScopeAccess.class));
        when(userService.getUserById(anyString())).thenReturn(user);
        when(userService.getUser(anyString())).thenReturn(user);
        when(authorizationService.authorizeCloudIdentityAdmin(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudUser(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(false);
        spy.listUserGlobalRoles(null, authToken, null);
        verify(spy).verifyDomain(org.mockito.Matchers.any(User.class), org.mockito.Matchers.any(User.class));
    }

    @Test
    public void listUserGlobalRoles_isDefaultUser_callsVerifySelf() throws Exception {
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        doReturn(user).when(spy).getUser(org.mockito.Matchers.any(ScopeAccess.class));
        when(userService.getUserById(anyString())).thenReturn(user);
        when(userService.getUser(anyString())).thenReturn(user);
        when(authorizationService.authorizeCloudIdentityAdmin(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudUser(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(true);
        spy.listUserGlobalRoles(null, authToken, null);
        verify(spy).verifySelf(anyString(), org.mockito.Matchers.any(User.class));
    }

    @Test
    public void listUserGlobalRoles_callsVerifyUserLevelAccess() throws Exception {
        spy.listUserGlobalRoles(null, authToken, null);
        verify(spy).verifyUserLevelAccess(authToken);
    }

    @Test
    public void listUserGlobalRoles_notCloudIdentityAdminAndNotCloudServiceAdminResponseOk_returns200() throws Exception {
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);
        Response.ResponseBuilder responseBuilder = spy.listUserGlobalRoles(httpHeaders, authToken, userId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUserGlobalRolesByServiceId_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listUserGlobalRolesByServiceId(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listUserGlobalRolesByServiceId_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listUserGlobalRolesByServiceId(httpHeaders, authToken, userId, "serviceId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listGroups_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listGroups(null, authToken, null, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void listGroups_responseOk_returns200() throws Exception {
        List<Group> groups = new ArrayList<Group>();
        groups.add(group);
        when(userGroupService.getGroups("marker", 1)).thenReturn(groups);
        Response.ResponseBuilder responseBuilder = spy.listGroups(null, authToken, null, "marker", 1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUsers_callerIsNotDefaultUser_callsVerifyUserAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(false);
        spy.listUsers(null, authToken, null, 0);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void listUsersForTenant_CallsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listUsersForTenant(null, authToken, null, null, 0);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void listUsersForTenant_callsVerifyTokenHasTenantAccess() throws Exception {
        spy.listUsersForTenant(null, authToken, tenantId, null, null);
        verify(spy).verifyTokenHasTenantAccess(authToken, tenantId);
    }

    @Test
    public void listUsersForTenant_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listUsersForTenant(httpHeaders, authToken, tenantId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUsersWithRoleForTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listUsersWithRoleForTenant(null, authToken, null, null, null, 0);
        verify(spy).verifyUserAdminLevelAccess(authToken);
    }

    @Test
    public void listUsersWithRoleForTenant_callsVerifyTokenHasTenantAccess() throws Exception {
        spy.listUsersWithRoleForTenant(null, authToken, tenantId, null, null, null);
        verify(spy).verifyTokenHasTenantAccess(authToken, tenantId);
    }

    @Test
    public void listUsersWithRoleForTenant_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listUsersWithRoleForTenant(httpHeaders, authToken, tenantId, roleId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void setUserEnabled_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.setUserEnabled(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void setUserEnabled_userService_callsUpdateUser() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setEnabled(true);
        spy.setUserEnabled(null, authToken, userId, user1);
        verify(userService).updateUser(any(User.class), eq(false));
    }

    @Test
    public void setUserEnabled_responseOk_returns200() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setEnabled(true);
        Response.ResponseBuilder responseBuilder = spy.setUserEnabled(null, authToken, userId, user1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void updateSecretQA_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.updateSecretQA(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void updateSecretQA_secretAnswerIsBlankThrowsBadRequest_returns400() throws Exception {
        secretQA.setAnswer("");
        Response.ResponseBuilder responseBuilder = spy.updateSecretQA(null, authToken, null, secretQA);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void updateSecretQA_secretQuestionIsBlankThrowsBadRequest_returns400() throws Exception {
        secretQA.setQuestion("");
        Response.ResponseBuilder responseBuilder = spy.updateSecretQA(null, authToken, null, secretQA);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void updateSecretQA_userService_callsUpdateUser() throws Exception {
        spy.updateSecretQA(null, authToken, userId, secretQA);
        verify(userService).updateUser(any(User.class), eq(false));
    }

    @Test
    public void updateSecretQA_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.updateSecretQA(null, authToken, userId, secretQA);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void updateTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.updateTenant(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void updateTenant_tenantService_callsUpdateTenant() throws Exception {
        org.openstack.docs.identity.api.v2.Tenant tenant1 = new org.openstack.docs.identity.api.v2.Tenant();
        tenant1.setEnabled(true);
        tenant1.setName("tenant");
        spy.updateTenant(null, authToken, tenantId, tenant1);
        verify(tenantService).updateTenant(any(Tenant.class));
    }

    @Test
    public void updateTenant_responseOk_returns200() throws Exception {
        org.openstack.docs.identity.api.v2.Tenant tenant1 = new org.openstack.docs.identity.api.v2.Tenant();
        tenant1.setEnabled(true);
        tenant1.setName("tenant");
        Response.ResponseBuilder responseBuilder = spy.updateTenant(null, authToken, tenantId, tenant1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test(expected = ForbiddenException.class)
    public void verifyDomain_domainIdIsNull_throwsForbiddenException() throws Exception {
        User caller = new User();
        User retrievedUser = new User();
        retrievedUser.setDomainId("domainId");
        spy.verifyDomain(retrievedUser, caller);
    }

    @Test(expected = ForbiddenException.class)
    public void verifyDomain_callerDomainIdNotMatchRetrievedUserDomainId_throwsForbiddenException() throws Exception {
        User caller = new User();
        caller.setDomainId("notSame");
        User retrievedUser = new User();
        retrievedUser.setDomainId("domainId");
        spy.verifyDomain(retrievedUser, caller);
    }

    @Test
    public void verifyDomain_sameDomainId_success() throws Exception {
        User caller = new User();
        caller.setDomainId("domainId");
        User retrievedUser = new User();
        retrievedUser.setDomainId("domainId");
        spy.verifyDomain(retrievedUser, caller);
    }

    @Test
    public void updateUser_callsVerifyUserLevelAccess() throws Exception {
        spy.updateUser(null, authToken, null, null);
        verify(spy).verifyUserLevelAccess(authToken);
    }

    @Test
    public void updateUser_calls_userService_updateUserById() throws Exception {
        userOS.setId(userId);
        spy.updateUser(null, authToken, userId, userOS);
        verify(userService).updateUserById(any(User.class), anyBoolean());
    }

    @Test
    public void updateUser_callsAuthorizeCloudUser() throws Exception {
        User user = new User();
        user.setId(userId);
        userOS.setId(userId);
        ScopeAccess scopeAccess = new ScopeAccess();
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        doReturn(user).when(spy).checkAndGetUser(anyString());
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        spy.updateUser(null, authToken, userId, userOS);
        verify(authorizationService).authorizeCloudUser(scopeAccess);
    }

    @Test
    public void updateUser_passwordNotNull_callsValidatePassword() throws Exception {
        userOS.setPassword("123");
        userOS.setId(userId);
        spy.updateUser(null, authToken, userId, userOS);
        verify(spy).validatePassword("123");
    }

    @Test
    public void updateUser_authorizationServiceAuthorizeCloudUserIsTrue_callsUserServiceGetUserByAuthToken() throws Exception {
        User user = new User();
        user.setId(userId);
        userOS.setId(userId);
        doReturn(user).when(spy).checkAndGetUser(userId);
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        spy.updateUser(null, authToken, userId, userOS);
        verify(userService).getUserByAuthToken(authToken);
    }

    @Test
    public void updateUser_authorizationServiceAuthorizeCloudUserIsTrueIdNotMatch_returns403() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        User user1 = new User();
        user1.setId(userId);
        userOS.setId(userId);
        user.setId("notMatch");
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doReturn(user1).when(spy).checkAndGetUser(userId);
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        when(exceptionHandler.exceptionResponse(any(ForbiddenException.class))).thenReturn(responseBuilder);
        assertThat("response code", spy.updateUser(null, authToken, userId, userOS), equalTo(responseBuilder));
    }

    @Test
    public void updateUser_authorizationServiceAuthorizeCloudUserIsTrueIdMatch_returns200() throws Exception {
        userOS.setEnabled(false);
        User user1 = new User();
        user1.setId("123");
        userOS.setId("123");
        user.setId("123");
        User user2 = new User();
        user2.setId("456");
        doReturn(user1).when(spy).checkAndGetUser("123");
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUserByAuthToken(authToken)).thenReturn(user).thenReturn(user2);
        Response.ResponseBuilder responseBuilder = spy.updateUser(null, authToken, "123", userOS);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void updateUser_cloudUserAdminIsTrue_callsUserServiceGetUserByAuthToken() throws Exception {
        User user = new User();
        user.setId(userId);
        userOS.setId(userId);
        doReturn(user).when(spy).checkAndGetUser(userId);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        spy.updateUser(null, authToken, userId, userOS);
        verify(userService).getUserByAuthToken(authToken);
    }

    @Test
    public void updateUser_cloudUserAdminIsTrue_callsVerifyDomain() throws Exception {
        User user = new User();
        user.setId(userId);
        userOS.setId(userId);
        doReturn(user).when(spy).checkAndGetUser(userId);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        spy.updateUser(null, authToken, userId, userOS);
        verify(spy).verifyDomain(any(User.class), eq(user));
    }

    @Test
    public void updateUser_userDisabled_callsScopeAccessServiceExpiresAllTokensForUsers() throws Exception {
        userOS.setId(userId);
        User user = mock(User.class);
        doReturn(user).when(spy).checkAndGetUser(userId);
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(user);
        doNothing().when(user).copyChanges(any(User.class));
        when(user.getId()).thenReturn(userId);
        when(user.isDisabled()).thenReturn(true);
        spy.updateUser(httpHeaders, authToken, userId, userOS);
        verify(scopeAccessService).expireAllTokensForUser(user.getUsername());
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_missingUsername_throwsBadRequestException() throws Exception {
        defaultCloud20Service.validateUser(new org.openstack.docs.identity.api.v2.User());
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_missingEmail_throwsBadRequestException() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        defaultCloud20Service.validateUser(user1);
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_withInvalidEmail_throwsBadRequestException() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("foo");
        defaultCloud20Service.validateUser(user1);
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_withInvalidEmail2_throwsBadRequestException() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("foo@");
        defaultCloud20Service.validateUser(user1);
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_withInvalidEmail3_throwsBadRequestException() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("foo.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test(expected = BadRequestException.class)
    public void validateUser_withInvalidEmail4_throwsBadRequestException() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("foo@.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("foo@bar.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail2_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("racker@rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail3_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("john.smith@rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail4_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("john.\"elGuapo\".smith@rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail5_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("1@rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail6_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("1@1.net");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail7_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("1@1.rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void validateUser_withValidEmail8_succeeds() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setUsername("username");
        user1.setEmail("R_a_c_K_e_r_4000@rackspace.com");
        defaultCloud20Service.validateUser(user1);
    }

    @Test
    public void updateUserApiKeyCredentials_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.updateUserApiKeyCredentials(null, authToken, null, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void updateUserApiKeyCredentials_apiKeyIsBlankThrowsBadRequest_returns400() throws Exception {
        ApiKeyCredentials creds = new ApiKeyCredentials();
        creds.setApiKey("");
        Response.ResponseBuilder responseBuilder = spy.updateUserApiKeyCredentials(null, authToken, null, null, creds);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void updateUserApiKeyCredentials_callsValidateUsername() throws Exception {
        ApiKeyCredentials creds = new ApiKeyCredentials();
        creds.setApiKey("123");
        creds.setUsername("username");
        spy.updateUserApiKeyCredentials(null, authToken, null, null, creds);
        verify(spy).validateUsername("username");
    }

    @Test
    public void updateUserApiKeyCredentials_credUserIsNullThrowsNotFoundException_returns404() throws Exception {
        ApiKeyCredentials creds = new ApiKeyCredentials();
        creds.setApiKey("123");
        creds.setUsername("username");
        Response.ResponseBuilder responseBuilder = spy.updateUserApiKeyCredentials(null, authToken, null, null, creds);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void updateUserApiKeyCredentials_credsUsernameNotMatchUserGetUsername_returns400() throws Exception {
        ApiKeyCredentials creds = new ApiKeyCredentials();
        creds.setApiKey("123");
        creds.setUsername("username");
        when(userService.getUser(anyString())).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.updateUserApiKeyCredentials(null, authToken, userId, null, creds);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void updateUserApiKeyCredentials_userService_callsUpdateUser() throws Exception {
        ApiKeyCredentials creds = new ApiKeyCredentials();
        creds.setApiKey("123");
        creds.setUsername("username");
        user.setUsername("username");
        when(userService.getUser(anyString())).thenReturn(user);
        spy.updateUserApiKeyCredentials(null, authToken, userId, null, creds);
        verify(userService).updateUser(any(User.class), eq(false));
    }

    @Test
    public void updateUserApiKeyCredentials_responseOk_returns200() throws Exception {
        ApiKeyCredentials creds = new ApiKeyCredentials();
        creds.setApiKey("123");
        creds.setUsername("username");
        user.setUsername("username");
        when(userService.getUser(anyString())).thenReturn(user);
        when(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.updateUserApiKeyCredentials(null, authToken, userId, null, creds);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }


    @Test
    public void updateUserPasswordCredentials_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.updateUserPasswordCredentials(null, authToken, null, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void updateUserPasswordCredentials_credsUsernameNotMatchUserGetUsername_returns400() throws Exception {
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername();
        creds.setUsername("username");
        creds.setPassword("ABCdef123");
        Response.ResponseBuilder responseBuilder = spy.updateUserPasswordCredentials(null, authToken, userId, null, creds);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void updateUserPasswordCredentials_withNullPassword_returns400() throws Exception {
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername();
        creds.setUsername("username");
        Response.ResponseBuilder responseBuilder = spy.updateUserPasswordCredentials(null, authToken, userId, null, creds);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void updateUserPasswordCredentials_withNullUsername_returns400() throws Exception {
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername();
        creds.setUsername(null);
        creds.setPassword("foo");
        Response.ResponseBuilder responseBuilder = spy.updateUserPasswordCredentials(null, authToken, userId, null, creds);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void updateUserPasswordCredentials_withValidCredentials_callsUserService_updateUserMethod() throws Exception {
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername();
        creds.setUsername(userId);
        creds.setPassword("ABCdef123");
        spy.updateUserPasswordCredentials(null, authToken, userId, null, creds);
        verify(userService).updateUser(user, false);
    }

    @Test
    public void validatePassword_ValidPassword_succeeds() throws Exception {
        defaultCloud20Service.validatePassword("Ab345678");
    }

    @Test(expected = BadRequestException.class)
    public void validatePassword_LessThan8CharactersLong_throwsException() throws Exception {
        defaultCloud20Service.validatePassword("123");
    }

    @Test(expected = BadRequestException.class)
    public void validatePassword_DoesNotContainUpperCaseLetter_throwsException() throws Exception {
        defaultCloud20Service.validatePassword("ab345678");
    }

    @Test(expected = BadRequestException.class)
    public void validatePassword_DoesNotContainLowerCaseLetter_throwsException() throws Exception {
        defaultCloud20Service.validatePassword("AB345678");
    }

    @Test(expected = BadRequestException.class)
    public void validatePassword_DoesNotContainNumericCharacter_throwsException() throws Exception {
        defaultCloud20Service.validatePassword("Abcdefghik");
    }

    @Test
    public void setDomainId_callsAuthorizationService_authorizeCloudUserAdmin() throws Exception {
        ScopeAccess scopeAccessByAccessToken = new ScopeAccess();
        User userDO = new User();
        defaultCloud20Service.setDomainId(scopeAccessByAccessToken, userDO);
        verify(authorizationService).authorizeCloudUserAdmin(scopeAccessByAccessToken);
    }

    @Test
    public void setDomainId_callerIsUserAdmin_callsUserService() throws Exception {
        User userDO = new User();
        userDO.setUsername("dude");
        Attribute[] attributes = {new Attribute(LdapRepository.ATTR_UID, "dude")};
        ScopeAccess scopeAccessByAccessToken = mock(ScopeAccess.class);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccessByAccessToken)).thenReturn(true);
        when(userService.getUser("dude")).thenReturn(userDO);
        when(scopeAccessByAccessToken.getLDAPEntry()).thenReturn(new SearchResultEntry("", attributes, new Control[]{}));
        defaultCloud20Service.setDomainId(scopeAccessByAccessToken, userDO);
        verify(userService).getUser("dude");
    }

    @Test(expected = NotAuthorizedException.class)
    public void verifyServiceAdminLevelAccess_withNullToken_throwsNotAuthorizedException() throws Exception {
        defaultCloud20Service.verifyServiceAdminLevelAccess(null);
    }

    @Test(expected = NotAuthorizedException.class)
    public void verifyServiceAdminLevelAccess_withBlankToken_throwsNotAuthorizedException() throws Exception {
        defaultCloud20Service.verifyServiceAdminLevelAccess("");
    }

    @Test
    public void verifyServiceAdminLevelAccess_identityAdminCaller_succeeds() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString("admin");
        userScopeAccess.setAccessTokenExp(new Date(2099, 1, 1));
        when(scopeAccessService.getScopeAccessByAccessToken("admin")).thenReturn(userScopeAccess);
        when(authorizationService.authorizeCloudIdentityAdmin(userScopeAccess)).thenReturn(true);
        defaultCloud20Service.verifyServiceAdminLevelAccess("admin");
    }

    @Test(expected = ForbiddenException.class)
    public void verifyServiceAdminLevelAccess_userAdminCaller_throwsForbiddenException() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenString("admin");
        userScopeAccess.setAccessTokenExp(new Date(2099, 1, 1));
        when(scopeAccessService.getScopeAccessByAccessToken("admin")).thenReturn(userScopeAccess);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);
        defaultCloud20Service.verifyServiceAdminLevelAccess("admin");
    }

    @Test(expected = ForbiddenException.class)
    public void checkXAUTHTOKEN_notAuthorized_throwsForbiddenException() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        defaultCloud20Service.checkXAUTHTOKEN(authToken, true, tenantId);
    }

    @Test(expected = ForbiddenException.class)
    public void checkXAUTHTOKEN_isCloudUserAdminAndAdminTenantIdNotMatch_throwsForbidden() throws Exception {
        List<Tenant> adminTenants = new ArrayList<Tenant>();
        adminTenants.add(tenant);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(tenantService.getTenantsForScopeAccessByTenantRoles(any(ScopeAccess.class))).thenReturn(adminTenants);
        defaultCloud20Service.checkXAUTHTOKEN(authToken, false, "notMatch");
    }

    @Test
    public void checkXAUTHTOKEN_isCloudUserAdminAndAdminTenantIdMatch_succeeds() throws Exception {
        List<Tenant> adminTenants = new ArrayList<Tenant>();
        adminTenants.add(tenant);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(tenantService.getTenantsForScopeAccessByTenantRoles(any(ScopeAccess.class))).thenReturn(adminTenants);
        defaultCloud20Service.checkXAUTHTOKEN(authToken, false, tenantId);
    }

    @Test(expected = ForbiddenException.class)
    public void checkXAUTHTOKEN_notCloudIdentityAdminAndNotCloudUserAdmin_throwsForbidden() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(false);
        defaultCloud20Service.checkXAUTHTOKEN(authToken, false, tenantId);
    }

    @Test
    public void checkXAUTHTOKEN_authorizationServiceReturnsTrue_doesNotThrowException() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        defaultCloud20Service.checkXAUTHTOKEN(authToken, false, "tenantId");
        assertTrue("no exceptions", true);
    }

    @Test
    public void checkXAUTHTOKEN_hasUserAdminAccessAndTenantIdIsNull_doesNotThrowException() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        defaultCloud20Service.checkXAUTHTOKEN(authToken, false, null);
        assertTrue("no exceptions", true);
    }

    @Test
    public void verifyTokenHasTenantAccessForAuthenticate_callsVerifyTokenHasTenant() throws Exception {
        List<Tenant> adminTenants = new ArrayList<Tenant>();
        adminTenants.add(tenant);
        when(tenantService.getTenantsForScopeAccessByTenantRoles(any(ScopeAccess.class))).thenReturn(adminTenants);
        spy.verifyTokenHasTenantAccessForAuthenticate(authToken, tenantId);
        verify(spy).verifyTokenHasTenant(eq(tenantId), any(ScopeAccess.class));
    }

    @Test
    public void stripEndpoints_succeeds() throws Exception {
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        List<CloudBaseUrl> cloudBaseUrls = new ArrayList<CloudBaseUrl>();
        cloudBaseUrls.add(cloudBaseUrl);
        OpenstackEndpoint endpoint = new OpenstackEndpoint();
        endpoint.setBaseUrls(cloudBaseUrls);
        endpoints.add(endpoint);
        defaultCloud20Service.stripEndpoints(endpoints);
    }

    @Test
    public void listEndpointsForToken_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.listEndpointsForToken(null, null, null);
        verify(spy).verifyServiceAdminLevelAccess(null);
    }

    @Test
    public void listEndpointsForToken_throwsNotAuthorizedException_returnsResponseBuilder() throws Exception {
        NotAuthorizedException notAuthorizedException = new NotAuthorizedException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doThrow(notAuthorizedException).when(spy).verifyServiceAdminLevelAccess(null);
        when(exceptionHandler.exceptionResponse(notAuthorizedException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.listEndpointsForToken(null, null, null), equalTo(responseBuilder));
    }

    @Test
    public void listEndpointsForToken_responseOk_returns200() throws Exception {
        List<OpenstackEndpoint> openstackEndpointList = new ArrayList<OpenstackEndpoint>();
        doReturn(new ScopeAccess()).when(spy).checkAndGetToken("tokenId");
        when(scopeAccessService.getOpenstackEndpointsForScopeAccess(any(ScopeAccess.class))).thenReturn(openstackEndpointList);
        when(endpointConverterCloudV20.toEndpointList(openstackEndpointList)).thenReturn(new EndpointList());
        Response.ResponseBuilder responseBuilder = spy.listEndpointsForToken(httpHeaders, authToken, "tokenId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listExtensions_currentExtensionsIsNullResponseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.listExtensions(httpHeaders);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listExtension_currentExtensionsNotNullResponseOk_returns200() throws Exception {
        JAXBContext jaxbContext = JAXBContextResolver.get();
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        InputStream is = StringUtils.class.getResourceAsStream("/extensions.xml");
        StreamSource ss = new StreamSource(is);
        currentExtensions = unmarshaller.unmarshal(ss, Extensions.class);
        defaultCloud20Service.setCurrentExtensions(currentExtensions);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.listExtensions(httpHeaders);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void addGroup_callsCloudGroupBuilder() throws Exception {
        CloudGroupBuilder cloudGrpBuilder = mock(CloudGroupBuilder.class);
        defaultCloud20Service.setCloudGroupBuilder(cloudGrpBuilder);
    }

    @Test
    public void addGroup_validGroup_returns201() throws Exception {
        CloudGroupBuilder cloudGroupBuilder = mock(CloudGroupBuilder.class);
        CloudKsGroupBuilder cloudKsGroupBuilder = mock(CloudKsGroupBuilder.class);
        when(cloudGroupBuilder.build(null)).thenReturn(group);
        when(cloudKsGroupBuilder.build(org.mockito.Matchers.<Group>any())).thenReturn(groupKs);
        when(uriInfo.getRequestUriBuilder()).thenReturn(UriBuilder.fromPath("path"));
        defaultCloud20Service.setCloudGroupBuilder(cloudGroupBuilder);
        defaultCloud20Service.setCloudKsGroupBuilder(cloudKsGroupBuilder);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.addGroup(null, uriInfo, authToken, groupKs);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addGroup_duplicateGroup_returns409() throws Exception {
        CloudGroupBuilder cloudGroupBuilder = mock(CloudGroupBuilder.class);
        when(cloudGroupBuilder.build((com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group) any())).thenReturn(group);
        doThrow(new DuplicateException()).when(userGroupService).addGroup(org.mockito.Matchers.<Group>any());
        defaultCloud20Service.setCloudGroupBuilder(cloudGroupBuilder);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.addGroup(null, null, authToken, groupKs);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void addGroup_emptyName_returns400() throws Exception {
        groupKs.setName("");
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.addGroup(null, null, authToken, groupKs);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void verifyTokenHasTenantAccess_isIdentityAdmin_returns() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(false);
        spy.verifyTokenHasTenantAccess(authToken, "tenantId");
    }

    @Test
    public void verifyTokenHasTenantAccess_isServiceAdmin_returns() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(true);
        spy.verifyTokenHasTenantAccess(authToken, "tenantId");
    }

    @Test
    public void verifyTokenHasTenantAccess_callsTenantService() throws Exception {
        ArrayList<Tenant> list = new ArrayList<Tenant>();
        Tenant tenant1 = new Tenant();
        tenant1.setTenantId("1");
        list.add(tenant1);
        when(tenantService.getTenantsForScopeAccessByTenantRoles(any(ScopeAccess.class))).thenReturn(list);
        when(authorizationService.authorizeCloudIdentityAdmin((ScopeAccess) anyObject())).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin((ScopeAccess) anyObject())).thenReturn(false);
        defaultCloud20Service.verifyTokenHasTenantAccess(authToken, tenant1.getTenantId());
        verify(tenantService).getTenantsForScopeAccessByTenantRoles(any(ScopeAccess.class));
    }

    @Test(expected = ForbiddenException.class)
    public void verifyTokenHasTenantAccess_NoTenants_throwsException() throws Exception {
        when(tenantService.getTenantsForScopeAccessByTenantRoles(any(ScopeAccess.class))).thenReturn(new ArrayList<Tenant>());
        when(authorizationService.authorizeCloudIdentityAdmin((ScopeAccess) anyObject())).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin((ScopeAccess) anyObject())).thenReturn(false);
        defaultCloud20Service.verifyTokenHasTenantAccess(authToken, null);
    }

    @Test
    public void verifyTokenHasTenantAccess_WithMatchingTenant_succeeds() throws Exception {
        ArrayList<Tenant> list = new ArrayList<Tenant>();
        Tenant tenant1 = new Tenant();
        tenant1.setTenantId("1");
        list.add(tenant1);
        when(tenantService.getTenantsForScopeAccessByTenantRoles(any(ScopeAccess.class))).thenReturn(list);
        defaultCloud20Service.verifyTokenHasTenantAccess(authToken, tenant1.getTenantId());
    }

    @Test
    public void validateKsGroup_validGroup() {
        defaultCloud20Service.validateKsGroup(groupKs);
    }

    @Test(expected = BadRequestException.class)
    public void validateKsGroup_groupNameIsNull_throwsBadRequestException() {
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group = mock(com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group.class);
        when(group.getName()).thenReturn("1").thenReturn(null);
        defaultCloud20Service.validateKsGroup(group);
    }

    @Test(expected = BadRequestException.class)
    public void validateKsGroup_groupDescriptionMoreThan1000Characters_throwsBadRequest() {
        groupKs.setName("valid");
        String moreThan1000Chars = org.apache.commons.lang.StringUtils.repeat("a", 1001);
        groupKs.setDescription(moreThan1000Chars);
        defaultCloud20Service.validateKsGroup(groupKs);
    }

    @Test(expected = BadRequestException.class)
    public void validateKsGroup_invalidGroup_throwsBadRequest() {
        groupKs.setName("");
        defaultCloud20Service.validateKsGroup(groupKs);
    }

    @Test
    public void validateKsGroup_invalidGroupLength_throwsBadRequestMessage() {
        groupKs.setName("Invalidnamellllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll");
        try {
            defaultCloud20Service.validateKsGroup(groupKs);
        } catch (Exception e) {
            assertThat("Exception", e.getMessage(), equalTo("Group name length cannot exceed 200 characters"));

        }
    }

    @Test(expected = BadRequestException.class)
    public void validateKsGroup_invalidGroupLength_throwsBadRequest() {
        groupKs.setName("Invalidnamellllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllllll");
        defaultCloud20Service.validateKsGroup(groupKs);
    }

    @Test
    public void updateGroup_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.updateGroup(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void updateGroup_callsValidateKsGroup() throws Exception {
        spy.updateGroup(null, authToken, null, groupKs);
        verify(spy).validateKsGroup(groupKs);
    }

    @Test
    public void updateGroup_callsValidateGroupId() throws Exception {
        spy.updateGroup(null, authToken, "1", groupKs);
        verify(spy).validateGroupId("1");
    }

    @Test
    public void updateGroup_responseOk_returns200() throws Exception {
        CloudGroupBuilder cloudGroupBuilder = mock(CloudGroupBuilder.class);
        spy.setCloudGroupBuilder(cloudGroupBuilder);
        Response.ResponseBuilder responseBuilder = spy.updateGroup(httpHeaders, authToken, "1", groupKs);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void updateGroup_cloudGroupServiceUpdateGroupThrowsDuplicateException_returns409() throws Exception {
        CloudGroupBuilder cloudGroupBuilder = mock(CloudGroupBuilder.class);
        spy.setCloudGroupBuilder(cloudGroupBuilder);
        doThrow(new DuplicateException()).when(userGroupService).updateGroup(any(Group.class));
        Response.ResponseBuilder responseBuilder = spy.updateGroup(httpHeaders, authToken, "1", groupKs);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(409));
    }

    @Test
    public void deleteGroup_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.deleteGroup(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void deleteGroup_callsValidateGroupId() throws Exception {
        spy.deleteGroup(null, authToken, "1");
        verify(spy).validateGroupId("1");
    }

    @Test
    public void deleteGroup_cloudGroupService_callsDeleteGroup() throws Exception {
        spy.deleteGroup(null, authToken, "1");
        verify(userGroupService).deleteGroup("1");
    }

    @Test
    public void deleteGroup_responseNoContent_returns204() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.deleteGroup(httpHeaders, authToken, "1");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void validateGroupId_validGroupId() {
        defaultCloud20Service.validateGroupId("1");
    }

    @Test
    public void validateGroupId_validGroupIdwithSpaces() {
        defaultCloud20Service.validateGroupId("  1   ");
    }

    @Test(expected = BadRequestException.class)
    public void validateGroupId_inValidGroupId() {
        defaultCloud20Service.validateGroupId("a");
    }

    @Test
    public void validateGroupId_inValidGroupId_throwBadRequest() {
        try {
            defaultCloud20Service.validateGroupId(" ");
        } catch (Exception e) {
            assertThat("Exception", e.getMessage(), equalTo("Invalid group id"));
        }
    }

    @Test
    public void validateGroupId_inValidGroupIdWithSpaces_throwBadRequest() {
        try {
            defaultCloud20Service.validateGroupId(" a ");
        } catch (Exception e) {
            assertThat("Exception", e.getMessage(), equalTo("Invalid group id"));
        }
    }

    @Test(expected = BadRequestException.class)
    public void validateGroupId_groupIdIsNull_throwsBadRequest() throws Exception {
        defaultCloud20Service.validateGroupId(null);
    }

    @Test
    public void addUserToGroup_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.addUserToGroup(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void addUserToGroup_callsValidateGroupId() throws Exception {
        spy.addUserToGroup(null, authToken, "1", null);
        verify(spy).validateGroupId("1");
    }

    @Test
    public void addUserToGroup_cloudGroupService_callsAddGroupToUser() throws Exception {
        spy.addUserToGroup(null, authToken, "1", userId);
        verify(userGroupService).addGroupToUser(1, userId);
    }

    @Test
    public void addUserToGroup_responseNoContent_returns204() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.addUserToGroup(httpHeaders, authToken, "1", userId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void removeUserFromGroup_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.removeUserFromGroup(null, authToken, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void removeUserFromGroup_callsValidateGropuId() throws Exception {
        spy.removeUserFromGroup(null, authToken, "1", null);
        verify(spy).validateGroupId("1");
    }

    @Test
    public void removeUserFromGroup_userIdIsNullThrowsBadRequest_returns400() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.removeUserFromGroup(null, authToken, "1", null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void removeUserFromGroup_userIdIsBlankSpaceThrowsBadRequest_returns400() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.removeUserFromGroup(null, authToken, "1", " ");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }

    @Test
    public void removeUserFromGroup_cloudGroupService_callsDeleteGroupFromUser() throws Exception {
        spy.removeUserFromGroup(null, authToken, "1", userId);
        verify(userGroupService).deleteGroupFromUser(1, userId);
    }

    @Test
    public void removeUserFromGroup_responseNoContent_returns204() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.removeUserFromGroup(httpHeaders, authToken, "1", userId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void getUsersForGroup_emptyGroup_returns404() throws Exception {
        List<User> userList = new ArrayList<User>();
        Users users = new Users();
        users.setUsers(userList);
        when(userGroupService.getGroupById(1)).thenReturn(group);
        when(userGroupService.getAllEnabledUsers(any(FilterParam[].class), anyString(), anyInt())).thenReturn(users);
        Response.ResponseBuilder responseBuilder = spy.getUsersForGroup(null, authToken, "1", "1", 1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getUsersForGroup_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.getUsersForGroup(null, authToken, null, null, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getUsersForGroup_callsValidateGroupId() throws Exception {
        spy.getUsersForGroup(null, authToken, "1", null, null);
        verify(spy).validateGroupId("1");
    }

    @Test
    public void getUsersForGroup_groupNotExistThrowsNotFoundException_returns404() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getUsersForGroup(null, authToken, "1", null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void getUsersForGroup_responseOk_returns200() throws Exception {
        List<User> userList = new ArrayList<User>();
        userList.add(user);
        Users users = new Users();
        users.setUsers(userList);
        when(userGroupService.getGroupById(1)).thenReturn(group);
        when(userGroupService.getAllEnabledUsers(any(FilterParam[].class), anyString(), anyInt())).thenReturn(users);
        Response.ResponseBuilder responseBuilder = spy.getUsersForGroup(null, authToken, "1", null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUsers_callerIsIdentityAdmin_callsGetAllUsers() throws Exception {
        UserScopeAccess scopeAccess = new UserScopeAccess();
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        User user1 = new User();
        user1.setDomainId("testDomain");
        doReturn(user1).when(spy).getUser(any(ScopeAccess.class));
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(false);
        spy.listUsers(null, authToken, null, null);
        verify(userService).getAllUsers(any(FilterParam[].class), any(Integer.class), any(Integer.class));
    }

    @Test
    public void listUsers_callerIsServiceAdmin_callsGetAllUsers() throws Exception {
        UserScopeAccess scopeAccess = new UserScopeAccess();
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(true);
        spy.listUsers(null, authToken, null, null);
        verify(userService).getAllUsers(null, null, null);
    }

    @Test
    public void listUsers_callerIsNotServiceOrIdentityAdmin_domainIdIsNull_responseOk_returns200() throws Exception {
        User userTest = new User();
        userTest.setDomainId(null);
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        doReturn(userTest).when(spy).getUser(scopeAccess);
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        when(authorizationService.authorizeCloudUser(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(false);
        Response.ResponseBuilder responseBuilder = spy.listUsers(httpHeaders, authToken, 1, 1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUsers_callerIsNotServiceOrIdentityAdmin_domainIdNotNull_responseOk_returns200() throws Exception {
        User userTest = new User();
        userTest.setDomainId("domainId");
        ScopeAccess scopeAccess = new ScopeAccess();
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        doReturn(userTest).when(spy).getUser(scopeAccess);
        doNothing().when(spy).verifyUserAdminLevelAccess(authToken);
        when(authorizationService.authorizeCloudUser(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(false);
        when(userService.getAllUsers(any(FilterParam[].class), any(Integer.class), any(Integer.class))).thenReturn(new Users());
        Response.ResponseBuilder responseBuilder = spy.listUsers(httpHeaders, authToken, 1, 1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUsers_callerIsDefaultUser_returns200() throws Exception {
        UserScopeAccess scopeAccess = new UserScopeAccess();
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUser(scopeAccess)).thenReturn(true);
        Response.ResponseBuilder responseBuilder = spy.listUsers(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUsers_withNullScopeAccess_returns401() throws Exception {
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.listUsers(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void listGroupWithQueryParam_validName_returns200() throws Exception {
        CloudKsGroupBuilder cloudKsGroupBuilder = mock(CloudKsGroupBuilder.class);
        when(userGroupService.getGroupByName(org.mockito.Matchers.<String>anyObject())).thenReturn(group);
        when(cloudKsGroupBuilder.build(org.mockito.Matchers.<Group>any())).thenReturn(groupKs);
        defaultCloud20Service.setCloudKsGroupBuilder(cloudKsGroupBuilder);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.getGroup(null, authToken, "group1");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void setDomainId_callsAuthorizeCloudUserAdmin() throws Exception {
        defaultCloud20Service.setDomainId(null, null);
        verify(authorizationService).authorizeCloudUserAdmin(null);
    }

    @Test
    public void assignProperRole_callsAuthorizeCloudUserAdmin() throws Exception {
        defaultCloud20Service.assignProperRole(null, authToken, null, null);
        verify(authorizationService).authorizeCloudUserAdmin(null);
    }

    @Test
    public void assignProperRole_callsAuthorizeCloudServiceAdmin() throws Exception {
        defaultCloud20Service.assignProperRole(null, authToken, null, null);
        verify(authorizationService).authorizeCloudServiceAdmin(null);
    }

    @Test
    public void assignProperRole_callsAuthorizeCloudIdentityAdmin() throws Exception {
        defaultCloud20Service.assignProperRole(null, authToken, null, null);
        verify(authorizationService).authorizeCloudIdentityAdmin(null);
    }

    @Test
    public void assignProperRole_cloudUserAdmin_callsAddUserRole() throws Exception {
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(clientService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(clientRole);
        spy.assignProperRole(null, authToken, null, user);
        verify(spy).addUserRole(null, authToken, user.getId(), clientRole.getId());
    }

    @Test
    public void assignProperRole_cloudServiceAdmin_callsAddUserRole() throws Exception {
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(clientService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(clientRole);
        spy.assignProperRole(null, authToken, null, user);
        verify(spy).addUserRole(null, authToken, user.getId(), clientRole.getId());
    }

    @Test
    public void assignProperRole_cloudIdentityAdmin_callsAddUserRole() throws Exception {
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(clientService.getClientRoleByClientIdAndRoleName(anyString(), anyString())).thenReturn(clientRole);
        spy.assignProperRole(null, authToken, null, user);
        verify(spy).addUserRole(null, authToken, user.getId(), clientRole.getId());
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_withEmptyString_throwBadRequestException() throws Exception {
        defaultCloud20Service.validateUsername("");
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_withNullString_throwBadRequestException() throws Exception {
        defaultCloud20Service.validateUsername(null);
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_withWhiteSpaceContainingString_throwBadRequestException() throws Exception {
        defaultCloud20Service.validateUsername("first last");
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_withWhiteSpaceContainingString2_throwBadRequestException() throws Exception {
        defaultCloud20Service.validateUsername(" firstlast");
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_withWhiteSpaceContainingString3_throwBadRequestException() throws Exception {
        defaultCloud20Service.validateUsername("firstlast ");
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_withTabContainingString_throwBadRequestException() throws Exception {
        defaultCloud20Service.validateUsername("first   last");
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_withNonAlphChara_throwBadRequestException() throws Exception {
        defaultCloud20Service.validateUsernameForUpdateOrCreate("12nogood");
    }

    @Test(expected = BadRequestException.class)
    public void validateUsername_withSpecialChara_throwBadRequestException() throws Exception {
        defaultCloud20Service.validateUsernameForUpdateOrCreate("jorgenogood!");
    }

    @Test
    public void validateUsername_validUserName() throws Exception {
        defaultCloud20Service.validateUsernameForUpdateOrCreate("jorgegood");
    }

    @Test
    public void validateApiKeyCredentials_validApiKey_noException() throws Exception {
        ApiKeyCredentials apiKeyCredentials = new ApiKeyCredentials();
        apiKeyCredentials.setApiKey("1234568790");
        apiKeyCredentials.setUsername("test");
        defaultCloud20Service.validateApiKeyCredentials(apiKeyCredentials);
    }

    @Test(expected = BadRequestException.class)
    public void validateApiKeyCredentials_validApiKey_BadRequestException() throws Exception {
        ApiKeyCredentials apiKeyCredentials = new ApiKeyCredentials();
        apiKeyCredentials.setApiKey("");
        apiKeyCredentials.setUsername("test");
        defaultCloud20Service.validateApiKeyCredentials(apiKeyCredentials);
    }

    @Test
    public void deleteTenant_validTenantAdminAndServiceAdmin_return204() throws Exception {
        UserScopeAccess scopeAccess = new UserScopeAccess();
        //Current time plus 10 min
        scopeAccess.setAccessTokenExp(new Date(System.currentTimeMillis() + 600000));
        scopeAccess.setRefreshTokenExp(new Date(System.currentTimeMillis() + 600000));
        scopeAccess.setAccessTokenString("token");
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(true);
        when(tenantService.getTenant(org.mockito.Matchers.<String>any())).thenReturn(tenant);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.deleteTenant(null, authToken, "1");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void deleteTenant_throwsForbiddenException_returnResponseBuilder() throws Exception {
        ForbiddenException forbiddenException = new ForbiddenException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doThrow(forbiddenException).when(spy).verifyServiceAdminLevelAccess(authToken);
        when(exceptionHandler.exceptionResponse(forbiddenException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.deleteTenant(null, authToken, "1"), equalTo(responseBuilder));
    }

    @Test
    public void getExtension_badAlias_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        when(exceptionHandler.exceptionResponse(any(BadRequestException.class))).thenReturn(responseBuilder);
        assertThat("response builder", defaultCloud20Service.getExtension(null, ""), equalTo(responseBuilder));
    }

    @Test
    public void getExtension_ValidAlias_return200() throws Exception {
        org.openstack.docs.common.api.v1.ObjectFactory objectFactory = new org.openstack.docs.common.api.v1.ObjectFactory();
        when(jaxbObjectFactories.getOpenStackCommonV1Factory()).thenReturn(objectFactory);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.getExtension(null, "RAX-KSKEY");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getExtension_invalidAlias_returnResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        org.openstack.docs.common.api.v1.ObjectFactory objectFactory = new org.openstack.docs.common.api.v1.ObjectFactory();
        when(jaxbObjectFactories.getOpenStackCommonV1Factory()).thenReturn(objectFactory);
        when(exceptionHandler.exceptionResponse(any(NotFoundException.class))).thenReturn(responseBuilder);
        assertThat("response builder", defaultCloud20Service.getExtension(null, "bad"), equalTo(responseBuilder));
    }

    @Test
    public void getExtension_extensionMapNotNullResponseOk_returns200() throws Exception {
        Extension extension = new Extension();
        JAXBElement<Extension> someValue = new JAXBElement(new QName("http://docs.openstack.org/common/api/v1.0", "extension"), Extension.class, null, extension);
        extensionMap = mock(HashMap.class);
        defaultCloud20Service.setExtensionMap(extensionMap);
        when(extensionMap.containsKey(anyObject())).thenReturn(true);
        when(extensionMap.get(any())).thenReturn(someValue);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.getExtension(null, "RAX-KSKEY");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getExtension_currentExtensionNotNullThrowsException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        JAXBContext jaxbContext = JAXBContextResolver.get();
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        InputStream is = StringUtils.class.getResourceAsStream("/extensions.xml");
        StreamSource ss = new StreamSource(is);
        currentExtensions = unmarshaller.unmarshal(ss, Extensions.class);
        defaultCloud20Service.setCurrentExtensions(currentExtensions);
        when(exceptionHandler.exceptionResponse(any(NullPointerException.class))).thenReturn(responseBuilder);
        assertThat("response builder", defaultCloud20Service.getExtension(null, "RAX-KSKEY"), equalTo(responseBuilder));
    }

    @Test
    public void getGroup_callsVerifyServiceAdminLevelAccess() throws Exception {
        spy.getGroup(null, authToken, null);
        verify(spy).verifyServiceAdminLevelAccess(authToken);
    }

    @Test
    public void getGroup_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getGroup(httpHeaders, authToken, "groupName");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void impersonate_callsVerifyRackerOrServiceAdminAccess() throws Exception {
        user.setEnabled(true);
        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("impersonateUser");
        impersonateUser.setId("impersonateUserId");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(impersonateUser);
        when(authorizationService.authorizeRacker(any(ScopeAccess.class))).thenReturn(true);
        when(delegateCloud20Service.impersonateUser(anyString(), anyString(), anyString())).thenReturn("impersonatingToken");
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        spy.impersonate(null, authToken, impersonationRequest);
        verify(spy).verifyRackerOrServiceAdminAccess(authToken);
    }

    @Test
    public void impersonate_callsValidateImpersonationRequest() throws Exception {
        user.setEnabled(true);
        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("impersonateUser");
        impersonateUser.setId("impersonateUserId");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(impersonateUser);
        when(authorizationService.authorizeRacker(any(ScopeAccess.class))).thenReturn(true);
        when(delegateCloud20Service.impersonateUser(anyString(), anyString(), anyString())).thenReturn("impersonatingToken");
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        spy.impersonate(null, authToken, impersonationRequest);
        verify(spy).validateImpersonationRequest(impersonationRequest);
    }

    @Test(expected = ForbiddenException.class)
    public void impersonate_userNotNullAndNotEnabled_throwsForbiddenException() throws Exception {
        user.setEnabled(false);
        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("impersonateUser");
        impersonateUser.setId("impersonateUserId");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(impersonateUser);
        when(authorizationService.authorizeRacker(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUser("impersonateUser")).thenReturn(user);
        spy.impersonate(null, authToken, impersonationRequest);
    }

    @Test(expected = BadRequestException.class)
    public void impersonate_userNotNullAndEnabledAndNotValidImpersonatee_throwsBadRequestException() throws Exception {
        user.setEnabled(true);
        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("impersonateUser");
        impersonateUser.setId("impersonateUserId");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(impersonateUser);
        when(authorizationService.authorizeRacker(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUser("impersonateUser")).thenReturn(user);
        spy.impersonate(null, authToken, impersonationRequest);
    }

    @Test
    public void impersonate_userNotNullAndEnabledAndAccessTokenExpired_callsUpdateExpiredUserScopeAccess() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExpired();
        userScopeAccess.setAccessTokenString("token");
        user.setEnabled(true);
        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("impersonateUser");
        impersonateUser.setId("impersonateUserId");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(impersonateUser);
        when(authorizationService.authorizeRacker(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUser("impersonateUser")).thenReturn(user);
        doReturn(true).when(spy).isValidImpersonatee(user);
        when(scopeAccessService.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(userScopeAccess);
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        spy.impersonate(null, authToken, impersonationRequest);
        verify(scopeAccessService).updateExpiredUserScopeAccess(userScopeAccess);
    }

    @Test(expected = BadRequestException.class)
    public void impersonate_impersonatingTokenIsBlankString_throwsBadRequestException() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExpired();
        userScopeAccess.setAccessTokenString("");
        user.setEnabled(true);
        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("impersonateUser");
        impersonateUser.setId("impersonateUserId");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(impersonateUser);
        when(authorizationService.authorizeRacker(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUser("impersonateUser")).thenReturn(user);
        doReturn(true).when(spy).isValidImpersonatee(user);
        when(scopeAccessService.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(userScopeAccess);
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        spy.impersonate(null, authToken, impersonationRequest);
    }

    @Test(expected = BadRequestException.class)
    public void impersonate_impersonatingUserNameIsBlankString_throwsBadRequestException() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExpired();
        userScopeAccess.setAccessTokenString("token");
        user.setEnabled(true);
        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("");
        impersonateUser.setId("impersonateUserId");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(impersonateUser);
        when(authorizationService.authorizeRacker(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUser("impersonateUser")).thenReturn(user);
        doReturn(true).when(spy).isValidImpersonatee(user);
        when(scopeAccessService.getDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(userScopeAccess);
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        spy.impersonate(null, authToken, impersonationRequest);
    }

    @Test
    public void impersonate_scopeAccessInstanceOfRackerScopeAccess_returns200() throws Exception {
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        user.setEnabled(true);
        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("impersonateUser");
        impersonateUser.setId("impersonateUserId");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(impersonateUser);
        when(authorizationService.authorizeRacker(any(ScopeAccess.class))).thenReturn(true);
        when(delegateCloud20Service.impersonateUser(anyString(), anyString(), anyString())).thenReturn("impersonatingToken");
        doReturn(rackerScopeAccess).when(spy).checkAndGetToken(authToken);
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.impersonate(null, authToken, impersonationRequest);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void impersonate_scopeAccessInstanceOfUserScopeAccess_returns200() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        user.setEnabled(true);
        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("impersonateUser");
        impersonateUser.setId("impersonateUserId");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(impersonateUser);
        when(authorizationService.authorizeRacker(any(ScopeAccess.class))).thenReturn(true);
        when(delegateCloud20Service.impersonateUser(anyString(), anyString(), anyString())).thenReturn("impersonatingToken");
        doReturn(userScopeAccess).when(spy).checkAndGetToken(authToken);
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.impersonate(null, authToken, impersonationRequest);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test(expected = NotAuthorizedException.class)
    public void impersonate_scopeAccessNotInstanceOfUserOrRackerScopeAccess_throwsNotAuthorizedException() throws Exception {
        ClientScopeAccess clientScopeAccess = new ClientScopeAccess();
        user.setEnabled(true);
        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("impersonateUser");
        impersonateUser.setId("impersonateUserId");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(impersonateUser);
        when(authorizationService.authorizeRacker(any(ScopeAccess.class))).thenReturn(true);
        when(delegateCloud20Service.impersonateUser(anyString(), anyString(), anyString())).thenReturn("impersonatingToken");
        doReturn(clientScopeAccess).when(spy).checkAndGetToken(authToken);
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        spy.impersonate(null, authToken, impersonationRequest);
    }

    @Test
    public void verifySelf_callsUserService_getUserByScopeAccess() throws Exception {
        User user1 = new User();
        user1.setId("foo");
        user1.setUsername("foo");
        user1.setUniqueId("foo");
        when(userService.getUserByScopeAccess(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(user1);
        defaultCloud20Service.verifySelf("token", user1);
        verify(userService).getUserByScopeAccess(org.mockito.Matchers.any(ScopeAccess.class));
    }

    @Test(expected = ForbiddenException.class)
    public void verifySelf_differentUsername_throwsForbiddenException() throws Exception {
        User user1 = new User();
        user1.setId("foo");
        user1.setUsername("foo");
        user1.setUniqueId("foo");
        when(userService.getUserByScopeAccess(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(user1);
        User user2 = new User();
        user2.setId("foo");
        user2.setUsername("!foo");
        user2.setUniqueId("foo");
        defaultCloud20Service.verifySelf("token", user2);
        verify(userService).getUserByScopeAccess(org.mockito.Matchers.any(ScopeAccess.class));
    }

    @Test(expected = ForbiddenException.class)
    public void verifySelf_differentUniqueId_throwsForbiddenException() throws Exception {
        User user1 = new User();
        user1.setId("foo");
        user1.setUsername("foo");
        user1.setUniqueId("foo");
        when(userService.getUserByScopeAccess(org.mockito.Matchers.any(ScopeAccess.class))).thenReturn(user1);
        User user2 = new User();
        user2.setId("foo");
        user2.setUsername("foo");
        user2.setUniqueId("!foo");
        defaultCloud20Service.verifySelf("token", user2);
        verify(userService).getUserByScopeAccess(org.mockito.Matchers.any(ScopeAccess.class));
    }

    @Test
    public void deleteUserFromSoftDeleted_callsCheckXAUTHTOKEN() throws Exception {
        spy.deleteUserFromSoftDeleted(null, authToken, null);
        verify(spy).checkXAUTHTOKEN(authToken, true, null);
    }

    @Test
    public void deleteUserFromSoftDeleted_callsCheckAndGetSoftDeletedUser() throws Exception {
        spy.deleteUserFromSoftDeleted(null, authToken, "userId");
        verify(spy).checkAndGetSoftDeletedUser("userId");
    }

    @Test
    public void deleteUserFromSoftDeleted_callsUserServiceDeleteUser() throws Exception {
        doReturn(new User()).when(spy).checkAndGetSoftDeletedUser("userId");
        spy.deleteUserFromSoftDeleted(null, authToken, "userId");
        verify(userService).deleteUser(any(User.class));
    }

    @Test
    public void deleteUserFromSoftDeleted_responseNoContent_returns204() throws Exception {
        doReturn(new User()).when(spy).checkAndGetSoftDeletedUser("userId");
        doNothing().when(userService).deleteUser(any(User.class));
        Response.ResponseBuilder responseBuilder = spy.deleteUserFromSoftDeleted(null, authToken, "userId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void deleteUserFromSoftDeleted_throwsExceptionResponseBadRequest_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        BadRequestException badRequestException = new BadRequestException();
        doThrow(badRequestException).when(spy).checkXAUTHTOKEN(authToken, true, null);
        when(exceptionHandler.exceptionResponse(badRequestException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.deleteUserFromSoftDeleted(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void badRequestExceptionResponse_doesNotSetDetails() throws Exception {
        BadRequestFault badRequestFault = mock(BadRequestFault.class);
        ObjectFactory objectFactory = mock(ObjectFactory.class);
        JAXBElement<BadRequestFault> someFault = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "badRequest"), BadRequestFault.class, null, badRequestFault);
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(objectFactory);
        when(objectFactory.createBadRequestFault()).thenReturn(badRequestFault);
        when(objectFactory.createBadRequest(badRequestFault)).thenReturn(someFault);
        defaultCloud20Service.badRequestExceptionResponse("message");
        verify(badRequestFault, never()).setDetails(anyString());
    }

    @Test
    public void convertTenantEntityToApi_returnsTenantForAuthenticateResponse(){
        Tenant test = new Tenant();
        test.setName("test");
        test.setTenantId("test");
        TenantForAuthenticateResponse testTenant = defaultCloud20Service.convertTenantEntityToApi(test);
        assertThat("Verify Tenant",testTenant.getId(),equalTo(test.getTenantId()));
        assertThat("Verify Tenant",testTenant.getName(),equalTo(test.getName()));
    }

    @Test (expected = NotAuthenticatedException.class)
    public void getUserByUsernameForAuthentication_throwsNotAuthenticatedException() throws Exception {
        doThrow(new NotFoundException()).when(spy).checkAndGetUserByName("username");
        spy.getUserByUsernameForAuthentication("username");
    }

    @Test (expected = NotAuthenticatedException.class)
    public void getUserByIdForAuthentication_throwsNotAuthenticatedException() throws Exception {
        doThrow(new NotFoundException()).when(spy).checkAndGetUser("id");
        spy.getUserByIdForAuthentication("id");
    }

    @Test
    public void authenticate_credentialTypeIsNeitherApiOrPassword_cannotGetUserInfo_throwsNullPointer_returnResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        Ec2CredentialsType ec2CredentialsType = new Ec2CredentialsType();
        JAXBElement<Ec2CredentialsType> creds = new JAXBElement<Ec2CredentialsType>(new QName("http://docs.openstack.org/identity/api/v2.0", "pw"), Ec2CredentialsType.class, ec2CredentialsType);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setTenantName("tenantName");
        authenticationRequest.setToken(null);
        authenticationRequest.setCredential(creds);

        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setUserRsId("rsId");
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        userScopeAccess.setAccessTokenString("notExpired");

        when(scopeAccessService.getScopeAccessByAccessToken("tokenId")).thenReturn(userScopeAccess);
        when(scopeAccessService.updateExpiredUserScopeAccess(userScopeAccess)).thenReturn(userScopeAccess);
        doReturn(new User()).when(spy).getUserByIdForAuthentication("rsId");
        when(tenantService.hasTenantAccess(userScopeAccess, "tenantName")).thenReturn(false);
        when(exceptionHandler.exceptionResponse(any(NullPointerException.class))).thenReturn(responseBuilder);

        assertThat("response code",spy.authenticate(httpHeaders, authenticationRequest), equalTo(responseBuilder));
    }

    @Test
    public void authenticate_authenticationRequestTokenIsNull_tenantNameIsNotBlankAndHasNoAccess_returnsResponseBuilder() throws Exception {
        User userTest = new User();
        userTest.setUsername("userTestUsername");
        userTest.setId("userTestId");
        Response.ResponseBuilder responseBuilder= new ResponseBuilderImpl();

        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setPassword("password");
        passwordCredentialsRequiredUsername.setUsername("username");

        JAXBElement<PasswordCredentialsRequiredUsername> creds = new JAXBElement<PasswordCredentialsRequiredUsername>(new QName("http://docs.openstack.org/identity/api/v2.0", "pw"), PasswordCredentialsRequiredUsername.class, passwordCredentialsRequiredUsername);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setTenantName("tenantName");
        authenticationRequest.setToken(null);
        authenticationRequest.setCredential(creds);

        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setUserRsId("rsId");
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        userScopeAccess.setAccessTokenString("notExpired");

        when(tenantService.hasTenantAccess(userScopeAccess, "tenantName")).thenReturn(false);
        doNothing().when(spy).validatePasswordCredentials(passwordCredentialsRequiredUsername);
        doReturn(userTest).when(spy).getUserByUsernameForAuthentication("username");
        when(config.getString("cloudAuth.clientId")).thenReturn("clientId");
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword("username", "password", "clientId")).thenReturn(userScopeAccess);
        when(exceptionHandler.exceptionResponse(any(NotAuthenticatedException.class))).thenReturn(responseBuilder);

        assertThat("response code",spy.authenticate(httpHeaders, authenticationRequest), equalTo(responseBuilder));
    }

    @Test
    public void authenticate_scopeAccessInstanceOfImpersonatedAndTokenExpired_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();

        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");

        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setPassword("password");
        passwordCredentialsRequiredUsername.setUsername("username");

        JAXBElement<PasswordCredentialsRequiredUsername> creds = new JAXBElement<PasswordCredentialsRequiredUsername>(new QName("http://docs.openstack.org/identity/api/v2.0", "pw"), PasswordCredentialsRequiredUsername.class, passwordCredentialsRequiredUsername);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setTenantName("tenantName");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        authenticationRequest.setCredential(creds);

        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));

        when(scopeAccessService.getScopeAccessByAccessToken("tokenId")).thenReturn(impersonatedScopeAccess);
        when(exceptionHandler.exceptionResponse(any(NotAuthorizedException.class))).thenReturn(responseBuilder);

        assertThat("response code", spy.authenticate(httpHeaders, authenticationRequest), equalTo(responseBuilder));
    }

    @Test
    public void authenticate_scopeAccessWasImpersonatedScopeAccessThenCannotFindScopeAccessWithImpersonatingToken_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();

        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");

        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setPassword("password");
        passwordCredentialsRequiredUsername.setUsername("username");

        JAXBElement<PasswordCredentialsRequiredUsername> creds = new JAXBElement<PasswordCredentialsRequiredUsername>(new QName("http://docs.openstack.org/identity/api/v2.0", "pw"), PasswordCredentialsRequiredUsername.class, passwordCredentialsRequiredUsername);

        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setTenantName("tenantName");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        authenticationRequest.setCredential(creds);

        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        impersonatedScopeAccess.setAccessTokenString("notExpired");
        impersonatedScopeAccess.setImpersonatingToken("impersonatingToken");

        when(scopeAccessService.getScopeAccessByAccessToken("tokenId")).thenReturn(impersonatedScopeAccess);
        when(scopeAccessService.getScopeAccessByAccessToken("impersonatingToken")).thenReturn(null);
        when(exceptionHandler.exceptionResponse(any(NotAuthenticatedException.class))).thenReturn(responseBuilder);

        assertThat("response code", spy.authenticate(httpHeaders, authenticationRequest), equalTo(responseBuilder));
    }

    @Test
    public void authenticate_scopeAccessWasImpersonatedScopeAccessResponseOk_returns200() throws Exception {
        ArrayList<OpenstackEndpoint> openstackEndpoints = new ArrayList<OpenstackEndpoint>();
        ArrayList<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        User userTest = new User();
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setUserRsId("rsId");
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        userScopeAccess.setAccessTokenString("notExpired");
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setPassword("password");
        passwordCredentialsRequiredUsername.setUsername("username");
        JAXBElement<PasswordCredentialsRequiredUsername> creds = new JAXBElement<PasswordCredentialsRequiredUsername>(new QName("http://docs.openstack.org/identity/api/v2.0", "pw"), PasswordCredentialsRequiredUsername.class, passwordCredentialsRequiredUsername);
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setTenantName("tenantName");
        authenticationRequest.setToken(tokenForAuthenticationRequest);
        authenticationRequest.setCredential(creds);
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        impersonatedScopeAccess.setAccessTokenString("notExpired");
        impersonatedScopeAccess.setImpersonatingToken("impersonatingToken");
        when(scopeAccessService.getScopeAccessByAccessToken("tokenId")).thenReturn(impersonatedScopeAccess);
        when(scopeAccessService.getScopeAccessByAccessToken("impersonatingToken")).thenReturn(userScopeAccess);
        when(scopeAccessService.updateExpiredUserScopeAccess(userScopeAccess)).thenReturn(userScopeAccess);
        doReturn(userTest).when(spy).getUserByIdForAuthentication("rsId");
        when(tenantService.hasTenantAccess(userScopeAccess, "tenantName")).thenReturn(true);
        when(scopeAccessService.getOpenstackEndpointsForScopeAccess(userScopeAccess)).thenReturn(openstackEndpoints);
        when(authorizationService.authorizeCloudIdentityAdmin(userScopeAccess)).thenReturn(true);
        when(tenantService.getTenantRolesForScopeAccess(userScopeAccess)).thenReturn(tenantRoles);
        when(tenantService.getTenantByName("tenantName")).thenReturn(new Tenant());
        when(tokenConverterCloudV20.toToken(impersonatedScopeAccess)).thenReturn(new Token());
        when(authConverterCloudV20.toAuthenticationResponse(userTest, userScopeAccess, tenantRoles, openstackEndpoints)).thenReturn(new AuthenticateResponse());
        Response.ResponseBuilder responseBuilder = spy.authenticate(httpHeaders, authenticationRequest);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void authenticate_tenantNameNotBlank_tenantNameEqualsEndpointTenantName_addsEndpoint() throws Exception {
        ArgumentCaptor<List> argumentCaptor = ArgumentCaptor.forClass(List.class);
        Role role = new Role();
        RoleList roleList = new RoleList();
        List<Role> listOfRoles = roleList.getRole();
        listOfRoles.add(role);
        UserForAuthenticateResponse userForAuthenticateResponse = new UserForAuthenticateResponse();
        userForAuthenticateResponse.setRoles(roleList);
        AuthenticateResponse authenticateResponse = new AuthenticateResponse();
        authenticateResponse.setUser(userForAuthenticateResponse);
        Tenant tenant = new Tenant();
        tenant.setName("tenantName");
        tenant.setTenantId("tenantId");
        Token token = new Token();
        TenantRole tenantRole = new TenantRole();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(tenantRole);
        OpenstackEndpoint openstackEndpoint = new OpenstackEndpoint();
        openstackEndpoint.setTenantName("tenantName");
        OpenstackEndpoint anotherOpenstackEndpoint = new OpenstackEndpoint();
        anotherOpenstackEndpoint.setTenantName("differentTenantName");
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        endpoints.add(openstackEndpoint);
        endpoints.add(anotherOpenstackEndpoint);
        User userTest = new User();
        userTest.setUsername("userTestUsername");
        userTest.setId("userTestId");
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setPassword("password");
        passwordCredentialsRequiredUsername.setUsername("username");
        JAXBElement<PasswordCredentialsRequiredUsername> creds = new JAXBElement<PasswordCredentialsRequiredUsername>(new QName("http://docs.openstack.org/identity/api/v2.0", "pw"), PasswordCredentialsRequiredUsername.class, passwordCredentialsRequiredUsername);
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setTenantName("tenantName");
        authenticationRequest.setToken(null);
        authenticationRequest.setCredential(creds);
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setUserRsId("rsId");
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        userScopeAccess.setAccessTokenString("notExpired");
        when(tenantService.hasTenantAccess(userScopeAccess, "tenantName")).thenReturn(true);
        doNothing().when(spy).validatePasswordCredentials(passwordCredentialsRequiredUsername);
        doReturn(userTest).when(spy).getUserByUsernameForAuthentication("username");
        when(config.getString("cloudAuth.clientId")).thenReturn("clientId");
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword("username", "password", "clientId")).thenReturn(userScopeAccess);
        when(scopeAccessService.getOpenstackEndpointsForScopeAccess(userScopeAccess)).thenReturn(endpoints);
        when(authorizationService.authorizeCloudIdentityAdmin(userScopeAccess)).thenReturn(true);
        when(tenantService.getTenantRolesForScopeAccess(userScopeAccess)).thenReturn(roles);
        when(tokenConverterCloudV20.toToken(userScopeAccess)).thenReturn(token);
        when(tenantService.getTenantByName("tenantName")).thenReturn(tenant);
        when(authConverterCloudV20.toAuthenticationResponse(eq(userTest), eq(userScopeAccess), eq(roles), any(List.class))).thenReturn(authenticateResponse);
        spy.authenticate(httpHeaders, authenticationRequest);
        verify(authConverterCloudV20).toAuthenticationResponse(eq(userTest),  eq(userScopeAccess), eq(roles), argumentCaptor.capture());
        List<OpenstackEndpoint> result = argumentCaptor.getValue();
        assertThat("endpoint", result.get(0).getTenantName(), equalTo("tenantName"));
    }

    @Test
    public void authenticate_tenantNameNotBlank_endpointsIsEmptyAndAuthGetUsersIsNull_returns200() throws Exception {
        AuthenticateResponse authenticateResponse = new AuthenticateResponse();
        authenticateResponse.setUser(null);
        Tenant tenant = new Tenant();
        tenant.setName("tenantName");
        tenant.setTenantId("tenantId");
        Token token = new Token();
        TenantRole tenantRole = new TenantRole();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        roles.add(tenantRole);
        List<OpenstackEndpoint> endpoints = new ArrayList<OpenstackEndpoint>();
        User userTest = new User();
        userTest.setUsername("userTestUsername");
        userTest.setId("userTestId");
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setPassword("password");
        passwordCredentialsRequiredUsername.setUsername("username");
        JAXBElement<PasswordCredentialsRequiredUsername> creds = new JAXBElement<PasswordCredentialsRequiredUsername>(new QName("http://docs.openstack.org/identity/api/v2.0", "pw"), PasswordCredentialsRequiredUsername.class, passwordCredentialsRequiredUsername);
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setTenantName("tenantName");
        authenticationRequest.setToken(null);
        authenticationRequest.setCredential(creds);
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setUserRsId("rsId");
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        userScopeAccess.setAccessTokenString("notExpired");
        when(tenantService.hasTenantAccess(userScopeAccess, "tenantName")).thenReturn(true);
        doNothing().when(spy).validatePasswordCredentials(passwordCredentialsRequiredUsername);
        doReturn(userTest).when(spy).getUserByUsernameForAuthentication("username");
        when(config.getString("cloudAuth.clientId")).thenReturn("clientId");
        when(scopeAccessService.getUserScopeAccessForClientIdByUsernameAndPassword("username", "password", "clientId")).thenReturn(userScopeAccess);
        when(scopeAccessService.getOpenstackEndpointsForScopeAccess(userScopeAccess)).thenReturn(endpoints);
        when(authorizationService.authorizeCloudIdentityAdmin(userScopeAccess)).thenReturn(true);
        when(tenantService.getTenantRolesForScopeAccess(userScopeAccess)).thenReturn(roles);
        when(tokenConverterCloudV20.toToken(userScopeAccess)).thenReturn(token);
        when(tenantService.getTenantByName("tenantName")).thenReturn(tenant);
        when(authConverterCloudV20.toAuthenticationResponse(eq(userTest), eq(userScopeAccess), eq(roles), any(List.class))).thenReturn(authenticateResponse);
        Response.ResponseBuilder responseBuilder = spy.authenticate(httpHeaders, authenticationRequest);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listRoles_throwsForbiddenException_returnsResponseBuilder() throws Exception {
        ForbiddenException forbiddenException = new ForbiddenException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doThrow(forbiddenException).when(spy).verifyServiceAdminLevelAccess(authToken);
        when(exceptionHandler.exceptionResponse(forbiddenException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.listRoles(httpHeaders, authToken, null, null, null), equalTo(responseBuilder));
    }

    @Test
    public void listTenants_scopeAccessServiceCallsGetScopeAccessByAccessTokenReturnsNull_responseIsOk_returns200() throws Exception {
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        when(scopeAccessService.getAccessTokenByAuthHeader(authToken)).thenReturn(new ScopeAccess());
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.listTenants(httpHeaders, authToken, "marker", 1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUserGlobalRoles_notIdentityAdminAndIsServiceAdmin_responseOk_returns200() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        when(userService.getUserById("userId")).thenReturn(user);
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).getUser(scopeAccess);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(true);
        when(tenantService.getGlobalRolesForUser(user)).thenReturn(new ArrayList<TenantRole>());
        Response.ResponseBuilder responseBuilder = spy.listUserGlobalRoles(httpHeaders, authToken, "userId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUserGlobalRoles_isIdentityAdminAndNotServiceAdmin_responseOk_returns200() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doNothing().when(spy).verifyUserLevelAccess(authToken);
        when(userService.getUserById("userId")).thenReturn(user);
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        doReturn(new User()).when(spy).getUser(scopeAccess);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(false);
        when(tenantService.getGlobalRolesForUser(user)).thenReturn(new ArrayList<TenantRole>());
        Response.ResponseBuilder responseBuilder = spy.listUserGlobalRoles(httpHeaders, authToken, "userId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listGroups_verifyServiceAdmin_throwsForbiddenException_returns403() throws Exception {
        doThrow(new ForbiddenException()).when(spy).verifyServiceAdminLevelAccess(authToken);
        Response.ResponseBuilder responseBuilder = spy.listGroups(httpHeaders, authToken, null, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void getGroup_verifyServiceAdmin_throwsForbiddenException_returns403() throws Exception {
        doThrow(new ForbiddenException()).when(spy).verifyServiceAdminLevelAccess(authToken);
        Response.ResponseBuilder responseBuilder = spy.getGroup(httpHeaders, authToken, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test (expected = BadRequestException.class)
    public void impersonate_userIsEnabled_userScopeAccessTokenNotExpiredAndImpersonatingUsernameIsBlank_throwsBadRequestException() throws Exception {
        user.setEnabled(true);
        user.setUniqueId("uniqueId");
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        userScopeAccess.setAccessTokenString("notExpired");
        org.openstack.docs.identity.api.v2.User userTest = new org.openstack.docs.identity.api.v2.User();
        userTest.setUsername("");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(userTest);
        doNothing().when(spy).verifyRackerOrServiceAdminAccess(authToken);
        doNothing().when(spy).validateImpersonationRequest(impersonationRequest);
        when(userService.getUser("")).thenReturn(user);
        doReturn(true).when(spy).isValidImpersonatee(user);
        when(config.getString("cloudAuth.clientId")).thenReturn("clientId");
        when(scopeAccessService.getDirectScopeAccessForParentByClientId("uniqueId", "clientId")).thenReturn(userScopeAccess);
        spy.impersonate(httpHeaders, authToken, impersonationRequest);
    }

    @Test (expected = BadRequestException.class)
    public void impersonate_userIsEnabled_userScopeAccessTokenExpiredAndImpersonatingTokenIsBlank_throwsBadRequestException() throws Exception {
        user.setEnabled(true);
        user.setUniqueId("uniqueId");
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        userScopeAccess.setAccessTokenString("");
        org.openstack.docs.identity.api.v2.User userTest = new org.openstack.docs.identity.api.v2.User();
        userTest.setUsername("");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(userTest);
        doNothing().when(spy).verifyRackerOrServiceAdminAccess(authToken);
        doNothing().when(spy).validateImpersonationRequest(impersonationRequest);
        when(userService.getUser("")).thenReturn(user);
        doReturn(true).when(spy).isValidImpersonatee(user);
        when(config.getString("cloudAuth.clientId")).thenReturn("clientId");
        when(scopeAccessService.getDirectScopeAccessForParentByClientId("uniqueId", "clientId")).thenReturn(userScopeAccess);
        spy.impersonate(httpHeaders, authToken, impersonationRequest);
    }

    @Test
    public void listDefaultRegionServices_openStackServicesIsNull_responseOk_returns200() throws Exception {
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        when(clientService.getOpenStackServices()).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.listDefaultRegionServices(authToken);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listDefaultRegionServices_userForDefaultRegionIsNull_responseOk_returns200() throws Exception {
        Application application = new Application();
        application.setUseForDefaultRegion(null);
        List<Application> openStackServices = new ArrayList<Application>();
        openStackServices.add(application);
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        when(clientService.getOpenStackServices()).thenReturn(openStackServices);
        Response.ResponseBuilder responseBuilder = spy.listDefaultRegionServices(authToken);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listDefaultRegionServices_userForDefaultRegionNotNullAndIsFalse_responseOk_returns200() throws Exception {
        Application application = new Application();
        application.setUseForDefaultRegion(false);
        List<Application> openStackServices = new ArrayList<Application>();
        openStackServices.add(application);
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        when(clientService.getOpenStackServices()).thenReturn(openStackServices);
        Response.ResponseBuilder responseBuilder = spy.listDefaultRegionServices(authToken);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void validateImpersonationRequest_validRequest_doesNotThrowAnyExceptions() throws Exception {
        org.openstack.docs.identity.api.v2.User userTest = new org.openstack.docs.identity.api.v2.User();
        userTest.setUsername("username");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(userTest);
        impersonationRequest.setExpireInSeconds(2);
        spy.validateImpersonationRequest(impersonationRequest);
    }

    @Test
    public void listUserGroups_groupsSizeNotZero_responseOk_returns200() throws Exception {
        GroupService cloudGroupService = mock(GroupService.class);
        spy.setCloudGroupService(cloudGroupService);
        Group group = new Group();
        List<Group> groups = new ArrayList<Group>();
        groups.add(group);
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        when(cloudGroupService.getGroupsForUser("userId")).thenReturn(groups);
        when(cloudKsGroupBuilder.build(group)).thenReturn(new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group());
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(httpHeaders, authToken, "userId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test (expected = BadRequestException.class)
    public void validateKsGroup_groupNameIsEmpty() throws Exception {
        com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group group = new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group();
        group.setName("");
        spy.validateKsGroup(group);
    }

    @Test
    public void validateToken_scopeAccessInstanceOfRackerScopeAccess_rackerRolesIsNull_responseOk_returns200() throws Exception {
        UserForAuthenticateResponse userForAuthenticateResponse = new UserForAuthenticateResponse();
        List<TenantRole> roleList = new ArrayList<TenantRole>();
        Token token = new Token();
        Racker racker = new Racker();
        racker.setRackerId("rackerId");
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        rackerScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        rackerScopeAccess.setAccessTokenString("notExpired");
        rackerScopeAccess.setRackerId("rackerId");
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doReturn(rackerScopeAccess).when(spy).checkAndGetToken("tokenId");
        when(tokenConverterCloudV20.toToken(rackerScopeAccess)).thenReturn(token);
        when(userService.getRackerByRackerId("rackerId")).thenReturn(racker);
        when(tenantService.getTenantRolesForScopeAccess(rackerScopeAccess)).thenReturn(roleList);
        when(userService.getRackerRoles("rackerId")).thenReturn(null);
        when(userConverterCloudV20.toUserForAuthenticateResponse(racker, roleList)).thenReturn(userForAuthenticateResponse);
        Response.ResponseBuilder responseBuilder = spy.validateToken(httpHeaders, authToken, "tokenId", "belongsTo");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void validateToken_scopeAccessNotInstanceOfUserRackerOrImpersonatedScopeAccess_responseOk_returns200() throws Exception {
        Token token = new Token();
        ClientScopeAccess clientScopeAccess = new ClientScopeAccess();
        clientScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        clientScopeAccess.setAccessTokenString("notExpired");
        doNothing().when(spy).verifyServiceAdminLevelAccess(authToken);
        doReturn(clientScopeAccess).when(spy).checkAndGetToken("tokenId");
        when(tokenConverterCloudV20.toToken(clientScopeAccess)).thenReturn(token);
        Response.ResponseBuilder responseBuilder = spy.validateToken(httpHeaders, authToken, "tokenId", "belongsTo");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getRolesForScopeAccess_scopeAccessNotInstanceOfUserOrImpersonatedScopeAccess_returnsNull() throws Exception {
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        List<TenantRole> result = spy.getRolesForScopeAccess(rackerScopeAccess);
        assertThat("list", result, equalTo(null));
    }

    @Test
    public void checkForMultipleIdentityRoles_userRoleGetNameNotStartWithIdentity_throwsNoException() throws Exception {
        ClientRole clientRole = new ClientRole();
        clientRole.setName("identity:admin");
        TenantRole tenantRole = new TenantRole();
        tenantRole.setName("tenantName");
        List<TenantRole> tenantRoles = new ArrayList<TenantRole>();
        tenantRoles.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(user)).thenReturn(tenantRoles);
        spy.checkForMultipleIdentityRoles(user, clientRole);
        assertTrue("no errors", true);
    }

    @Test (expected = NotAuthorizedException.class)
    public void getScopeAccessForValidToken_scopeAccessIsNull_throwsNotAuthorizedException() throws Exception {
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(null);
        spy.getScopeAccessForValidToken(authToken);
    }

    @Test (expected = NotAuthorizedException.class)
    public void getScopeAccessForValidToken_scopeAccessIsExpired_throwsNotAuthorizedException() throws Exception {
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        rackerScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(rackerScopeAccess);
        spy.getScopeAccessForValidToken(authToken);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessIsServiceAdmin_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(true);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudUser(scopeAccess)).thenReturn(false);
        spy.verifyUserLevelAccess(authToken);
        assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessIsUserAdmin_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(true);
        when(authorizationService.authorizeCloudUser(scopeAccess)).thenReturn(false);
        spy.verifyUserLevelAccess(authToken);
        assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserLevelAccess_scopeAccessIsUser_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudUser(scopeAccess)).thenReturn(true);
        spy.verifyUserLevelAccess(authToken);
        assertTrue("no exceptions", true);
    }

    @Test
    public void verifyUserAdminLevelAccess_isUserAdmin_doesNotThrowException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(true);
        spy.verifyUserAdminLevelAccess(authToken);
    }

    @Test (expected =  ForbiddenException.class)
    public void verifyTokenHasTenant_tenantIdNotEquals_throwsForbiddenExcpetion() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        Tenant tenant = new Tenant();
        tenant.setTenantId("notMatch");
        List<Tenant> tenantList = new ArrayList<Tenant>();
        tenantList.add(tenant);
        when(tenantService.getTenantsForScopeAccessByTenantRoles(scopeAccess)).thenReturn(tenantList);
        spy.verifyTokenHasTenant("tenantId", scopeAccess);
    }

    @Test
    public void getJSONCredentials_credentialIsInstanceOfApiKeyCredentials_returnsJAXBElementOfApiKeyCredentials() throws Exception {
        String jsonBody = "{\"RAX-KSKEY:apiKeyCredentials\":{\"username\":\"test_user\",\"apiKey\":\"aaaaa-bbbbb-ccccc-12345678\"}}";
        when(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory());
        JAXBElement<? extends CredentialType> jaxbElement = spy.getJSONCredentials(jsonBody);
        ApiKeyCredentials result = (ApiKeyCredentials) jaxbElement.getValue();
        assertThat("apikey", result.getApiKey(), equalTo("aaaaa-bbbbb-ccccc-12345678"));
        assertThat("username", result.getUsername(), equalTo("test_user"));
    }
}
