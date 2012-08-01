package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.api.resource.cloud.CloudUserExtractor;
import com.rackspace.idm.domain.dao.impl.LdapUserRepository;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.ScopeAccessService;
import com.rackspace.idm.domain.service.impl.DefaultUserService;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.User;
import com.sun.org.apache.xerces.internal.jaxp.datatype.XMLGregorianCalendarImpl;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mortbay.jetty.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.internal.verification.VerificationModeFactory.times;


/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 8/30/11
 * Time: 9:19 PM
 */
public class DelegateCloud11ServiceTest {
    com.rackspace.idm.domain.entity.User idmUser;
    com.rackspace.idm.domain.entity.Users idmUsers;
    List<com.rackspace.idm.domain.entity.User> userList;
    DelegateCloud11Service delegateCloud11Service;
    DefaultCloud11Service defaultCloud11Service;
    com.rackspacecloud.docs.auth.api.v1.ObjectFactory OBJ_FACTORY;
    Configuration config;
    CloudClient cloudClient;
    Marshaller marshaller;
    HttpServletRequest request;
    private CredentialUnmarshaller credentialUnmarshaller;
    private String jsonBody = "{\"credentials\":{\"username\":\"user\",\"key\":\"key\"}}";
    private javax.ws.rs.core.HttpHeaders httpHeaders = mock(javax.ws.rs.core.HttpHeaders.class);
    private BaseURL baseUrl;
    private String url;
    private String userId = "userId";
    private LdapUserRepository ldapUserRepository;
    private CloudUserExtractor cloudUserExtractor;
    private DefaultUserService defaultUserService;
    private DelegateCloud11Service spy;
    private ScopeAccessService scopeAccessService;

    private Response.ResponseBuilder okResponse;
    private Response.ResponseBuilder notFoundResponse;

    @Before
    public void setUp() throws JAXBException {
        idmUser = mock(com.rackspace.idm.domain.entity.User.class);
        idmUsers = new com.rackspace.idm.domain.entity.Users();
        delegateCloud11Service = new DelegateCloud11Service();
        defaultCloud11Service = mock(DefaultCloud11Service.class);
        defaultUserService = mock(DefaultUserService.class);
        credentialUnmarshaller = mock(CredentialUnmarshaller.class);
        cloudUserExtractor = mock(CloudUserExtractor.class);
        scopeAccessService = mock(ScopeAccessService.class);
        delegateCloud11Service.setCredentialUnmarshaller(credentialUnmarshaller);
        delegateCloud11Service.setDefaultCloud11Service(defaultCloud11Service);
        delegateCloud11Service.setCloudUserExtractor(cloudUserExtractor);
        delegateCloud11Service.setDefaultUserService(defaultUserService);
        delegateCloud11Service.setScopeAccessService(scopeAccessService);
        OBJ_FACTORY = mock(com.rackspacecloud.docs.auth.api.v1.ObjectFactory.class);
        DelegateCloud11Service.setObjFactory(OBJ_FACTORY);
        config = mock(Configuration.class);
        delegateCloud11Service.setConfig(config);
        cloudClient = mock(CloudClient.class);
        delegateCloud11Service.setCloudClient(cloudClient);
        marshaller = mock(Marshaller.class);
        baseUrl = new BaseURL();
        baseUrl.setDefault(true);
        baseUrl.setId(1);
        delegateCloud11Service.setMarshaller(marshaller);
        ldapUserRepository = mock(LdapUserRepository.class);
        delegateCloud11Service.setLdapUserRepository(ldapUserRepository);
        request = mock(HttpServletRequest.class);
        when(OBJ_FACTORY.createBaseURL(baseUrl)).thenReturn(new JAXBElement(QName.valueOf("foo"), BaseURL.class, baseUrl));
        when(OBJ_FACTORY.createBaseURLRef(any(BaseURLRef.class))).thenReturn(new JAXBElement<BaseURLRef>(QName.valueOf("foo"), BaseURLRef.class, new BaseURLRef()));
        url = "http://foo.com/";
        when(config.getString("cloudAuth11url")).thenReturn(url);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic YXV0aDphdXRoMTIz");
        spy = spy(delegateCloud11Service);

        userList = new ArrayList<com.rackspace.idm.domain.entity.User>();
        userList.add(idmUser);
        idmUsers.setUsers(userList);

        okResponse = Response.ok();
        notFoundResponse = Response.status(404);
    }

