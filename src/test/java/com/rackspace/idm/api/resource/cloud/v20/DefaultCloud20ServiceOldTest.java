package com.rackspace.idm.api.resource.cloud.v20;


import com.rackspace.docs.identity.api.ext.rax_auth.v1.DefaultRegionServices;
import com.rackspace.docs.identity.api.ext.rax_ksqa.v1.SecretQA;
import com.rackspace.idm.api.converter.cloudv20.*;
import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories;
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient;
import com.rackspace.idm.api.resource.pagination.DefaultPaginator;
import com.rackspace.idm.domain.config.JAXBContextResolver;
import com.rackspace.idm.domain.entity.Application;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.ExceptionHandler;
import com.rackspace.idm.exception.NotFoundException;
import com.rackspace.idm.validation.Validator;
import com.rackspace.idm.validation.Validator20;
import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
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
import org.openstack.docs.identity.api.v2.Role;
import org.openstack.docs.identity.api.v2.Token;
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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/12/11
 * Time: 3:58 PM
 */
public class DefaultCloud20ServiceOldTest {

    private DefaultCloud20Service defaultCloud20Service;
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
        defaultCloud20Service.setJaxbObjectFactories(jaxbObjectFactories);
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
        cloudBaseUrl.setBaseUrlId("101");
        cloudBaseUrl.setGlobal(false);
        application = new Application();
        application.setClientId("clientId");
        group = new Group();
        group.setName("Group1");
        group.setDescription("Group Description");
        group.setGroupId("1");
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
        when(endpointService.getBaseUrlById("101")).thenReturn(cloudBaseUrl);
        when(clientService.getById(role.getServiceId())).thenReturn(application);
        when(clientService.getById("clientId")).thenReturn(application);
        when(clientService.getClientRoleById(role.getId())).thenReturn(clientRole);
        when(clientService.getClientRoleById(tenantRole.getRoleRsId())).thenReturn(clientRole);
        when(tenantService.getTenant(tenantId)).thenReturn(tenant);
        when(userService.getUserById(userId)).thenReturn(user);
        when(config.getString("rackspace.customerId")).thenReturn(null);
        when(userConverterCloudV20.fromUser(userOS)).thenReturn(user);
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_XML_TYPE);
        when(userGroupService.checkAndGetGroupById(anyString())).thenReturn(group);
        when(uriInfo.getAbsolutePath()).thenReturn(new URI("http://absolute.path/to/resource"));
    }

    @Test(expected = BadRequestException.class)
    public void setDefaultRegionService_returns400WhenServiceIsNotFound() throws Exception {
        ArrayList<Application> applications = new ArrayList<Application>();
        Application application1 = new Application();
        applications.add(application1);
        Application application2 = new Application("cloudFiles", "cloudFiles");
        applications.add(application2);
        Application application3 = new Application("cloudFilesCDN", "cloudFilesCDN");
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
        Application application2 = new Application("cloudFiles", "cloudFiles");
        applications.add(application2);
        Application application3 = new Application("cloudFilesCDN", "cloudFilesCDN");
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
        Application application2 = new Application("cloudFiles", "cloudFiles");
        applications.add(application2);
        Application application3 = new Application("cloudFilesCDN", "cloudFilesCDN");
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
        Application application2 = new Application("cloudFiles", "cloudFiles");
        applications.add(application2);
        Application application3 = new Application("cloudFilesCDN", "cloudFilesCDN");
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
    public void setDefaultRegionServices_returnsNotNullResponseBuilder() throws Exception {
        when(clientService.getOpenStackServices()).thenReturn(new ArrayList<Application>());
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.setDefaultRegionServices(authToken, new DefaultRegionServices());
        assertThat("response builder", responseBuilder, Matchers.notNullValue());
    }

    @Test
    public void setDefaultRegionServices_callsApplicationService_getOSServices() throws Exception {
        when(clientService.getOpenStackServices()).thenReturn(new ArrayList<Application>());
        defaultCloud20Service.setDefaultRegionServices(authToken, new DefaultRegionServices());
        verify(clientService).getOpenStackServices();
    }

    @Test
    public void listDefaultRegionServices_callsApplicationService_getOSServices() throws Exception {
        when(clientService.getOpenStackServices()).thenReturn(new ArrayList<Application>());
        defaultCloud20Service.listDefaultRegionServices(authToken);
        verify(clientService).getOpenStackServices();
    }

    @Test
    public void listDefaultRegionServices_returnsNotNullResponseBuilder() throws Exception {
        when(clientService.getOpenStackServices()).thenReturn(new ArrayList<Application>());
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.listDefaultRegionServices(authToken);
        assertThat("response builder", responseBuilder, Matchers.notNullValue());
    }

    @Test
    public void listDefaultRegionServices_returnsNotNullEntity() throws Exception {
        when(clientService.getOpenStackServices()).thenReturn(new ArrayList<Application>());
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.listDefaultRegionServices(authToken);
        assertThat("response builder", responseBuilder.build().getEntity(), Matchers.notNullValue());
    }

    @Test
    public void listDefaultRegionServices_returnsDefaultRegionServicesType() throws Exception {
        when(clientService.getOpenStackServices()).thenReturn(new ArrayList<Application>());
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.listDefaultRegionServices(authToken);
        assertThat("response builder", responseBuilder.build().getEntity(), instanceOf(DefaultRegionServices.class));
    }

    @Test
    public void listDefaultRegionServices_filtersByUseForDefaultRegionFlag() throws Exception {
        ArrayList<Application> applications = new ArrayList<Application>();
        Application application1 = new Application();
        applications.add(application1);
        Application application2 = new Application("cloudFiles", "cloudFiles");
        applications.add(application2);
        Application application3 = new Application("cloudFilesCDN", "cloudFilesCDN");
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
        Application application2 = new Application("cloudFiles", "cloudFiles");
        applications.add(application2);
        Application application3 = new Application("cloudFilesCDN", "cloudFilesCDN");
        application3.setUseForDefaultRegion(true);
        applications.add(application3);
        when(clientService.getOpenStackServices()).thenReturn(applications);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.listDefaultRegionServices(authToken);
        assertThat("response builder", ((DefaultRegionServices)responseBuilder.build().getEntity()).getServiceName().size(), equalTo(1));
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
    public void listGroupWithQueryParam_validName_returns200() throws Exception {
        CloudKsGroupBuilder cloudKsGroupBuilder = mock(CloudKsGroupBuilder.class);
        when(userGroupService.getGroupByName(org.mockito.Matchers.<String>anyObject())).thenReturn(group);
        when(cloudKsGroupBuilder.build(org.mockito.Matchers.<Group>any())).thenReturn(groupKs);
        defaultCloud20Service.setCloudKsGroupBuilder(cloudKsGroupBuilder);
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.getGroup(null, authToken, "group1");
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

//    @Test
//    public void assignProperRole_callsAuthorizeCloudUserAdmin() throws Exception {
//        defaultCloud20Service.assignProperRole(null, null);
//        verify(authorizationService).authorizeCloudUserAdmin(null);
//    }
//
//    @Test
//    public void assignProperRole_callsAuthorizeCloudIdentityAdmin() throws Exception {
//        defaultCloud20Service.assignProperRole(null, null);
//        verify(authorizationService).authorizeCloudIdentityAdmin(null);
//    }
//
//    @Test
//    public void assignProperRole_callsAuthorizeCloudServiceAdmin() throws Exception {
//        defaultCloud20Service.assignProperRole(null, null);
//        verify(authorizationService).authorizeCloudServiceAdmin(null);
//    }

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
    public void addDomain_valid_SuccessRequest() throws Exception {
        com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain newDomain = new com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain();
        newDomain.setId("1");
        newDomain.setName("domain");
        doReturn(domain).when(domainConverterCloudV20).fromDomain(newDomain);
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
        doReturn(domain).when(domainConverterCloudV20).fromDomain(newDomain);
        doReturn(newDomain).when(domainConverterCloudV20).toDomain(domain);
        when(uriInfo.getRequestUriBuilder()).thenReturn(UriBuilder.fromPath("path"));
        Response.ResponseBuilder responseBuilder = defaultCloud20Service.updateDomain(authToken, "1", newDomain);
        assertThat("response status", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void setEmptyUserValues() throws Exception {
        User user = new User();
        defaultCloud20Service.setEmptyUserValues(user);
        assertThat("email",user.getEmail(),equalTo(""));
        assertThat("domain",user.getDomainId(),equalTo(""));
        assertThat("region",user.getRegion(),equalTo(""));
    }
}
