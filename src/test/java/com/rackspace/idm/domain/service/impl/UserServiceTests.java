package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.dao.AuthDao;
import com.rackspace.idm.domain.dao.UserDao;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.util.CryptHelper;
import com.rackspace.idm.validation.Validator;
import junit.framework.Assert;
import org.apache.commons.configuration.Configuration;
import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class UserServiceTests {

    UserDao mockUserDao;
    UserService userService;
    UserService trustedUserService;
    DomainService mockDomainService;
    AuthDao mockRackerDao;
    ApplicationService mockClientService;
    ScopeAccessService mockScopeAccessService;
    PasswordComplexityService mockPasswordComplexityService;
    CloudRegionService cloudRegionService;
    TenantService tenantService;
    Validator validator = new Validator();
    PropertiesService propertiesService;
    CryptHelper cryptHelper;

    String customerId = "123456";
    String username = "testuser";
    String password = "secret";
    Password userpass = Password.newInstance(password);
    String firstname = "testfirstname";
    String lastname = "testlastname";
    String email = "test@example.com";
    String apiKey = "1234567890";

    String middlename = "middle";
    String secretQuestion = "question";
    String secretQuestionId = "id";
    String secretAnswer = "answer";
    String preferredLang = "en_US";
    String timeZone = "America/Chicago";
    
    String id = "XXX";
    
    String secureId = "XXX";

    String customerDN = "o=@!FFFF.FFFF.FFFF.FFFF!AAAA.AAAA,o=rackspace,dc=rackspace,dc=com";

    String nastId = "nastId";
    int mossoId = 6676;
    String domainId = "";

    int limit = 100;

    String rackerId = "rackerId";

    @Before
    public void setUp() throws Exception {

        mockUserDao = EasyMock.createMock(UserDao.class);
        mockRackerDao = EasyMock.createMock(AuthDao.class);
        mockClientService = EasyMock.createMock(ApplicationService.class);
        mockScopeAccessService = EasyMock.createMock(ScopeAccessService.class);
        mockDomainService = EasyMock.createMock(DomainService.class);
        mockPasswordComplexityService = EasyMock.createMock(PasswordComplexityService.class);
        cloudRegionService = EasyMock.createMock(DefaultCloudRegionService.class);
        validator = EasyMock.createMock(Validator.class);
        tenantService = EasyMock.createMock(TenantService.class);
        propertiesService = EasyMock.createMock(PropertiesService.class);
        cryptHelper = EasyMock.createMock(CryptHelper.class);


        Configuration appConfig = new PropertyFileConfiguration().getConfig();
        appConfig.setProperty("ldap.server.trusted", false);

        userService = new DefaultUserService();
        userService.setUserDao(mockUserDao);
        userService.setAuthDao(mockRackerDao);
        userService.setScopeAccessService(mockScopeAccessService);
        userService.setApplicationService(mockClientService);
        userService.setConfig(appConfig);
        userService.setCloudRegionService(cloudRegionService);
        userService.setValidator(validator);
        userService.setTenantService(tenantService);
        userService.setCryptHelper(cryptHelper);
        userService.setPropertiesService(propertiesService);

        Configuration appConfig2 = new PropertyFileConfiguration().getConfig();
        
        appConfig2.setProperty("ldap.server.trusted", true);

        trustedUserService =  new DefaultUserService();

        trustedUserService.setUserDao(mockUserDao);
        trustedUserService.setAuthDao(mockRackerDao);
        trustedUserService.setScopeAccessService(mockScopeAccessService);
        trustedUserService.setApplicationService(mockClientService);
        trustedUserService.setConfig(appConfig2);
    }

    @Test
    public void testingDateTime() {

        final DateTime d = new DateTime();
        final DateTime newD = d.plusDays(1000);

        d.toString();
        newD.toString();
    }

    @Test
    public void shouldNotAuthenticateUserThatDoestNotExist() {
        final String badUsername = "badusername";

        EasyMock.expect(mockUserDao.authenticate(badUsername, password)).andReturn(
                this.getFalseAuthenticationResult());
        EasyMock.replay(mockUserDao);

        final UserAuthenticationResult uaResult = userService.authenticate(badUsername, password);
        Assert.assertFalse(uaResult.isAuthenticated());
        EasyMock.verify(mockUserDao);
    }

    private Domain getFakeDomain() {
        final Domain domain = new Domain();
        domain.setDomainId("1");
        domain.setName("domain");
        return domain;
    }

    private UserScopeAccess getFakeUserScopeAccess() {
        UserScopeAccess usa = new UserScopeAccess();
        usa.setClientId(clientId);
        usa.setClientRCN(clientRCN);
        usa.setUserRCN(userRCN);
        usa.setUserRsId(userRsId);
        return usa;
    }
    
    String clientId = "clientId";
    String clientRCN = "clientRCN";
    String userRCN = "userRCN";
    String userRsId = "userRsId";

    private List<Application> getFakeClients() {

        final Application client1 = new Application();
        client1.setClientId("id1");
        final Application client2 = new Application();
        client2.setClientId("id2");

        final List<Application> clients = new ArrayList<Application>();
        clients.add(client1);
        clients.add(client2);

        return clients;
    }

    private UserAuthenticationResult getFalseAuthenticationResult() {
        return new UserAuthenticationResult(null, false);
    }
}