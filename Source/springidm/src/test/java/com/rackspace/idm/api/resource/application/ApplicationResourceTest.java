package com.rackspace.idm.api.resource.application;

import com.rackspace.api.idm.v1.Application;
import com.rackspace.idm.api.converter.ApplicationConverter;
import com.rackspace.idm.domain.service.ApplicationService;
import com.rackspace.idm.domain.service.AuthorizationService;
import com.rackspace.idm.exception.BadRequestException;
import com.sun.jersey.core.provider.EntityHolder;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Created by IntelliJ IDEA.
 * User: Hector
 * Date: 4/9/12
 * Time: 5:10 PM
 */
public class ApplicationResourceTest {

    ApplicationResource applicationResource;
    ApplicationService applicationService;
    ApplicationConverter applicationConverter;
    AuthorizationService authorizationService;

    @Before
    public void setUp() throws Exception {
        applicationService = mock(ApplicationService.class);
        applicationConverter = mock(ApplicationConverter.class);
        authorizationService = mock(AuthorizationService.class);
        applicationResource = new ApplicationResource(null,null,null,applicationService,null,applicationConverter,authorizationService,null);
    }

    @Test(expected = BadRequestException.class)
    public void updateApplication_withNonMatchingApplicationIdInPayload_throwsBadRequestException() throws Exception {
        com.rackspace.idm.domain.entity.Application application = new com.rackspace.idm.domain.entity.Application("clientId", null, null, null, null);
        doNothing().when(authorizationService).verifyIdmSuperAdminAccess(anyString());
        when(applicationConverter.toClientDO(Matchers.<Application>any())).thenReturn(application);
        when(applicationService.loadApplication(null)).thenReturn(null);
        Application application1 = new Application();
        application1.setClientId("foo");
        applicationResource.updateApplication(null,"bar",new EntityHolder<Application>(application1));
    }
}
