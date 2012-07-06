package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.api.serviceprofile.CloudContractDescriptionBuilder;
import com.rackspace.idm.api.serviceprofile.ServiceDescriptionTemplateUtil;
import com.rackspace.idm.domain.dao.ApiDocDao;
import com.rackspace.idm.domain.dao.impl.FileSystemApiDocRepository;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.openstack.docs.common.api.v1.VersionChoice;
import org.openstack.docs.identity.api.v2.AuthenticationRequest;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/6/12
 * Time: 9:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class Cloud20VersionResourceTest {
    Cloud20VersionResource cloud20VersionResource;
    Configuration config;
    CloudContractDescriptionBuilder cloudContractDescriptionBuilder;
    FileSystemApiDocRepository fileSystemApiDocRepository;
    ServiceDescriptionTemplateUtil serviceDescriptionTemplateUtil;
    HttpHeaders httpHeaders;
    AuthenticationRequest authenticationRequest;
    DelegateCloud20Service delegateCloud20Service;
    Cloud20VersionResource spy;

    @Before
    public void setUp() throws Exception {
        fileSystemApiDocRepository = new FileSystemApiDocRepository();
        serviceDescriptionTemplateUtil = new ServiceDescriptionTemplateUtil();
        config = mock(Configuration.class);
        cloudContractDescriptionBuilder = new CloudContractDescriptionBuilder(fileSystemApiDocRepository, serviceDescriptionTemplateUtil);

        cloud20VersionResource = new Cloud20VersionResource(config, cloudContractDescriptionBuilder);

        httpHeaders = mock(HttpHeaders.class);
        delegateCloud20Service = mock(DelegateCloud20Service.class);
        authenticationRequest = mock(AuthenticationRequest.class);

        spy = spy(cloud20VersionResource);
        doReturn(delegateCloud20Service).when(spy).getCloud20Service();
    }

    @Test
    public void getCloud20VersionInfo_returnsVersionInfo() throws Exception {
        Response response = cloud20VersionResource.getCloud20VersionInfo();
        JAXBElement<VersionChoice> jaxbElement = (JAXBElement)response.getEntity();
        VersionChoice result = jaxbElement.getValue();
        assertThat("version", result.getId(), equalTo("v2.0"));
    }

    @Test
    public void authenticate_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.authenticate(httpHeaders, authenticationRequest)).thenReturn(Response.ok());
        spy.authenticate(httpHeaders, authenticationRequest);
        verify(spy).getCloud20Service();
    }

    @Test
    public void authenticate_callsCloud20Service_callsAuthenticate() throws Exception {
        when(delegateCloud20Service.authenticate(httpHeaders, authenticationRequest)).thenReturn(Response.ok());
        spy.authenticate(httpHeaders, authenticationRequest);
        verify(delegateCloud20Service).authenticate(httpHeaders, authenticationRequest);
    }

    @Test
    public void authenticate_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.authenticate(httpHeaders, authenticationRequest)).thenReturn(Response.ok());
        Response result = spy.authenticate(httpHeaders, authenticationRequest);
        assertThat("reponse code", result.getStatus(), equalTo(200));
    }

    @Test
    public void validateToken_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.validateToken(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.validateToken(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void validateToken_callsCloud20Service_callsValidateToken() throws Exception {
        when(delegateCloud20Service.validateToken(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.validateToken(httpHeaders, null, null, null);
        verify(delegateCloud20Service).validateToken(httpHeaders, null, null, null);
    }

    @Test
    public void validateToken_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.validateToken(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.validateToken(httpHeaders, null, null, null);
        assertThat("reponse code", result.getStatus(), equalTo(200));
    }

    @Test
    public void checkToken_callsGetCloud20Service() throws Exception {
        when(delegateCloud20Service.validateToken(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.checkToken(httpHeaders, null, null, null);
        verify(spy).getCloud20Service();
    }

    @Test
    public void checkToken_callsCloud20Service_callsValidateToken() throws Exception {
        when(delegateCloud20Service.validateToken(httpHeaders, null, null, null)).thenReturn(Response.ok());
        spy.checkToken(httpHeaders, null, null, null);
        verify(delegateCloud20Service).validateToken(httpHeaders, null, null, null);
    }

    @Test
    public void checkToken_responseOk_returns200() throws Exception {
        when(delegateCloud20Service.validateToken(httpHeaders, null, null, null)).thenReturn(Response.ok());
        Response result = spy.checkToken(httpHeaders, null, null, null);
        assertThat("reponse code", result.getStatus(), equalTo(200));
    }
}
