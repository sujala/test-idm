package com.rackspace.idm.api.resource.cloud.v20;


import com.rackspace.docs.identity.api.ext.rax_auth.v1.*;
import com.rackspace.docs.identity.api.ext.rax_kskey.v1.ApiKeyCredentials;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.api.converter.cloudv20.*;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.Validator;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.pagination.DefaultPaginator;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.entity.Domain;
import com.rackspace.idm.domain.entity.Tenant;
import com.rackspace.idm.domain.entity.User;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.*;
import com.rackspace.idm.validation.Validator20;
import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
import org.apache.commons.configuration.Configuration;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.openstack.docs.common.api.v1.Extension;
import org.openstack.docs.common.api.v1.Extensions;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.UserForCreate;
import org.openstack.docs.identity.api.ext.os_kscatalog.v1.EndpointTemplate;
import org.openstack.docs.identity.api.ext.os_ksec2.v1.Ec2CredentialsType;
import org.openstack.docs.identity.api.v2.*;
import org.openstack.docs.identity.api.v2.ObjectFactory;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import javax.ws.rs.core.*;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.net.URI;
import java.util.*;

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


public class DefaultCloud20ServiceOldTest {

    private DefaultCloud20Service defaultCloud20Service;
    private DefaultCloud20Service spy;
    private Configuration config;
    private ExceptionHandler exceptionHandler;
    private UserService userService;
    private DomainService domainService;
    private GroupService userGroupService;
    private DefaultRegionService defaultRegionService;
    private JAXBObjectFactories jaxbObjectFactories;
    private ScopeAccessService scopeAccessService;
    private AuthorizationService authorizationService;
    private AuthenticationService authenticationService;
    private TenantService tenantService;
    private EndpointService endpointService;
    private ApplicationService clientService;
    private QuestionService questionService;
    private UserConverterCloudV20 userConverterCloudV20;
    private TenantConverterCloudV20 tenantConverterCloudV20;
    private TokenConverterCloudV20 tokenConverterCloudV20;
    private EndpointConverterCloudV20 endpointConverterCloudV20;
    private RoleConverterCloudV20 roleConverterCloudV20;
    private DomainConverterCloudV20 domainConverterCloudV20;
    private String authToken = "token";
    private EndpointTemplate endpointTemplate;
    private String userId = "id";
    private User user;
    private Users users;
    private Tenant tenant;
    private String tenantId = "tenantId";
    private Token token;
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
    private Validator20 validator20;
    private Validator validator;
    private Domain domain;
    private DefaultPaginator<User> userPaginator;
    private DefaultPaginator<ClientRole> clientRolePaginator;

    @Before
    public void setUp() throws Exception {
        defaultCloud20Service = new DefaultCloud20Service();

        //mocks
        userService = mock(UserService.class);
        domainService = mock(DomainService.class);
        userGroupService = mock(GroupService.class);
        jaxbObjectFactories = mock(JAXBObjectFactories.class);
        scopeAccessService = mock(ScopeAccessService.class);
        authorizationService = mock(AuthorizationService.class);
        authenticationService = mock(AuthenticationService.class);
        userConverterCloudV20 = mock(UserConverterCloudV20.class);
        tenantConverterCloudV20 = mock(TenantConverterCloudV20.class);
        tokenConverterCloudV20 = mock(TokenConverterCloudV20.class);
        endpointConverterCloudV20 = mock(EndpointConverterCloudV20.class);
        roleConverterCloudV20 = mock(RoleConverterCloudV20.class);
        domainConverterCloudV20 = mock(DomainConverterCloudV20.class);
        tenantService = mock(TenantService.class);
        endpointService = mock(EndpointService.class);
        clientService = mock(ApplicationService.class);
        questionService = mock(QuestionService.class);
        config = mock(Configuration.class);
        cloudKsGroupBuilder = mock(CloudKsGroupBuilder.class);
        atomHopperClient = mock(AtomHopperClient.class);
        httpHeaders = mock(HttpHeaders.class);
        authConverterCloudV20 = mock(AuthConverterCloudV20.class);
        serviceConverterCloudV20 = mock(ServiceConverterCloudV20.class);
        delegateCloud20Service = mock(DelegateCloud20Service.class);
        exceptionHandler = mock(ExceptionHandler.class);
        defaultRegionService = mock(DefaultRegionService.class);
        validator20 = mock(Validator20.class);
        validator = mock(Validator.class);
        userPaginator = mock(DefaultPaginator.class);
        clientRolePaginator = mock(DefaultPaginator.class);
        uriInfo = mock(UriInfo.class);

        //setting mocks
        defaultCloud20Service.setUserService(userService);
        defaultCloud20Service.setUserGroupService(userGroupService);
        defaultCloud20Service.setObjFactories(jaxbObjectFactories);
        defaultCloud20Service.setScopeAccessService(scopeAccessService);
        defaultCloud20Service.setAuthorizationService(authorizationService);
        defaultCloud20Service.setUserConverterCloudV20(userConverterCloudV20);
        defaultCloud20Service.setTenantConverterCloudV20(tenantConverterCloudV20);
        defaultCloud20Service.setEndpointConverterCloudV20(endpointConverterCloudV20);
        defaultCloud20Service.setTenantService(tenantService);
        defaultCloud20Service.setTokenConverterCloudV20(tokenConverterCloudV20);
        defaultCloud20Service.setEndpointService(endpointService);
        defaultCloud20Service.setApplicationService(clientService);
        defaultCloud20Service.setConfig(config);
        defaultCloud20Service.setCloudKsGroupBuilder(cloudKsGroupBuilder);
        defaultCloud20Service.setAtomHopperClient(atomHopperClient);
        defaultCloud20Service.setRoleConverterCloudV20(roleConverterCloudV20);
        defaultCloud20Service.setAuthConverterCloudV20(authConverterCloudV20);
        defaultCloud20Service.setServiceConverterCloudV20(serviceConverterCloudV20);
        defaultCloud20Service.setDelegateCloud20Service(delegateCloud20Service);
        defaultCloud20Service.setExceptionHandler(exceptionHandler);
        defaultCloud20Service.setValidator20(validator20);
        defaultCloud20Service.setValidator(validator);
        defaultCloud20Service.setDefaultRegionService(defaultRegionService);
        defaultCloud20Service.setDomainService(domainService);
        defaultCloud20Service.setDomainConverterCloudV20(domainConverterCloudV20);
        defaultCloud20Service.setAuthenticationService(authenticationService);
        defaultCloud20Service.setApplicationRolePaginator(clientRolePaginator);
        defaultCloud20Service.setUserPaginator(userPaginator);
        defaultCloud20Service.setQuestionService(questionService);

        //fields
        user = new User();
        user.setUsername(userId);
        user.setId(userId);
        user.setMossoId(123);
        user.setRegion("region");
        user.setDomainId("domain");
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
        token = new Token();
        token.setId("abcdefg");
        domain = new Domain();
        domain.setDomainId("1");
        domain.setName("domain");
        domain.setDescription("");
        domain.setEnabled(true);
        
        //stubbing
        when(jaxbObjectFactories.getRackspaceIdentityExtKsgrpV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.ObjectFactory());
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(new org.openstack.docs.identity.api.v2.ObjectFactory());
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(userScopeAccess);
        when(authorizationService.authorizeCloudServiceAdmin(userScopeAccess)).thenReturn(true);
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
        when(userGroupService.checkAndGetGroupById(anyInt())).thenReturn(group);
        when(uriInfo.getAbsolutePath()).thenReturn(new URI("http://absolute.path/to/resource"));

        spy = spy(defaultCloud20Service);
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
        verify(clientService,times(5)).updateClient(any(Application.class));
    }

