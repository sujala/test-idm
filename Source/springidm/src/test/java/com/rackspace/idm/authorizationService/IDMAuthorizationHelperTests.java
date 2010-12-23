package com.rackspace.idm.authorizationService;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.rackspace.idm.entities.Permission;
import com.rackspace.idm.interceptors.AuthorizationInterceptor;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.oauthAuthentication.HttpOauthAuthenticationService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.RoleService;
import com.rackspace.idm.test.stub.StubLogger;

public class IDMAuthorizationHelperTests {

    IDMAuthorizationHelper idmAuthorizationHelper;

    private OAuthService oauthService;
    private RoleService roleService;
    private ClientService clientService;
    private IDMAuthorizationHelper authorizationHelper;
    private AuthorizationService authorizationService;
    private Logger logger;

    @Before
    public void setUp() {

        oauthService = EasyMock.createMock(OAuthService.class);
        roleService = EasyMock.createMock(RoleService.class);
        clientService = EasyMock.createMock(ClientService.class);

        authorizationHelper = new IDMAuthorizationHelper();

        logger = new StubLogger();

        String propsFileLoc = "";
        org.apache.commons.configuration.Configuration config = null;

        propsFileLoc = "SunXACMLAuthorization.properties";
        try {
            config = new PropertiesConfiguration(propsFileLoc);
        } catch (ConfigurationException e) {
            System.out.println(e);
            logger.error("Could not load Axiomatics configuraiton.", e);
        }

        String sunAuthConfigFilePath = config
            .getString("sunAuthConfigPathForTesting");
        String xacmlPolicyFilePath = config
            .getString("xacmlPolicyFilePathForTesting");

        boolean testMode = true;
        authorizationService = new SunAuthorizationService(logger,
            sunAuthConfigFilePath, xacmlPolicyFilePath, testMode);

        idmAuthorizationHelper = new IDMAuthorizationHelper(oauthService,
            authorizationService, roleService, clientService, logger);
    }

    @Test
    public void shouldNotAuthorizeIfSubjectIsNull() {

        String authHeader = "Token token=goodToken";
        EasyMock
            .expect(oauthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(null);

        EasyMock.replay(oauthService);

        String userCompanyId = "Rackspace";
        String methodName = "dummyMethod";
        boolean result = idmAuthorizationHelper.checkAdminAuthorization(
            authHeader, userCompanyId, methodName);
        Assert.assertEquals(false, result);
    }

    @Test
    public void shouldNotAuthorizeIfCompanyIsNull() {

        String authHeader = "Token token=goodToken";
        EasyMock.expect(
            oauthService.getCustomerIdFromAuthHeaderToken(authHeader))
            .andReturn(null);

        EasyMock.replay(oauthService);

        String userCompanyId = "Rackspace";
        String methodName = "dummyMethod";
        boolean result = idmAuthorizationHelper.checkCompanyAuthorization(
            authHeader, userCompanyId, methodName);
        Assert.assertEquals(false, result);
    }

    @Test
    public void shouldNotAuthorizeIfSubjectUserIsNull() {

        String subjectUsername = null;
        String username = "devdatta";
        String methodName = "dummyMethod";
        boolean result = idmAuthorizationHelper.checkUserAuthorization(
            subjectUsername, username, methodName);

        Assert.assertEquals(false, result);
    }

    @Test
    public void shouldNotAuthorizeIfTargetUserIsNull() {

        String subjectUsername = "devdatta";
        String username = null;
        String methodName = "dummyMethod";
        boolean result = idmAuthorizationHelper.checkUserAuthorization(
            subjectUsername, username, methodName);

        Assert.assertEquals(false, result);
    }

    @Test
    public void shouldNotAuthorizeIfPermssionsAreNull() {

        String lookupPath = "PUT /customers/";
        String methodName = "dummyMethod";
        List<Permission> permissions = null;
        boolean result = idmAuthorizationHelper.checkPermission(permissions,
            methodName, lookupPath);

        Assert.assertEquals(false, result);
    }

    @Test
    public void shouldNotAuthorizeIfPermssionIsNull() {

        String lookupPath = "PUT /customers/";
        String methodName = "dummyMethod";
        List<Permission> permission = new ArrayList<Permission>();
        permission.add(new Permission(null, null, null, null));
        boolean result = idmAuthorizationHelper.checkPermission(permission,
            methodName, lookupPath);

        Assert.assertEquals(false, result);
    }

    @Test
    public void shouldNotAuthorizeIfRequestMethodAndURIAreNull() {

        String lookupPath = null;
        String methodName = null;
        List<Permission> permission = new ArrayList<Permission>();
        permission.add(new Permission("P1", "IDM", "POST /customers", "Rackspace"));
        boolean result = idmAuthorizationHelper.checkPermission(permission,
            methodName, lookupPath);

        Assert.assertEquals(false, result);
    }

    @Test
    public void shouldNotAuthorizeIfAuthHeaderIsNull() {

        String authHeader = null;
        boolean result = idmAuthorizationHelper
            .checkRackspaceEmployeeAuthorization(authHeader);

        Assert.assertEquals(false, result);
    }
}