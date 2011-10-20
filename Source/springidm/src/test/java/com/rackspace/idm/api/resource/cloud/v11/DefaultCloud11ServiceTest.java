package com.rackspace.idm.api.resource.cloud.v11;

import com.rackspace.idm.api.converter.cloudv11.UserConverterCloudV11;
import com.rackspace.idm.domain.service.EndpointService;
import com.rackspace.idm.domain.service.UserService;
import com.rackspace.idm.util.NastFacade;
import com.rackspacecloud.docs.auth.api.v1.User;
import com.sun.jersey.api.uri.UriBuilderImpl;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.UriInfo;
import java.net.URI;

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
    NastFacade nastFacade;
    UserService userService;
    EndpointService endpointService;
    UriInfo uriInfo;
    User user = new User();

    @Before
    public void setUp() throws Exception {
        userConverterCloudV11 = mock(UserConverterCloudV11.class);
        userService = mock(UserService.class);
        endpointService = mock(EndpointService.class);
        uriInfo = mock(UriInfo.class);
        UriBuilderImpl uriBuilder = mock(UriBuilderImpl.class);
        when(uriBuilder.build()).thenReturn(new URI(""));
        when(uriBuilder.path("userId")).thenReturn(uriBuilder);
        when(uriInfo.getRequestUriBuilder()).thenReturn(uriBuilder);
        com.rackspace.idm.domain.entity.User user1 = new com.rackspace.idm.domain.entity.User();
        user1.setId("userId");
        when(userConverterCloudV11.toUserDO(user)).thenReturn(user1);
        defaultCloud11Service = new DefaultCloud11Service(null,null,endpointService,userService,null,userConverterCloudV11,null);
        nastFacade = mock(NastFacade.class);
        defaultCloud11Service.setNastFacade(nastFacade);
    }

    @Test
    public void createUser_callsNastFacade() throws Exception {
        user.setId("userId");
        user.setMossoId(123);
        defaultCloud11Service.createUser(null,uriInfo, user);
        Mockito.verify(nastFacade).addNastUser(user);
    }
}