    @Test
    public void setDefaultRegionServices_callsVerifyServiceAdminAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.setDefaultRegionServices(authToken, new DefaultRegionServices());
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
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
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listDefaultRegionServices(authToken);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
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
    public void addUser_withServiceAdmin_domainId_throwsBadRequestException() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        User userHasDomain = new User();
        userHasDomain.setDomainId("135792468");

        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(false).when(authorizationService).authorizeCloudUser(any(ScopeAccess.class));
        doReturn(true).when(authorizationService).authorizeCloudServiceAdmin(any(ScopeAccess.class));
        doReturn(userHasDomain).when(userConverterCloudV20).toUserDO(userOS);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);

        defaultCloud20Service.addUser(httpHeaders, uriInfo, authToken, userOS);
    }

    @Test
    public void addUser_withIdentityAdmin_noDomainId_throwsBadRequest() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        ScopeAccess scopeAccess = new ScopeAccess();
        User userDO = new User();

        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(userConverterCloudV20.toUserDO(any(UserForCreate.class))).thenReturn(userDO);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);

        defaultCloud20Service.addUser(httpHeaders, uriInfo, authToken, userOS);

        assertThat("exception type", argumentCaptor.getValue(), instanceOf(BadRequestException.class));
    }

    @Test
    public void deleteUser_isUserAdmin_callsVerifyDomain() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        User caller = new User();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(true);
        when(userService.getUserByAuthToken(authToken)).thenReturn(caller);
        spy.deleteUser(null, authToken, userId);
        verify(authorizationService).verifyDomain(caller, user);
    }

    @Test
    public void deleteUser_userIsUserAdmin_callsUserService_hasSubUsers() throws Exception {
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(authorizationService.hasUserAdminRole(user)).thenReturn(true);
        spy.deleteUser(httpHeaders, authToken, userId);
        verify(userService).hasSubUsers(userId);
    }

    @Test
    public void deleteUser_userServiceHasSubUsersWithUserId_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        UserScopeAccess scopeAccess = new UserScopeAccess();

        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.checkAndGetUserById("userId")).thenReturn(user);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(false);
        when(scopeAccessService.getScopeAccessByUserId("userId")).thenReturn(scopeAccess);
        when(authorizationService.hasUserAdminRole(user)).thenReturn(true);
        when(userService.hasSubUsers("userId")).thenReturn(true);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);

        assertThat("response builder", spy.deleteUser(null, authToken, "userId"), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(BadRequestException.class));
    }

    @Test
    public void deleteUser_callsAuthService_hasUserAdminRole() throws Exception {
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(scopeAccessService.getScopeAccessByUserId(userId)).thenReturn(new UserScopeAccess());
        spy.deleteUser(httpHeaders, authToken, userId);
        verify(authorizationService).hasUserAdminRole(user);
    }

    @Test
    public void addUserCredential_returns200() throws Exception {
        ApiKeyCredentials apiKeyCredentials1 = new ApiKeyCredentials();
        UriBuilder uriBuilder = mock(UriBuilder.class);
        apiKeyCredentials1.setUsername(userId);
        apiKeyCredentials1.setApiKey("bar");
        doReturn(new JAXBElement<CredentialType>(new QName(""), CredentialType.class, apiKeyCredentials1)).when(spy).getXMLCredentials(anyString());
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(new URI("uri"));

        Response.ResponseBuilder responseBuilder = spy.addUserCredential(httpHeaders, uriInfo, authToken, userId, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
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
        when(tenantService.getGlobalRolesForUser(user)).thenReturn(tenantRoleList);
        boolean validImpersonatee = spy.isValidImpersonatee(user);
        assertThat("boolean value", validImpersonatee, equalTo(true));
    }

    @Test
    public void isValidImpersonatee_roleIsUserAdmin_returnsTrue() throws Exception {
        List<TenantRole> tenantRoleList = new ArrayList<TenantRole>();
        tenantRole.setName("identity:user-admin");
        tenantRoleList.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(user)).thenReturn(tenantRoleList);
        boolean validImpersonatee = spy.isValidImpersonatee(user);
        assertThat("boolean value", validImpersonatee, equalTo(true));
    }

    @Test
    public void isValidImpersonatee_roleIsNotDefaultOrUserAdmin_returnsFalse() throws Exception {
        List<TenantRole> tenantRoleList = new ArrayList<TenantRole>();
        tenantRole.setName("identity:neither");
        tenantRoleList.add(tenantRole);
        when(tenantService.getGlobalRolesForUser(user)).thenReturn(tenantRoleList);
        boolean validImpersonatee = spy.isValidImpersonatee(user);
        assertThat("boolean value", validImpersonatee, equalTo(false));
    }

    @Test
    public void validateToken_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(null);
        spy.validateToken(null, null, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void validateToken_whenExpiredToken_returns404() throws Exception {
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        rackerScopeAccess.setAccessTokenString("rackerToken");
        rackerScopeAccess.setAccessTokenExpired();
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(rackerScopeAccess).when(spy).checkAndGetToken("token");
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("Response Builder", spy.validateToken(null, authToken, "token", null), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(NotFoundException.class));
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
    public void validateToken_whenUserScopeAccessResponseOk_returns200() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        userScopeAccess.setAccessTokenString("userToken");
        doReturn(userScopeAccess).when(spy).checkAndGetToken("token");
        Response.ResponseBuilder responseBuilder = spy.validateToken(httpHeaders, authToken, "token", "belongsTo");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void validateToken_whenImpersonatedScopeAccess_callsValidateBelongsTo() throws Exception {
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        impersonatedScopeAccess.setAccessTokenString("impToken");
        impersonatedScopeAccess.setImpersonatingUsername("impersonating");

        List<TenantRole> roles = new ArrayList<TenantRole>();

        doReturn(impersonatedScopeAccess).when(spy).checkAndGetToken("token");
        when(userService.getUser("impersonating")).thenReturn(user);
        when(tenantService.getTenantRolesForUser(user)).thenReturn(roles);

        spy.validateToken(httpHeaders, authToken, "token", "tenantId");
        verify(validator20).validateTenantIdInRoles("tenantId", roles);
    }

    @Test
    public void validateToken_whenImpersonatedScopeAccessResponseOk_returns200() throws Exception {
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        impersonatedScopeAccess.setAccessTokenString("impToken");
        doReturn(impersonatedScopeAccess).when(spy).checkAndGetToken("token");
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.validateToken(httpHeaders, authToken, "token", "belongsTo");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void revokeSelfToken_returnsAccepted() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.revokeToken(httpHeaders, authToken);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void revokeToken_returnsAccepted() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.revokeToken(httpHeaders, authToken, "token");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void revokeToken_userAdminReturnsAccepted() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ScopeAccess scopeAccessAdmin = new ScopeAccess();
        when(scopeAccessService.getScopeAccessByAccessToken("tokenXXX")).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUserAdmin(org.mockito.Matchers.<ScopeAccess>anyObject())).thenReturn(true);
        Response.ResponseBuilder responseBuilder = spy.revokeToken(httpHeaders, authToken, "tokenXXX");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void revokeToken_userIdentityAdminReturnsAccepted() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        ScopeAccess scopeAccessAdmin = new ScopeAccess();
        when(scopeAccessService.getScopeAccessByAccessToken("tokenXXX")).thenReturn(scopeAccess);
        when(authorizationService.authorizeCloudUserAdmin(org.mockito.Matchers.<ScopeAccess>anyObject())).thenReturn(false);
        Response.ResponseBuilder responseBuilder = spy.revokeToken(httpHeaders, authToken, "tokenXXX");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test(expected = NotFoundException.class)
    public void revokeInvalidToken_returnsNotFound() throws Exception {
        when(scopeAccessService.getScopeAccessByAccessToken("invalid")).thenReturn(null);
        Response.ResponseBuilder responseBuilder = spy.revokeToken(httpHeaders, authToken, "invalid");
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
    public void authenticate_withTokenAndNoTenantId_returnsBadRequestStatus() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        TokenForAuthenticationRequest tokenForAuthenticationRequest = new TokenForAuthenticationRequest();
        tokenForAuthenticationRequest.setId("tokenId");
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setToken(tokenForAuthenticationRequest);

        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder",spy.authenticate(null, authenticationRequest),equalTo(responseBuilder));
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
        assertThat("message", argumentCaptor.getValue().getMessage(), equalTo("Invalid request. Specify tenantId OR tenantName, not both."));
    }

    @Test
    public void authenticate_withTenantIdAndNullToken_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        authenticationRequest.setTenantId("tenantId");
        authenticationRequest.setToken(null);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);

        assertThat("response builder", spy.authenticate(null, authenticationRequest), equalTo(responseBuilder));
        assertThat("response message", argumentCaptor.getValue().getMessage(), equalTo("Invalid request body: unable to parse Auth data. Please review XML or JSON formatting."));
    }

    @Test
    public void authenticate_withDomain_callsAuthenticateFederatedDomain() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain = new com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain();
        domain.setName("Rackspace");
        authenticationRequest.getAny().add(domain);
        authenticationRequest.setCredential(new JAXBElement(QName.valueOf("foo"), PasswordCredentialsRequiredUsername.class, passwordCredentialsRequiredUsername));
        spy.authenticate(null, authenticationRequest);
        verify(spy).authenticateFederatedDomain(null, authenticationRequest, domain);
    }

    @Test
    public void authenticate_withInvalidDomain_callsAuthenticateFederatedDomain() throws Exception {
        AuthenticationRequest authenticationRequest = new AuthenticationRequest();
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain = new com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain();
        domain.setName("NotRackspace");
        authenticationRequest.getAny().add(domain);
        authenticationRequest.setCredential(new JAXBElement(QName.valueOf("foo"), PasswordCredentialsRequiredUsername.class, passwordCredentialsRequiredUsername));
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.authenticate(null, authenticationRequest), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(BadRequestException.class));
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
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        UserForCreate userForCreate = new UserForCreate();
        userForCreate.setId("notSameId");
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);

        assertThat("response builder", spy.updateUser(httpHeaders, authToken, "123", userForCreate), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(BadRequestException.class));
    }

    @Test
    public void updateUser_userCannotDisableOwnAccount_throwsBadRequest() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        UserForCreate userForCreate = new UserForCreate();
        userForCreate.setEnabled(false);
        User user = new User();
        user.setEnabled(true);
        user.setId(userId);
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response code", spy.updateUser(httpHeaders, authToken, userId, userForCreate), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(BadRequestException.class));
    }

    @Test
    public void updateUser_withNoRegionAndPreviousRegionsExists_previousRegionRemains() throws Exception {
        UserForCreate userNoRegion = new UserForCreate();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(new User());
        User retrievedUser = new User("testUser");
        retrievedUser.setId("id");
        retrievedUser.setRegion("US of A");
        when(userService.checkAndGetUserById("id")).thenReturn(retrievedUser);
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
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.setUserConverterCloudV20(new UserConverterCloudV20());
        User retrievedUser = new User("testUser");
        retrievedUser.setId("id");
        retrievedUser.setRegion("US of A");
        when(userService.checkAndGetUserById("id")).thenReturn(retrievedUser);
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

        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doNothing().when(spy).assignProperRole(any(HttpHeaders.class), anyString(), any(ScopeAccess.class), any(User.class));
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
        User newUser = new User();
        newUser.setDomainId("domain");
        ArgumentCaptor<User> argumentCaptor = ArgumentCaptor.forClass(User.class);
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(domainService.createNewDomain(org.mockito.Matchers.<String>anyObject())).thenReturn("domain");
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(newUser);
        spy.addUser(httpHeaders, uriInfo, authToken, userNoRegion);
        verify(userService).addUser(argumentCaptor.capture());
        assertThat("user region", argumentCaptor.getValue().getRegion(), equalTo(null));
    }

    @Test
    public void addUser_callsVerifyUserAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.addUser(null, null, authToken, userOS);
        verify(authorizationService).verifyUserAdminLevelAccess(scopeAccess);
    }

    @Test
    public void addUser_withUserMissingUsername_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        BadRequestException badRequestException = new BadRequestException("missing username");
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(badRequestException).when(validator).validate20User(userOS);
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
        when(domainService.createNewDomain(org.mockito.Matchers.<String>anyObject())).thenReturn("domain");
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        spy.addUser(null, null, authToken, userOS);
        verify(userService).addUser(user);
    }

    @Test
    public void addUser_callsAuthorizationService_authorizeServiceAdmin() throws Exception {
        spy.addUser(null, null, authToken, userOS);
        verify(authorizationService).authorizeCloudIdentityAdmin(any(ScopeAccess.class));
    }

    @Test
    public void addUser_callerIsServiceAdmin_callsDefaultRegionService_validateDefaultRegion() throws Exception {
        when(domainService.createNewDomain(org.mockito.Matchers.<String>anyObject())).thenReturn("domain");
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        spy.addUser(null, null, authToken, userOS);
        verify(defaultRegionService).validateDefaultRegion(user.getRegion());
    }

    @Test
    public void addUser_callerIsServiceAdmin_defaultRegionMatchesUserRegion_setsRegion() throws Exception {
        when(domainService.createNewDomain(org.mockito.Matchers.<String>anyObject())).thenReturn("domain");
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(false);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);

        HashSet<String> defaultRegions = new HashSet<String>();
        defaultRegions.add("DFW");
        when(defaultRegionService.getDefaultRegionsForCloudServersOpenStack()).thenReturn(defaultRegions);
        user.setRegion("DFW");
        spy.addUser(null, null, authToken, userOS);
        ArgumentCaptor<User> argument = ArgumentCaptor.forClass(User.class);
        verify(userService).addUser(argument.capture());
        assertThat("exception", argument.getValue().getRegion(), equalTo("DFW"));
    }

    @Test
    public void addUser_userPasswordNotNull_callsValidatePassword() throws Exception {
        userOS.setPassword("password");
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.addUser(null, null, authToken, userOS);
        verify(validator).validatePasswordForCreateOrUpdate("password");
    }

    @Test
    public void addUser_userServiceDuplicateException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        when(domainService.createNewDomain(org.mockito.Matchers.<String>anyObject())).thenReturn("domain");
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        User newUser = new User();
        newUser.setDomainId("domain");
        when(userConverterCloudV20.toUserDO(userOS)).thenReturn(newUser);
        doThrow(new DuplicateException("duplicate")).when(userService).addUser(any(User.class));
        when(exceptionHandler.conflictExceptionResponse("duplicate")).thenReturn(responseBuilder);
        assertThat("response code", spy.addUser(null, null, authToken, userOS), equalTo(responseBuilder));
    }

    @Test
    public void addUser_userServiceDuplicateUserNameException_returnsResponseBuilder() throws Exception {
        User caller = new User();
        caller.setDomainId("domain");
        DuplicateUsernameException duplicateUsernameException = new DuplicateUsernameException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(domainService.createNewDomain(org.mockito.Matchers.<String>anyObject())).thenReturn("domain");
        when(userConverterCloudV20.toUserDO(userOS)).thenReturn(caller);
        when(userService.getUserByAuthToken(authToken)).thenReturn(caller);
        when(userService.getAllUsers(any(FilterParam[].class))).thenReturn(null);
        doNothing().when(spy).assignUserToCallersDomain(caller, caller);
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
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(userService.getUserById(userId)).thenReturn(user1);
        when(userService.getUserByAuthToken(authToken)).thenReturn(caller);
        spy.deleteUser(null, authToken, userId);
        verify(userService).softDeleteUser(any(User.class));
    }

    @Test
    public void deleteService_callsClientService_checkAndGetApplication() throws Exception {
        spy.deleteService(null, authToken, "clientId");
        verify(clientService).checkAndGetApplication("clientId");
    }

    @Test
    public void deleteService_callsClientService_deleteMethod() throws Exception {
        when(clientService.checkAndGetApplication("clientId")).thenReturn(application);
        spy.deleteService(null, authToken, "clientId");
        verify(clientService).delete("clientId");
    }

    @Test
    public void deleteRoleFromUserOnTenant_callsTenantService_checkAndGetTenantMethod() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.deleteRoleFromUserOnTenant(null, authToken, tenantId, userId, role.getId());
        verify(tenantService).checkAndGetTenant(tenantId);
    }

    @Test
    public void deleteRoleFromUserOnTenant_callsCheckAndGetUserById() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.deleteRoleFromUserOnTenant(null, authToken, tenantId, userId, role.getId());
        verify(userService).checkAndGetUserById(userId);
    }

    @Test
    public void deleteRole_withNullRole_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.deleteRole(null, authToken, null), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(BadRequestException.class));
    }

    @Test
    public void deleteEndpointTemplate_callsEndpointService_deleteBaseUrlMethod() throws Exception {
        when(endpointService.checkAndGetEndpointTemplate("101")).thenReturn(cloudBaseUrl);
        spy.deleteEndpointTemplate(null, authToken, "101");
        verify(endpointService).deleteBaseUrl(101);
    }

    @Test
    public void deleteEndpointTemplate_callsEndpointService_checkAndGetEndpointTemplate() throws Exception {
        spy.deleteEndpointTemplate(null, authToken, "101");
        verify(endpointService).checkAndGetEndpointTemplate("101");
    }

    @Test
    public void deleteEndpoint_callsTenantService_updateTenantMethod() throws Exception {
        when(tenantService.checkAndGetTenant(tenantId)).thenReturn(new Tenant());
        when(endpointService.checkAndGetEndpointTemplate("101")).thenReturn(cloudBaseUrl);
        spy.deleteEndpoint(null, authToken, tenantId, "101");
        verify(tenantService).updateTenant(any(Tenant.class));
    }

    @Test
    public void deleteEndpoint_callsTenantService_checkAndGetTenant() throws Exception {
        spy.deleteEndpoint(null, authToken, tenantId, "101");
        verify(tenantService).checkAndGetTenant(tenantId);
    }

    @Test
    public void deleteEndpoint_checkAndGetEndpointTemplate() throws Exception {
        spy.deleteEndpoint(null, authToken, tenantId, "101");
        verify(endpointService).checkAndGetEndpointTemplate("101");
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

    @Test(expected = ForbiddenException.class)
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
    public void addTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.addTenant(null, null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
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
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        tenantOS.setName(null);
        assertThat("response builder", spy.addTenant(null, null, authToken, tenantOS), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(BadRequestException.class));
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
    public void addService_callsClientService_add() throws Exception {
        spy.addService(null, null, authToken, service);
        verify(clientService).add(any(Application.class));
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
    public void addRole_isAdminCall_callsVerifyServiceAdminLevelAccess() throws Exception {
        Role role1 = new Role();
        role1.setServiceId("id");
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.addRole(null, null, authToken, role1);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void addRole_callsClientService_checkAndGetApplication() throws Exception {
        doReturn(new ScopeAccess()).when(spy).getScopeAccessForValidToken(authToken);
        spy.addRole(null, null, authToken, role);
        verify(clientService).checkAndGetApplication(role.getServiceId());
    }

    @Test
    public void addRole_callsClientService_addClientRole() throws Exception {
        doReturn(new ScopeAccess()).when(spy).getScopeAccessForValidToken(authToken);
        when(clientService.checkAndGetApplication(role.getServiceId())).thenReturn(application);
        spy.addRole(null, null, authToken, role);
        verify(clientService).addClientRole(any(ClientRole.class));
    }

    @Test
    public void addRole_roleWithNullName_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        Role role1 = new Role();
        role1.setName(null);
        assertThat("response builder", spy.addRole(null, null, authToken, role1), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(BadRequestException.class));
    }

    @Test
    public void addRole_nullRole_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response bulider", spy.addRole(null, null, authToken, null), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(BadRequestException.class));
    }

    @Test
    public void addRole_roleWithNullServiceId_returnsDefaults() throws Exception {
        Role role1 = new Role();
        role1.setName("roleName");
        role1.setServiceId("applicationId");
        when(clientService.checkAndGetApplication("applicationId")).thenReturn(application);
        spy.addRole(null, null, authToken, role1);
        verify(clientService).addClientRole(any(ClientRole.class));
    }

    @Test
    public void addRole_roleWithIdentityName_callsVerifyServiceAdminLevelAccess() throws Exception {
        Role role1 = new Role();
        role1.setName("Identity:role");
        role1.setServiceId("serviceId");
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.addRole(null, null, authToken, role1);
        verify(authorizationService).verifyServiceAdminLevelAccess(scopeAccess);
    }

    @Test
    public void addRole_roleWithIdentityNameWithNotIdenityAdmin_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<ForbiddenException> argumentCaptor = ArgumentCaptor.forClass(ForbiddenException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        Role role1 = new Role();
        role1.setName("Identity:role");
        role1.setServiceId("serviceId");
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(new ForbiddenException()).when(authorizationService).verifyServiceAdminLevelAccess(scopeAccess);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.addRole(null, null, authToken, role1), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(ForbiddenException.class));
    }

    @Test
    public void addRole_roleWithIdentityNameWithServiceAdmin_returns201Status() throws Exception {
        Role role1 = new Role();
        role1.setName("Identity:role");
        role1.setServiceId("applicationId");
        JAXBElement<Role> someValue = new JAXBElement(new QName("http://docs.openstack.org/identity/api/v2.0", "role"), Role.class, null, role);
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(clientService.checkAndGetApplication("applicationId")).thenReturn(application);
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
        when(clientService.checkAndGetApplication(role.getServiceId())).thenReturn(application);
        when(jaxbObjectFactories.getOpenStackIdentityV2Factory()).thenReturn(objectFactory);
        when(roleConverterCloudV20.toRoleFromClientRole(any(ClientRole.class))).thenReturn(role);
        when(objectFactory.createRole(role)).thenReturn(someValue);
        Response.ResponseBuilder responseBuilder = spy.addRole(httpHeaders, uriInfo, authToken, role);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addRole_roleConflictThrowsDuplicateException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(clientService.checkAndGetApplication(role.getServiceId())).thenReturn(application);
        doThrow(new DuplicateException("role conflict")).when(clientService).addClientRole(any(ClientRole.class));
        when(exceptionHandler.conflictExceptionResponse("role conflict")).thenReturn(responseBuilder);
        assertThat("response builder", spy.addRole(null, null, authToken, role), equalTo(responseBuilder));
    }

    @Test
    public void addRolesToUserOnTenant_callsVerifyTokenHasAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.addRolesToUserOnTenant(null, authToken, tenantId, null, null);
        verify(authorizationService).verifyTokenHasTenantAccess(tenantId, scopeAccess);
    }

    @Test
    public void addRolesToUserOnTenant_callsTenantService_checkAndGetTenant() throws Exception {
        clientRole.setName("name");
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(clientRole).when(spy).checkAndGetClientRole(role.getId());
        when(config.getString("cloudAuth.serviceAdminRole")).thenReturn("identity:service-admin");
        when(config.getString("cloudAuth.adminRole")).thenReturn("identity:admin");
        when(config.getString("cloudAuth.userAdminRole")).thenReturn("identity:user-admin");
        spy.addRolesToUserOnTenant(null, authToken, tenantId, userId, role.getId());
        verify(tenantService).checkAndGetTenant(tenantId);
    }

    @Test
    public void addRolesToUserOnTenant_checkAndGetUserById() throws Exception {
        clientRole.setName("name");
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(clientRole).when(spy).checkAndGetClientRole(role.getId());
        when(config.getString("cloudAuth.serviceAdminRole")).thenReturn("identity:service-admin");
        when(config.getString("cloudAuth.adminRole")).thenReturn("identity:admin");
        when(config.getString("cloudAuth.userAdminRole")).thenReturn("identity:user-admin");
        spy.addRolesToUserOnTenant(null, authToken, tenantId, userId, role.getId());
        verify(userService).checkAndGetUserById(userId);
    }

    @Test
    public void addRolesToUserOnTenant_roleNameEqualsCloudServiceAdmin_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        clientRole.setName("identity:admin");
        doReturn(null).when(spy).getScopeAccessForValidToken(null);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        doReturn(clientRole).when(spy).checkAndGetClientRole(null);
        when(config.getString("cloudAuth.serviceAdminRole")).thenReturn("identity:service-admin");
        when(config.getString("cloudAuth.adminRole")).thenReturn("identity:admin");
        when(config.getString("cloudAuth.userAdminRole")).thenReturn("identity:user-admin");
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.addRolesToUserOnTenant(null, null, null, userId, null), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(ForbiddenException.class));
    }

    @Test
    public void addRolesToUserOnTenant_roleNameEqualsUserAdmin_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        clientRole.setName("identity:user-admin");
        doReturn(null).when(spy).getScopeAccessForValidToken(null);
        when(userService.checkAndGetUserById(null)).thenReturn(user);
        doReturn(clientRole).when(spy).checkAndGetClientRole(null);
        when(config.getString("cloudAuth.serviceAdminRole")).thenReturn("identity:service-admin");
        when(config.getString("cloudAuth.adminRole")).thenReturn("identity:admin");
        when(config.getString("cloudAuth.userAdminRole")).thenReturn("identity:user-admin");
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("resonse builder", spy.addRolesToUserOnTenant(null, null, null, userId, null), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(ForbiddenException.class));
    }

    @Test
    public void addRolesToUserOnTenant_roleNameEqualsAdmin_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        clientRole.setName("identity:admin");
        doReturn(null).when(spy).getScopeAccessForValidToken(null);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        doReturn(clientRole).when(spy).checkAndGetClientRole(null);
        when(config.getString("cloudAuth.serviceAdminRole")).thenReturn("identity:service-admin");
        when(config.getString("cloudAuth.adminRole")).thenReturn("identity:admin");
        when(config.getString("cloudAuth.userAdminRole")).thenReturn("identity:user-admin");
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.addRolesToUserOnTenant(null, null, null, userId, null), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(ForbiddenException.class));
    }

    @Test
    public void addEndpoint_callsTenantService_updateTenant() throws Exception {
        when(tenantService.checkAndGetTenant(tenantId)).thenReturn(tenant);
        when(endpointService.checkAndGetEndpointTemplate(endpointTemplate.getId())).thenReturn(cloudBaseUrl);
        spy.addEndpoint(null, authToken, tenantId, endpointTemplate);
        verify(tenantService).updateTenant(tenant);
    }

    @Test
    public void addEndpoint_callsTenantService_checkAndGetTenant() throws Exception {
        spy.addEndpoint(null, authToken, tenantId, endpointTemplate);
        verify(tenantService).checkAndGetTenant(tenantId);
    }

    @Test
    public void addEndpoint_callsCheckAndGetEndpointTemplate() throws Exception {
        spy.addEndpoint(null, authToken, tenantId, endpointTemplate);
        verify(endpointService).checkAndGetEndpointTemplate(endpointTemplate.getId());
    }

    @Test
    public void addEndpoint_Global_throwBadRequestExceptionAndReturnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(endpointService.checkAndGetEndpointTemplate(endpointTemplate.getId())).thenReturn(cloudBaseUrl);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        cloudBaseUrl.setGlobal(true);
        assertThat("response builder", spy.addEndpoint(null, authToken, tenantId, endpointTemplate), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(BadRequestException.class));
    }

    @Test
    public void listUserGroups_noGroup_ReturnDefaultGroup() throws Exception {
        when(userGroupService.getGroupById(config.getInt(org.mockito.Matchers.<String>any()))).thenReturn(group);
        when(cloudKsGroupBuilder.build(org.mockito.Matchers.<Group>any())).thenReturn(groupKs);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.listUserGroups(null, authToken, userId);
        assertThat("Default Group added", ((com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups)responseBuilder.build().getEntity()).getGroup().get(0).getName(), equalTo("Group1"));
    }

    @Test
    public void listUserGroups_badRequestException_returnsResponseBuilder() throws Exception {
        BadRequestException badRequestException = new BadRequestException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(badRequestException).when(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
        when(exceptionHandler.exceptionResponse(badRequestException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.listUserGroups(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void listUserGroups_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listUserGroups(null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
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
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.getGroupById(null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void getGroupById_throwsBadRequestException_returnsResponseBuilder() throws Exception {
        BadRequestException badRequestException = new BadRequestException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(badRequestException).when(validator20).validateGroupId(null);
        when(exceptionHandler.exceptionResponse(badRequestException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.getGroupById(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void getGroupById_callsValidateGroupId() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(null);
        spy.getGroupById(null, authToken, "1");
        verify(validator20).validateGroupId("1");
    }

    @Test
    public void getGroupById_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getGroupById(httpHeaders, authToken, "1");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listTenants_callsUserLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listTenants(null, authToken, null, null);
        verify(authorizationService).verifyUserLevelAccess(scopeAccess);
    }

    @Test
    public void listTenants_scopeAccessSaNotNullResponseOk_returns200() throws Exception {
        doReturn(new ScopeAccess()).when(spy).getScopeAccessForValidToken(authToken);
        Response.ResponseBuilder responseBuilder = spy.listTenants(httpHeaders, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void addEndpoint_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.addEndpoint(null, authToken, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void addEndpointTemplate_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.addEndpointTemplate(null, null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
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
        ArgumentCaptor<BaseUrlConflictException> argumentCaptor = ArgumentCaptor.forClass(BaseUrlConflictException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(endpointConverterCloudV20.toCloudBaseUrl(endpointTemplate)).thenReturn(cloudBaseUrl);
        doThrow(new BaseUrlConflictException()).when(endpointService).addBaseUrl(cloudBaseUrl);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.addEndpointTemplate(null, null, authToken, endpointTemplate), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(BaseUrlConflictException.class));
    }

    @Test
    public void addEndpointTemplate_endPointServiceThrowsDuplicateException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(endpointConverterCloudV20.toCloudBaseUrl(endpointTemplate)).thenReturn(cloudBaseUrl);
        when(exceptionHandler.conflictExceptionResponse("duplicate exception")).thenReturn(responseBuilder);
        doThrow(new DuplicateException("duplicate exception")).when(endpointService).addBaseUrl(cloudBaseUrl);
        assertThat("response builder", spy.addEndpointTemplate(null, null, authToken, endpointTemplate), equalTo(responseBuilder));
    }

    @Test
    public void addRolesToUserOnTenant_callsVerifyUserAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.addRolesToUserOnTenant(null, authToken, null, null, null);
        verify(authorizationService).verifyUserAdminLevelAccess(scopeAccess);
    }

    @Test
    public void addUserCredential_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.addUserCredential(null, uriInfo, authToken, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void addUserCredential_mediaTypeXML_callsGetXMLCredentials() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE)).thenReturn(true);
        spy.addUserCredential(httpHeaders, uriInfo, authToken, null, null);
        verify(spy).getXMLCredentials(null);
    }

    @Test
    public void addUserCredential_mediaTypeJSON_callsGetJSONCredentials() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)).thenReturn(true);
        spy.addUserCredential(httpHeaders, uriInfo, authToken, null, null);
        verify(spy).getJSONCredentials(null);
    }

    @Test
    public void addUserCredential_passwordCredentials_callsValidatePasswordCredentialsForCreateOrUpdate() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        user.setUsername("test_user");
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)).thenReturn(true);
        doReturn(new JAXBElement<PasswordCredentialsRequiredUsername>(QName.valueOf("foo"),PasswordCredentialsRequiredUsername.class,passwordCredentialsRequiredUsername)).when(spy).getJSONCredentials(jsonBody);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        spy.addUserCredential(httpHeaders, uriInfo, authToken, userId, jsonBody);
        verify(validator20).validatePasswordCredentialsForCreateOrUpdate(passwordCredentialsRequiredUsername);
    }

    @Test
    public void addUserCredential_passwordCredentials_callsCheckAndGetUserById() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        user.setUsername("test_user");
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)).thenReturn(true);
        doReturn(new JAXBElement<PasswordCredentialsRequiredUsername>(QName.valueOf("foo"),PasswordCredentialsRequiredUsername.class,passwordCredentialsRequiredUsername)).when(spy).getJSONCredentials(jsonBody);
        spy.addUserCredential(httpHeaders, uriInfo, authToken, userId, jsonBody);
        verify(userService).checkAndGetUserById(userId);
    }

    @Test
    public void addUserCredential_passwordCredentials_callsUserServiceUpdateUser() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        user.setUsername("test_user");
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)).thenReturn(true);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        spy.addUserCredential(httpHeaders, uriInfo, authToken, userId, jsonBody);
        verify(userService).updateUser(user, false);
    }

    @Test
    public void addUserCredential_passwordCredentialsUserCredentialNotMatchUserName_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setUsername("username");
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        MediaType mediaType = mock(MediaType.class);
        user.setUsername("wrong_user");
        JAXBElement<PasswordCredentialsRequiredUsername> jaxbElement = new JAXBElement<PasswordCredentialsRequiredUsername>(QName.valueOf("credentials"),PasswordCredentialsRequiredUsername.class,passwordCredentialsRequiredUsername);
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(jaxbElement).when(spy).getJSONCredentials(jsonBody);
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)).thenReturn(true);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.addUserCredential(httpHeaders, uriInfo, authToken, userId, jsonBody), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(BadRequestException.class));
    }

    @Test
    public void addUserCredential_passwordCredentialOkResponseCreated_returns200() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        user.setUsername("test_user");
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_JSON_TYPE)).thenReturn(true);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(new URI("uri"));
        Response.ResponseBuilder responseBuilder = spy.addUserCredential(httpHeaders, uriInfo, authToken, userId, jsonBody);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addUserCredential_apiKeyCredentials_callsValidateApiKeyCredentials() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        ApiKeyCredentials apiKeyCredentials1 = new ApiKeyCredentials();
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE)).thenReturn(true);
        doReturn(new JAXBElement<ApiKeyCredentials>(QName.valueOf("foo"),ApiKeyCredentials.class,apiKeyCredentials1)).when(spy).getXMLCredentials(jsonBody);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        spy.addUserCredential(httpHeaders, uriInfo, authToken, userId, jsonBody);
        verify(validator20).validateApiKeyCredentials(apiKeyCredentials1);
    }

    @Test
    public void addUserCredential_apiKeyCredentials_callsCheckAndGetUserById() throws Exception {
        MediaType mediaType = mock(MediaType.class);
        ApiKeyCredentials apiKeyCredentials1 = new ApiKeyCredentials();
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE)).thenReturn(true);
        doReturn(new JAXBElement<ApiKeyCredentials>(QName.valueOf("foo"),ApiKeyCredentials.class,apiKeyCredentials1)).when(spy).getXMLCredentials(jsonBody);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        spy.addUserCredential(httpHeaders, uriInfo, authToken, userId, jsonBody);
        verify(userService).checkAndGetUserById(userId);
    }

    @Test
    public void addUserCredential_apiKeyCredentialsUserCredentialNotMatchUserName_returnsBodyBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        ApiKeyCredentials apiCredentials = new ApiKeyCredentials();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        apiCredentials.setUsername("user");
        String jsonBody = "body";
        MediaType mediaType = mock(MediaType.class);
        user.setUsername("wrong_user");
        JAXBElement<ApiKeyCredentials> jaxbElement = new JAXBElement<ApiKeyCredentials>(QName.valueOf("credentials"),ApiKeyCredentials.class,apiCredentials);
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(jaxbElement).when(spy).getXMLCredentials(jsonBody);
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE)).thenReturn(true);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.addUserCredential(httpHeaders, uriInfo, authToken, userId, jsonBody), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(BadRequestException.class));
    }

    @Test
    public void addUserCredential_apiKeyCredentialsOkResponse_returns200() throws Exception {
        String jsonBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<apiKeyCredentials\n" +
                "    xmlns=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\"\n" +
                "    username=\"id\"\n" +
                "    apiKey=\"aaaaa-bbbbb-ccccc-12345678\"/>";
        MediaType mediaType = mock(MediaType.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(new URI("uri"));
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE)).thenReturn(true);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.addUserCredential(httpHeaders, uriInfo, authToken, userId, jsonBody);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addUserCredentialEmptyXMLNamespace_apiKeyCredentialsOkResponse_returns200() throws Exception {
        String jsonBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<apiKeyCredentials\n" +
                "    username=\"id\"\n" +
                "    apiKey=\"aaaaa-bbbbb-ccccc-12345678\"/>";
        MediaType mediaType = mock(MediaType.class);
        UriBuilder uriBuilder = mock(UriBuilder.class);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(new URI("uri"));
        when(httpHeaders.getMediaType()).thenReturn(mediaType);
        when(mediaType.isCompatible(MediaType.APPLICATION_XML_TYPE)).thenReturn(true);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.addUserCredential(httpHeaders, uriInfo, authToken, userId, jsonBody);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addUserCredential_notApiKeyCredentialsAndNotPasswordCredentials_returns200() throws Exception {
        JAXBElement<Ec2CredentialsType> credentials = new JAXBElement<Ec2CredentialsType>(new QName("ec2"), Ec2CredentialsType.class, new Ec2CredentialsType());
        UriBuilder uriBuilder = mock(UriBuilder.class);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(new URI("uri"));
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_XML_TYPE);
        doReturn(credentials).when(spy).getXMLCredentials("body");
        Response.ResponseBuilder responseBuilder = spy.addUserCredential(httpHeaders, uriInfo, authToken, "userId", "body");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void addUserRole_callsVerifyUserAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.addUserRole(null, authToken, authToken, null);
        verify(authorizationService).verifyUserAdminLevelAccess(scopeAccess);
    }

    @Test
    public void checkToken_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.checkToken(null, authToken, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void checkToken_tenantIdNotBlank_callsIsTenantIdContainedInTenantRoles() throws Exception {
        UserScopeAccess scopeAccess = new UserScopeAccess();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        doReturn(scopeAccess).when(spy).checkAndGetToken("tokenId");
        when(tenantService.getTenantRolesForScopeAccess(scopeAccess)).thenReturn(roles);
        spy.checkToken(null, authToken, "tokenId", "tenantId");
        verify(tenantService).isTenantIdContainedInTenantRoles("tenantId",roles);
    }

    @Test
    public void checkToken_tenantIdNotBlankThrowsNotFoundException_returns404() throws Exception {
        List<TenantRole> roles = new ArrayList<TenantRole>();
        doReturn(userScopeAccess).when(spy).checkAndGetToken("tokenId");
        when(tenantService.getTenantRolesForScopeAccess(userScopeAccess)).thenReturn(roles);
        when(tenantService.isTenantIdContainedInTenantRoles("belongsTo", roles)).thenReturn(false);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, "tokenId", "belongsTo");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void checkToken_TenantIdInRoles_returns200() throws Exception {
        List<TenantRole> roles = new ArrayList<TenantRole>();
        doReturn(userScopeAccess).when(spy).checkAndGetToken("tokenId");
        when(tenantService.getTenantRolesForScopeAccess(userScopeAccess)).thenReturn(roles);
        when(tenantService.isTenantIdContainedInTenantRoles("tenantId",roles)).thenReturn(true);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, "tokenId", "tenantId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void checkToken_responseOk_returns200() throws Exception {
        doReturn(null).when(spy).checkAndGetToken("tokenId");
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, "tokenId", "");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void checkToken_throwsBadRequestException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(new BadRequestException("message")).when(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
        when(exceptionHandler.badRequestExceptionResponse("message")).thenReturn(responseBuilder);
        assertThat("response builder", spy.checkToken(null, authToken, null, null), equalTo(responseBuilder));
    }

    @Test
    public void checkToken_throwsNotAuthorizedException_returns401() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(new NotAuthorizedException()).when(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(401));
    }

    @Test
    public void checkToken_throwsForbiddenException_returns403() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(new ForbiddenException()).when(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(403));
    }

    @Test
    public void checkToken_throwsNotFoundException_returns404() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(new NotFoundException()).when(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(404));
    }

    @Test
    public void checkToken_throwsException_returns500() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(new NullPointerException()).when(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
        Response.ResponseBuilder responseBuilder = spy.checkToken(null, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(500));
    }

    @Test
    public void deleteEndpoint_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.deleteEndpoint(null, authToken, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void deleteEndpoint_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(new NotFoundException()).when(tenantService).checkAndGetTenant(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response bulider", spy.deleteEndpoint(null, authToken, authToken, null), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(NotFoundException.class));
    }

    @Test
    public void deleteEndpointTemplate_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.deleteEndpointTemplate(null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void deleteEndpointTemplate_throwsException_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(new NotFoundException()).when(endpointService).checkAndGetEndpointTemplate(null);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.deleteEndpointTemplate(null, authToken, null), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(NotFoundException.class));
    }

    @Test
    public void deleteRoleFromUserOnTenant_callsVerifyTokenHasTenantAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.deleteRoleFromUserOnTenant(null, authToken, tenantId, null, null);
        verify(authorizationService).verifyTokenHasTenantAccess(tenantId, scopeAccess);
    }

    @Test
    public void deleteRoleFromUserOnTenant_callsVerifyUserAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.deleteRoleFromUserOnTenant(null, authToken, tenantId, null, null);
        verify(authorizationService).verifyUserAdminLevelAccess(scopeAccess);
    }

    @Test
    public void deleteRoleFromUserOnTenant_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        NotFoundException notFoundException = new NotFoundException();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(notFoundException).when(userService).checkAndGetUserById(userId);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.deleteRoleFromUserOnTenant(null, authToken, tenantId, userId, null), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(NotFoundException.class));
    }

    @Test
    public void deleteService_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.deleteService(null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void deleteService_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(notFoundException).when(clientService).checkAndGetApplication(null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.deleteService(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void deleteTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.deleteTenant(null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void deleteTenant_callsCheckAndGetTenant() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.deleteTenant(null, authToken, tenantId);
        verify(tenantService).checkAndGetTenant(tenantId);
    }

    @Test
    public void deleteUser_callsVerifyUserAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.deleteUser(null, authToken, null);
        verify(authorizationService).verifyUserAdminLevelAccess(scopeAccess);
    }

    @Test
    public void deleteUser_callsCheckAndGetUserById() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.deleteUser(null, authToken, userId);
        verify(userService).checkAndGetUserById(userId);
    }

    @Test
    public void deleteUser_userAdmin_differentDomain_throwsForbiddenException() throws Exception {
        ForbiddenException forbiddenException = new ForbiddenException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.checkAndGetUserById("dude")).thenReturn(user);
        when(userService.getUserById("dude")).thenReturn(new User());
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        doThrow(forbiddenException).when(authorizationService).verifyDomain(user, user);
        when(exceptionHandler.exceptionResponse(forbiddenException)).thenReturn(responseBuilder);
        assertThat("response code", spy.deleteUser(null, authToken, "dude"), equalTo(responseBuilder));
    }

    @Test
    public void deleteUserCredential_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.deleteUserCredential(null, authToken, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void deleteUserCredential_passwordCredentials_returns501() throws Exception {
        String credentialType = "passwordCredentials";
        Response.ResponseBuilder responseBuilder = spy.deleteUserCredential(null, authToken, null, credentialType);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(501));
    }

    @Test
    public void deleteUserCredential_notPasswordCredentialAndNotAPIKEYCredential_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        String credentialType = "";
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder",spy.deleteUserCredential(null, authToken, null, credentialType) , equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(BadRequestException.class));
    }

    @Test
    public void deleteUserCredential_APIKEYCredential_callsCheckAndGetUser() throws Exception {
        String credentialType = "RAX-KSKEY:apiKeyCredentials";
        spy.deleteUserCredential(null, authToken, "userId", credentialType);
        verify(userService).checkAndGetUserById("userId");
    }

    @Test
    public void deleteUserCredential_APIKeyCredential_callsUserServiceUpdateUser() throws Exception {
        String credentialType = "RAX-KSKEY:apiKeyCredentials";
        User user = new User();
        user.setApiKey("123");
        when(userService.checkAndGetUserById("userId")).thenReturn(user);
        spy.deleteUserCredential(null, authToken, "userId", credentialType);
        verify(userService).updateUser(any(User.class), eq(false));
    }

    @Test
    public void deleteUserCredential_APIKeyCredentialResponseNoContent_returns204() throws Exception {
        String credentialType = "RAX-KSKEY:apiKeyCredentials";
        User user = new User();
        user.setApiKey("123");
        when(userService.checkAndGetUserById("userId")).thenReturn(user);
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
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.checkAndGetUserById("userId")).thenReturn(user);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.deleteUserCredential(null, authToken, "userId", credentialType), equalTo(responseBuilder));
        assertThat("message", (argumentCaptor.getValue().getMessage()),
                equalTo("Credential type RAX-KSKEY:apiKeyCredentials was not found for User with Id: 123"));
    }

    @Test
    public void deleteUserRole_callsVerifyUserAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.deleteUserRole(null, authToken, null, null);
        verify(authorizationService).verifyUserAdminLevelAccess(scopeAccess);
    }

    @Test
    public void deleteUserRole_callsCheckAndGetUserById() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.deleteUserRole(null, authToken, userId, null);
        verify(userService).checkAndGetUserById(userId);
    }

    @Test
    public void deleteUserRole_callsTenantServiceGetGlobalRolesForUser() throws Exception {
        spy.deleteUserRole(null, authToken, userId, null);
        verify(tenantService).getGlobalRolesForUser(any(User.class));
    }

    @Test
    public void deleteUserRole_roleIsNull_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.deleteUserRole(null, authToken, userId, null), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(NotFoundException.class));
    }

    @Test
    public void getEndpoint_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.getEndpoint(null, authToken, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void getEndpoint_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(tenantService.checkAndGetTenant(tenantId)).thenReturn(tenant);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.getEndpoint(null, authToken, tenantId, null), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(),instanceOf(NotFoundException.class));
    }

    @Test
    public void getEndpoint_callsCheckAndGetTenant() throws Exception {
        tenant.addBaseUrlId("endpointId");
        when(tenantService.checkAndGetTenant(tenantId)).thenReturn(tenant);
        spy.getEndpoint(null, authToken, tenantId, "endpointId");
        verify(tenantService).checkAndGetTenant(tenantId);
    }

    @Test
    public void getEndpoint_callsCheckAndGetEndPointTemplate() throws Exception {
        tenant.addBaseUrlId("endpointId");
        when(tenantService.checkAndGetTenant(tenantId)).thenReturn(tenant);
        spy.getEndpoint(null, authToken, tenantId, "endpointId");
        verify(endpointService).checkAndGetEndpointTemplate("endpointId");
    }

    @Test
    public void getEndpoint_responseOk_returns200() throws Exception {
        tenant.addBaseUrlId("endpointId");
        when(tenantService.checkAndGetTenant(tenantId)).thenReturn(tenant);
        when(endpointService.checkAndGetEndpointTemplate("endpointId")).thenReturn(cloudBaseUrl);
        Response.ResponseBuilder responseBuilder = spy.getEndpoint(httpHeaders, authToken, tenantId, "endpointId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getEndpointTemplate_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.getEndpointTemplate(null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void getEndpointTemplate_callsCheckAndGetEndpointTemplate() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.getEndpointTemplate(null, authToken, "endpointTemplateId");
        verify(endpointService).checkAndGetEndpointTemplate("endpointTemplateId");
    }

    @Test
    public void getEndpointTemplate_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        NotFoundException notFoundException = new NotFoundException();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(notFoundException).when(endpointService).checkAndGetEndpointTemplate(null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.getEndpointTemplate(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void getEndpointTemplate_responseOk_returns200() throws Exception {
        when(endpointService.checkAndGetEndpointTemplate("endpointTemplateId")).thenReturn(cloudBaseUrl);
        when(endpointConverterCloudV20.toEndpointTemplate(cloudBaseUrl)).thenReturn(endpointTemplate);
        when(jaxbObjectFactories.getOpenStackIdentityExtKscatalogV1Factory()).thenReturn(new org.openstack.docs.identity.api.ext.os_kscatalog.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.getEndpointTemplate(httpHeaders, authToken, "endpointTemplateId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getRole_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.getRole(null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void getRole_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
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
        doReturn(null).when(spy).getScopeAccessForValidToken("authToken");
        when(userService.checkAndGetUserById("userId")).thenReturn(user);
        com.rackspace.docs.identity.api.ext.rax_ksqa.v1.ObjectFactory objectFactory = mock(com.rackspace.docs.identity.api.ext.rax_ksqa.v1.ObjectFactory.class);
        when(jaxbObjectFactories.getRackspaceIdentityExtKsqaV1Factory()).thenReturn(objectFactory);
        when(objectFactory.createSecretQA()).thenReturn(secretQA2);
        when(objectFactory.createSecretQA(secretQA2)).thenReturn(new JAXBElement<SecretQA>(QName.valueOf("foo"),SecretQA.class,secretQA2));
        Response result = spy.getSecretQA(null, "authToken", "userId").build();
        assertThat("username", ( (SecretQA) result.getEntity()).getUsername(), equalTo(null));
    }

    @Test
    public void getSecretQA_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.getSecretQA(null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void getSecretQA_callsCheckAndGetUserById() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.getSecretQA(null, authToken, userId);
        verify(userService).checkAndGetUserById(userId);
    }

    @Test
    public void getSecretQA_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(notFoundException).when(userService).checkAndGetUserById(userId);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.getSecretQA(null, authToken, userId), equalTo(responseBuilder));
    }

    @Test
    public void getSecretQA_responseOk_returns200() throws Exception {
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(jaxbObjectFactories.getRackspaceIdentityExtKsqaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_ksqa.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.getSecretQA(httpHeaders, authToken, userId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getService_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.getService(null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void getService_callsCheckAndGetApplication() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.getService(null, authToken, "clientId");
        verify(clientService).checkAndGetApplication("clientId");
    }

    @Test
    public void getService_throwsNotFoundException_returnResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(notFoundException).when(clientService).checkAndGetApplication(null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.getService(null, authToken, null), equalTo(responseBuilder));
    }


    @Test
    public void getService_responseOk_returns200() throws Exception {
        when(clientService.checkAndGetApplication("serviceId")).thenReturn(application);
        when(jaxbObjectFactories.getOpenStackIdentityExtKsadmnV1Factory()).thenReturn(new org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory());
        when(serviceConverterCloudV20.toService(any(Application.class))).thenReturn(new Service());
        Response.ResponseBuilder responseBuilder = spy.getService(httpHeaders, authToken, "serviceId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getTenantById_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.getTenantById(null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void getTenantById_callsCheckAndGetTenant() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.getTenantById(null, authToken, tenantId);
        verify(tenantService).checkAndGetTenant(tenantId);
    }

    @Test
    public void getTenantById_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(notFoundException).when(tenantService).checkAndGetTenant(null);
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
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.getTenantByName(null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void getTenantByName_tenantIsNullThrowsNotFoundException_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.getTenantByName(null, authToken, null), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(NotFoundException.class));
    }

    @Test
    public void getTenantByName_responseOk_returns200() throws Exception {
        when(tenantService.getTenantByName(tenantId)).thenReturn(tenant);
        Response.ResponseBuilder responseBuilder = spy.getTenantByName(httpHeaders, authToken, tenantId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserById_callerDoesNotHaveDefaultUserRole_callsVerifyUserAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.getUserById(null, authToken, null);
        verify(authorizationService).verifyUserAdminLevelAccess(scopeAccess);
    }

    @Test
    public void getUserById_callerDoesNotHaveDefaultUserRole_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(new User()).when(spy).getUser(scopeAccess);
        when(authorizationService.authorizeCloudUser(scopeAccess)).thenReturn(false);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserById(null, authToken, null), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(NotFoundException.class));
    }

    @Test
    public void getUserById_callerHasUserAdminRole_callsVerifyDomain() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        User caller= new User();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(caller).when(spy).getUser(scopeAccess);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(true);
        when(this.userService.getUserById(userId)).thenReturn(user);
        spy.getUserById(null, authToken, userId);
        verify(authorizationService).verifyDomain(caller, user);
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
        ArgumentCaptor<ForbiddenException> argumentCaptor = ArgumentCaptor.forClass(ForbiddenException.class);
        ScopeAccess scopeAccess = new ScopeAccess();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(authorizationService.authorizeCloudUser(scopeAccess)).thenReturn(true);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserById(httpHeaders, authToken, null), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(ForbiddenException.class));
    }

    @Test
    public void getUserById_responseOk_returns200() throws Exception {
        doReturn(new User()).when(spy).getUser(any(ScopeAccess.class));
        Response.ResponseBuilder responseBuilder = spy.getUserById(httpHeaders, authToken, userId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserByName_callsVerifyUserLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.getUserByName(null, authToken, null);
        verify(authorizationService).verifyUserLevelAccess(scopeAccess);
    }

    @Test
    public void getUserByName_userIsNullThrowsNotFoundException_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserByName(null, authToken, null), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(NotFoundException.class));
    }

    @Test
    public void getUserByName_adminUser_callsVerifyDomain() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        User adminUser = new User();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.getUser("userName")).thenReturn(user);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(true);
        when(userService.getUserByScopeAccess(scopeAccess)).thenReturn(adminUser);
        spy.getUserByName(null, authToken, "userName");
        verify(authorizationService).verifyDomain(adminUser, user);
    }

    @Test
    public void getUserByName_defaultUser_callsVerifySelf() throws Exception {
        User requester = new User();
        ScopeAccess scopeAccess = new ScopeAccess();

        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.getUser("userName")).thenReturn(user);
        when(authorizationService.authorizeCloudUserAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudUser(scopeAccess)).thenReturn(true);
        when(userService.getUserByScopeAccess(scopeAccess)).thenReturn(requester);

        spy.getUserByName(null, authToken, "userName");
        verify(authorizationService).verifySelf(requester, user);
    }

    @Test
    public void getUserCredential_callsVerifyUserLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.getUserCredential(null, authToken, null, null);
        verify(authorizationService).verifyUserLevelAccess(scopeAccess);
    }

    @Test
    public void getUserCredential_notPasswordCredentialOrAPIKeyCredentialThrowsBadRequest_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserCredential(null, authToken, null, ""), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(BadRequestException.class));
    }

    @Test
    public void getUserCredential_cloudUser_callsGetUser() throws Exception {
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        spy.getUserCredential(null, authToken, userId, apiKeyCredentials);
        verify(spy).getUser(any(ScopeAccess.class));
    }

    @Test
    public void getUserCredential_cloudUserIdNotMatchThrowsForbiddenException_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<ForbiddenException> argumentCaptor = ArgumentCaptor.forClass(ForbiddenException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserCredential(null, authToken, "", apiKeyCredentials), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(),instanceOf(ForbiddenException.class));
    }

    @Test
    public void getUserCredential_userIsNull_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.getUserById("id")).thenReturn(null);
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserCredential(null, authToken, "id", apiKeyCredentials), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(NotFoundException.class));
    }

    @Test
    public void getUserCredential_cloudAdminUser_callsGetUser() throws Exception {
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        spy.getUserCredential(null, authToken, userId, passwordCredentials);
        verify(spy).getUser(any(ScopeAccess.class));
    }

    @Test
    public void getUserCredential_cloudAdminUserIdNotMatchThrowsForbiddenException_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<ForbiddenException> argumentCaptor = ArgumentCaptor.forClass(ForbiddenException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response code",spy.getUserCredential(null, authToken, "", passwordCredentials), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(),instanceOf(ForbiddenException.class));
    }

    @Test
    public void getUserCredential_userIsNullThrowsNotFoundException_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserCredential(null, authToken, "", apiKeyCredentials), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(NotFoundException.class));
    }

    @Test
    public void getUserCredential_passwordCredentialUserPasswordIsBlank_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        when(userService.getUserById(userId)).thenReturn(new User());
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserCredential(null, authToken, userId, passwordCredentials), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(NotFoundException.class));
    }

    @Test
    public void getUserCredential_passwordCredentialResponseOk_returns200() throws Exception {
        user.setPassword("123");
        when(userService.getUserById(userId)).thenReturn(user);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        Response.ResponseBuilder responseBuilder = spy.getUserCredential(httpHeaders, authToken, userId, passwordCredentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserCredential_apiKeyCredentialResponseOk_returns200() throws Exception {
        user.setApiKey("123");
        when(userService.getUserById(userId)).thenReturn(user);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        when(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.getUserCredential(httpHeaders, authToken, userId, apiKeyCredentials);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUserCredential_apiKeyCredentialAPIKeyIsBlank_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        when(userService.getUserById(userId)).thenReturn(user);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUserCredential(httpHeaders, authToken, userId, apiKeyCredentials), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(NotFoundException.class));
    }

    @Test
    public void getUserRole_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.getUserRole(null, authToken, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void getUserRole_callsCheckAndGetUserById() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.getUserRole(null, authToken, userId, null);
        verify(userService).checkAndGetUserById(userId);
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
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.checkAndGetUserById("userId")).thenReturn(user);
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
    public void listCredentials_callsVerifyUserLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listCredentials(null, authToken, null, null, 0);
        verify(authorizationService).verifyUserLevelAccess(scopeAccess);
    }

    @Test
    public void listCredentials_callsCheckAndGetUserById() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listCredentials(null, authToken, userId, null, 0);
        verify(userService).checkAndGetUserById(userId);
    }

    @Test
    public void listCredentials_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(notFoundException).when(userService).checkAndGetUserById(userId);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.listCredentials(null, authToken, userId, null, 0), equalTo(responseBuilder));
    }


    @Test
    public void listCredentials_userPasswordIsNotBlankResponseOk_returns200() throws Exception {
        user.setPassword("123");
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.listCredentials(httpHeaders, authToken, userId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listCredential_userApiKeyIsNotBlankResponseOk_returns200() throws Exception {
        user.setApiKey("123");
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.listCredentials(httpHeaders, authToken, userId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listCredentials_userPasswordIsBlankAndApiKeyIsBlankResponseOk_returns200() throws Exception {
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.listCredentials(httpHeaders, authToken, userId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listCredentials_userPasswordIsNotBlankAndApiKeyIsNotBlank_returns200() throws Exception {
        user.setApiKey("123");
        user.setPassword("123");
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.listCredentials(httpHeaders, authToken, userId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listEndpoints_verifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listEndpoints(null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void listEndpoints_callsCheckAndGetTenant() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.listEndpoints(null, authToken, tenantId);
        verify(tenantService).checkAndGetTenant(tenantId);
    }

    @Test
    public void listEndpoints_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(notFoundException).when(tenantService).checkAndGetTenant(null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.listEndpoints(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void listEndpoints_baseUrlIdsIsNotNullResponseOk_returns200() throws Exception {
        when(tenantService.checkAndGetTenant(tenantId)).thenReturn(tenant);
        Response.ResponseBuilder responseBuilder = spy.listEndpoints(httpHeaders, authToken, tenantId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listEndpoints_baseUrlIdsIsNotNullCallsEndpointService_getBaseUrlById() throws Exception {
        String[] ids = {"1"};
        tenant.setBaseUrlIds(ids);
        ArrayList<CloudBaseUrl> cloudBaseUrlList = new ArrayList<CloudBaseUrl>();
        cloudBaseUrlList.add(cloudBaseUrl);
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(tenantService.checkAndGetTenant(tenantId)).thenReturn(tenant);
        when(endpointService.getGlobalBaseUrls()).thenReturn(cloudBaseUrlList);
        when(endpointService.getBaseUrlById(1)).thenReturn(cloudBaseUrl);
        spy.listEndpoints(httpHeaders, authToken, tenantId);
        verify(endpointService).getBaseUrlById(1);
    }

    @Test
    public void listEndpoints_baseUrlIdsIsNullResponseOk_returns200() throws Exception {
        tenant.setBaseUrlIds(null);
        when(tenantService.checkAndGetTenant(tenantId)).thenReturn(tenant);
        Response.ResponseBuilder responseBuilder = spy.listEndpoints(httpHeaders, authToken, tenantId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listEndpointTemplates_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listEndpointTemplates(null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void listEndpointTemplates_callsCheckAndGetApplication() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.listEndpointTemplates(null, authToken, "clientId");
        verify(clientService).checkAndGetApplication("clientId");
    }

    @Test
    public void listEndpointTemplates_throwsNotAuthorizedException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        NotAuthorizedException notAuthorizedException = new NotAuthorizedException();
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(notAuthorizedException).when(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
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
        when(clientService.checkAndGetApplication("serviceId")).thenReturn(application);
        when(endpointService.getBaseUrlsByServiceType(anyString())).thenReturn(cloudBaseUrlList);
        Response.ResponseBuilder responseBuilder = spy.listEndpointTemplates(httpHeaders, authToken, "serviceId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listRolesForTenant_verifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listRolesForTenant(null, authToken, null, null, 0);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void listRolesForTenant_callsCheckAndGetTenant() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.listRolesForTenant(null, authToken, tenantId, null, 0);
        verify(tenantService).checkAndGetTenant(tenantId);
    }

    @Test
    public void listRolesForTenant_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        NotFoundException notFoundException = new NotFoundException();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(notFoundException).when(tenantService).checkAndGetTenant(null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.listRolesForTenant(null, authToken, null, null, 0), equalTo(responseBuilder));
    }

    @Test
    public void listRolesForTenant_responseOk_returns200() throws Exception {
        when(tenantService.checkAndGetTenant(tenantId)).thenReturn(tenant);
        Response.ResponseBuilder responseBuilder = spy.listRolesForTenant(httpHeaders, authToken, tenantId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listRolesForUserOnTenant_verifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listRolesForUserOnTenant(null, authToken, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void listRolesForUserOnTenant_callsCheckAndGetTenant() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.listRolesForUserOnTenant(null, authToken, tenantId, null);
        verify(tenantService).checkAndGetTenant(tenantId);
    }

    @Test
    public void listRolesForUserOnTenant_callsCheckAndGetUserById() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.listRolesForUserOnTenant(null, authToken, tenantId, userId);
        verify(userService).checkAndGetUserById(userId);
    }

    @Test
    public void listRolesForUserOnTenant_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(notFoundException).when(tenantService).checkAndGetTenant(null);
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
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listServices(null, authToken, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void listServices_throwsForbiddenException_returnsResponseBuilder() throws Exception {
        ForbiddenException forbiddenException = new ForbiddenException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(forbiddenException).when(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
        when(exceptionHandler.exceptionResponse(forbiddenException)).thenReturn(responseBuilder);
        assertThat("response builder",spy.listServices(null, authToken, null, null),equalTo(responseBuilder));
    }

    @Test
    public void listServices_responseOk_returns200() throws Exception {
        when(jaxbObjectFactories.getOpenStackIdentityExtKsadmnV1Factory()).thenReturn(new org.openstack.docs.identity.api.ext.os_ksadm.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.listServices(httpHeaders, authToken, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUserGlobalRoles_isUserAdmin_callsVerifyDomain() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        User caller = new User();

        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(caller).when(spy).getUser(scopeAccess);
        when(userService.getUserById(userId)).thenReturn(user);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudUser(scopeAccess)).thenReturn(false);

        spy.listUserGlobalRoles(null, authToken, userId);
        verify(authorizationService).verifyDomain(caller, user);
    }

    @Test
    public void listUserGlobalRoles_isDefaultUser_callsVerifySelf() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        User requester = new User();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(user).when(spy).getUser(scopeAccess);
        when(userService.getUserById(userId)).thenReturn(user);
        doReturn(requester).when(spy).getUser(scopeAccess);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudUser(scopeAccess)).thenReturn(true);
        spy.listUserGlobalRoles(null, authToken, userId);
        verify(authorizationService).verifySelf(requester, user);
    }

    @Test
    public void listUserGlobalRoles_callsVerifyUserLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listUserGlobalRoles(null, authToken, null);
        verify(authorizationService).verifyUserLevelAccess(scopeAccess);
    }

    @Test
    public void listUserGlobalRoles_throwsForbiddenException_returnsResponseBody() throws Exception {
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(userId);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.listUserGlobalRoles(null, authToken, null), equalTo(responseBuilder));
        assertThat("correct exception",argumentCaptor.getValue(),instanceOf(NotFoundException.class));
    }

    @Test
    public void listUserGlobalRoles_notCloudServiceAdminAndNotCloudServiceAdminResponseOk_returns200() throws Exception {
        doReturn(user).when(spy).getUser(any(ScopeAccess.class));
        when(authorizationService.authorizeCloudServiceAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(authorizationService.authorizeCloudIdentityAdmin(any(ScopeAccess.class))).thenReturn(true);
        Response.ResponseBuilder responseBuilder = spy.listUserGlobalRoles(httpHeaders, authToken, userId);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUserGlobalRolesByServiceId_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listUserGlobalRolesByServiceId(null, authToken, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void listUserGlobalRolesByServiceId_callsCheckAndGetUserById() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.listUserGlobalRolesByServiceId(null, authToken, userId, null);
        verify(userService).checkAndGetUserById(userId);
    }

    @Test
    public void listUserGlobalRolesByServiceId_throwsForbiddenException_returnsResponseBuilder() throws Exception {
        ForbiddenException forbiddenException = new ForbiddenException();
        Response.ResponseBuilder  responseBuilder = new ResponseBuilderImpl();
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(forbiddenException).when(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
        when(exceptionHandler.exceptionResponse(forbiddenException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.listUserGlobalRolesByServiceId(null, authToken, null, null), equalTo(responseBuilder));
    }


    @Test
    public void listUserGlobalRolesByServiceId_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listUserGlobalRolesByServiceId(httpHeaders, authToken, userId, "serviceId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listGroups_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listGroups(null, authToken, null, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
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
    public void listUsersForTenant_CallsVerifyUserAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listUsersForTenant(null, authToken, null, null, 0);
        verify(authorizationService).verifyUserAdminLevelAccess(scopeAccess);
    }

    @Test
    public void listUsersForTenant_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(notFoundException).when(authorizationService).verifyTokenHasTenantAccess(tenantId, null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.listUsersForTenant(null, authToken, tenantId, null, 0), equalTo(responseBuilder));
    }

    @Test
    public void listUsersForTenant_callsVerifyTokenHasTenantAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listUsersForTenant(null, authToken, tenantId, null, null);
        verify(authorizationService).verifyTokenHasTenantAccess(tenantId,scopeAccess);
    }

    @Test
    public void listUsersForTenant_callsCheckAndGetTenant() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listUsersForTenant(null, authToken, tenantId, null, null);
        verify(tenantService).checkAndGetTenant(tenantId);
    }

    @Test
    public void listUsersForTenant_responseOk_returns200() throws Exception {
        when(tenantService.checkAndGetTenant(tenantId)).thenReturn(tenant);
        Response.ResponseBuilder responseBuilder = spy.listUsersForTenant(httpHeaders, authToken, tenantId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUsersWithRoleForTenant_callsVerifyUserAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listUsersWithRoleForTenant(null, authToken, null, null, null, 0);
        verify(authorizationService).verifyUserAdminLevelAccess(scopeAccess);
    }

    @Test
    public void listUsersWithRoleForTenant_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(notFoundException).when(authorizationService).verifyTokenHasTenantAccess(tenantId,null);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.listUsersWithRoleForTenant(null, authToken, tenantId, null, null, 0), equalTo(responseBuilder));
    }

    @Test
    public void listUsersWithRoleForTenant_callsVerifyTokenHasTenantAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listUsersWithRoleForTenant(null, authToken, tenantId, null, null, null);
        verify(authorizationService).verifyTokenHasTenantAccess(tenantId,scopeAccess);
    }

    @Test
    public void listUsersWithRoleForTenant_callsCheckAndGetTenant() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.listUsersWithRoleForTenant(null, authToken, tenantId, null, null, null);
        verify(tenantService).checkAndGetTenant(tenantId);
    }

    @Test
    public void listUsersWithRoleForTenant_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.listUsersWithRoleForTenant(httpHeaders, authToken, tenantId, roleId, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void setUserEnabled_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.setUserEnabled(null, authToken, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void setUserEnabled_callsCheckAndGetUserById() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.setUserEnabled(null, authToken, userId, null);
        verify(userService).checkAndGetUserById(userId);
    }

    @Test
    public void setUserEnabled_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        NotFoundException notFoundException = new NotFoundException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(notFoundException).when(userService).checkAndGetUserById(userId);
        when(exceptionHandler.exceptionResponse(notFoundException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.setUserEnabled(null, authToken, userId, null), equalTo(responseBuilder));
    }

    @Test
    public void setUserEnabled_userService_callsUpdateUser() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setEnabled(true);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        spy.setUserEnabled(null, authToken, userId, user1);
        verify(userService).updateUser(any(User.class), eq(false));
    }

    @Test
    public void setUserEnabled_responseOk_returns200() throws Exception {
        org.openstack.docs.identity.api.v2.User user1 = new org.openstack.docs.identity.api.v2.User();
        user1.setEnabled(true);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.setUserEnabled(null, authToken, userId, user1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void updateSecretQA_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.updateSecretQA(null, authToken, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void updateSecretQA_callsCheckAndGetUserById() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.updateSecretQA(null, authToken, userId, secretQA);
        verify(userService).checkAndGetUserById(userId);
    }

    @Test
    public void updateSecretQA_secretAnswerIsBlankThrowsBadRequest_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        secretQA.setAnswer("");
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.updateSecretQA(null, authToken, null, secretQA), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(BadRequestException.class));
    }

    @Test
    public void updateSecretQA_secretQuestionIsBlankThrowsBadRequest_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        secretQA.setQuestion("");
        assertThat("response code", spy.updateSecretQA(null, authToken, null, secretQA), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(BadRequestException.class));
    }

    @Test
    public void updateSecretQA_userService_callsUpdateUser() throws Exception {
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        spy.updateSecretQA(null, authToken, userId, secretQA);
        verify(userService).updateUser(any(User.class), eq(false));
    }

    @Test
    public void updateSecretQA_responseOk_returns200() throws Exception {
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        Response.ResponseBuilder responseBuilder = spy.updateSecretQA(null, authToken, userId, secretQA);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void updateTenant_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.updateTenant(null, authToken, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void updateTenant_callsCheckAndGetTenant() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.updateTenant(null, authToken, tenantId, null);
        verify(tenantService).checkAndGetTenant(tenantId);
    }

    @Test
    public void updateTenant_throwsNotFoundException_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();

        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(new NotFoundException()).when(tenantService).checkAndGetTenant(null);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);

        assertThat("response builder", spy.updateTenant(null, authToken, null, null), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(NotFoundException.class));
    }

    @Test
    public void updateTenant_tenantService_callsUpdateTenant() throws Exception {
        org.openstack.docs.identity.api.v2.Tenant tenant1 = new org.openstack.docs.identity.api.v2.Tenant();
        tenant1.setEnabled(true);
        tenant1.setName("tenant");
        when(tenantService.checkAndGetTenant(tenantId)).thenReturn(tenant);
        spy.updateTenant(null, authToken, tenantId, tenant1);
        verify(tenantService).updateTenant(any(Tenant.class));
    }

    @Test
    public void updateTenant_responseOk_returns200() throws Exception {
        org.openstack.docs.identity.api.v2.Tenant tenant1 = new org.openstack.docs.identity.api.v2.Tenant();
        tenant1.setEnabled(true);
        tenant1.setName("tenant");
        when(tenantService.checkAndGetTenant(tenantId)).thenReturn(tenant);
        Response.ResponseBuilder responseBuilder = spy.updateTenant(null, authToken, tenantId, tenant1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void updateUser_callsVerifyUserLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.updateUser(null, authToken, null, null);
        verify(authorizationService).verifyUserLevelAccess(scopeAccess);
    }

    @Test
    public void updateUser_callsUserService_checkAndGetUserById() throws Exception {
        userOS.setId(userId);
        spy.updateUser(null, authToken, userId, userOS);
        verify(userService).checkAndGetUserById(userId);
    }

    @Test
    public void updateUser_callsUserService_updateUserById() throws Exception {
        userOS.setId(userId);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        spy.updateUser(null, authToken, userId, userOS);
        verify(userService).updateUserById(any(User.class), anyBoolean());
    }

    @Test
    public void updateUser_callsAuthorizeCloudUser() throws Exception {
        User user = new User();
        user.setId(userId);
        userOS.setId(userId);
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        spy.updateUser(null, authToken, userId, userOS);
        verify(authorizationService).authorizeCloudUser(scopeAccess);
    }

    @Test
    public void updateUser_callsGetScopeAccessByUserId() throws Exception {
        User user = new User();
        user.setId(userId);
        userOS.setId(userId);
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        spy.updateUser(null, authToken, userId, userOS);
        verify(scopeAccessService).getScopeAccessByUserId(userId);
    }

    @Test
    public void updateUser_userIsDefaultUser_callsDefaultRegionService() throws Exception {
        User user = new User();
        user.setId(userId);
        userOS.setId(userId);
        ScopeAccess scopeAccess = new ScopeAccess();
        UserScopeAccess scopeAccessForDefaultUser = new UserScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(scopeAccessService.getScopeAccessByUserId(userId)).thenReturn(scopeAccessForDefaultUser);
        when(authorizationService.authorizeCloudUser(scopeAccessForDefaultUser)).thenReturn(true);
        ScopeAccess value = new ScopeAccess();
        when(scopeAccessService.getScopeAccessByUserId(userId)).thenReturn(value);
        when(authorizationService.hasDefaultUserRole(user)).thenReturn(true);
        spy.updateUser(null, authToken, userId, userOS);
        verify(defaultRegionService).validateDefaultRegion(anyString(), any(ScopeAccess.class));
    }

    @Test
    public void updateUser_userIsUserAdminUser_callsDefaultRegionService() throws Exception {
        User user = new User();
        user.setId(userId);
        userOS.setId(userId);
        ScopeAccess scopeAccess = new ScopeAccess();
        UserScopeAccess scopeAccessForUserAdmin = new UserScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(scopeAccessService.getScopeAccessByUserId(userId)).thenReturn(scopeAccessForUserAdmin);
        when(authorizationService.hasUserAdminRole(user)).thenReturn(true);
        spy.updateUser(null, authToken, userId, userOS);
        verify(defaultRegionService).validateDefaultRegion(anyString(), any(ScopeAccess.class));
    }

    @Test
    public void updateUser_passwordNotNull_callsValidatePassword() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        userOS.setPassword("123");
        userOS.setId(userId);
        spy.updateUser(null, authToken, userId, userOS);
        verify(validator).validatePasswordForCreateOrUpdate("123");
    }

    @Test
    public void updateUser_usernameNotBlank_callsValidateUsernameForUpdateOrCreate() throws Exception {
        User user = new User();
        user.setId(userId);
        userOS.setId(userId);
        userOS.setUsername("username");
        ScopeAccess scopeAccess = new ScopeAccess();
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(authorizationService.authorizeCloudUser(scopeAccess)).thenReturn(false);
        spy.updateUser(null, authToken, userId, userOS);
        verify(validator).isUsernameValid("username");
    }


    @Test
    public void updateUser_authorizationServiceAuthorizeCloudUserIsTrue_callsUserServiceGetUserByAuthToken() throws Exception {
        User user = new User();
        user.setId(userId);
        userOS.setId(userId);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        spy.updateUser(null, authToken, userId, userOS);
        verify(userService).getUserByAuthToken(authToken);
    }

    @Test
    public void updateUser_authorizationServiceAuthorizeCloudUserIsTrueIdNotMatch_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<ForbiddenException> argumentCaptor = ArgumentCaptor.forClass(ForbiddenException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        User user1 = new User();
        user1.setId(userId);
        userOS.setId(userId);
        user.setId("notMatch");
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.checkAndGetUserById(userId)).thenReturn(user1);
        when(authorizationService.authorizeCloudUser(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.updateUser(null, authToken, userId, userOS), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(ForbiddenException.class));
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
        when(userService.checkAndGetUserById("123")).thenReturn(user1);
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
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        spy.updateUser(null, authToken, userId, userOS);
        verify(userService).getUserByAuthToken(authToken);
    }

    @Test
    public void updateUser_userIdUserAdmin_callsDefaultRegionService() throws Exception {
        User user = new User();
        user.setRegion("region");
        user.setId(userId);
        userOS.setId(userId);
        User user2 = new User();
        user2.setRegion("region2");
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(user2);
        ScopeAccess value = new ScopeAccess();
        when(scopeAccessService.getScopeAccessByUserId(userId)).thenReturn(value);
        when(authorizationService.hasUserAdminRole(user)).thenReturn(true);
        spy.updateUser(null, authToken, userId, userOS);
        verify(defaultRegionService).validateDefaultRegion(anyString(), any(ScopeAccess.class));
    }

    @Test
    public void updateUser_cloudUserAdminIsTrue_callsVerifyDomain() throws Exception {
        User user = new User();
        user.setId(userId);
        userOS.setId(userId);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(authorizationService.authorizeCloudUserAdmin(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);
        spy.updateUser(null, authToken, userId, userOS);
        verify(authorizationService).verifyDomain(user, user);
    }

    @Test
    public void updateUser_userDisabled_callsScopeAccessServiceExpiresAllTokensForUsers() throws Exception {
        userOS.setId(userId);
        User user = mock(User.class);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(user);
        doNothing().when(user).copyChanges(any(User.class));
        when(user.getId()).thenReturn(userId);
        when(user.isDisabled()).thenReturn(true);
        spy.updateUser(httpHeaders, authToken, userId, userOS);
        verify(scopeAccessService).expireAllTokensForUser(user.getUsername());
    }

    @Test
    public void updateUser_regionDefined_callsValidateDefaultRegion() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();

        User user = new User();
        user.setId(userId);
        user.setRegion("region");
        userOS.setId(userId);

        User user2 = new User();
        user2.setRegion("region2");

        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);

        when(userConverterCloudV20.toUserDO(any(org.openstack.docs.identity.api.v2.User.class))).thenReturn(user2);

        doReturn(true).when(authorizationService).authorizeCloudUser(any(ScopeAccess.class));
        doReturn(false).when(authorizationService).authorizeCloudUserAdmin(any(ScopeAccess.class));
        when(userService.getUserByAuthToken(authToken)).thenReturn(user);

        spy.updateUser(null, authToken, userId, userOS);
        verify(defaultRegionService).validateDefaultRegion(anyString(), any(ScopeAccess.class));
    }

    @Test
    public void updateUserApiKeyCredentials_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.updateUserApiKeyCredentials(null, authToken, null, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void updateUserApiKeyCredentials_apiKeyIsBlankThrowsBadRequest_returnsResponseBuilder() throws Exception {
        ApiKeyCredentials creds = new ApiKeyCredentials();
        creds.setApiKey("");
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.updateUserApiKeyCredentials(null, authToken, null, null, creds), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(BadRequestException.class));
    }

    @Test
    public void updateUserApiKeyCredentials_callsValidateUsername() throws Exception {
        ApiKeyCredentials creds = new ApiKeyCredentials();
        creds.setApiKey("123");
        creds.setUsername("username");
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.updateUserApiKeyCredentials(null, authToken, null, null, creds);
        verify(validator20).validateUsername("username");
    }

    @Test
    public void updateUserApiKeyCredentials_callsCheckAndGetUserById() throws Exception {
        ApiKeyCredentials creds = new ApiKeyCredentials();
        creds.setApiKey("123");
        creds.setUsername("username");
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.getUser("username")).thenReturn(user);
        spy.updateUserApiKeyCredentials(null, authToken, userId, null, creds);
        verify(userService).checkAndGetUserById(userId);
    }

    @Test
    public void updateUserApiKeyCredentials_credUserIsNullThrowsNotFoundException_returnsResponseBuilder() throws Exception {
        ApiKeyCredentials creds = new ApiKeyCredentials();
        creds.setApiKey("123");
        creds.setUsername("username");
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.updateUserApiKeyCredentials(null, authToken, null, null, creds), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(),instanceOf(NotFoundException.class));
    }

    @Test
    public void updateUserApiKeyCredentials_credsUsernameNotMatchUserGetUsername_returnsResponseBuilder() throws Exception {
        ApiKeyCredentials creds = new ApiKeyCredentials();
        creds.setApiKey("123");
        creds.setUsername("username");
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.getUser("username")).thenReturn(user);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response code", spy.updateUserApiKeyCredentials(null, authToken, userId, null, creds), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(BadRequestException.class));
    }

    @Test
    public void updateUserApiKeyCredentials_userService_callsUpdateUser() throws Exception {
        ApiKeyCredentials creds = new ApiKeyCredentials();
        creds.setApiKey("123");
        creds.setUsername("username");
        user.setUsername("username");
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
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
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(userService.getUser(anyString())).thenReturn(user);
        when(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.updateUserApiKeyCredentials(null, authToken, userId, null, creds);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void resetUserApiKeyCredentials_callsVerifyUserLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.resetUserApiKeyCredentials(null, authToken, null, null);
        verify(authorizationService).verifyUserLevelAccess(scopeAccess);
    }

    @Test
    public void resetUserApiKeyCredentials_callsCheckAndGetUserById() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.getUser("username")).thenReturn(user);
        spy.resetUserApiKeyCredentials(null, authToken, userId, null);
        verify(userService).checkAndGetUserById(userId);
    }

    @Test
    public void resetUserApiKeyCredentialsSelf_responseOk_returns200() throws Exception {
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(userService.getUserByAuthToken(anyString())).thenReturn(user);
        when(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.resetUserApiKeyCredentials(null, authToken, userId, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void resetUserApiKeyCredentialsSubUser_responseOk_returns200() throws Exception {
        User subuser = new User();
        subuser.setId("2");
        subuser.setDomainId(user.getDomainId());
        subuser.setUsername("subuser");
        when(authorizationService.authorizeCloudUserAdmin(org.mockito.Matchers.<ScopeAccess>anyObject())).thenReturn(true);
        when(userService.checkAndGetUserById("2")).thenReturn(subuser);
        when(userService.getUserByAuthToken(anyString())).thenReturn(user);
        when(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory());
        Response.ResponseBuilder responseBuilder = spy.resetUserApiKeyCredentials(null, authToken, subuser.getId(), null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void resetUserApiKeyCredentials_callerUserHasNoAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        User subuser = new User();
        subuser.setId("2");
        subuser.setDomainId(user.getDomainId());
        subuser.setUsername("subuser");
        when(authorizationService.authorizeCloudUserAdmin(org.mockito.Matchers.<ScopeAccess>anyObject())).thenReturn(false);
        when(authorizationService.authorizeCloudUser(org.mockito.Matchers.<ScopeAccess>anyObject())).thenReturn(true);
        when(userService.checkAndGetUserById(anyString())).thenReturn(user);
        when(userService.getUserByAuthToken(anyString())).thenReturn(subuser);
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.resetUserApiKeyCredentials(null, authToken, null, null);
        verify(exceptionHandler).exceptionResponse(any(ForbiddenException.class));
    }

    @Test
    public void resetUserApiKeyCredentials_callerIdentityAdminHasNoAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        User svcuser = new User();
        svcuser.setId("999");
        svcuser.setUsername("svcAdmin");
        when(authorizationService.authorizeCloudUserAdmin(org.mockito.Matchers.<ScopeAccess>anyObject())).thenReturn(false);
        when(authorizationService.authorizeCloudUser(org.mockito.Matchers.<ScopeAccess>anyObject())).thenReturn(false);
        when(authorizationService.authorizeCloudIdentityAdmin(org.mockito.Matchers.<ScopeAccess>anyObject())).thenReturn(true);
        when(userService.checkAndGetUserById(anyString())).thenReturn(user);
        when(userService.getUserByAuthToken(anyString())).thenReturn(svcuser);
        when(scopeAccessService.getUserScopeAccessForClientId(anyString(), anyString())).thenReturn(userScopeAccess);
        when(authorizationService.hasServiceAdminRole(user)).thenReturn(true);
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.resetUserApiKeyCredentials(null, authToken, null, null);
        verify(exceptionHandler).exceptionResponse(any(ForbiddenException.class));
    }

    @Test
    public void updateUserPasswordCredentials_callsValidatePasswordCredentialsForCreateOrUpdate() throws Exception {
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        passwordCredentialsRequiredUsername.setUsername("someName");
        passwordCredentialsRequiredUsername.setPassword("Password1");
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.updateUserPasswordCredentials(null, authToken, null, null, passwordCredentialsRequiredUsername);
        verify(validator).validatePasswordForCreateOrUpdate(passwordCredentialsRequiredUsername.getPassword());
    }

    @Test
    public void updateUserPasswordCredentials_callsCheckAndGetUserById() throws Exception {
        PasswordCredentialsRequiredUsername passwordCredentialsRequiredUsername = new PasswordCredentialsRequiredUsername();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.updateUserPasswordCredentials(null, authToken, userId, null, passwordCredentialsRequiredUsername);
        verify(userService).checkAndGetUserById(userId);
    }

    @Test
    public void updateUserPasswordCredentials_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.updateUserPasswordCredentials(null, authToken, null, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void updateUserPasswordCredentials_credsUsernameNotMatchUserGetUsername_returnsResponseBuilder() throws Exception {
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername();
        creds.setUsername("username");
        creds.setPassword("ABCdef123");
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.updateUserPasswordCredentials(null, authToken, userId, null, creds), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(BadRequestException.class));
    }

    @Test
    public void updateUserPasswordCredentials_withValidCredentials_callsUserService_updateUserMethod() throws Exception {
        PasswordCredentialsRequiredUsername creds = new PasswordCredentialsRequiredUsername();
        creds.setUsername(userId);
        creds.setPassword("ABCdef123");
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        spy.updateUserPasswordCredentials(null, authToken, userId, null, creds);
        verify(userService).updateUser(user, false);
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
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(null);
        spy.listEndpointsForToken(null, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void listEndpointsForToken_throwsNotAuthorizedException_returnsResponseBuilder() throws Exception {
        NotAuthorizedException notAuthorizedException = new NotAuthorizedException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(null);
        doThrow(notAuthorizedException).when(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
        when(exceptionHandler.exceptionResponse(notAuthorizedException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.listEndpointsForToken(null, null, null), equalTo(responseBuilder));
    }

    @Test
    public void listEndpointsForToken_scopeAccessIsImpersonated_callsScopeAccessService_getScopeAccessByAccessToken() throws Exception {
        ImpersonatedScopeAccess impersonatedScopeAccess = new ImpersonatedScopeAccess();
        impersonatedScopeAccess.setImpersonatingToken("impersonatingToken");
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(impersonatedScopeAccess).when(spy).checkAndGetToken("tokenId");
        spy.listEndpointsForToken(null, authToken, "tokenId");
        verify(scopeAccessService).getScopeAccessByAccessToken("impersonatingToken");
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
    public void addGroup__callsValidateKsGroup() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.addGroup(httpHeaders,null,authToken,groupKs);
        verify(validator20).validateKsGroup(groupKs);
    }

    @Test
    public void addGroup_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.addGroup(httpHeaders,null,authToken,null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
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
    public void addGroup_duplicateGroup_returnsResponseBuilder() throws Exception {
        CloudGroupBuilder cloudGroupBuilder = mock(CloudGroupBuilder.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(cloudGroupBuilder.build((com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group) any())).thenReturn(group);
        doThrow(new DuplicateException("message")).when(userGroupService).addGroup(org.mockito.Matchers.<Group>any());
        when(exceptionHandler.conflictExceptionResponse("message")).thenReturn(responseBuilder);
        defaultCloud20Service.setCloudGroupBuilder(cloudGroupBuilder);
        assertThat("response builder", defaultCloud20Service.addGroup(null, null, authToken, groupKs), equalTo(responseBuilder));
    }

    @Test
    public void addGroup_throwsBadRequestException_returnsResponseBuilder() throws Exception {
        BadRequestException badRequestException = new BadRequestException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(badRequestException).when(validator20).validateKsGroup(groupKs);
        when(exceptionHandler.exceptionResponse(badRequestException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.addGroup(null, null, authToken, groupKs), equalTo(responseBuilder));
    }

    @Test
    public void updateGroup_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.updateGroup(null, authToken, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void updateGroup_throwsForbiddenException_returnsResponseBuilder() throws Exception {
        ForbiddenException forbiddenException = new ForbiddenException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(forbiddenException).when(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
        when(exceptionHandler.exceptionResponse(forbiddenException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.updateGroup(null, authToken, null, null), equalTo(responseBuilder));
    }

    @Test
    public void updateGroup_callsValidateKsGroup() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.updateGroup(null, authToken, null, groupKs);
        verify(validator20).validateKsGroup(groupKs);
    }

    @Test
    public void updateGroup_callsValidateGroupId() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.updateGroup(null, authToken, "1", groupKs);
        verify(validator20).validateGroupId("1");
    }

    @Test
    public void updateGroup_responseOk_returns200() throws Exception {
        CloudGroupBuilder cloudGroupBuilder = mock(CloudGroupBuilder.class);
        spy.setCloudGroupBuilder(cloudGroupBuilder);
        Response.ResponseBuilder responseBuilder = spy.updateGroup(httpHeaders, authToken, "1", groupKs);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void updateGroup_cloudGroupServiceUpdateGroupThrowsDuplicateException_returnsResponseBuilder() throws Exception {
        CloudGroupBuilder cloudGroupBuilder = mock(CloudGroupBuilder.class);
        spy.setCloudGroupBuilder(cloudGroupBuilder);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(new DuplicateException("message")).when(userGroupService).updateGroup(any(Group.class));
        when(exceptionHandler.conflictExceptionResponse("message")).thenReturn(responseBuilder);
        assertThat("response builder", spy.updateGroup(httpHeaders, authToken, "1", groupKs), equalTo(responseBuilder));
    }

    @Test
    public void deleteGroup_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.deleteGroup(null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void deleteGroup_throwsBadRequestException_returnsResponseBuilder() throws Exception {
        BadRequestException badRequestException = new BadRequestException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(badRequestException).when(validator20).validateGroupId(null);
        when(exceptionHandler.exceptionResponse(badRequestException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.deleteGroup(null, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void deleteGroup_callsValidateGroupId() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.deleteGroup(null, authToken, "1");
        verify(validator20).validateGroupId("1");
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
    public void addUserToGroup_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.addUserToGroup(null, authToken, "1", null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void addUserToGroup_throwsBadRequestException_returnsResponseBuilder() throws Exception {
        BadRequestException badRequestException = new BadRequestException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doThrow(badRequestException).when(validator20).validateGroupId(null);
        when(exceptionHandler.exceptionResponse(badRequestException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.addUserToGroup(null, authToken, null, null), equalTo(responseBuilder));
    }

    @Test
    public void addUserToGroup_callsValidateGroupId() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.addUserToGroup(null, authToken, "1", null);
        verify(validator20).validateGroupId("1");
    }

    @Test
    public void removeUserFromGroup_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.removeUserFromGroup(null, authToken, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void removeUserFromGroup_callsValidateGropuId() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.removeUserFromGroup(null, authToken, "1", null);
        verify(validator20).validateGroupId("1");
    }

    @Test
    public void removeUserFromGroup_userIdIsNullThrowsBadRequest_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.removeUserFromGroup(null, authToken, "1", null), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(BadRequestException.class));
    }

    @Test
    public void removeUserFromGroup_userIdIsBlankSpaceThrowsBadRequest_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.removeUserFromGroup(null, authToken, "1", " "), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(BadRequestException.class));
    }

    @Test
    public void getUsersForGroup_emptyGroup_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<UserList> argumentCaptor = ArgumentCaptor.forClass(UserList.class);
        List<User> userList = new ArrayList<User>();
        Users users = new Users();
        users.setUsers(userList);
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(userGroupService.getGroupById(1)).thenReturn(group);
        when(userGroupService.getAllEnabledUsers(any(FilterParam[].class), anyString(), anyInt())).thenReturn(users);
        Response.ResponseBuilder responseBuilder = spy.getUsersForGroup(null, authToken, "1", "1", "1");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getUsersForGroup_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.getUsersForGroup(null, authToken, null, null, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void getUsersForGroup_callsValidateGroupId() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        spy.getUsersForGroup(null, authToken, "1", null, null);
        verify(validator20).validateGroupId("1");
    }

    @Test
    public void getUsersForGroup_groupNotExistThrowsNotFoundException_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", spy.getUsersForGroup(null, authToken, "1", null, null), equalTo(responseBuilder));
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
    public void listGroupWithQueryParam_validName_returns200() throws Exception {
        CloudKsGroupBuilder cloudKsGroupBuilder = mock(CloudKsGroupBuilder.class);
        when(userGroupService.getGroupByName(org.mockito.Matchers.<String>anyObject())).thenReturn(group);
        when(cloudKsGroupBuilder.build(org.mockito.Matchers.<Group>any())).thenReturn(groupKs);
        defaultCloud20Service.setCloudKsGroupBuilder(cloudKsGroupBuilder);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.getGroup(null, authToken, "group1");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void assignProperRole_callsAuthorizeCloudUserAdmin() throws Exception {
        defaultCloud20Service.assignProperRole(null, authToken, null, null);
        verify(authorizationService).authorizeCloudUserAdmin(null);
    }

    @Test
    public void assignProperRole_callsAuthorizeCloudIdentityAdmin() throws Exception {
        defaultCloud20Service.assignProperRole(null, authToken, null, null);
        verify(authorizationService).authorizeCloudIdentityAdmin(null);
    }

    @Test
    public void assignProperRole_callsAuthorizeCloudServiceAdmin() throws Exception {
        defaultCloud20Service.assignProperRole(null, authToken, null, null);
        verify(authorizationService).authorizeCloudServiceAdmin(null);
    }

    @Test
    public void deleteTenant_validTenantAdminAndServiceAdmin_return204() throws Exception {
        UserScopeAccess scopeAccess = new UserScopeAccess();
        //Current time plus 10 min
        scopeAccess.setAccessTokenExp(new Date(System.currentTimeMillis() + 600000));
        scopeAccess.setRefreshTokenExp(new Date(System.currentTimeMillis() + 600000));
        scopeAccess.setAccessTokenString("token");
        when(scopeAccessService.getScopeAccessByAccessToken(authToken)).thenReturn(scopeAccess);
        when(tenantService.checkAndGetTenant("1")).thenReturn(tenant);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.deleteTenant(null, authToken, "1");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void deleteTenant_throwsForbiddenException_returnResponseBuilder() throws Exception {
        ForbiddenException forbiddenException = new ForbiddenException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(forbiddenException).when(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
        when(exceptionHandler.exceptionResponse(forbiddenException)).thenReturn(responseBuilder);
        assertThat("response builder", spy.deleteTenant(null, authToken, "1"), equalTo(responseBuilder));
    }

    @Test
    public void getExtension_badAlias_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", defaultCloud20Service.getExtension(null, ""), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(BadRequestException.class));
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
        ArgumentCaptor<NotFoundException> argumentCaptor = ArgumentCaptor.forClass(NotFoundException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        org.openstack.docs.common.api.v1.ObjectFactory objectFactory = new org.openstack.docs.common.api.v1.ObjectFactory();
        when(jaxbObjectFactories.getOpenStackCommonV1Factory()).thenReturn(objectFactory);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", defaultCloud20Service.getExtension(null, "bad"), equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(NotFoundException.class));
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
        ArgumentCaptor<NullPointerException> argumentCaptor = ArgumentCaptor.forClass(NullPointerException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        JAXBContext jaxbContext = JAXBContextResolver.get();
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        InputStream is = StringUtils.class.getResourceAsStream("/extensions.xml");
        StreamSource ss = new StreamSource(is);
        currentExtensions = unmarshaller.unmarshal(ss, Extensions.class);
        defaultCloud20Service.setCurrentExtensions(currentExtensions);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder", defaultCloud20Service.getExtension(null, "RAX-KSKEY"), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(NullPointerException.class));
    }

    @Test
    public void getGroup_callsVerifyServiceAdminLevelAccess() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        spy.getGroup(null, authToken, null);
        verify(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
    }

    @Test
    public void getGroup_responseOk_returns200() throws Exception {
        Response.ResponseBuilder responseBuilder = spy.getGroup(httpHeaders, authToken, "groupName");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void impersonate_callsVerifyRackerOrServiceAdminAccess() throws Exception {
        user.setEnabled(true);
        ScopeAccess scopeAccess = new ScopeAccess();
        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("impersonateUser");
        impersonateUser.setId("impersonateUserId");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(impersonateUser);

        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(authorizationService.authorizeRacker(any(ScopeAccess.class))).thenReturn(true);
        when(delegateCloud20Service.impersonateUser(anyString(), anyString(), anyString())).thenReturn("impersonatingToken");
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());

        spy.impersonate(null, authToken, impersonationRequest);
        verify(authorizationService).verifyRackerOrIdentityAdminAccess(scopeAccess);
    }

    @Test
    public void impersonate_callsValidateImpersonationRequest() throws Exception {
        user.setEnabled(true);
        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("impersonateUser");
        impersonateUser.setId("impersonateUserId");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(impersonateUser);
        when(config.getString("ga.username")).thenReturn("ga.username");
        when(config.getString("ga.password")).thenReturn("ga.password");
        when(delegateCloud20Service.impersonateUser("impersonateUser", "ga.username", "ga.password")).thenReturn("impersonatingToken");
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        spy.impersonate(null, authToken, impersonationRequest);
        verify(validator20).validateImpersonationRequest(impersonationRequest);
    }

    @Test
    public void impersonate_userNotNullAndNotEnabled_returnsResponseBuilder() throws Exception {
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExpired();
        userScopeAccess.setAccessTokenString("token");
        user.setEnabled(false);
        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("impersonateUser");
        impersonateUser.setId("impersonateUserId");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(impersonateUser);
        when(authorizationService.authorizeRacker(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUser("impersonateUser")).thenReturn(user);
        doReturn(true).when(spy).isValidImpersonatee(user);
        when(scopeAccessService.getMostRecentDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(userScopeAccess);
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        spy.impersonate(null, authToken, impersonationRequest);
        verify(scopeAccessService).updateExpiredUserScopeAccess(userScopeAccess, true);
    }

    @Test
    public void impersonate_userNotNullAndEnabledAndNotValidImpersonateeThrowsBadRequestException_returnsResponseBuilder() throws Exception {
        user.setEnabled(true);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);

        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("impersonateUser");
        impersonateUser.setId("impersonateUserId");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(impersonateUser);

        when(authorizationService.authorizeRacker(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUser("impersonateUser")).thenReturn(user);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);

        assertThat("returns response builder", spy.impersonate(null, authToken, impersonationRequest), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(), instanceOf(BadRequestException.class));
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
        when(scopeAccessService.getMostRecentDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(userScopeAccess);
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        spy.impersonate(null, authToken, impersonationRequest);
        verify(scopeAccessService).updateExpiredUserScopeAccess(userScopeAccess, false);
    }

    @Test
    public void impersonate_impersonatingTokenIsBlankStringThrowsBadRequestException_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();

        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExpired();
        userScopeAccess.setAccessTokenString("");

        user.setEnabled(true);

        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("impersonateUser");
        impersonateUser.setId("impersonateUserId");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(impersonateUser);
        UserScopeAccess userScopeAccess1 = new UserScopeAccess();

        when(authorizationService.authorizeRacker(any(ScopeAccess.class))).thenReturn(true);
        when(userService.getUser("impersonateUser")).thenReturn(user);
        doReturn(true).when(spy).isValidImpersonatee(user);
        when(scopeAccessService.getMostRecentDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(userScopeAccess);
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        when(scopeAccessService.updateExpiredUserScopeAccess(any(UserScopeAccess.class), anyBoolean())).thenReturn(userScopeAccess1);

        assertThat("response builder", spy.impersonate(null, authToken, impersonationRequest), equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(),instanceOf(BadRequestException.class));
    }

    @Test
    public void impersonate_impersonatingUserNameIsBlankStringThrowsBadRequestException_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();

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
        when(scopeAccessService.getMostRecentDirectScopeAccessForParentByClientId(anyString(), anyString())).thenReturn(userScopeAccess);
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);

        assertThat("response builder",spy.impersonate(null, authToken, impersonationRequest),equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(),instanceOf(BadRequestException.class));
    }

    @Test
    public void impersonate_scopeAccessInstanceOfRackerScopeAccess_returns200() throws Exception {
        RackerScopeAccess rackerScopeAccess = new RackerScopeAccess();
        user.setEnabled(true);
        when(spy.isCloudAuthRoutingEnabled()).thenReturn(true);
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
        when(spy.isCloudAuthRoutingEnabled()).thenReturn(true);
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

    @Test
    public void impersonate_scopeAccessNotInstanceOfUserOrRackerScopeAccessThrowsNotAuthorizedException_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<NotAuthorizedException> argumentCaptor = ArgumentCaptor.forClass(NotAuthorizedException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ClientScopeAccess clientScopeAccess = new ClientScopeAccess();
        user.setEnabled(true);

        org.openstack.docs.identity.api.v2.User impersonateUser = new org.openstack.docs.identity.api.v2.User();
        impersonateUser.setUsername("impersonateUser");
        impersonateUser.setId("impersonateUserId");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(impersonateUser);

        when(spy.isCloudAuthRoutingEnabled()).thenReturn(true);
        when(authorizationService.authorizeRacker(any(ScopeAccess.class))).thenReturn(true);
        when(delegateCloud20Service.impersonateUser(anyString(), anyString(), anyString())).thenReturn("impersonatingToken");
        doReturn(clientScopeAccess).when(spy).checkAndGetToken(authToken);
        when(jaxbObjectFactories.getRackspaceIdentityExtRaxgaV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory());
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);

        assertThat("response builder",spy.impersonate(null, authToken, impersonationRequest),equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(NotAuthorizedException.class));
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
    public void convertTenantEntityToApi_returnsTenantForAuthenticateResponse(){
        Tenant test = new Tenant();
        test.setName("test");
        test.setTenantId("test");
        TenantForAuthenticateResponse testTenant = defaultCloud20Service.convertTenantEntityToApi(test);
        assertThat("Verify Tenant", testTenant.getId(), equalTo(test.getTenantId()));
        assertThat("Verify Tenant", testTenant.getName(), equalTo(test.getName()));
    }

    @Test
    public void getUserByIdForAuthentication_succeeds_returnsUser() throws Exception {
        when(userService.checkAndGetUserById(userId)).thenReturn(user);
        assertThat("user", defaultCloud20Service.getUserByIdForAuthentication(userId), equalTo(user));
    }

    @Test (expected = NotAuthenticatedException.class)
    public void getUserByIdForAuthentication_throwsNotAuthenticatedException() throws Exception {
        doThrow(new NotFoundException()).when(userService).checkAndGetUserById("id");
        spy.getUserByIdForAuthentication("id");
    }

    @Test
    public void listTenants_scopeAccessServiceCallsGetScopeAccessByAccessTokenReturnsNull_responseIsOk_returns200() throws Exception {
        doReturn(new ScopeAccess()).when(spy).getScopeAccessForValidToken(authToken);
        Response.ResponseBuilder responseBuilder = spy.listTenants(httpHeaders, authToken, "marker", 1);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUserGlobalRoles_notServiceAdminAndIsServiceAdmin_responseOk_returns200() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.getUserById("userId")).thenReturn(user);
        doReturn(new User()).when(spy).getUser(scopeAccess);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(false);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(true);
        when(tenantService.getGlobalRolesForUser(user)).thenReturn(new ArrayList<TenantRole>());
        Response.ResponseBuilder responseBuilder = spy.listUserGlobalRoles(httpHeaders, authToken, "userId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUserGlobalRoles_isServiceAdminAndNotServiceAdmin_responseOk_returns200() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.getUserById("userId")).thenReturn(user);
        doReturn(new User()).when(spy).getUser(scopeAccess);
        when(authorizationService.authorizeCloudServiceAdmin(scopeAccess)).thenReturn(true);
        when(authorizationService.authorizeCloudIdentityAdmin(scopeAccess)).thenReturn(false);
        when(tenantService.getGlobalRolesForUser(user)).thenReturn(new ArrayList<TenantRole>());
        Response.ResponseBuilder responseBuilder = spy.listUserGlobalRoles(httpHeaders, authToken, "userId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listGroups_throwsForbiddenException_returnsResponseBuilder() throws Exception {
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ForbiddenException forbiddenException = new ForbiddenException();
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(forbiddenException).when(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
        when(exceptionHandler.exceptionResponse(forbiddenException)).thenReturn(responseBuilder);
        assertThat("response code", spy.listGroups(httpHeaders, authToken, null, null, null), equalTo(responseBuilder));
    }

    @Test
    public void getGroup_throwsForbiddenException_returnsResponseBuilder() throws Exception {
        ForbiddenException forbiddenException = new ForbiddenException();
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        ScopeAccess scopeAccess = new ScopeAccess();
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doThrow(forbiddenException).when(authorizationService).verifyIdentityAdminLevelAccess(scopeAccess);
        when(exceptionHandler.exceptionResponse(forbiddenException)).thenReturn(responseBuilder);
        assertThat("response code", spy.getGroup(httpHeaders, authToken, null), equalTo(responseBuilder));
    }

    @Test
    public void impersonate_userIsEnabled_userScopeAccessTokenNotExpiredAndImpersonatingUsernameIsBlankThrowsBadRequestException_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();

        user.setEnabled(true);
        user.setUniqueId("uniqueId");

        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        userScopeAccess.setAccessTokenString("notExpired");

        org.openstack.docs.identity.api.v2.User userTest = new org.openstack.docs.identity.api.v2.User();
        userTest.setUsername("");

        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(userTest);

        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.getUser("")).thenReturn(user);
        doReturn(true).when(spy).isValidImpersonatee(user);
        when(config.getString("cloudAuth.clientId")).thenReturn("clientId");
        when(scopeAccessService.getMostRecentDirectScopeAccessForParentByClientId("uniqueId", "clientId")).thenReturn(userScopeAccess);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);

        assertThat("response builder",spy.impersonate(httpHeaders, authToken, impersonationRequest),equalTo(responseBuilder));
        assertThat("exception type", argumentCaptor.getValue(),instanceOf(BadRequestException.class));
    }

    @Test
    public void impersonate_userIsEnabled_userScopeAccessTokenExpiredAndImpersonatingTokenIsBlankThrowsBadRequestException_returnsResponseBuilder() throws Exception {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();

        user.setEnabled(true);
        user.setUniqueId("uniqueId");
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setAccessTokenExp(new Date(3000, 1, 1));
        userScopeAccess.setAccessTokenString("");
        org.openstack.docs.identity.api.v2.User userTest = new org.openstack.docs.identity.api.v2.User();
        userTest.setUsername("");
        ImpersonationRequest impersonationRequest = new ImpersonationRequest();
        impersonationRequest.setUser(userTest);
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(userService.getUser("")).thenReturn(user);
        doReturn(true).when(spy).isValidImpersonatee(user);
        when(config.getString("cloudAuth.clientId")).thenReturn("clientId");
        when(scopeAccessService.getMostRecentDirectScopeAccessForParentByClientId("uniqueId", "clientId")).thenReturn(userScopeAccess);
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder",spy.impersonate(httpHeaders, authToken, impersonationRequest),equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(BadRequestException.class));
    }

    @Test
    public void listDefaultRegionServices_openStackServicesIsNull_responseOk_returns200() throws Exception {
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
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
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
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
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(clientService.getOpenStackServices()).thenReturn(openStackServices);
        Response.ResponseBuilder responseBuilder = spy.listDefaultRegionServices(authToken);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void listUserGroups_groupsSizeNotZero_responseOk_returns200() throws Exception {
        GroupService cloudGroupService = mock(GroupService.class);
        spy.setGroupService(cloudGroupService);
        Group group = new Group();
        List<Group> groups = new ArrayList<Group>();
        groups.add(group);
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        when(cloudGroupService.getGroupsForUser("userId")).thenReturn(groups);
        when(cloudKsGroupBuilder.build(group)).thenReturn(new com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Group());
        Response.ResponseBuilder responseBuilder = spy.listUserGroups(httpHeaders, authToken, "userId");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
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
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
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
        doReturn(null).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(clientScopeAccess).when(spy).checkAndGetToken("tokenId");
        when(tokenConverterCloudV20.toToken(clientScopeAccess)).thenReturn(token);
        Response.ResponseBuilder responseBuilder = spy.validateToken(httpHeaders, authToken, "tokenId", "belongsTo");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
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
    public void getJSONCredentials_credentialIsInstanceOfApiKeyCredentials_returnsJAXBElementOfApiKeyCredentials() throws Exception {
        String jsonBody = "{\"RAX-KSKEY:apiKeyCredentials\":{\"username\":\"test_user\",\"apiKey\":\"aaaaa-bbbbb-ccccc-12345678\"}}";
        when(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory());
        JAXBElement<? extends CredentialType> jaxbElement = spy.getJSONCredentials(jsonBody);
        ApiKeyCredentials result = (ApiKeyCredentials) jaxbElement.getValue();
        assertThat("apikey", result.getApiKey(), equalTo("aaaaa-bbbbb-ccccc-12345678"));
        assertThat("username", result.getUsername(), equalTo("test_user"));
    }

    @Test
    public void getJSONCredentials_credentialIsInstanceOfDifferentCredentials_returnsNull() throws Exception {
        String jsonBody = "{\"RAX-KSKEY:apiKeyCredentials\":{\"username\":\"test_user\",\"apiKey\":\"aaaaa-bbbbb-ccccc-12345678\"}}";
        when(jaxbObjectFactories.getRackspaceIdentityExtKskeyV1Factory()).thenReturn(new com.rackspace.docs.identity.api.ext.rax_kskey.v1.ObjectFactory());
        JAXBElement<? extends CredentialType> jaxbElement = spy.getJSONCredentials(jsonBody);
        ApiKeyCredentials result = (ApiKeyCredentials) jaxbElement.getValue();
        assertThat("apikey", result.getApiKey(), equalTo("aaaaa-bbbbb-ccccc-12345678"));
        assertThat("username", result.getUsername(), equalTo("test_user"));
    }

    @Test (expected = BadRequestException.class)
    public void getXMLCredentials_callsUnmarshaller_unmarshall_throwsJAXBException() throws Exception {
        spy.getXMLCredentials("body");
    }

    @Test
    public void addDomain_emptyName_throwBadRequest() {
        ArgumentCaptor<BadRequestException> argumentCaptor = ArgumentCaptor.forClass(BadRequestException.class);
        Response.ResponseBuilder responseBuilder = new ResponseBuilderImpl();
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain = new com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain();
        domain.setId("1");
        domain.setName("");
        when(exceptionHandler.exceptionResponse(argumentCaptor.capture())).thenReturn(responseBuilder);
        assertThat("response builder",spy.addDomain(authToken, null, domain),equalTo(responseBuilder));
        assertThat("exception type",argumentCaptor.getValue(),instanceOf(BadRequestException.class));
    }

    @Test
    public void addDomain_valid_SuccessRequest() throws Exception {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain newDomain = new com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain();
        newDomain.setId("1");
        newDomain.setName("domain");
        doReturn(domain).when(domainConverterCloudV20).toDomainDO(newDomain);
        doReturn(newDomain).when(domainConverterCloudV20).toDomain(domain);
        when(uriInfo.getRequestUriBuilder()).thenReturn(UriBuilder.fromPath("path"));
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.addDomain(authToken, uriInfo, newDomain);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(201));
    }

    @Test
    public void updateDomain_valid_SuccessRequest() throws Exception {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain newDomain = new com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain();
        newDomain.setId("1");
        newDomain.setName("domain");
        when(domainService.checkAndGetDomain("1")).thenReturn(domain);
        doReturn(domain).when(domainConverterCloudV20).toDomainDO(newDomain);
        doReturn(newDomain).when(domainConverterCloudV20).toDomain(domain);
        when(uriInfo.getRequestUriBuilder()).thenReturn(UriBuilder.fromPath("path"));
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.updateDomain(authToken, "1", newDomain);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test (expected = BadRequestException.class)
    public void validateDomain_DifferentId_throws400() throws Exception {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain = new com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain();
        domain.setId("1");
        defaultCloud20Service.validateDomain(domain,"2");
    }

    @Test (expected = BadRequestException.class)
    public void validateDomain_emptyDomainName_throws400() throws Exception {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain = new com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain();
        domain.setId("1");
        defaultCloud20Service.validateDomain(domain,"1");
    }

    @Test
    public void validateDomain_validDomain() throws Exception {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain = new com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain();
        domain.setId("1");
        domain.setName("testDomain");
        defaultCloud20Service.validateDomain(domain,"1");
    }

    @Test
    public void setDomainEmptyValues_setsCorrectValues() throws Exception {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain domain = new com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain();
        domain.setId("");
        domain.setName("");
        domain.setDescription("");
        defaultCloud20Service.setDomainEmptyValues(domain,"1");
        assertThat("Description", domain.getDescription(),equalTo(null));
        assertThat("Description", domain.getName(),equalTo(null));
        assertThat("Description", domain.getId(),equalTo("1"));
    }
    
    @Test (expected = ForbiddenException.class)
    public void addUserToDomain_Admin_expectsForbidden() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        User user = new User();
        com.rackspace.idm.domain.entity.Domain domain = new com.rackspace.idm.domain.entity.Domain();
        domain.setEnabled(true);
        List<TenantRole> roles = new ArrayList<TenantRole>();
        TenantRole role = new TenantRole();
        role.setName("identity:user-admin");
        roles.add(role);

        when(config.getString(anyString())).thenReturn("identity:user-admin");
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(user).when(userService).checkAndGetUserById(userId);
        doReturn(roles).when(tenantService).getGlobalRolesForUser(user);
        doReturn(domain).when(domainService).checkAndGetDomain("123");

        defaultCloud20Service.addUserToDomain(authToken, "123", userId);
    }

    @Test (expected = ForbiddenException.class)
    public void addUserToDomain_ServiceAdmin_expectsForbidden() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        com.rackspace.idm.domain.entity.Domain domain = new com.rackspace.idm.domain.entity.Domain();
        domain.setEnabled(true);
        User user = new User();
        List<TenantRole> roles = new ArrayList<TenantRole>();
        TenantRole role = new TenantRole();
        role.setName("identity:service-admin");
        roles.add(role);
        Domain domain1 = new Domain();
        domain1.setEnabled(true);

        when(config.getString(anyString())).thenReturn("identity:service-admin");
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(user).when(userService).checkAndGetUserById(userId);
        doReturn(roles).when(tenantService).getGlobalRolesForUser(user);
        doReturn(domain).when(domainService).checkAndGetDomain("123");
        defaultCloud20Service.addUserToDomain(authToken, "123", userId);
    }

    @Test (expected = ForbiddenException.class)
    public void addUserToDomain_disabledDomain_expectsForbidden() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        com.rackspace.idm.domain.entity.Domain domain = new com.rackspace.idm.domain.entity.Domain();
        domain.setEnabled(false);

        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(domain).when(domainService).checkAndGetDomain("123");

        defaultCloud20Service.addUserToDomain(authToken, "123", userId);
    }

    @Test (expected = ForbiddenException.class)
    public void addUserToDomain_noRoles_expectsForbidden() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        com.rackspace.idm.domain.entity.Domain domain = new com.rackspace.idm.domain.entity.Domain();
        domain.setEnabled(true);
        User user = new User();

        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(domain).when(domainService).checkAndGetDomain("123");
        doReturn(user).when(userService).checkAndGetUserById("123");

        defaultCloud20Service.addUserToDomain(authToken, "123", "123");
    }

    @Test
    public void deleteDomain_emptyDomain_expectsSuccess() {
        ScopeAccess scopeAccess = new ScopeAccess();
        List<User> userList = new ArrayList<User>();
        com.rackspace.idm.domain.entity.Domain domain = new com.rackspace.idm.domain.entity.Domain();
        domain.setDomainId("123");
        Users users = new Users();
        users.setUsers(userList);

        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        when(domainService.getUsersByDomainId("123")).thenReturn(users);
        when(domainService.checkAndGetDomain("123")).thenReturn(domain);

        Response.ResponseBuilder responseBuilder = defaultCloud20Service.deleteDomain(authToken, "123");
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Test
    public void deleteDomain_nonEmptyDomain_expectsBadRequest() {
        ScopeAccess scopeAccess = new ScopeAccess();
        List<User> userList = new ArrayList<User>();
        User user = new User();
        userList.add(user);
        Users users = new Users();
        users.setUsers(userList);
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);
        doReturn(users).when(domainService).getUsersByDomainId(null);

        defaultCloud20Service.deleteDomain(authToken, null);
        verify(exceptionHandler).exceptionResponse(any(BadRequestException.class));
    }

    @Test
    public void addTenantToDomain_validTenantId() {
        ScopeAccess scopeAccess = new ScopeAccess();

        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);

        Response.ResponseBuilder responseBuilder = defaultCloud20Service.addTenantToDomain(authToken, "135792468", "999999");
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(204));
    }

    @Ignore
    @Test (expected = NotFoundException.class)
    public void addTenantToDomain_invalidTenantId_expectsNotFound() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        NotFoundException exception = new NotFoundException();

        doThrow(exception).when(tenantService).checkAndGetTenant("9999999");
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);

        defaultCloud20Service.addTenantToDomain(authToken, "135792468", "9999999");
    }

    @Test
    public void getDomainTenants_validDomainId() {
        ScopeAccess scopeAccess = new ScopeAccess();

        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);

        Response.ResponseBuilder responseBuilder = defaultCloud20Service.getDomainTenants(authToken, "135792468", "999999");
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Ignore
    @Test (expected = NotFoundException.class)
    public void getDomainTenants_invalidDomainId_expectsNotFound() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        NotFoundException exception = new NotFoundException();

        doThrow(exception).when(domainService).checkAndGetDomain("135792468");
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);

        defaultCloud20Service.getDomainTenants(authToken, "135792468", "true");
    }

    @Test
    public void getUsersByDomainId_validDomainId() {
        ScopeAccess scopeAccess = new ScopeAccess();
        List<User> userList = new ArrayList<User>();
        User user = new User();
        userList.add(user);
        Users users = new Users();
        users.setUsers(userList);

        doReturn(users).when(domainService).getUsersByDomainId("135792468", true);
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);

        Response.ResponseBuilder responseBuilder = defaultCloud20Service.getUsersByDomainId(authToken, "135792468", "true");
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Ignore
    @Test (expected = NotFoundException.class)
    public void getUsersByDomainId_invalidDomainId_expectsNotFound() throws Exception {
        ScopeAccess scopeAccess = new ScopeAccess();
        NotFoundException exception = new NotFoundException();

        doThrow(exception).when(domainService).checkAndGetDomain("135792468");
        doReturn(scopeAccess).when(spy).getScopeAccessForValidToken(authToken);

        defaultCloud20Service.getUsersByDomainId(authToken, "135792468", "true");
    }

    @Test
    public void setEmptyUserValues() throws Exception {
        User user = new User();
        defaultCloud20Service.setEmptyUserValues(user);
        assertThat("email",user.getEmail(),equalTo(""));
        assertThat("domain",user.getDomainId(),equalTo(""));
        assertThat("region",user.getRegion(),equalTo(""));
    }

    @Test(expected = BadRequestException.class)
    public void validateMaker_return400() throws Exception {
        defaultCloud20Service.validateMarker("asdf");
    }

    @Test
    public void validateMaker_validMarker_returnsStringValue() throws Exception {
        String marker = defaultCloud20Service.validateMarker("1");
        assertThat("Check Marker",marker,equalTo("1"));
    }

    @Test
    public void validateMaker_nullMarker_returnsZero() throws Exception {
        String marker = defaultCloud20Service.validateMarker(null);
        assertThat("Check Marker",marker,equalTo("0"));
    }

    @Test (expected = BadRequestException.class)
    public void validateMarker_negativeMarker_returnsBadRequest() throws Exception {
        defaultCloud20Service.validateMarker("-5");
    }

    @Test (expected = BadRequestException.class)
    public void validateOffset_negativeOffset_returnsBadRequest() throws Exception {
        defaultCloud20Service.validateOffset("-5");
    }

    @Test
    public void setFilters_returnsFilterParamWithOutDomainId() {
        FilterParam[] compareTo = new FilterParam[]{new FilterParam(FilterParam.FilterParamName.ROLE_ID, "roleName")};

        FilterParam[] filters = defaultCloud20Service.setFilters("roleName", null);

        assertThat(filters[0].getParam(), equalTo(compareTo[0].getParam()));
    }

    @Test
    public void setFilters_returnsFilterParamWithDomainId() {
        FilterParam[] compareTo = new FilterParam[]{new FilterParam(FilterParam.FilterParamName.DOMAIN_ID, "domainId"),
                new FilterParam(FilterParam.FilterParamName.ROLE_ID, "roleName")};

        FilterParam[] filters = defaultCloud20Service.setFilters("roleName", "domainId");

        assertThat(filters[0].getParam(), equalTo(compareTo[0].getParam()));
        assertThat(filters[1].getParam(), equalTo(compareTo[1].getParam()));
    }
}
