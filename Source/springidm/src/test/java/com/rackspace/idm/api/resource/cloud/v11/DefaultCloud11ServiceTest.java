package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11;
import com.rackspace.idm.domain.dao.impl.LdapCloudAdminRepository;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.util.NastFacade;
import com.rackspacecloud.docs.auth.api.v1.Credentials;
import com.rackspacecloud.docs.auth.api.v1.User;
import com.rackspacecloud.docs.auth.api.v1.UserCredentials;
import com.sun.jersey.api.uri.UriBuilderImpl;
import org.apache.commons.configuration.Configuration;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mortbay.jetty.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.net.URI;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 10/18/11
 * Time: 6:19 PM
 */
public class DefaultCloud11ServiceTest {

    DefaultCloud11Service defaultCloud11Service;
    UserConverterCloudV11 userConverterCloudV11;
    LdapCloudAdminRepository ldapCloudAdminRepository;
    NastFacade nastFacade;
    UserService userService;
    EndpointService endpointService;
    Configuration config;
    UriInfo uriInfo;
    User user = new User();
    HttpServletRequest request;

    @Before
    public void setUp() throws Exception {
        userConverterCloudV11 = mock(UserConverterCloudV11.class);
        ldapCloudAdminRepository = mock(LdapCloudAdminRepository.class);
        when(ldapCloudAdminRepository.authenticate("auth", "auth123")).thenReturn(true);
        userService = mock(UserService.class);
        endpointService = mock(EndpointService.class);
        uriInfo = mock(UriInfo.class);
        config = mock(Configuration.class);
        request = mock(HttpServletRequest.class);
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic YXV0aDphdXRoMTIz");
        UriBuilderImpl uriBuilder = mock(UriBuilderImpl.class);
        when(uriBuilder.build()).thenReturn(new URI(""));
        when(uriBuilder.path("userId")).thenReturn(uriBuilder);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        com.rackspace.idm.domain.entity.User user1 = new com.rackspace.idm.domain.entity.User();
        user1.setId("userId");
        when(userConverterCloudV11.toUserDO(user)).thenReturn(user1);
        when(config.getBoolean("nast.xmlrpc.enabled")).thenReturn(true);
        defaultCloud11Service = new DefaultCloud11Service(config,null,endpointService,userService,null,userConverterCloudV11,null, ldapCloudAdminRepository);
        nastFacade = mock(NastFacade.class);
        defaultCloud11Service.setNastFacade(nastFacade);
    }

    @Test
    public void createUser_callsNastFacade() throws Exception {
        user.setId("userId");
        user.setMossoId(123);
        defaultCloud11Service.createUser(request, null,uriInfo, user);
        Mockito.verify(nastFacade).addNastUser(user);
    }

    @Test
    public void authenticateResponse_usernameIsNull_returns400() throws Exception {
        JAXBElement<Credentials> cred = new JAXBElement<Credentials>(new QName(""),Credentials.class,new UserCredentials());
        Response.ResponseBuilder responseBuilder = defaultCloud11Service.authenticateResponse(cred, null, null, null);
        assertThat("response code", responseBuilder.build().getStatus(), equalTo(400));
    }
}