    @Test
    public void authenticate_withJsonBody_callsCredentialUnmarshaller() throws Exception {
        JAXBElement jaxbElement = new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, new UserCredentials());
        when(cloudUserExtractor.getUserByCredentialType(jaxbElement)).thenReturn(null);
        when(defaultCloud11Service.authenticate(null, null, httpHeaders, jsonBody)).thenReturn(Response.status(404));
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(credentialUnmarshaller.unmarshallCredentialsFromJSON(jsonBody)).thenReturn(jaxbElement);
        when(cloudClient.post(eq(url + "auth"), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString())).thenReturn(notFoundResponse);
        when(defaultUserService.isMigratedUser(null)).thenReturn(false);
        delegateCloud11Service.authenticate(null, null, httpHeaders, jsonBody);
        verify(credentialUnmarshaller).unmarshallCredentialsFromJSON(jsonBody);
    }

    @Test
    public void adminAuthenticate_withJsonBody_callsCredentialUnmarshaller() throws Exception {
        JAXBElement jaxbElement = new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, new UserCredentials());
        when(defaultCloud11Service.adminAuthenticate(null, null, httpHeaders, jsonBody)).thenReturn(Response.status(404));
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(credentialUnmarshaller.unmarshallCredentialsFromJSON(jsonBody)).thenReturn(jaxbElement);
        when(cloudClient.post(eq(url + "auth-admin"), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString())).thenReturn(notFoundResponse);
        delegateCloud11Service.adminAuthenticate(null, null, httpHeaders, jsonBody);
        verify(credentialUnmarshaller).unmarshallCredentialsFromJSON(jsonBody);
    }

    @Test
    public void adminAuthenticate_withXmlBody_doesNotCallUnmarshaller() throws Exception {
        JAXBElement jaxbElement = new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, new UserCredentials());
        when(defaultCloud11Service.adminAuthenticate(null, null, httpHeaders, jsonBody)).thenReturn(Response.status(404));
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_XML_TYPE);
        when(credentialUnmarshaller.unmarshallCredentialsFromJSON(jsonBody)).thenReturn(jaxbElement);
        when(cloudClient.post(eq(url + "auth-admin"), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString())).thenReturn(notFoundResponse);
        delegateCloud11Service.adminAuthenticate(null, null, httpHeaders, jsonBody);
        verify(credentialUnmarshaller, never()).unmarshallCredentialsFromJSON(jsonBody);
    }

    @Test
    public void createUser_RoutingFalseAndGAIsNotSourceOfTruth_callsDefaultService() throws Exception {
        User user = new User();
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        when(OBJ_FACTORY.createUser(user)).thenReturn(new JAXBElement<User>(new QName(""), User.class, user));
        delegateCloud11Service.createUser(null, null, null, user);
        verify(defaultCloud11Service).createUser(null, null, null, user);
    }

    @Test
    public void createUser_RoutingFalseAndGAIsSourceOfTruth_callsDefaultService() throws Exception {
        User user = new User();
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        when(OBJ_FACTORY.createUser(user)).thenReturn(new JAXBElement<User>(new QName(""), User.class, user));
        delegateCloud11Service.createUser(null, null, null, user);
        verify(defaultCloud11Service).createUser(null, null, null, user);
    }

    @Test
    public void createUser_RoutingTrueAndGAIsNotSourceOfTruth_callsDefaultService() throws Exception {
        User user = new User();
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        when(OBJ_FACTORY.createUser(user)).thenReturn(new JAXBElement<User>(new QName(""), User.class, user));
        delegateCloud11Service.createUser(null, null, null, user);
        verify(cloudClient).post(eq(url + "users"), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString());
    }

    @Test
    public void createUser_RoutingTrueAndGAIsSourceOfTruth_callsDefaultService() throws Exception {
        User user = new User();
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        when(OBJ_FACTORY.createUser(user)).thenReturn(new JAXBElement<User>(new QName(""), User.class, user));
        delegateCloud11Service.createUser(null, null, null, user);
        verify(defaultCloud11Service).createUser(null, null, null, user);
    }

    @Test
    public void createUser_useCloudAuthFlagSetToTrue_callsConfigGetBoolean() throws Exception {
        User user = new User();
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(OBJ_FACTORY.createUser(user)).thenReturn(new JAXBElement<User>(new QName(""), User.class, user));
        delegateCloud11Service.createUser(null, null, null, user);
        verify(config).getBoolean("useCloudAuth");
    }

    @Test
    public void createUser_useCloudAuthFlagSetToTrue_clientGetsCalled() throws Exception {
        User user = new User();
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(config.getString("cloudAuth11url")).thenReturn("");
        when(OBJ_FACTORY.createUser(user)).thenReturn(new JAXBElement<User>(new QName(""), User.class, user));
        delegateCloud11Service.createUser(null, null, null, user);
        verify(cloudClient).post(eq("users"), any(javax.ws.rs.core.HttpHeaders.class), anyString());
    }

    @Test
    public void createUser_useCloudAuthFlagSetToFalse_clientDoesNotGetCalled() throws Exception {
        User user = new User();
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(config.getString("cloudAuth11url")).thenReturn("");
        when(OBJ_FACTORY.createUser(user)).thenReturn(new JAXBElement<User>(new QName(""), User.class, user));
        delegateCloud11Service.createUser(null, null, null, user);
        verify(cloudClient, VerificationModeFactory.times(0)).post(anyString(), Matchers.<javax.ws.rs.core.HttpHeaders>anyObject(), anyString());
    }

    @Test
    public void addBaseUrl_routingFalseAndGASourceOfTruthFalse_callsDefault() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud11Service.addBaseURL(null, null, null);
        verify(defaultCloud11Service).addBaseURL(null, null, null);
    }

    @Test
    public void addBaseUrl_routingFalseAndGASourceOfTruthTrue_callsDefault() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud11Service.addBaseURL(null, null, null);
        verify(defaultCloud11Service).addBaseURL(null, null, null);
    }

    @Test
    public void addBaseUrl_routingTrueAndGASourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud11Service.addBaseURL(null, null, baseUrl);
        verify(cloudClient).post(eq(url + "baseURLs"), Matchers.<javax.ws.rs.core.HttpHeaders>any(), Matchers.<String>any());
    }

    @Test
    public void addBaseUrl_routingTrueAndGASourceOfTruthTrue_callsDefault() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud11Service.addBaseURL(null, null, null);
        verify(defaultCloud11Service).addBaseURL(null, null, null);
    }

    @Test
    public void addBaseUrl_checksForUseCloudAuthEnable() throws Exception {
        delegateCloud11Service.addBaseURL(null, null, null);
        verify(config).getBoolean("useCloudAuth");
    }

    @Test
    public void addBaseUrl_whenUseCloudAuthEnabled_callsClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        delegateCloud11Service.addBaseURL(null, null, baseUrl);
        verify(cloudClient).post(eq(url + "baseURLs"), any(javax.ws.rs.core.HttpHeaders.class), contains("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><foo default=\"true\" id=\"1\""));
    }

    @Test
    public void addBaseUrl_whenUseCloudAuthDisabled_doesNotCallClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        delegateCloud11Service.addBaseURL(null, null, baseUrl);
        verify(cloudClient, times(0)).post(url + "baseURLs", new HashMap<String,String>(), "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><foo default=\"true\" id=\"1\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\"/>");
    }

    @Test
    public void addBaseUrlRef_checkForUseCloudAuthEnabled() throws Exception {
        delegateCloud11Service.addBaseURLRef(null, userId, null, null, null);
        verify(config).getBoolean("useCloudAuth");
    }

    @Test
    public void addBaseUrlRef_withRoutingOn_callsUserExists() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        delegateCloud11Service.addBaseURLRef(null, userId, null, null, null);
        verify(ldapUserRepository).getUserByUsername(userId);
    }

    @Test
    public void addBaseUrlRef_userExists_callsDefaultService() throws Exception {
        when(ldapUserRepository.getUserById(userId)).thenReturn(new com.rackspace.idm.domain.entity.User(userId));
        delegateCloud11Service.addBaseURLRef(null, userId, null, null, null);
        verify(defaultCloud11Service).addBaseURLRef(null, userId, null, null, null);
    }

    @Test
    public void addBaseUrlRef_userExists_useCloudAuthDisabled_callsDefaultService() throws Exception {
        when(ldapUserRepository.getUserById(userId)).thenReturn(new com.rackspace.idm.domain.entity.User(userId));
        delegateCloud11Service.addBaseURLRef(null, userId, null, null, null);
        verify(defaultCloud11Service).addBaseURLRef(null, userId, null, null, null);
    }

    @Test
    public void addBaseUrlRef_userDoesntExist_useCloudAuthEnabled_callsClient() throws Exception {
        when(ldapUserRepository.getUserById(userId)).thenReturn(null);
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        delegateCloud11Service.addBaseURLRef(null, userId, null, null, null);
        verify(cloudClient).post(anyString(), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString());
    }

    @Test
    public void getBaseUrls_useCloudAuthDisabled_callsDefaultService() throws Exception {
        delegateCloud11Service.getBaseURLs(null, "service", null);
        verify(defaultCloud11Service).getBaseURLs(null, "service", null);
    }

    @Test
    public void getBaseUrls_useCloudAuthEnabled_callsClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        delegateCloud11Service.getBaseURLs(null, "service", null);
        verify(cloudClient).get(eq(url + "baseURLs?serviceName=service"), Matchers.<javax.ws.rs.core.HttpHeaders>any());
    }

    @Test
    public void getBaseUrlRefs_useCloudAuthDisabled_callsDefaultService() throws Exception {
        delegateCloud11Service.getBaseURLRefs(null, userId, null);
        verify(defaultCloud11Service).getBaseURLRefs(null, userId, null);
    }

    @Test
    public void getBaseUrlRefs_useCloudAuthEnabledAndUserDoesNotExistsInGA_callsClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(ldapUserRepository.getUserById(userId)).thenReturn(null);
        delegateCloud11Service.getBaseURLRefs(null, userId, null);
        verify(cloudClient).get(eq(url + "users/" + userId + "/baseURLRefs"), Matchers.<javax.ws.rs.core.HttpHeaders>any());
    }

    @Test
    public void getBaseUrlRefs_useCloudAuthEnabledAndUserExistsInGA_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getBaseURLRefs(null, userId, null);
        verify(defaultCloud11Service).getBaseURLRefs(null, userId, null);
    }

    @Test
    public void getUserGroups_routingDisabled_callsDefaultService() throws Exception {
        delegateCloud11Service.getUserGroups(null, userId, null);
        verify(defaultCloud11Service).getUserGroups(null, userId, null);
    }

    @Test
    public void getUserGroups_RoutingFalseAndUserDoesNotExistInGA_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(ldapUserRepository.getUserById(userId)).thenReturn(null);
        delegateCloud11Service.getUserGroups(null, userId, null);
        verify(defaultCloud11Service).getUserGroups(null, userId, null);
    }

    @Test
    public void getUserGroups_RoutingFalseAndUserExistsInGA_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(ldapUserRepository.getUserById(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getUserGroups(null, userId, null);
        verify(defaultCloud11Service).getUserGroups(null, userId, null);
    }

    @Test
    public void getUserGroups_RoutingTrueAndUserDoesNotExistsInGA_callsClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(ldapUserRepository.getUserById(userId)).thenReturn(null);
        delegateCloud11Service.getUserGroups(null, userId, null);
        verify(cloudClient).get(eq(url + "users/" + userId + "/groups"), Matchers.<javax.ws.rs.core.HttpHeaders>any());
    }

    @Test
    public void getUserGroups_RoutingTrueAndUserExistsInGA_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getUserGroups(null, userId, null);
        verify(defaultCloud11Service).getUserGroups(null, userId, null);
    }

    @Test
    public void deleteBaseURLRef_RoutingFalseAndUserDoesNotExistInGA_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(ldapUserRepository.getUserById(userId)).thenReturn(null);
        delegateCloud11Service.deleteBaseURLRef(null, userId, null, null);
        verify(defaultCloud11Service).deleteBaseURLRef(null, userId, null, null);
    }

    @Test
    public void deleteBaseURLRef_RoutingFalseAndUserExistsInGA_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(ldapUserRepository.getUserById(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.deleteBaseURLRef(null, userId, null, null);
        verify(defaultCloud11Service).deleteBaseURLRef(null, userId, null, null);
    }

    @Test
    public void deleteBaseURLRef_RoutingTrueAndUserDoesNotExistsInGA_callsClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(ldapUserRepository.getUserById(userId)).thenReturn(null);
        delegateCloud11Service.deleteBaseURLRef(null, userId, "id", null);
        verify(cloudClient).delete(eq(url + "users/" + userId + "/baseURLRefs/id"), Matchers.<javax.ws.rs.core.HttpHeaders>any());
    }

    @Test
    public void deleteBaseURLRef_RoutingTrueAndUserExistsInGA_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.deleteBaseURLRef(null, userId, null, null);
        verify(defaultCloud11Service).deleteBaseURLRef(null, userId, null, null);
    }

    @Test
    public void getBaseURLRef_RoutingFalseAndUserDoesNotExistInGA_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(ldapUserRepository.getUserById(userId)).thenReturn(null);
        delegateCloud11Service.getBaseURLRef(null, userId, null, null);
        verify(defaultCloud11Service).getBaseURLRef(null, userId, null, null);
    }

    @Test
    public void getBaseURLRef_RoutingFalseAndUserExistsInGA_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(ldapUserRepository.getUserById(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getBaseURLRef(null, userId, null, null);
        verify(defaultCloud11Service).getBaseURLRef(null, userId, null, null);
    }

    @Test
    public void getBaseURLRef_RoutingTrueAndUserDoesNotExistsInGA_callsClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(ldapUserRepository.getUserById(userId)).thenReturn(null);
        delegateCloud11Service.getBaseURLRef(null, userId, "id", null);
        verify(cloudClient).get(eq(url + "users/" + userId + "/baseURLRefs/id"), Matchers.<javax.ws.rs.core.HttpHeaders>any());
    }

    @Test
    public void getBaseURLRef_RoutingTrueAndUserExistsInGA_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getBaseURLRef(null, userId, null, null);
        verify(defaultCloud11Service).getBaseURLRef(null, userId, null, null);
    }

    @Test
    public void addBaseURLRef_RoutingFalseAndUserDoesNotExistInGA_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(ldapUserRepository.getUserById(userId)).thenReturn(null);
        delegateCloud11Service.addBaseURLRef(null, userId, null, null, null);
        verify(defaultCloud11Service).addBaseURLRef(null, userId, null, null, null);
    }

    @Test
    public void addBaseURLRef_RoutingFalseAndUserExistsInGA_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(ldapUserRepository.getUserById(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.addBaseURLRef(null, userId, null, null, null);
        verify(defaultCloud11Service).addBaseURLRef(null, userId, null, null, null);
    }

    @Test
    public void addBaseURLRef_RoutingTrueAndUserDoesNotExistsInGA_callsClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(ldapUserRepository.getUserById(userId)).thenReturn(null);
        delegateCloud11Service.addBaseURLRef(null, userId, null, null, null);
        verify(cloudClient).post(eq(url + "users/" + userId + "/baseURLRefs"), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString());
    }

    @Test
    public void addBaseURLRef_RoutingTrueAndUserExistsInGA_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.addBaseURLRef(null, userId, null, null, null);
        verify(defaultCloud11Service).addBaseURLRef(null, userId, null, null, null);
    }

    @Test
    public void getUserEnabled_RoutingFalseAndUserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(null);
        delegateCloud11Service.getUserEnabled(null, userId, null);
        verify(defaultCloud11Service).getUserEnabled(null, userId, null);
    }

    @Test
    public void getUserEnabled_RoutingFalseAndUserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getUserEnabled(null, userId, null);
        verify(defaultCloud11Service).getUserEnabled(null, userId, null);
    }

    @Test
    public void getUserEnabled_RoutingTrueAndUserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(null);
        javax.ws.rs.core.HttpHeaders mockHeaders = mock(javax.ws.rs.core.HttpHeaders.class);
        delegateCloud11Service.getUserEnabled(null, userId, mockHeaders);
        verify(cloudClient).get(url + "users/" + userId + "/enabled", mockHeaders);
    }

    @Test
    public void getUserEnabled_RoutingTrueAndUserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getUserEnabled(null, userId, null);
        verify(defaultCloud11Service).getUserEnabled(null, userId, null);
    }

    @Test
    public void getServiceCatalog_routingFalse_userExistsInGAFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        doReturn(false).when(spy).userExistsInGA(userId);
        spy.getServiceCatalog(null, userId, null);
        verify(defaultCloud11Service).getServiceCatalog(null, userId, null);
    }

    @Test
    public void getServiceCatalog_routingFalse_userExistsInGATrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        doReturn(true).when(spy).userExistsInGA(userId);
        spy.getServiceCatalog(null, userId, null);
        verify(defaultCloud11Service).getServiceCatalog(null, userId, null);
    }

    @Test
    public void getServiceCatalog_routingTrue_userExistsInGAFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        doReturn(false).when(spy).userExistsInGA(userId);
        javax.ws.rs.core.HttpHeaders mockHeaders = mock(javax.ws.rs.core.HttpHeaders.class);
        spy.getServiceCatalog(null, userId, mockHeaders);
        verify(cloudClient).get(url + "users/" + userId + "/serviceCatalog", mockHeaders);
    }

    @Test
    public void getServiceCatalog_routingTrue_userExistsInGATrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        doReturn(true).when(spy).userExistsInGA(userId);
        spy.getServiceCatalog(null, userId, null);
        verify(defaultCloud11Service).getServiceCatalog(null, userId, null);
    }

    @Test
    public void setUserKey_routingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(null);
        delegateCloud11Service.setUserKey(null, userId, null, null);
        verify(defaultCloud11Service).setUserKey(null, userId, null, null);
    }

    @Test
    public void setUserKey_routingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.setUserKey(null, userId, null, null);
        verify(defaultCloud11Service).setUserKey(null, userId, null, null);
    }

    @Test
    public void setUserKey_routingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(null);
        UserWithOnlyKey user = new UserWithOnlyKey();
        user.setKey("key");
        when(OBJ_FACTORY.createUser(user)).thenReturn(new JAXBElement<User>(QName.valueOf("name"), User.class, null));
        delegateCloud11Service.setUserKey(null, userId, null, user);
        verify(cloudClient).put(eq(url + "users/" + userId + "/key"), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString());
    }

    @Test
    public void setUserKey_routingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.setUserKey(null, userId, null, null);
        verify(defaultCloud11Service).setUserKey(null, userId, null, null);
    }

    @Test
    public void deleteUser_routingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(null);
        delegateCloud11Service.deleteUser(null, userId, null);
        verify(defaultCloud11Service).deleteUser(null, userId, null);
    }

    @Test
    public void deleteUser_routingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.deleteUser(null, userId, null);
        verify(defaultCloud11Service).deleteUser(null, userId, null);
    }

    @Test
    public void deleteUser_routingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(null);
        delegateCloud11Service.deleteUser(null, userId, null);
        verify(cloudClient).delete(eq(url + "users/" + userId), Matchers.<javax.ws.rs.core.HttpHeaders>any());
    }

    @Test
    public void deleteUser_routingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        com.rackspace.idm.domain.entity.User user = new com.rackspace.idm.domain.entity.User();
        user.setId(userId);
        when(defaultUserService.getUserById(userId)).thenReturn(user);
        when(defaultUserService.isMigratedUser(Matchers.<com.rackspace.idm.domain.entity.User>any())).thenReturn(true);
        delegateCloud11Service.deleteUser(null, userId, null);
        verify(defaultCloud11Service).deleteUser(null, userId, null);
    }

    @Test
    public void deleteUser_userNotMigratedUser_callsDefaultCloud11DeleteUser() throws Exception {
        com.rackspace.idm.domain.entity.User user = new com.rackspace.idm.domain.entity.User();
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(defaultUserService.getUserById("userId")).thenReturn(user);
        when(defaultUserService.isMigratedUser(user)).thenReturn(false);
        delegateCloud11Service.deleteUser(null, "userId", httpHeaders);
        verify(defaultCloud11Service).deleteUser(null, "userId", httpHeaders);
    }

    @Test
    public void getUserKey_routingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(null);
        delegateCloud11Service.getUserKey(null, userId, null);
        verify(defaultCloud11Service).getUserKey(null, userId, null);
    }

    @Test
    public void getUserKey_routingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getUserKey(null, userId, null);
        verify(defaultCloud11Service).getUserKey(null, userId, null);
    }

    @Test
    public void getUserKey_routingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(null);
        delegateCloud11Service.getUserKey(null, userId, null);
        verify(cloudClient).get(eq(url + "users/" + userId + "/key"), Matchers.<javax.ws.rs.core.HttpHeaders>any());
    }

    @Test
    public void getUserKey_routingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getUserKey(null, userId, null);
        verify(defaultCloud11Service).getUserKey(null, userId, null);
    }

    @Test
    public void setUserEnabled_routingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(null);
        delegateCloud11Service.setUserEnabled(null, userId, null, null);
        verify(defaultCloud11Service).setUserEnabled(null, userId, null, null);
    }

    @Test
    public void setUserEnabled_routingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.setUserEnabled(null, userId, null, null);
        verify(defaultCloud11Service).setUserEnabled(null, userId, null, null);
    }

    @Test
    public void setUserEnabled_routingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(null);
        UserWithOnlyEnabled userWithOnlyEnabled = new UserWithOnlyEnabled();
        when(OBJ_FACTORY.createUser(userWithOnlyEnabled)).thenReturn(new JAXBElement<User>(QName.valueOf("name"), User.class, null));
        delegateCloud11Service.setUserEnabled(null, userId, userWithOnlyEnabled, null);
        verify(cloudClient).put(eq(url + "users/" + userId + "/enabled"), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString());
    }

    @Test
    public void setUserEnabled_routingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.setUserEnabled(null, userId, null, null);
        verify(defaultCloud11Service).setUserEnabled(null, userId, null, null);
    }

    @Test
    public void setUserEnabled_callsOBJ_FACTORY_createUser() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(null);
        UserWithOnlyEnabled userWithOnlyEnabled = new UserWithOnlyEnabled();
        when(OBJ_FACTORY.createUser(userWithOnlyEnabled)).thenReturn(new JAXBElement<User>(QName.valueOf("name"), User.class, null));
        delegateCloud11Service.setUserEnabled(null, userId, userWithOnlyEnabled, null);
        verify(OBJ_FACTORY).createUser(Matchers.<User>any());
    }

    @Test
    public void updateUser_RoutingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(null);
        delegateCloud11Service.updateUser(null,userId,null,null);
        verify(defaultCloud11Service).updateUser(null,userId,null,null);
    }

    @Test
    public void updateUser_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.updateUser(null,userId,null,null);
        verify(defaultCloud11Service).updateUser(null,userId,null,null);
    }

    @Test
    public void updateUser_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(null);
        User value = new User();
        when(OBJ_FACTORY.createUser(value)).thenReturn(new JAXBElement<User>(QName.valueOf("name"), User.class, null));
        delegateCloud11Service.updateUser(null,userId,null,value);
        verify(cloudClient).put(eq(url + "users/" + userId), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString());
    }

    @Test
    public void updateUser_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.updateUser(null,userId,null,null);
        verify(defaultCloud11Service).updateUser(null,userId,null,null);
    }

    @Test
    public void getUser_RoutingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(null);
        delegateCloud11Service.getUser(null, userId, null);
        verify(defaultCloud11Service).getUser(null, userId, null);
    }

    @Test
    public void getUser_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getUser(null, userId, null);
        verify(defaultCloud11Service).getUser(null, userId, null);
    }

    @Test
    public void getUser_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(null);
        delegateCloud11Service.getUser(null, userId, null);
        verify(cloudClient).get(eq(url + "users/" + userId), Matchers.<javax.ws.rs.core.HttpHeaders>any());
    }

    @Test
    public void getUser_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getUser(null, userId, null);
        verify(defaultCloud11Service).getUser(null, userId, null);
    }

    @Test
    public void getUserFromNastId_RoutingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUsersByNastId(userId)).thenReturn(null);
        delegateCloud11Service.getUserFromNastId(null, userId, null);
        verify(defaultCloud11Service).getUserFromNastId(null, userId, null);
    }

    @Test
    public void getUserFromNastId_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUsersByNastId(userId)).thenReturn(new com.rackspace.idm.domain.entity.Users());
        delegateCloud11Service.getUserFromNastId(null, userId, null);
        verify(defaultCloud11Service).getUserFromNastId(null, userId, null);
    }

    @Test
    public void getUserFromNastId_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUsersByNastId(userId)).thenReturn(new Users());
        delegateCloud11Service.getUserFromNastId(null, userId, null);
        verify(cloudClient).get(eq(url + "nast/" + userId), Matchers.<javax.ws.rs.core.HttpHeaders>any());
    }

    @Test
    public void getUserFromNastId_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUsersByNastId(userId)).thenReturn(idmUsers);
        delegateCloud11Service.getUserFromNastId(null, userId, null);
        verify(defaultCloud11Service).getUserFromNastId(null, userId, null);
    }

    @Test
    public void getUserFromMossoId_RoutingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUsersByMossoId(0)).thenReturn(null);
        delegateCloud11Service.getUserFromMossoId(null, 0, null);
        verify(defaultCloud11Service).getUserFromMossoId(null, 0, null);
    }

    @Test
    public void getUserFromMossoId_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUsersByMossoId(0)).thenReturn(new com.rackspace.idm.domain.entity.Users());
        delegateCloud11Service.getUserFromMossoId(null, 0, null);
        verify(defaultCloud11Service).getUserFromMossoId(null, 0, null);
    }

    @Test
    public void getUserFromMossoId_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        idmUsers.getUsers().remove(0);
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUsersByMossoId(0)).thenReturn(idmUsers);
        delegateCloud11Service.getUserFromMossoId(null, 0, null);
        verify(cloudClient).get(eq(url + "mosso/" + 0), Matchers.<javax.ws.rs.core.HttpHeaders>any());
    }

    @Test
    public void getUserFromMossoId_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUsersByMossoId(0)).thenReturn(idmUsers);
        delegateCloud11Service.getUserFromMossoId(null, 0, null);
        verify(defaultCloud11Service).getUserFromMossoId(null, 0, null);
    }

    @Test
    public void getBaseURLId_routingFalse_gaSourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud11Service.getBaseURLById(null, 0, null, null);
        verify(defaultCloud11Service).getBaseURLById(null, 0, null, null);
    }

    @Test
    public void getBaseURLId_routingFalse_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud11Service.getBaseURLById(null, 0, null, null);
        verify(defaultCloud11Service).getBaseURLById(null, 0, null, null);
    }

    @Test
    public void getBaseURLId_routingTrue_gaSourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        javax.ws.rs.core.HttpHeaders mockHeaders = mock(javax.ws.rs.core.HttpHeaders.class);
        delegateCloud11Service.getBaseURLById(null, 0, null, mockHeaders);
        verify(cloudClient).get(url + "baseURLs/" + 0, mockHeaders);
    }

    @Test
    public void getBaseURLId_routingTrue_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud11Service.getBaseURLById(null, 0, null, null);
        verify(defaultCloud11Service).getBaseURLById(null, 0, null, null);
    }

    @Test
    public void getEnabledBaseURL_routingFalse_gaSourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud11Service.getEnabledBaseURL(null, null, null);
        verify(defaultCloud11Service).getEnabledBaseURL(null, null, null);
    }

    @Test
    public void getEnabledBaseURL_routingFalse_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud11Service.getEnabledBaseURL(null, null, null);
        verify(defaultCloud11Service).getEnabledBaseURL(null, null, null);
    }

    @Test
    public void getEnabledBaseURL_routingTrue_gaSourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        javax.ws.rs.core.HttpHeaders mockHeaders = mock(javax.ws.rs.core.HttpHeaders.class);
        delegateCloud11Service.getEnabledBaseURL(null, null, mockHeaders);
        verify(cloudClient).get(url + "baseURLs/enabled", mockHeaders);
    }

    @Test
    public void getEnabledBaseURL_routingTrue_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud11Service.getEnabledBaseURL(null, null, null);
        verify(defaultCloud11Service).getEnabledBaseURL(null, null, null);
    }

    @Test
    public void getBaseURLs_routingFalse_gaSourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud11Service.getBaseURLs(null, null, null);
        verify(defaultCloud11Service).getBaseURLs(null, null, null);
    }

    @Test
    public void getBaseURLs_routingFalse_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud11Service.getBaseURLs(null, null, null);
        verify(defaultCloud11Service).getBaseURLs(null, null, null);
    }

    @Test
    public void getBaseURLs_routingTrue_gaSourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        javax.ws.rs.core.HttpHeaders mockHeaders = mock(javax.ws.rs.core.HttpHeaders.class);
        delegateCloud11Service.getBaseURLs(null, "serviceFoo", mockHeaders);
        verify(cloudClient).get(url + "baseURLs?serviceName=serviceFoo", mockHeaders);
    }

    @Test
    public void getBaseURLs_routingTrue_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud11Service.getBaseURLs(null, null, null);
        verify(defaultCloud11Service).getBaseURLs(null, null, null);
    }

    @Test
    public void validateToken_HttpServletResponseNotFound_callsCloudClient() throws Exception {
        Response.ResponseBuilder responseClone = mock(Response.ResponseBuilder.class);
        when(defaultCloud11Service.validateToken(request, "tokenId", "belongTo", "type", httpHeaders)).thenReturn(responseClone);
        when(responseClone.clone()).thenReturn(responseClone);
        when(responseClone.build()).thenReturn(Response.status(404).build());
        delegateCloud11Service.validateToken(request, "tokenId", "belongTo", "type", httpHeaders);
        verify(cloudClient).get(url + "token/tokenId?type=type&belongsTo=belongTo", httpHeaders);
    }

    @Test
    public void validateToken_HttpServletResponseUnauthorized_callsCloudClient() throws Exception {
        Response.ResponseBuilder responseClone = mock(Response.ResponseBuilder.class);
        when(defaultCloud11Service.validateToken(request, "tokenId", "belongTo", "type", httpHeaders)).thenReturn(responseClone);
        when(responseClone.clone()).thenReturn(responseClone);
        when(responseClone.build()).thenReturn(Response.status(401).build());
        delegateCloud11Service.validateToken(request, "tokenId", "belongTo", "type", httpHeaders);
        verify(cloudClient).get(url + "token/tokenId?type=type&belongsTo=belongTo", httpHeaders);
    }

    @Test
    public void validateToken_HttpServletResponseFound_callsServiceResponse() throws Exception {
        Response.ResponseBuilder responseClone = mock(Response.ResponseBuilder.class);
        when(defaultCloud11Service.validateToken(request, "tokenId", "belongTo", "type", httpHeaders)).thenReturn(responseClone);
        when(responseClone.clone()).thenReturn(responseClone);
        when(responseClone.build()).thenReturn(Response.ok().build());
        Response.ResponseBuilder response = delegateCloud11Service.validateToken(request, "tokenId", "belongTo", "type", httpHeaders);
        assertThat("Reponse Code", response, equalTo(responseClone));
    }

    @Test
    public void authenticate_isMigratedUser_callsCloud11ServiceAuthenticate() throws Exception {
        JAXBElement<? extends Credentials> jaxbElement = new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, new UserCredentials());
        when(cloudUserExtractor.getUserByCredentialType(jaxbElement)).thenReturn(null);
        when(defaultUserService.isMigratedUser(null)).thenReturn(true);
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        delegateCloud11Service.authenticate(null, null, httpHeaders, jsonBody);
        verify(defaultCloud11Service).authenticate(null, null, httpHeaders, jsonBody);
    }

    @Test
    public void authenticate_statusIs302_callsServiceResponseLocation() throws Exception {
        Response.ResponseBuilder response = mock(Response.ResponseBuilder.class);
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        com.rackspace.idm.domain.entity.User user = mock(com.rackspace.idm.domain.entity.User.class);
        JAXBElement jaxbElement = new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, new UserCredentials());
        when(cloudUserExtractor.getUserByCredentialType(jaxbElement)).thenReturn(user);
        when(defaultUserService.isMigratedUser(user)).thenReturn(false);
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(defaultCloud11Service.authenticate(request, httpServletResponse, httpHeaders, jsonBody)).thenReturn(response);
        when(credentialUnmarshaller.unmarshallCredentialsFromJSON(jsonBody)).thenReturn(jaxbElement);
        when(cloudClient.post(eq(url + "auth"), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString())).thenReturn(response);
        when(response.clone()).thenReturn(Response.status(302).clone());
        delegateCloud11Service.authenticate(request, httpServletResponse, httpHeaders, jsonBody);
        verify(response).location(new URI(config.getString("ga.endpoint")+"cloud/v1.1/auth-admin"));
    }

    @Test
    public void authenticate_statusIsOKAndUserIsNotNullAndAuthResultIsNull_returnsServiceResponse() throws Exception {
        Response.ResponseBuilder response = Response.status(200).entity("");
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        com.rackspace.idm.domain.entity.User user = new com.rackspace.idm.domain.entity.User("username");
        JAXBElement jaxbElement = new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, new UserCredentials());
        when(cloudUserExtractor.getUserByCredentialType(jaxbElement)).thenReturn(user);
        when(defaultUserService.isMigratedUser(user)).thenReturn(false);
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(defaultCloud11Service.authenticate(request, httpServletResponse, httpHeaders, jsonBody)).thenReturn(response);
        when(credentialUnmarshaller.unmarshallCredentialsFromJSON(jsonBody)).thenReturn(jaxbElement);
        when(cloudClient.post(eq(url + "auth"), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString())).thenReturn(response);
        doReturn(null).when(spy).getAuthFromResponse("");
        Response.ResponseBuilder authenticate = delegateCloud11Service.authenticate(request, httpServletResponse, httpHeaders, jsonBody);
        assertThat("Response Code", authenticate.build().getStatus(), equalTo(200));
    }

    @Test
    public void authenticate_statusIsOKAndUserIsNotNullAndAuthResultIsNotNull_returnsServiceResponse() throws Exception {
        Response.ResponseBuilder response = Response.status(200).entity("notNull");
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        XMLGregorianCalendar xmlGregorianCalendar = new XMLGregorianCalendarImpl();
        Token token = new Token();
        token.setId("tokenId");
        token.setExpires(xmlGregorianCalendar);
        com.rackspacecloud.docs.auth.api.v1.AuthData authResult = new com.rackspacecloud.docs.auth.api.v1.AuthData();
        authResult.setToken(token);
        com.rackspace.idm.domain.entity.User user = new com.rackspace.idm.domain.entity.User("username");
        JAXBElement jaxbElement = new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, new UserCredentials());
        when(cloudUserExtractor.getUserByCredentialType(jaxbElement)).thenReturn(user);
        when(defaultUserService.isMigratedUser(user)).thenReturn(false);
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(defaultCloud11Service.authenticate(request, httpServletResponse, httpHeaders, jsonBody)).thenReturn(response);
        when(credentialUnmarshaller.unmarshallCredentialsFromJSON(jsonBody)).thenReturn(jaxbElement);
        when(cloudClient.post(eq(url + "auth"), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString())).thenReturn(response);
        doReturn(authResult).when(spy).getAuthFromResponse(anyString());
        spy.authenticate(request, httpServletResponse, httpHeaders, jsonBody);
        verify(scopeAccessService).updateUserScopeAccessTokenForClientIdByUser(user, null, "tokenId", xmlGregorianCalendar.toGregorianCalendar().getTime());
    }

    @Test
    public void authenticate_statusIsNotOkAndUserIsNotNull_returnsCloud11ServiceAuthenticate() throws Exception {
        Response.ResponseBuilder response = Response.status(300)      ;
        Response.ResponseBuilder responseClone = mock(Response.ResponseBuilder.class);
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        Response dummyResponse = mock(Response.class);
        com.rackspace.idm.domain.entity.User user = new com.rackspace.idm.domain.entity.User("username");
        JAXBElement jaxbElement = new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, new UserCredentials());
        when(cloudUserExtractor.getUserByCredentialType(jaxbElement)).thenReturn(user);
        when(defaultUserService.isMigratedUser(user)).thenReturn(false);
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(defaultCloud11Service.authenticate(request, httpServletResponse, httpHeaders, jsonBody)).thenReturn(response);
        when(credentialUnmarshaller.unmarshallCredentialsFromJSON(jsonBody)).thenReturn(jaxbElement);
        when(cloudClient.post(eq(url + "auth"), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString())).thenReturn(response);
        when(responseClone.build()).thenReturn(dummyResponse);
        when(dummyResponse.getStatus()).thenReturn(300);
        delegateCloud11Service.authenticate(request, httpServletResponse, httpHeaders, jsonBody);
        verify(defaultCloud11Service).authenticate(request, httpServletResponse, httpHeaders, jsonBody);
    }

    @Test
    public void adminAuthenticate_statusIsOKAndUserIsNotNullAndAuthResultIsNull_returnsServiceResponse() throws Exception {
        Response.ResponseBuilder response = Response.status(200).entity("");
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        com.rackspace.idm.domain.entity.User user = new com.rackspace.idm.domain.entity.User("username");
        JAXBElement jaxbElement = new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, new UserCredentials());
        when(cloudUserExtractor.getUserByCredentialType(jaxbElement)).thenReturn(user);
        when(defaultUserService.isMigratedUser(user)).thenReturn(false);
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(defaultCloud11Service.authenticate(request, httpServletResponse, httpHeaders, jsonBody)).thenReturn(response);
        when(credentialUnmarshaller.unmarshallCredentialsFromJSON(jsonBody)).thenReturn(jaxbElement);
        when(cloudClient.post(eq(url + "auth-admin"), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString())).thenReturn(response);
        doReturn(null).when(spy).getAuthFromResponse("");
        Response.ResponseBuilder adminAuthenticate = delegateCloud11Service.adminAuthenticate(request, httpServletResponse, httpHeaders, jsonBody);
        assertThat("Response Code", adminAuthenticate.build().getStatus(), equalTo(200));
    }

    @Test
    public void adminAuthenticate_statusIsOKAndUserIsNotNullAndAuthResultIsNotNull_returnsServiceResponse() throws Exception {
        Response.ResponseBuilder response = Response.status(200).entity("notNull");
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        XMLGregorianCalendar xmlGregorianCalendar = new XMLGregorianCalendarImpl();
        Token token = new Token();
        token.setId("tokenId");
        token.setExpires(xmlGregorianCalendar);
        com.rackspacecloud.docs.auth.api.v1.AuthData authResult = new com.rackspacecloud.docs.auth.api.v1.AuthData();
        authResult.setToken(token);
        com.rackspace.idm.domain.entity.User user = new com.rackspace.idm.domain.entity.User("username");
        JAXBElement jaxbElement = new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, new UserCredentials());
        when(cloudUserExtractor.getUserByCredentialType(jaxbElement)).thenReturn(user);
        when(defaultUserService.isMigratedUser(user)).thenReturn(false);
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(defaultCloud11Service.adminAuthenticate(request, httpServletResponse, httpHeaders, jsonBody)).thenReturn(response);
        when(credentialUnmarshaller.unmarshallCredentialsFromJSON(jsonBody)).thenReturn(jaxbElement);
        when(cloudClient.post(eq(url + "auth-admin"), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString())).thenReturn(response);
        doReturn(authResult).when(spy).getAuthFromResponse(anyString());
        spy.adminAuthenticate(request, httpServletResponse, httpHeaders, jsonBody);
        verify(scopeAccessService).updateUserScopeAccessTokenForClientIdByUser(user, null, "tokenId", xmlGregorianCalendar.toGregorianCalendar().getTime());
    }

    @Test
    public void adminAuthenticate_statusIsNotOkAndUserIsNotNull_returnsCloud11ServiceAuthenticate() throws Exception {
        Response.ResponseBuilder response = Response.status(300)      ;
        Response.ResponseBuilder responseClone = mock(Response.ResponseBuilder.class);
        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        Response dummyResponse = mock(Response.class);
        com.rackspace.idm.domain.entity.User user = new com.rackspace.idm.domain.entity.User("username");
        JAXBElement jaxbElement = new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, new UserCredentials());
        when(cloudUserExtractor.getUserByCredentialType(jaxbElement)).thenReturn(user);
        when(defaultUserService.isMigratedUser(user)).thenReturn(false);
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        when(defaultCloud11Service.adminAuthenticate(request, httpServletResponse, httpHeaders, jsonBody)).thenReturn(response);
        when(credentialUnmarshaller.unmarshallCredentialsFromJSON(jsonBody)).thenReturn(jaxbElement);
        when(cloudClient.post(eq(url + "auth-admin"), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString())).thenReturn(response);
        when(responseClone.build()).thenReturn(dummyResponse);
        when(dummyResponse.getStatus()).thenReturn(300);
        delegateCloud11Service.adminAuthenticate(request, httpServletResponse, httpHeaders, jsonBody);
        verify(defaultCloud11Service).adminAuthenticate(request, httpServletResponse, httpHeaders, jsonBody);
    }

    @Test
    public void revokeToken_callsCloudClient_delete() throws Exception {
        when(cloudClient.delete(url+"token/null", null)).thenReturn(Response.ok());
        spy.revokeToken(null, null, null);
        verify(cloudClient).delete(url+"token/null", null);
    }

    @Test
    public void revokeToken_callsDefaultCloud11Service_revokeToken() throws Exception {
        when(cloudClient.delete(url+"token/null", null)).thenReturn(Response.ok());
        spy.revokeToken(null, null, null);
        verify(defaultCloud11Service).revokeToken(null, null, null);
    }

    @Test
    public void revokeToken_httpServletResponseNoContent_returnsCloudResponse() throws Exception {
        when(cloudClient.delete(url+"token/null", null)).thenReturn(Response.status(204));
        Response.ResponseBuilder result = spy.revokeToken(null, null, null);
        assertThat("response code", result.build().getStatus(), equalTo(204));
    }

    @Test
    public void revokeToken_httpServletResponseNotNoContent_returnsDefaultResponse() throws Exception {
        when(cloudClient.delete(url+"token/null", null)).thenReturn(Response.status(401));
        doReturn(Response.ok()).when(spy).revokeToken(null, null, null);
        Response.ResponseBuilder result = spy.revokeToken(null, null, null);
        assertThat("response code", result.build().getStatus(), equalTo(200));
    }

    @Test
    public void extensions_cloudRoutingEnabledAndGASourceOfTruthNotEnabled_callsCloudClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(config.getBoolean("gaIsSourceOfTruth")).thenReturn(false);
        delegateCloud11Service.extensions(httpHeaders);
        verify(cloudClient).get(url+"extensions", httpHeaders);
    }

    @Test
    public void extensions_cloudRoutingEnabledAndGASourceOfTruthEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(config.getBoolean("gaIsSourceOfTruth")).thenReturn(true);
        delegateCloud11Service.extensions(httpHeaders);
        verify(defaultCloud11Service).extensions(httpHeaders);
    }

    @Test
    public void extensions_cloudRoutingNotEnabledAndGASourceOfTruthEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(config.getBoolean("gaIsSourceOfTruth")).thenReturn(true);
        delegateCloud11Service.extensions(httpHeaders);
        verify(defaultCloud11Service).extensions(httpHeaders);
    }

    @Test
    public void extensions_cloudRoutingNotEnabledAndGASourceOfTruthNotEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(config.getBoolean("gaIsSourceOfTruth")).thenReturn(false);
        delegateCloud11Service.extensions(httpHeaders);
        verify(defaultCloud11Service).extensions(httpHeaders);
    }

    @Test
    public void getExtension_cloudRoutingEnabledAndGASourceOfTruthNotEnabled_callsCloudClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(config.getBoolean("gaIsSourceOfTruth")).thenReturn(false);
        delegateCloud11Service.getExtension(httpHeaders,"EXAMPLE");
        verify(cloudClient).get(url+"extensions/EXAMPLE", httpHeaders);
    }

    @Test
    public void getExtension_cloudRoutingEnabledAndGASourceOfTruthEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(config.getBoolean("gaIsSourceOfTruth")).thenReturn(true);
        delegateCloud11Service.getExtension(httpHeaders,"EXAMPLE");
        verify(defaultCloud11Service).extensions(httpHeaders);
    }

    @Test
    public void getExtension_cloudRoutingNotEnabledAndGASourceOfTruthEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(config.getBoolean("gaIsSourceOfTruth")).thenReturn(true);
        delegateCloud11Service.getExtension(httpHeaders,"EXAMPLE");
        verify(defaultCloud11Service).extensions(httpHeaders);
    }

    @Test
    public void getExtension_cloudRoutingNotEnabledAndGASourceOfTruthNotEnabled_callsDefaultService() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(config.getBoolean("gaIsSourceOfTruth")).thenReturn(false);
        delegateCloud11Service.getExtension(httpHeaders,"EXAMPLE");
        verify(defaultCloud11Service).extensions(httpHeaders);
    }

    @Test
    public void userExistsInGAByMossoId_getUsersIsNull_returnsFalse() throws Exception {
        when(ldapUserRepository.getUsersByMossoId(1)).thenReturn(new Users());
        boolean result = delegateCloud11Service.userExistsInGAByMossoId(1);
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void userExistsInGAByMossoId_getUsersIsNull_returnsTrue() throws Exception {
        Users users = new Users();
        ArrayList<com.rackspace.idm.domain.entity.User> userList = new ArrayList<com.rackspace.idm.domain.entity.User>();
        userList.add(new com.rackspace.idm.domain.entity.User());
        users.setUsers(userList);
        when(ldapUserRepository.getUsersByMossoId(1)).thenReturn(users);
        boolean result = delegateCloud11Service.userExistsInGAByMossoId(1);
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void userExistsInGAByNastId_usersSizeIs0_returnsFalse() throws Exception {
        Users users = new Users();
        users.setUsers(new ArrayList<com.rackspace.idm.domain.entity.User>());
        when(ldapUserRepository.getUsersByNastId("nastId")).thenReturn(users);
        boolean result = delegateCloud11Service.userExistsInGAByNastId("nastId");
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void userExistsInGAByNastId_usersSizeNot0_returnsTrue() throws Exception {
        Users users = new Users();
        ArrayList<com.rackspace.idm.domain.entity.User> userList = new ArrayList<com.rackspace.idm.domain.entity.User>();
        userList.add(new com.rackspace.idm.domain.entity.User());
        users.setUsers(userList);
        when(ldapUserRepository.getUsersByNastId("nastId")).thenReturn(users);
        boolean result = delegateCloud11Service.userExistsInGAByNastId("nastId");
        assertThat("boolean", result, equalTo(true));
    }

    @Test
    public void getPath_queryParamsIsNull_returnsPathWithOutChanges() throws Exception {
        String result = delegateCloud11Service.getPath("noChange", null);
        assertThat("path", result, equalTo("noChange"));
    }

    @Test
    public void extractCredentials_notXMLCompatibleType_callsExtractJSONCredentials() throws Exception {
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_JSON_TYPE);
        doReturn(null).when(spy).extractJSONCredentials("body");
        spy.extractCredentials(httpHeaders, "body");
        verify(spy).extractJSONCredentials("body");
    }

    @Test
    public void extractCredentials_isXMLCompatibleType_callsExtractXMLCredentials() throws Exception {
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_XML_TYPE);
        doReturn(null).when(spy).extractXMLCredentials("body");
        spy.extractCredentials(httpHeaders, "body");
        verify(spy).extractXMLCredentials("body");
    }
}
