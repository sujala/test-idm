package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.resource.cloud.CloudClient;
import com.rackspace.idm.domain.service.UserService;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.openstack.docs.identity.api.ext.os_ksadm.v1.Service;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;
import org.openstack.docs.identity.api.v2.Role;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.io.IOException;
import java.util.HashMap;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 9/14/11
 * Time: 5:25 PM
 */
public class DelegateCloud20ServiceTest {

    DummyCloud20Service dummyCloud20Service;
    DelegateCloud20Service delegateCloud20Service;
    DefaultCloud20Service defaultCloud20Service = mock(DefaultCloud20Service.class);
    UserService userService = mock(UserService.class);
    CloudClient cloudClient = mock(CloudClient.class);
    HttpHeaders httpHeaders = mock(HttpHeaders.class);
    Marshaller marshaller = mock(Marshaller.class);
    private final Configuration config = mock(Configuration.class);
    AuthenticationRequest authenticationRequest = mock(AuthenticationRequest.class);
    String url = "http://url.com/";
    Boolean disabled = true;
    String body = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns12:endpointTemplate xsi:nil=\"true\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
    String bodyPassword = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns7:passwordCredentials xsi:nil=\"true\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
    String bodyApiCredentials = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns8:apiKeyCredentials xsi:nil=\"true\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
    String bodyUser = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns7:user xsi:nil=\"true\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
    String bodyTenant = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns7:tenant xsi:nil=\"true\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>";
    private String bodyRole = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns7:role xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\"/>";
    private String bodyService = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns11:service xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\"/>";
    private String roleId = "roleId";
    private String userId = "userId";
    private String tenantId = "tenantId";
    private String serviceId = "serviceId";
    private Role role = new Role();
    private Service service = new Service();
    private String username = "username";

    @Before
    public void setUp() throws IOException, JAXBException {
        dummyCloud20Service = new DummyCloud20Service();
        delegateCloud20Service = new DelegateCloud20Service();
        delegateCloud20Service.setDummyCloud20Service(dummyCloud20Service);
        delegateCloud20Service.setCloudClient(cloudClient);
        delegateCloud20Service.setDefaultCloud20Service(defaultCloud20Service);
        delegateCloud20Service.setUserService(userService);
        when(config.getString("cloudAuth20url")).thenReturn(url);
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(disabled);
        delegateCloud20Service.setConfig(config);
        when(cloudClient.post(anyString(), Matchers.<HttpHeaders>any(), anyString())).thenReturn(Response.ok());
    }

    @Test
    public void authenticate_returnsResponse() throws Exception {
        when(defaultCloud20Service.authenticate(httpHeaders, authenticationRequest)).thenReturn(Response.noContent());
        assertThat("response", delegateCloud20Service.authenticate(httpHeaders, authenticationRequest), instanceOf(Response.ResponseBuilder.class));
    }

    @Test
    public void appendQueryParams_withNoParams_remainsSame() {
        String request = "http://localhost/echo";

        HashMap<String, Object> params = new HashMap<String, Object>();
        String updatedRequest = delegateCloud20Service.appendQueryParams(request, params);

        assertThat("appendQueryParams", request, equalTo(updatedRequest));
    }

    @Test
    public void appendQueryParams_withOneParams_returnsCorrectValue() {
        String request = "http://localhost/echo";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("param", "value");
        String updatedRequest = delegateCloud20Service.appendQueryParams(request, params);

        assertThat("appendQueryParams", updatedRequest, equalTo("http://localhost/echo?param=value"));
    }

    @Test
    public void appendQueryParams_withTwoParams_returnsCorrectValue() {
        String request = "http://localhost/echo";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("param", "value");
        params.put("param2", "value2");
        String updatedRequest = delegateCloud20Service.appendQueryParams(request, params);

        assertThat("appendQueryParams", updatedRequest, anyOf(
                equalTo("http://localhost/echo?param=value&param2=value2"),
                equalTo("http://localhost/echo?param2=value2&param=value")
        ));
    }

    @Test
    public void appendQueryParams_withNullParam_returnsCorrectValue() {
        String request = "http://localhost/echo";

        HashMap<String, Object> params = new HashMap<String, Object>();
        params.put("param", "value");
        params.put("param2", null);
        String updatedRequest = delegateCloud20Service.appendQueryParams(request, params);

        assertThat("appendQueryParams", updatedRequest, equalTo("http://localhost/echo?param=value"));
    }

