package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.resource.cloud.v11.CredentialUnmarshaller;
import com.rackspace.idm.api.resource.cloud.v11.DefaultCloud11Service;
import com.rackspace.idm.api.resource.cloud.v11.DelegateCloud11Service;
import com.rackspace.idm.api.resource.cloud.v11.DummyCloud11Service;
import com.rackspace.idm.domain.dao.impl.LdapUserRepository;
import com.rackspacecloud.docs.auth.api.v1.*;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.internal.verification.VerificationModeFactory;
import org.mortbay.jetty.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 8/30/11
 * Time: 9:19 PM
 */
public class DelegateCloud11ServiceTest {
    DelegateCloud11Service delegateCloud11Service;
    DefaultCloud11Service defaultCloud11Service;
    DummyCloud11Service dummyCloud11Service = new DummyCloud11Service();
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

    @Before
    public void setUp() throws JAXBException {
        delegateCloud11Service = new DelegateCloud11Service();
        defaultCloud11Service = mock(DefaultCloud11Service.class);
        credentialUnmarshaller = mock(CredentialUnmarshaller.class);

        delegateCloud11Service.setCredentialUnmarshaller(credentialUnmarshaller);
        delegateCloud11Service.setDefaultCloud11Service(defaultCloud11Service);
        delegateCloud11Service.setDummyCloud11Service(dummyCloud11Service);
        OBJ_FACTORY = mock(com.rackspacecloud.docs.auth.api.v1.ObjectFactory.class);
        DelegateCloud11Service.setOBJ_FACTORY(OBJ_FACTORY);
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
        when(httpHeaders.getMediaType()).thenReturn(new MediaType("application/json", null));
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic YXV0aDphdXRoMTIz");
    }

