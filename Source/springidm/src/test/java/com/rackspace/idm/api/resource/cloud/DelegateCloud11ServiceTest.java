package com.rackspace.idm.api.resource.cloud;

import com.rackspace.idm.api.resource.cloud.v11.CredentialUnmarshaller;
import com.rackspace.idm.api.resource.cloud.v11.DefaultCloud11Service;
import com.rackspace.idm.api.resource.cloud.v11.DelegateCloud11Service;
import com.rackspace.idm.api.resource.cloud.v11.DummyCloud11Service;
import com.rackspace.idm.domain.dao.impl.LdapUserRepository;
import com.rackspacecloud.docs.auth.api.v1.*;
import com.sun.jersey.core.spi.factory.ResponseBuilderImpl;
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
        when(OBJ_FACTORY.createBaseURL(baseUrl)).thenReturn(new JAXBElement(QName.valueOf("foo"),BaseURL.class, baseUrl));
        when(OBJ_FACTORY.createBaseURLRef(any(BaseURLRef.class))).thenReturn(new JAXBElement<BaseURLRef>(QName.valueOf("foo"),BaseURLRef.class,new BaseURLRef()));
        url = "http://foo.com/";
        when(config.getString("cloudAuth11url")).thenReturn(url);
        when(httpHeaders.getMediaType()).thenReturn(new MediaType("application/json",null));
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic YXV0aDphdXRoMTIz");
    }

    @Test
    public void authenticate_withJsonBody_callsCredentialUnmarshaller() throws Exception {
        JAXBElement jaxbElement= new JAXBElement<UserCredentials>(QName.valueOf("foo"),UserCredentials.class, new UserCredentials());
        when(defaultCloud11Service.authenticate(null,null,httpHeaders, jsonBody)).thenReturn(Response.status(404));
        when(credentialUnmarshaller.unmarshallCredentialsFromJSON(jsonBody)).thenReturn(jaxbElement);
        delegateCloud11Service.authenticate(null, null, httpHeaders, jsonBody);
        verify(credentialUnmarshaller).unmarshallCredentialsFromJSON(jsonBody);
    }

    @Test
    public void adminAuthenticate_withJsonBody_callsCredentialUnmarshaller() throws Exception {
        JAXBElement jaxbElement= new JAXBElement<UserCredentials>(QName.valueOf("foo"),UserCredentials.class, new UserCredentials());
        when(defaultCloud11Service.adminAuthenticate(null,null,httpHeaders, jsonBody)).thenReturn(Response.status(404));
        when(credentialUnmarshaller.unmarshallCredentialsFromJSON(jsonBody)).thenReturn(jaxbElement);
        delegateCloud11Service.adminAuthenticate(null, null, httpHeaders, jsonBody);
        verify(credentialUnmarshaller).unmarshallCredentialsFromJSON(jsonBody);
    }

    @Test
    public void setUserEnabled_callsOBJ_FACTORY_createUser() throws Exception {
        UserWithOnlyEnabled user = new UserWithOnlyEnabled();
        user.setEnabled(true);
        when(defaultCloud11Service.setUserEnabled(request, "testId",user,null)).thenReturn(new ResponseBuilderImpl().status(404));
        when(OBJ_FACTORY.createUser(user)).thenReturn(new JAXBElement<User>(QName.valueOf("name"),User.class,null));
        delegateCloud11Service.setUserEnabled(request, "testId", user, null);
        verify(OBJ_FACTORY).createUser(Matchers.<User>any());
    }

    @Test
    public void createUser_useCloudAuthFlagSetToTrue_callsConfigGetBoolean() throws Exception {
        User user = new User();
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(OBJ_FACTORY.createUser(user)).thenReturn(new JAXBElement<User>(new QName(""), User.class, user));
        delegateCloud11Service.createUser(null,null,null, user);
        verify(config).getBoolean("useCloudAuth");
    }

    @Test
    public void createUser_useCloudAuthFlagSetToTrue_clientGetsCalled() throws Exception {
        User user = new User();
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        when(config.getString("cloudAuth11url")).thenReturn("");
        when(OBJ_FACTORY.createUser(user)).thenReturn(new JAXBElement<User>(new QName(""), User.class, user));
        delegateCloud11Service.createUser(null, null, null, user);
        verify(cloudClient).post("users",null,"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>< xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\"/>");
    }

    @Test
    public void createUser_useCloudAuthFlagSetToFalse_clientDoesNotGetCalled() throws Exception {
        User user = new User();
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(config.getString("cloudAuth11url")).thenReturn("");
        when(OBJ_FACTORY.createUser(user)).thenReturn(new JAXBElement<User>(new QName(""), User.class, user));
        delegateCloud11Service.createUser(null,null,null, user);
        verify(cloudClient, VerificationModeFactory.times(0)).post(anyString(),Matchers.<javax.ws.rs.core.HttpHeaders>anyObject(),anyString());
    }

    @Test
    public void addBaseUrl_checksForUseCloudAuthEnable() throws Exception {
        delegateCloud11Service.addBaseURL(null,null,null);
        verify(config).getBoolean("useCloudAuth");
    }

    @Test
    public void addBaseUrl_whenUseCloudAuthEnabled_callsClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        delegateCloud11Service.addBaseURL(null,null,baseUrl);
        verify(cloudClient).post(url+"baseURLs",null,"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><foo default=\"true\" id=\"1\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\"/>");
    }

    @Test
    public void addBaseUrl_whenUseCloudAuthDisabled_doesNotCallClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        delegateCloud11Service.addBaseURL(null,null, baseUrl);
        verify(cloudClient,times(0)).post(url+"baseURLs",null,"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><foo default=\"true\" id=\"1\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\"/>");
    }

    @Test
    public void addBaseUrlRef_checkForUseCloudAuthEnabled() throws Exception {
        delegateCloud11Service.addBaseURLRef(null,userId,null,null,null);
        verify(config).getBoolean("useCloudAuth");
    }

    @Test
    public void addBaseUrlRef_callsUserExists() throws Exception {
        delegateCloud11Service.addBaseURLRef(null,userId,null,null,null);
        verify(ldapUserRepository).getUserById(userId);
    }

    @Test
    public void addBaseUrlRef_userExists_callsDefaultService() throws Exception {
        when(ldapUserRepository.getUserById(userId)).thenReturn(new com.rackspace.idm.domain.entity.User(userId));
        delegateCloud11Service.addBaseURLRef(null,userId,null,null,null);
        verify(defaultCloud11Service).addBaseURLRef(null,userId,null,null,null);
    }

    @Test
    public void addBaseUrlRef_userExists_useCloudAuthDisabled_callsDefaultService() throws Exception {
        when(ldapUserRepository.getUserById(userId)).thenReturn(new com.rackspace.idm.domain.entity.User(userId));
        delegateCloud11Service.addBaseURLRef(null,userId,null,null,null);
        verify(defaultCloud11Service).addBaseURLRef(null, userId, null, null, null);
    }

    @Test
    public void addBaseUrlRef_userDoesntExist_useCloudAuthEnabled_callsClient() throws Exception {
        when(ldapUserRepository.getUserById(userId)).thenReturn(null);
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        delegateCloud11Service.addBaseURLRef(null,userId,null,null,null);
        verify(cloudClient).post(anyString(), Matchers.<javax.ws.rs.core.HttpHeaders>any(), anyString());
    }
}