    @Test
    public void listExtensions_useCloudAuthIsTrue_callsCloudClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        delegateCloud20Service.listExtensions(null);
        verify(cloudClient).get(url + "extensions", null);
    }

    @Test
    public void listExtensions_callsConfigUseCloudAuth() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        delegateCloud20Service.listExtensions(null);
        verify(config).getBoolean("useCloudAuth");
    }

    @Test
    public void listExtensions_useCloudAuthIsFalse_callsCloudClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        delegateCloud20Service.listExtensions(null);
        verify(cloudClient, times(0)).get(url, null);
    }

    @Test
    public void listTenants_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listTenants(null, null, null, null);
        verify(defaultCloud20Service).listTenants(null, null, null, null);
    }

    @Test
    public void listTenants_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listTenants(null, null, null, null);
        verify(defaultCloud20Service).listTenants(null, null, null, null);
    }

    @Test
    public void listTenants_RoutingTrueAndGASourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listTenants(null, null, null, null);
        verify(cloudClient).get(url + "tenants", null);
    }

    @Test
    public void listTenants_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listTenants(null, null, null, null);
        verify(defaultCloud20Service).listTenants(null, null, null, null);
    }

    @Test
    public void getTenantByName_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getTenantByName(null, null, null);
        verify(defaultCloud20Service).getTenantByName(null, null, null);
    }

    @Test
    public void getTenantByName_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getTenantByName(null, null, null);
        verify(defaultCloud20Service).getTenantByName(null, null, null);
    }

    @Test
    public void getTenantByName_RoutingTrueAndGASourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getTenantByName(null, null, "myName");
        verify(cloudClient).get(url + "tenants?name=myName", null);
    }

    @Test
    public void getTenantByName_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getTenantByName(null, null, null);
        verify(defaultCloud20Service).getTenantByName(null, null, null);
    }

    @Test
    public void getTenantById_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getTenantById(null, null, null);
        verify(defaultCloud20Service).getTenantById(null, null, null);
    }

    @Test
    public void getTenantById_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getTenantById(null, null, null);
        verify(defaultCloud20Service).getTenantById(null, null, null);
    }

    @Test
    public void getTenantById_RoutingTrueAndGASourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getTenantById(null, null, "myId");
        verify(cloudClient).get(url + "tenants/myId", null);
    }

    @Test
    public void getTenantById_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getTenantById(null, null, null);
        verify(defaultCloud20Service).getTenantById(null, null, null);
    }

    @Test
    public void validateToken_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.validateToken(null, null, null, null);
        verify(defaultCloud20Service).validateToken(null, null, null, null);
    }

    @Test
    public void validateToken_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.validateToken(null, null, null, null);
        verify(defaultCloud20Service).validateToken(null, null, null, null);
    }

    @Test
    public void validateToken_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.validateToken(null, null, "1", null);
        verify(cloudClient).get(url + "tokens/1", null);
    }

    @Test
    public void validateToken_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.validateToken(null, null, null, null);
        verify(defaultCloud20Service).validateToken(null, null, null, null);
    }

    @Test
    public void listEndpointsForToken_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listEndpointsForToken(null, null, null);
        verify(defaultCloud20Service).listEndpointsForToken(null, null, null);
    }

    @Test
    public void listEndpointsForToken_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listEndpointsForToken(null, null, null);
        verify(defaultCloud20Service).listEndpointsForToken(null, null, null);
    }

    @Test
    public void listEndpointsForToken_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listEndpointsForToken(null, null, "1");
        verify(cloudClient).get(url + "tokens/1/endpoints", null);
    }

    @Test
    public void listEndpointsForToken_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listEndpointsForToken(null, null, null);
        verify(defaultCloud20Service).listEndpointsForToken(null, null, null);
    }

    @Test
    public void listExtensions_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listExtensions(null);
        verify(defaultCloud20Service).listExtensions(null);
    }

    @Test
    public void listExtensions_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listExtensions(null);
        verify(defaultCloud20Service).listExtensions(null);
    }

    @Test
    public void listExtensions_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listExtensions(null);
        verify(cloudClient).get(url + "extensions", null);
    }

    @Test
    public void listExtensions_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listExtensions(null);
        verify(defaultCloud20Service).listExtensions(null);
    }

    @Test
    public void listRoles_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listRoles(null, null, null, null, null);
        verify(defaultCloud20Service).listRoles(null, null, null, null, null);
    }

    @Test
    public void listRoles_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listRoles(null, null, null, null, null);
        verify(defaultCloud20Service).listRoles(null, null, null, null, null);
    }

    @Test
    public void listRoles_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listRoles(null, null, null, null, null);
        verify(cloudClient).get(url + "OS-KSADM/roles", null);
    }

    @Test
    public void listRoles_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listRoles(null, null, null, null, null);
        verify(defaultCloud20Service).listRoles(null, null, null, null, null);
    }

    @Test
    public void addRole_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addRole(null, null, null, null);
        verify(defaultCloud20Service).addRole(null, null, null, null);
    }

    @Test
    public void addRole_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addRole(null, null, null, null);
        verify(defaultCloud20Service).addRole(null, null, null, null);
    }

    @Test
    public void addRole_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addRole(null, null, null, null);
        verify(cloudClient).post(eq(url + "OS-KSADM/roles"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void addRole_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addRole(null, null, null, null);
        verify(defaultCloud20Service).addRole(null, null, null, null);
    }

    @Test
    public void getRole_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getRole(null, null, null);
        verify(defaultCloud20Service).getRole(null, null, null);
    }

    @Test
    public void getRole_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getRole(null, null, null);
        verify(defaultCloud20Service).getRole(null, null, null);
    }

    @Test
    public void getRole_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getRole(null, null, "1");
        verify(cloudClient).get(url + "OS-KSADM/roles/1", null);
    }

    @Test
    public void getRole_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getRole(null, null, null);
        verify(defaultCloud20Service).getRole(null, null, null);
    }

    @Test
    public void deleteRole_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteRole(null, null, "1");
        verify(defaultCloud20Service).deleteRole(null, null, "1");
    }

    @Test
    public void deleteRole_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteRole(null, null, "1");
        verify(defaultCloud20Service).deleteRole(null, null, "1");
    }

    @Test
    public void deleteRole_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteRole(null, null, "1");
        verify(cloudClient).delete(url + "OS-KSADM/roles/1", null);
    }

    @Test
    public void deleteRole_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteRole(null, null, "1");
        verify(defaultCloud20Service).deleteRole(null, null, "1");
    }

    @Test
    public void addTenant_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addTenant(null, null, null, null);
        verify(defaultCloud20Service).addTenant(null, null, null, null);
    }

    @Test
    public void addTenant_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addTenant(null, null, null, null);
        verify(defaultCloud20Service).addTenant(null, null, null, null);
    }

    @Test
    public void addTenant_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addTenant(null, null, null, null);
        verify(cloudClient).post(eq(url + "tenants"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void addTenant_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addTenant(null, null, null, null);
        verify(defaultCloud20Service).addTenant(null, null, null, null);
    }

    @Test
    public void updateTenant_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.updateTenant(null, null, "1", null);
        verify(defaultCloud20Service).updateTenant(null, null, "1", null);
    }

    @Test
    public void updateTenant_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.updateTenant(null, null, "1", null);
        verify(defaultCloud20Service).updateTenant(null, null, "1", null);
    }

    @Test
    public void updateTenant_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.updateTenant(null, null, "1", null);
        verify(cloudClient).post(eq(url + "tenants/1"),Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void updateTenant_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.updateTenant(null, null, "1", null);
        verify(defaultCloud20Service).updateTenant(null, null, "1", null);
    }

    @Test
    public void listUsersForTenant_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listUsersForTenant(null, null, null, null, null);
        verify(defaultCloud20Service).listUsersForTenant(null, null, null, null, null);
    }

    @Test
    public void listUsersForTenant_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listUsersForTenant(null, null, null, null, null);
        verify(defaultCloud20Service).listUsersForTenant(null, null, null, null, null);
    }

    @Test
    public void listUsersForTenant_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listUsersForTenant(null, null, "1", null, null);
        verify(cloudClient).get(url + "tenants/1/users", null);
    }

    @Test
    public void listUsersForTenant_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listUsersForTenant(null, null, null, null, null);
        verify(defaultCloud20Service).listUsersForTenant(null, null, null, null, null);
    }

    @Test
    public void listUsersWithRoleForTenant_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listUsersWithRoleForTenant(null, null, "1", "2", null, null);
        verify(defaultCloud20Service).listUsersWithRoleForTenant(null, null, "1", "2", null, null);
    }

    @Test
    public void listUsersWithRoleForTenant_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listUsersWithRoleForTenant(null, null, "1", "2", null, null);
        verify(defaultCloud20Service).listUsersWithRoleForTenant(null, null, "1", "2", null, null);
    }

    @Test
    public void listUsersWithRoleForTenant_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listUsersWithRoleForTenant(null, null, "1", "2", null, null);
        verify(cloudClient).get(url + "tenants/1/users?roleId=2", null);
    }

    @Test
    public void listUsersWithRoleForTenant_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listUsersWithRoleForTenant(null, null, "1", "2", null, null);
        verify(defaultCloud20Service).listUsersWithRoleForTenant(null, null, "1", "2", null, null);
    }

    @Test
    public void deleteTenant_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteTenant(null, null, "1");
        verify(defaultCloud20Service).deleteTenant(null, null, "1");
    }

    @Test
    public void deleteTenant_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteTenant(null, null, "1");
        verify(defaultCloud20Service).deleteTenant(null, null, "1");
    }

    @Test
    public void deleteTenant_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteTenant(null, null, "1");
        verify(cloudClient).delete(url + "tenants/1", null);
    }

    @Test
    public void deleteTenant_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteTenant(null, null, "1");
        verify(defaultCloud20Service).deleteTenant(null, null, "1");
    }

    @Test
    public void listRolesForTenant_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listRolesForTenant(null, null, "1", null, null);
        verify(defaultCloud20Service).listRolesForTenant(null, null, "1", null, null);
    }

    @Test
    public void listRolesForTenant_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listRolesForTenant(null, null, "1", null, null);
        verify(defaultCloud20Service).listRolesForTenant(null, null, "1", null, null);
    }

    @Test
    public void listRolesForTenant_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listRolesForTenant(null, null, "1", null, null);
        verify(cloudClient).get(url + "tenants/1/OS-KSADM/roles", null);
    }

    @Test
    public void listRolesForTenant_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listRolesForTenant(null, null, "1", null, null);
        verify(defaultCloud20Service).listRolesForTenant(null, null, "1", null, null);
    }

    @Test
    public void listServices_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listServices(null, null, null, null);
        verify(defaultCloud20Service).listServices(null, null, null, null);
    }

    @Test
    public void listServices_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listServices(null, null, null, null);
        verify(defaultCloud20Service).listServices(null, null, null, null);
    }

    @Test
    public void listServices_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listServices(null, null, null, null);
        verify(cloudClient).get(url + "OS-KSADM/services", null);
    }

    @Test
    public void listServices_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listServices(null, null, null, null);
        verify(defaultCloud20Service).listServices(null, null, null, null);
    }

    @Test
    public void addService_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addService(null, null, null, null);
        verify(defaultCloud20Service).addService(null, null, null, null);
    }

    @Test
    public void addService_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addService(null, null, null, null);
        verify(defaultCloud20Service).addService(null, null, null, null);
    }

    @Test
    public void addService_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addService(null, null, null, null);
        verify(cloudClient).post(eq(url + "OS-KSADM/services"), Matchers.<HttpHeaders>any(), Matchers.anyString());
    }

    @Test
    public void addService_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addService(null, null, null, null);
        verify(defaultCloud20Service).addService(null, null, null, null);
    }

    @Test
    public void getService_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getService(null, null, "1");
        verify(defaultCloud20Service).getService(null, null, "1");
    }

    @Test
    public void getService_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getService(null, null, "1");
        verify(defaultCloud20Service).getService(null, null, "1");
    }

    @Test
    public void getService_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getService(null, null, "1");
        verify(cloudClient).get(url + "OS-KSADM/services/1", null);
    }

    @Test
    public void getService_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getService(null, null, "1");
        verify(defaultCloud20Service).getService(null, null, "1");
    }

    @Test
    public void deleteService_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteService(null, null, "1");
        verify(defaultCloud20Service).deleteService(null, null, "1");
    }

    @Test
    public void deleteService_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteService(null, null, "1");
        verify(defaultCloud20Service).deleteService(null, null, "1");;
    }

    @Test
    public void deleteService_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteService(null, null, "1");
        verify(cloudClient).delete(url + "OS-KSADM/services/1", null);
    }

    @Test
    public void deleteService_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteService(null, null, "1");
        verify(defaultCloud20Service).deleteService(null, null, "1");
    }

    @Test
    public void getEndpointTemplate_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getEndpointTemplate(null, null, "1");
        verify(defaultCloud20Service).getEndpointTemplate(null, null, "1");
    }

    @Test
    public void getEndpointTemplate_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getEndpointTemplate(null, null, "1");
        verify(defaultCloud20Service).getEndpointTemplate(null, null, "1");
    }

    @Test
    public void getEndpointTemplate_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getEndpointTemplate(null, null, "1");
        verify(cloudClient).get(url + "OS-KSCATALOG/endpointTemplates/1", null);
    }

    @Test
    public void getEndpointTemplate_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getEndpointTemplate(null, null, "1");
        verify(defaultCloud20Service).getEndpointTemplate(null, null, "1");
    }

    @Test
    public void deleteEndpointTemplate_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteEndpointTemplate(null, null, "1");
        verify(defaultCloud20Service).deleteEndpointTemplate(null, null, "1");
    }

    @Test
    public void deleteEndpointTemplate_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteEndpointTemplate(null, null, "1");
        verify(defaultCloud20Service).deleteEndpointTemplate(null, null, "1");
    }

    @Test
    public void deleteEndpointTemplate_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteEndpointTemplate(null, null, "1");
        verify(cloudClient).delete(url + "OS-KSCATALOG/endpointTemplates/1", null);
    }

    @Test
    public void deleteEndpointTemplate_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteEndpointTemplate(null, null, "1");
        verify(defaultCloud20Service).deleteEndpointTemplate(null, null, "1");
    }

    @Test
    public void listEndpoints_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listEndpoints(null, null , "1");
        verify(defaultCloud20Service).listEndpoints(null, null, "1");
    }

    @Test
    public void listEndpoints_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listEndpoints(null, null, "1");
        verify(defaultCloud20Service).listEndpoints(null, null, "1");
    }

    @Test
    public void listEndpoints_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listEndpoints(null, null, "1");
        verify(cloudClient).get(url + "tenants/1/OS-KSCATALOG/endpoints", null);
    }

    @Test
    public void listEndpoints_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listEndpoints(null, null, "1");
        verify(defaultCloud20Service).listEndpoints(null, null, "1");
    }

    @Test
    public void checkToken_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.checkToken(null, null , "1", null);
        verify(defaultCloud20Service).checkToken(null, null , "1", null);
    }

    @Test
    public void checkToken_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.checkToken(null, null , "1", null);
        verify(defaultCloud20Service).checkToken(null, null , "1", null);
    }

    @Test
    public void checkToken_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.checkToken(null, null , "1", null);
        verify(cloudClient).get(url + "tokens/1", null);
    }

    @Test
    public void checkToken_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.checkToken(null, null, "1", null);
        verify(defaultCloud20Service).checkToken(null, null, "1", null);
    }

    @Test
    public void getExtension_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getExtension(null, null);
        verify(defaultCloud20Service).getExtension(null, null);
    }

    @Test
    public void getExtension_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getExtension(null, null);
        verify(defaultCloud20Service).getExtension(null, null);
    }

    @Test
    public void getExtension_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getExtension(null, "RAX-KSKEY");
        verify(cloudClient).get(url + "extensions/RAX-KSKEY", null);
    }

    @Test
    public void getExtension_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getExtension(null, null);
        verify(defaultCloud20Service).getExtension(null, null);
    }

    @Test
    public void getEndpoint_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getEndpoint(null, null, "1", "2");
        verify(defaultCloud20Service).getEndpoint(null, null, "1", "2");
    }

    @Test
    public void getEndpoint_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getEndpoint(null, null, "1", "2");
        verify(defaultCloud20Service).getEndpoint(null, null, "1", "2");
    }

    @Test
    public void getEndpoint_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.getEndpoint(null, null, "1", "2");
        verify(cloudClient).get(url + "tenants/2/OS-KSCATALOG/endpoints/1", null);
    }

    @Test
    public void getEndpoint_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.getEndpoint(null, null, "1", "2");
        verify(defaultCloud20Service).getEndpoint(null, null, "1", "2");
    }

    @Test
    public void deleteEndpoint_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteEndpoint(null, null, "1", "2");
        verify(defaultCloud20Service).deleteEndpoint(null, null, "1", "2");
    }

    @Test
    public void deleteEndpoint_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteEndpoint(null, null, "1", "2");
        verify(defaultCloud20Service).deleteEndpoint(null, null, "1", "2");
    }

    @Test
    public void deleteEndpoint_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.deleteEndpoint(null, null, "1", "2");
        verify(cloudClient).delete(url + "tenants/2/OS-KSCATALOG/endpoints/1", null);
    }

    @Test
    public void deleteEndpoint_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.deleteEndpoint(null, null, "1", "2");
        verify(defaultCloud20Service).deleteEndpoint(null, null, "1", "2");
    }

    @Test
    public void addEndpoint_RoutingFalseAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addEndpoint(null, null, "1", null);
        verify(defaultCloud20Service).addEndpoint(null, null, "1", null);
    }

    @Test
    public void addEndpoint_RoutingFalseAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addEndpoint(null, null, "1", null);
        verify(defaultCloud20Service).addEndpoint(null, null, "1", null);
    }

    @Test
    public void addEndpoint_RoutingTrueAndGASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addEndpoint(null, null, "1", null);
        verify(cloudClient).post(eq(url + "tenants/1/OS-KSCATALOG/endpoints"), Matchers.<HttpHeaders>any(), Matchers.anyString());
    }

    @Test
    public void addEndpoint_RoutingTrueAndGASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(delegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(delegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addEndpoint(null, null, "1", null);
        verify(defaultCloud20Service).addEndpoint(null, null, "1", null);
    }

    @Test
    public void listTenants_useCloudAuthIsTrue_callsCloudClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        delegateCloud20Service.listTenants(null, "token", null, null);
        verify(cloudClient).get(url + "tenants", null);
    }

    @Test
    public void listTenants_useCloudAuthIsFalse_doesntCallCloudClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        delegateCloud20Service.listTenants(null, "token", null, null);
        verify(cloudClient, times(0)).get(url + "tenants", null);
    }

    @Test
    public void listTenants_useCloudAuthIsFalse_returns200() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        when(defaultCloud20Service.listTenants(null, "token", null, null)).thenReturn(Response.ok());
        Response.ResponseBuilder responseBuilder = delegateCloud20Service.listTenants(null, "token", null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(200));
    }

    @Test
    public void getSecretQA_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.getSecretQA(null, null, null)).thenReturn(Response.status(401));
        delegateCloud20Service.getSecretQA(null, null, null);
        verify(cloudClient).get(url + "users/null/RAX-KSQA/secretqa/", null);
    }

    @Test
    public void getSecretQA_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.getSecretQA(null, null, null)).thenReturn(Response.status(404));
        delegateCloud20Service.getSecretQA(null, null, null);
        verify(cloudClient).get(url + "users/null/RAX-KSQA/secretqa/", null);
    }

    @Test
    public void updateSecretQA_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.updateSecretQA(null, null, null, null)).thenReturn(Response.status(401));
        delegateCloud20Service.updateSecretQA(null, null, null, null);
        verify(cloudClient).post(url + "users/null/RAX-KSQA/secretqa/", null, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns13:secretQA xsi:nil=\"true\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
    }

    @Test
    public void updateSecretQA_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.updateSecretQA(null, null, null, null)).thenReturn(Response.status(404));
        delegateCloud20Service.updateSecretQA(null, null, null, null);
        verify(cloudClient).post(url + "users/null/RAX-KSQA/secretqa/", null, "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?><ns13:secretQA xsi:nil=\"true\" xmlns:ns14=\"http://fault.common.api.rackspace.com/v1.0\" xmlns:ns9=\"http://docs.openstack.org/identity/api/ext/OS-KSEC2/v1.0\" xmlns:ns5=\"http://docs.openstack.org/common/api/v1.0\" xmlns:ns12=\"http://docs.openstack.org/identity/api/ext/OS-KSCATALOG/v1.0\" xmlns:ns6=\"http://docs.openstack.org/compute/api/v1.1\" xmlns:ns13=\"http://docs.rackspace.com/identity/api/ext/RAX-KSQA/v1.0\" xmlns:ns7=\"http://docs.openstack.org/identity/api/v2.0\" xmlns:ns10=\"http://docs.rackspace.com/identity/api/ext/RAX-KSGRP/v1.0\" xmlns:ns8=\"http://docs.rackspace.com/identity/api/ext/RAX-KSKEY/v1.0\" xmlns:ns11=\"http://docs.openstack.org/identity/api/ext/OS-KSADM/v1.0\" xmlns:ns2=\"http://www.w3.org/2005/Atom\" xmlns:ns4=\"http://docs.rackspacecloud.com/auth/api/v1.1\" xmlns:ns3=\"http://idm.api.rackspace.com/v1.0\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"/>");
    }

    @Test
    public void addUserRole_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        roleId = "roleId";
        userId = "userId";
        when(defaultCloud20Service.addUserRole(null, null, userId, roleId)).thenReturn(Response.status(401));
        delegateCloud20Service.addUserRole(null, null, userId, roleId);
        verify(cloudClient).put(url + "users/" + userId + "/roles/OS-KSADM/" + roleId, null, "");
    }

    @Test
    public void addUserRole_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.addUserRole(null, null, userId, roleId)).thenReturn(Response.status(404));
        delegateCloud20Service.addUserRole(null, null, userId, roleId);
        verify(cloudClient).put(url + "users/" + userId + "/roles/OS-KSADM/" + roleId, null, "");
    }

    @Test
    public void getUserRole_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.getUserRole(null, null, userId, roleId)).thenReturn(Response.status(401));
        delegateCloud20Service.getUserRole(null, null, userId, roleId);
        verify(cloudClient).get(url + "users/" + userId + "/roles/OS-KSADM/" + roleId, null);
    }

    @Test
    public void getUserRole_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.getUserRole(null, null, userId, roleId)).thenReturn(Response.status(404));
        delegateCloud20Service.getUserRole(null, null, userId, roleId);
        verify(cloudClient).get(url + "users/" + userId + "/roles/OS-KSADM/" + roleId, null);
    }

    @Test
    public void deleteUserRole_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.deleteUserRole(null, null, userId, roleId)).thenReturn(Response.status(401));
        delegateCloud20Service.deleteUserRole(null, null, userId, roleId);
        verify(cloudClient).delete(url + "users/" + userId + "/roles/OS-KSADM/" + roleId, null);
    }

    @Test
    public void deleteUserRole_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.deleteUserRole(null, null, userId, roleId)).thenReturn(Response.status(404));
        delegateCloud20Service.deleteUserRole(null, null, userId, roleId);
        verify(cloudClient).delete(url + "users/" + userId + "/roles/OS-KSADM/" + roleId, null);
    }

    @Test
    public void addRolesToUserOnTenant_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.addRolesToUserOnTenant(null, null, tenantId, userId, roleId)).thenReturn(Response.status(401));
        delegateCloud20Service.addRolesToUserOnTenant(null, null, tenantId, userId, roleId);
        verify(cloudClient).put(url + "tenants/" + tenantId + "/users/" + userId + "/roles/OS-KSADM/" + roleId, null, "");
    }

    @Test
    public void addRolesToUserOnTenant_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.addRolesToUserOnTenant(null, null, tenantId, userId, roleId)).thenReturn(Response.status(404));
        delegateCloud20Service.addRolesToUserOnTenant(null, null, tenantId, userId, roleId);
        verify(cloudClient).put(url + "tenants/" + tenantId + "/users/" + userId + "/roles/OS-KSADM/" + roleId, null, "");
    }

    @Test
    public void deleteRoleFromUserOnTenant_defaultServiceReturns401_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.deleteRoleFromUserOnTenant(null, null, tenantId, userId, roleId)).thenReturn(Response.status(401));
        delegateCloud20Service.deleteRoleFromUserOnTenant(null, null, tenantId, userId, roleId);
        verify(cloudClient).delete(url + "tenants/" + tenantId + "/users/" + userId + "/roles/OS-KSADM/" + roleId, null);
    }

    @Test
    public void deleteRoleFromUserOnTenant_defaultServiceReturns404_callsClient() throws Exception {
        when(config.getBoolean("GAKeystoneDisabled")).thenReturn(false);
        when(defaultCloud20Service.deleteRoleFromUserOnTenant(null, null, tenantId, userId, roleId)).thenReturn(Response.status(404));
        delegateCloud20Service.deleteRoleFromUserOnTenant(null, null, tenantId, userId, roleId);
        verify(cloudClient).delete(url + "tenants/" + tenantId + "/users/" + userId + "/roles/OS-KSADM/" + roleId, null);
    }

    @Test
    public void addEndpoint_checksForUseCloudAuthEnable() throws Exception {
        delegateCloud20Service.addEndpoint(null, null, tenantId, null);
        verify(config).getBoolean("useCloudAuth");
    }

    @Test
    public void addEndpoint_whenUseCloudAuthEnabled_callsClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        delegateCloud20Service.addEndpoint(null, null, tenantId, null);
        verify(cloudClient).post(eq(url + "tenants/" + tenantId + "/OS-KSCATALOG/endpoints"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void addEndpoint_whenUseCloudAuthDisabled_doesNotCallClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        delegateCloud20Service.addEndpoint(null, null, tenantId, null);
        verify(cloudClient, times(0)).post(eq(url + "tenants/" + tenantId + "/OS-KSCATALOG/endpoints"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void listExtensions_checksForUseCloudAuthEnable() throws Exception {
        delegateCloud20Service.listExtensions(null);
        verify(config).getBoolean("useCloudAuth");
    }

    @Test
    public void getExtensions_whenUseCloudAuthEnabled_callsClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(true);
        delegateCloud20Service.listExtensions(null);
        verify(cloudClient).get(url + "extensions", null);
    }

    @Test
    public void getExtensions_whenUseCloudAuthDisabled_doesNotCallClient() throws Exception {
        when(config.getBoolean("useCloudAuth")).thenReturn(false);
        delegateCloud20Service.listExtensions(null);
        verify(cloudClient, times(0)).get(url + "extensions", null);
    }

    @Test
    public void getUserById_RoutingFalse_UserExistsInGaFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.getUserById(null, null, userId);
        verify(defaultCloud20Service).getUserById(null, null, userId);
    }

    @Test
    public void getUserById_RoutingFalse_UserExistsInGaTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.getUserById(null, null, userId);
        verify(defaultCloud20Service).getUserById(null, null, userId);
    }

    @Test
    public void getUserById_RoutingFalse_UserExistsInGaTrue_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.getUserById(null, null, userId);
        verify(cloudClient).get(url + "users/" + userId, null);
    }

    @Test
    public void getUserById_RoutingTrue_UserExistsInGaTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.getUserById(null, null, userId);
        verify(defaultCloud20Service).getUserById(null, null, userId);
    }

    @Test
    public void listEndpointTemplates_RoutingFalse_GASourceOFTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listEndpointTemplates(null, null, serviceId);
        verify(defaultCloud20Service).listEndpointTemplates(null, null, serviceId);
    }

    @Test
    public void listEndpointTemplates_RoutingFalse_GASourceOFTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listEndpointTemplates(null, null, serviceId);
        verify(defaultCloud20Service).listEndpointTemplates(null, null, serviceId);
    }

    @Test
    public void listEndpointTemplates_RoutingFalse_GANotSourceOFTruth_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listEndpointTemplates(null, null, serviceId);
        verify(cloudClient).get(url + "OS-KSCATALOG/endpointTemplates?serviceId=serviceId", null);
    }

    @Test
    public void listEndpointTemplates_RoutingTrue_GANotSourceOFTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listEndpointTemplates(null, null, serviceId);
        verify(defaultCloud20Service).listEndpointTemplates(null, null, serviceId);
    }

    @Test
    public void addEndpointTemplate_RoutingFalse_GASourceOFTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addEndpointTemplate(null, null, null, null);
        verify(defaultCloud20Service).addEndpointTemplate(null, null, null, null);
    }

    @Test
    public void addEndpointTemplate_RoutingFalse_GASourceOFTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addEndpointTemplate(null, null, null, null);
        verify(defaultCloud20Service).addEndpointTemplate(null, null, null, null);
    }

    @Test
    public void addEndpointTemplate_RoutingFalse_GANotSourceOFTruth_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addEndpointTemplate(null, null, null, null);
        verify(cloudClient).post(eq(url + "OS-KSCATALOG/endpointTemplates"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void addEndpointTemplate_RoutingTrue_GANotSourceOFTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addEndpointTemplate(null, null, null, null);
        verify(defaultCloud20Service).addEndpointTemplate(null, null, null, null);
    }

    @Test
    public void listUserGroups_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.listUserGroups(null, null, userId);
        verify(defaultCloud20Service).listUserGroups(null, null, userId);
    }

    @Test
    public void listUserGroups_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listUserGroups(null, null, userId);
        verify(defaultCloud20Service).listUserGroups(null, null, userId);
    }

    @Test
    public void listUserGroups_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.listUserGroups(null, null, userId);
        verify(cloudClient).get(url + "users/" + userId + "/RAX-KSGRP", null);
    }

    @Test
    public void listUserGroups_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listUserGroups(null, null, userId);
        verify(defaultCloud20Service).listUserGroups(null, null, userId);
    }

    @Test
    public void getUserByName_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsByUsername(username)).thenReturn(false);
        delegateCloud20Service.getUserByName(null, null, username);
        verify(defaultCloud20Service).getUserByName(null, null, username);
    }

    @Test
    public void getUserByName_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsByUsername(userId)).thenReturn(true);
        delegateCloud20Service.getUserByName(null, null, username);
        verify(defaultCloud20Service).getUserByName(null, null, username);
    }

    @Test
    public void getUserByName_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsByUsername(userId)).thenReturn(false);
        delegateCloud20Service.getUserByName(null, null, username);
        verify(cloudClient).get(url + "users?name=" + username, null);
    }

    @Test
    public void getUserByName_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsByUsername(username)).thenReturn(true);
        delegateCloud20Service.getUserByName(null, null, username);
        verify(defaultCloud20Service).getUserByName(null, null, username);
    }

    @Test
    public void listUserGlobalRoles_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.listUserGlobalRoles(null, null, userId);
        verify(defaultCloud20Service).listUserGlobalRoles(null, null, userId);
    }

    @Test
    public void listUserGlobalRoles_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listUserGlobalRoles(null, null, userId);
        verify(defaultCloud20Service).listUserGlobalRoles(null, null, userId);
    }

    @Test
    public void listUserGlobalRoles_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.listUserGlobalRoles(null, null, userId);
        verify(cloudClient).get(url + "users/" + userId + "/roles", null);
    }

    @Test
    public void listUserGlobalRoles_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listUserGlobalRoles(null, null, userId);
        verify(defaultCloud20Service).listUserGlobalRoles(null, null, userId);
    }

    @Test
    public void listUserGlobalRolesByServiceId_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.listUserGlobalRolesByServiceId(null, null, userId, serviceId);
        verify(defaultCloud20Service).listUserGlobalRolesByServiceId(null, null, userId, serviceId);
    }

    @Test
    public void listUserGlobalRolesByServiceId_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listUserGlobalRolesByServiceId(null, null, userId, serviceId);
        verify(defaultCloud20Service).listUserGlobalRolesByServiceId(null, null, userId, serviceId);
    }

    @Test
    public void listUserGlobalRolesByServiceId_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.listUserGlobalRolesByServiceId(null, null, userId, serviceId);
        verify(cloudClient).get(url + "users/" + userId + "/roles?serviceId="+serviceId, null);
    }

    @Test
    public void listUserGlobalRolesByServiceId_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listUserGlobalRolesByServiceId(null, null, userId, serviceId);
        verify(defaultCloud20Service).listUserGlobalRolesByServiceId(null, null, userId, serviceId);
    }

    @Test
    public void addUserCredential_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.addUserCredential(null,null,userId,null);
        verify(defaultCloud20Service).addUserCredential(null,null,userId,null);
    }

    @Test
    public void addUserCredential_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.addUserCredential(null,null,userId,null);
        verify(defaultCloud20Service).addUserCredential(null,null,userId,null);
    }

    @Test
    public void addUserCredential_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        when(httpHeaders.getMediaType()).thenReturn(MediaType.APPLICATION_XML_TYPE);
        delegateCloud20Service.addUserCredential(httpHeaders,null,userId,null);
        verify(cloudClient).post(url + "users/"+userId + "/OS-KSADM/credentials", httpHeaders, null);
    }

    @Test
    public void addUserCredential_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.addUserCredential(null,null,userId,null);
        verify(defaultCloud20Service).addUserCredential(null,null,userId,null);
    }


    @Test
    public void listCredentials_RoutingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.listCredentials(null,null,userId,null,null);
        verify(defaultCloud20Service).listCredentials(null,null,userId,null,null);
    }

    @Test
    public void listCredentials_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listCredentials(null,null,userId,null,null);
        verify(defaultCloud20Service).listCredentials(null,null,userId,null,null);
    }

    @Test
    public void listCredentials_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.listCredentials(null,null,userId,null,null);
        verify(cloudClient).get(url + "users/" + userId + "/OS-KSADM/credentials", null);
    }

    @Test
    public void listCredentials_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listCredentials(null,null,userId,null,null);
        verify(defaultCloud20Service).listCredentials(null,null,userId,null,null);
    }

    @Test
    public void updateUserPasswordCredentials_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.updateUserPasswordCredentials(null,null,userId,null,null);
        verify(defaultCloud20Service).updateUserPasswordCredentials(null,null,userId,null,null);
    }

    @Test
    public void updateUserPasswordCredentials_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.updateUserPasswordCredentials(null,null,userId,null,null);
        verify(defaultCloud20Service).updateUserPasswordCredentials(null,null,userId,null,null);
    }

    @Test
    public void updateUserPasswordCredentials_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.updateUserPasswordCredentials(null,null,userId,"type",null);
        verify(cloudClient).post(eq(url+"users/"+userId+"/OS-KSADM/credentials/type"),Matchers.<HttpHeaders>any(),anyString());
    }

    @Test
    public void updateUserPasswordCredentials_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.updateUserPasswordCredentials(null,null,userId,null,null);
        verify(defaultCloud20Service).updateUserPasswordCredentials(null,null,userId,null,null);
    }

    @Test
    public void listUsers_RoutingFalse_gaSourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listUsers(null,null,null,null);
        verify(defaultCloud20Service).listUsers(null,null,null,null);
    }

    @Test
    public void listUsers_RoutingFalse_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listUsers(null,null,null,null);
        verify(defaultCloud20Service).listUsers(null,null,null,null);
    }

    @Test
    public void listUsers_RoutingTrue_gaSourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.listUsers(null,null,null,null);
        verify(cloudClient).get(url + "users", null);
    }

    @Test
    public void listUsers_RoutingTrue_gaSourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.listUsers(null,null,null,null);
        verify(defaultCloud20Service).listUsers(null,null,null,null);
    }

    @Test
    public void updateUserApiKeyCredentials_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.updateUserApiKeyCredentials(null,null,userId,"type",null);
        verify(defaultCloud20Service).updateUserApiKeyCredentials(null,null,userId,"type",null);
    }

    @Test
    public void updateUserApiKeyCredentials_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.updateUserApiKeyCredentials(null,null,userId,"type",null);
        verify(defaultCloud20Service).updateUserApiKeyCredentials(null,null,userId,"type",null);
    }

    @Test
    public void updateUserApiKeyCredentials_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.updateUserApiKeyCredentials(null,null,userId,"type",null);
        verify(cloudClient).post(eq(url + "users/" + userId + "/OS-KSADM/credentials/type"), Matchers.<HttpHeaders>any(),anyString());
    }

    @Test
    public void updateUserApiKeyCredentials_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.updateUserApiKeyCredentials(null,null,userId,"type",null);
        verify(defaultCloud20Service).updateUserApiKeyCredentials(null,null,userId,"type",null);
    }

    @Test
    public void getUserCredential_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.getUserCredential(null,null,userId,"type");
        verify(defaultCloud20Service).getUserCredential(null,null,userId,"type");
    }

    @Test
    public void getUserCredential_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.getUserCredential(null,null,userId,"type");
        verify(defaultCloud20Service).getUserCredential(null,null,userId,"type");
    }

    @Test
    public void getUserCredential_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.getUserCredential(null,null,userId,"type");
        verify(cloudClient).get(url + "users/" + userId + "/OS-KSADM/credentials/type", null);
    }

    @Test
    public void getUserCredential_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.getUserCredential(null,null,userId,"type");
        verify(defaultCloud20Service).getUserCredential(null,null,userId,"type");
    }

    @Test
    public void deleteUserCredential_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.deleteUserCredential(null, null, userId, "type");
        verify(defaultCloud20Service).deleteUserCredential(null, null, userId, "type");
    }

    @Test
    public void deleteUserCredential_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.deleteUserCredential(null, null, userId, "type");
        verify(defaultCloud20Service).deleteUserCredential(null, null, userId, "type");
    }

    @Test
    public void deleteUserCredential_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.deleteUserCredential(null, null, userId, "type");
        verify(cloudClient).delete(url + "users/" + userId + "/OS-KSADM/credentials/type", null);
    }

    @Test
    public void deleteUserCredential_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.deleteUserCredential(null, null, userId, "type");
        verify(defaultCloud20Service).deleteUserCredential(null, null, userId, "type");
    }

    @Test
    public void listRolesForUserOnTenant_RoutingFalse_UserExistsFalse_callDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.listRolesForUserOnTenant(null,null,tenantId,userId);
        verify(defaultCloud20Service).listRolesForUserOnTenant(null,null,tenantId,userId);
    }

    @Test
    public void listRolesForUserOnTenant_RoutingFalse_UserExistsTrue_callDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listRolesForUserOnTenant(null,null,tenantId,userId);
        verify(defaultCloud20Service).listRolesForUserOnTenant(null,null,tenantId,userId);
    }

    @Test
    public void listRolesForUserOnTenant_RoutingTrue_UserExistsFalse_callClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.listRolesForUserOnTenant(null,null,tenantId,userId);
        verify(cloudClient).get(url + "tenants/" + tenantId + "/users/" + userId + "/roles", null);
    }

    @Test
    public void listRolesForUserOnTenant_RoutingTrue_UserExistsTrue_callDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.listRolesForUserOnTenant(null,null,tenantId,userId);
        verify(defaultCloud20Service).listRolesForUserOnTenant(null,null,tenantId,userId);
    }

    @Test
    public void addUser_RoutingFalse_GASourceOfTruthFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addUser(null,null,null,null);
        verify(defaultCloud20Service).addUser(null,null,null,null);
    }

    @Test
    public void addUser_RoutingFalse_GASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addUser(null,null,null,null);
        verify(defaultCloud20Service).addUser(null,null,null,null);
    }

    @Test
    public void addUser_RoutingTrue_GASourceOfTruthFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(false);
        delegateCloud20Service.addUser(null,null,null,null);
        verify(cloudClient).post(eq(url+"users"),Matchers.<HttpHeaders>any(),anyString());
    }
    
    @Test
    public void addUser_RoutingTrue_GASourceOfTruthTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(config.getBoolean(DelegateCloud20Service.GA_SOURCE_OF_TRUTH)).thenReturn(true);
        delegateCloud20Service.addUser(null,null,null,null);
        verify(defaultCloud20Service).addUser(null,null,null,null);
    }

    @Test
    public void updateUser_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.updateUser(null,null, userId, null);
        verify(defaultCloud20Service).updateUser(null,null,userId,null);
    }

    @Test
    public void updateUser_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.updateUser(null,null, userId, null);
        verify(defaultCloud20Service).updateUser(null,null,userId,null);
    }

    @Test
    public void updateUser_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.updateUser(null,null, userId, null);
        verify(cloudClient).post(eq(url+"users/"+userId),Matchers.<HttpHeaders>any(),anyString());
    }

    @Test
    public void updateUser_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.updateUser(null,null, userId, null);
        verify(defaultCloud20Service).updateUser(null,null,userId,null);
    }

    @Test
    public void deleteUser_RoutingFalse_UserExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.deleteUser(null, null, userId);
        verify(defaultCloud20Service).deleteUser(null, null, userId);
    }

    @Test
    public void deleteUser_RoutingFalse_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.deleteUser(null, null, userId);
        verify(defaultCloud20Service).deleteUser(null, null, userId);
    }

    @Test
    public void deleteUserUser_RoutingTrue_UserExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.deleteUser(null, null, userId);
        verify(cloudClient).delete(eq(url + "users/" + userId), Matchers.<HttpHeaders>any());
    }

    @Test
    public void deleteUserUser_RoutingTrue_UserExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.deleteUser(null, null, userId);
        verify(defaultCloud20Service).deleteUser(null, null, userId);
    }

    @Test
    public void setUserEnabled_RoutingFalse_userExistsFalse_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.setUserEnabled(null,null,userId,null);
        verify(defaultCloud20Service).setUserEnabled(null,null,userId,null);
    }

    @Test
    public void setUserEnabled_RoutingFalse_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(false);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.setUserEnabled(null,null,userId,null);
        verify(defaultCloud20Service).setUserEnabled(null,null,userId,null);
    }

    @Test
    public void setUserEnabled_RoutingTrue_userExistsFalse_callsClient() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(false);
        delegateCloud20Service.setUserEnabled(null,null,userId,null);
        verify(cloudClient).put(eq(url + "users/" + userId + "/OS-KSADM/enabled"), Matchers.<HttpHeaders>any(), anyString());
    }

    @Test
    public void setUserEnabled_RoutingTrue_userExistsTrue_callsDefaultService() throws Exception {
        when(config.getBoolean(DelegateCloud20Service.CLOUD_AUTH_ROUTING)).thenReturn(true);
        when(userService.userExistsById(userId)).thenReturn(true);
        delegateCloud20Service.setUserEnabled(null,null,userId,null);
        verify(defaultCloud20Service).setUserEnabled(null,null,userId,null);
    }
}