    @Test
    public void authenticate_withJsonBody_callsCredentialUnmarshaller() throws Exception {
        JAXBElement jaxbElement = new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, new UserCredentials());
        when(defaultCloud11Service.authenticate(null, null, httpHeaders, jsonBody)).thenReturn(Response.status(404));
        when(credentialUnmarshaller.unmarshallCredentialsFromJSON(jsonBody)).thenReturn(jaxbElement);
        delegateCloud11Service.authenticate(null, null, httpHeaders, jsonBody);
        verify(credentialUnmarshaller).unmarshallCredentialsFromJSON(jsonBody);
    }

    @Test
    public void adminAuthenticate_withJsonBody_callsCredentialUnmarshaller() throws Exception {
        JAXBElement jaxbElement = new JAXBElement<UserCredentials>(QName.valueOf("foo"), UserCredentials.class, new UserCredentials());
        when(defaultCloud11Service.adminAuthenticate(null, null, httpHeaders, jsonBody)).thenReturn(Response.status(404));
        when(credentialUnmarshaller.unmarshallCredentialsFromJSON(jsonBody)).thenReturn(jaxbElement);
        delegateCloud11Service.adminAuthenticate(null, null, httpHeaders, jsonBody);
        verify(credentialUnmarshaller).unmarshallCredentialsFromJSON(jsonBody);
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
        verify(cloudClient).post("users", null, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>< xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\"/>");
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
        verify(cloudClient).post(url + "baseURLs", null, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><foo default=\"true\" id=\"1\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\"/>");
    }

    @Test
    public void addBaseUrl_whenUseCloudAuthDisabled_doesNotCallClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        delegateCloud11Service.addBaseURL(null, null, baseUrl);
        verify(cloudClient, times(0)).post(url + "baseURLs", null, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><foo default=\"true\" id=\"1\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\"/>");
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
        delegateCloud11Service.getUserEnabled(null, userId, null);
        verify(cloudClient).get(url + "users/" + userId + "/enabled", null);
    }

    @Test
    public void getUserEnabled_RoutingTrueAndUserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getUserEnabled(null, userId, null);
        verify(defaultCloud11Service).getUserEnabled(null, userId, null);
    }

    @Test
    public void getServiceCatalog_routingFalse_gaSourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud11Service.getServiceCatalog(null, userId, null);
        verify(defaultCloud11Service).getServiceCatalog(null, userId, null);
    }

    @Test
    public void getServiceCatalog_routingFalse_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud11Service.getServiceCatalog(null, userId, null);
        verify(defaultCloud11Service).getServiceCatalog(null, userId, null);
    }

    @Test
    public void getServiceCatalog_routingTrue_gaSourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud11Service.getServiceCatalog(null, userId, null);
        verify(cloudClient).get(url + "users/" + userId + "/serviceCatalog", null);
    }

    @Test
    public void getServiceCatalog_routingTrue_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud11Service.getServiceCatalog(null, userId, null);
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
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.deleteUser(null, userId, null);
        verify(defaultCloud11Service).deleteUser(null, userId, null);
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
        delegateCloud11Service.getUser(null,userId,null);
        verify(defaultCloud11Service).getUser(null,userId,null);
    }

    @Test
    public void getUser_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getUser(null,userId,null);
        verify(defaultCloud11Service).getUser(null,userId,null);
    }

    @Test
    public void getUser_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(null);
        delegateCloud11Service.getUser(null,userId,null);
        verify(cloudClient).get(eq(url + "users/" + userId), Matchers.<javax.ws.rs.core.HttpHeaders>any());
    }

    @Test
    public void getUser_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByUsername(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getUser(null,userId,null);
        verify(defaultCloud11Service).getUser(null,userId,null);
    }

    @Test
    public void getUserFromNastId_RoutingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByNastId(userId)).thenReturn(null);
        delegateCloud11Service.getUserFromNastId(null,userId,null);
        verify(defaultCloud11Service).getUserFromNastId(null,userId,null);
    }

    @Test
    public void getUserFromNastId_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByNastId(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getUserFromNastId(null,userId,null);
        verify(defaultCloud11Service).getUserFromNastId(null,userId,null);
    }

    @Test
    public void getUserFromNastId_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByNastId(userId)).thenReturn(null);
        delegateCloud11Service.getUserFromNastId(null,userId,null);
        verify(cloudClient).get(eq(url + "nast/" + userId), Matchers.<javax.ws.rs.core.HttpHeaders>any());
    }

    @Test
    public void getUserFromNastId_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByNastId(userId)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getUserFromNastId(null,userId,null);
        verify(defaultCloud11Service).getUserFromNastId(null,userId,null);
    }

    @Test
    public void getUserFromMossoId_RoutingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByMossoId(0)).thenReturn(null);
        delegateCloud11Service.getUserFromMossoId(null,0,null);
        verify(defaultCloud11Service).getUserFromMossoId(null,0,null);
    }

    @Test
    public void getUserFromMossoId_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(ldapUserRepository.getUserByMossoId(0)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getUserFromMossoId(null,0,null);
        verify(defaultCloud11Service).getUserFromMossoId(null,0,null);
    }

    @Test
    public void getUserFromMossoId_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByMossoId(0)).thenReturn(null);
        delegateCloud11Service.getUserFromMossoId(null,0,null);
        verify(cloudClient).get(eq(url + "mosso/" + 0), Matchers.<javax.ws.rs.core.HttpHeaders>any());
    }

    @Test
    public void getUserFromMossoId_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(ldapUserRepository.getUserByMossoId(0)).thenReturn(new com.rackspace.idm.domain.entity.User());
        delegateCloud11Service.getUserFromMossoId(null,0,null);
        verify(defaultCloud11Service).getUserFromMossoId(null,0,null);
    }

    @Test
    public void getBaseURLId_routingFalse_gaSourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud11Service.getBaseURLId(null, 0,null,null);
        verify(defaultCloud11Service).getBaseURLId(null, 0,null,null);
    }

    @Test
    public void getBaseURLId_routingFalse_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud11Service.getBaseURLId(null, 0,null,null);
        verify(defaultCloud11Service).getBaseURLId(null, 0,null,null);
    }

    @Test
    public void getBaseURLId_routingTrue_gaSourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud11Service.getBaseURLId(null, 0,null,null);
        verify(cloudClient).get(url + "baseURLs/" + 0, null);
    }

    @Test
    public void getBaseURLId_routingTrue_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud11Service.getBaseURLId(null, 0,null,null);
        verify(defaultCloud11Service).getBaseURLId(null, 0,null,null);
    }

    @Test
    public void getEnabledBaseURL_routingFalse_gaSourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud11Service.getEnabledBaseURL(null,null,null);
        verify(defaultCloud11Service).getEnabledBaseURL(null, null,null);
    }

    @Test
    public void getEnabledBaseURL_routingFalse_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud11Service.getEnabledBaseURL(null, null,null);
        verify(defaultCloud11Service).getEnabledBaseURL(null, null,null);
    }

    @Test
    public void getEnabledBaseURL_routingTrue_gaSourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud11Service.getEnabledBaseURL(null, null,null);
        verify(cloudClient).get(url + "baseURLs/enabled", null);
    }

    @Test
    public void getEnabledBaseURL_routingTrue_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud11Service.getEnabledBaseURL(null, null,null);
        verify(defaultCloud11Service).getEnabledBaseURL(null, null,null);
    }

    @Test
    public void getBaseURLs_routingFalse_gaSourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud11Service.getBaseURLs(null,null,null);
        verify(defaultCloud11Service).getBaseURLs(null, null,null);
    }

    @Test
    public void getBaseURLs_routingFalse_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud11Service.getBaseURLs(null, null,null);
        verify(defaultCloud11Service).getBaseURLs(null, null,null);
    }

    @Test
    public void getBaseURLs_routingTrue_gaSourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud11Service.getBaseURLs(null, "serviceFoo",null);
        verify(cloudClient).get(url + "baseURLs?serviceName=serviceFoo", null);
    }

    @Test
    public void getBaseURLs_routingTrue_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud11Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud11Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud11Service.getBaseURLs(null, null,null);
        verify(defaultCloud11Service).getBaseURLs(null, null,null);
    }
}
