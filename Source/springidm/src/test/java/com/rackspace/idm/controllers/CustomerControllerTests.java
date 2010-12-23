package com.rackspace.idm.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.ws.rs.core.Response;

import junit.framework.Assert;

import org.easymock.EasyMock;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletResponse;

import com.rackspace.idm.authorizationService.AuthorizationRequest;
import com.rackspace.idm.authorizationService.AuthorizationService;
import com.rackspace.idm.authorizationService.Entity;
import com.rackspace.idm.authorizationService.IDMAuthorizationHelper;
import com.rackspace.idm.authorizationService.SunAuthorizationService;
import com.rackspace.idm.config.LoggerFactoryWrapper;
import com.rackspace.idm.converters.ClientConverter;
import com.rackspace.idm.converters.CustomerConverter;
import com.rackspace.idm.converters.PermissionConverter;
import com.rackspace.idm.converters.RoleConverter;
import com.rackspace.idm.converters.UserConverter;
import com.rackspace.idm.entities.AccessToken;
import com.rackspace.idm.entities.Client;
import com.rackspace.idm.entities.ClientSecret;
import com.rackspace.idm.entities.ClientStatus;
import com.rackspace.idm.entities.Customer;
import com.rackspace.idm.entities.CustomerStatus;
import com.rackspace.idm.entities.Password;
import com.rackspace.idm.entities.Role;
import com.rackspace.idm.entities.RoleStatus;
import com.rackspace.idm.entities.User;
import com.rackspace.idm.entities.UserStatus;
import com.rackspace.idm.entities.Users;
import com.rackspace.idm.entities.AccessToken.IDM_SCOPE;
import com.rackspace.idm.entities.passwordcomplexity.PasswordComplexityResult;
import com.rackspace.idm.entities.passwordcomplexity.PasswordRuleResult;
import com.rackspace.idm.exceptions.ApiException;
import com.rackspace.idm.exceptions.BadRequestException;
import com.rackspace.idm.exceptions.CustomerConflictException;
import com.rackspace.idm.exceptions.DuplicateException;
import com.rackspace.idm.exceptions.DuplicateUsernameException;
import com.rackspace.idm.exceptions.ForbiddenException;
import com.rackspace.idm.exceptions.NotFoundException;
import com.rackspace.idm.exceptions.PasswordValidationException;
import com.rackspace.idm.exceptions.XACMLRequestCreationException;
import com.rackspace.idm.oauth.OAuthService;
import com.rackspace.idm.services.ClientService;
import com.rackspace.idm.services.CustomerService;
import com.rackspace.idm.services.PasswordComplexityService;
import com.rackspace.idm.services.RoleService;
import com.rackspace.idm.services.UserService;
import com.rackspace.idm.validation.InputValidator;

public class CustomerControllerTests {

    CustomerController controller;

    OAuthService mockOAuthService;
    CustomerService mockCustomerService;
    UserService mockUserService;
    RoleService mockRoleService;
    ClientService mockClientService;
    AuthorizationService mockAuthorizationService;
    PasswordComplexityService mockComplexityService;

    PermissionConverter permissionConverter = new PermissionConverter();
    RoleConverter roleConverter = new RoleConverter(permissionConverter);
    ClientConverter clientConverter = new ClientConverter(permissionConverter);
    UserConverter userConverter = new UserConverter(roleConverter);
    CustomerConverter customerConverter = new CustomerConverter();

    Customer testCustomer;
    Client testClient;
    Role testRole;
    User testUser;

    String authHeader = "Token token=asdf1234";

    String roleName = "Admin";
    String customerId = "123456";
    String username = "testuser";
    String password = "secret";
    Password userpass = Password.newInstance(password);
    String firstname = "testfirstname";
    String lastname = "testlastname";
    String email = "test@example.com";

    String middlename = "middle";
    String secretQuestion = "question";
    String secretAnswer = "answer";
    String preferredLang = "en_US";
    String timeZone = "America/Chicago";
    UserStatus userstatus = UserStatus.ACTIVE;

    String country = "USA";
    String displayName = "Test User";
    String apiKey = "ABCDEFGHIJ";
    String personId = "RPN-111-222-333";

    String clientId = "Id";
    ClientSecret clientSecret = ClientSecret.newInstance("secret");
    String iname = "iname";
    String inum = "inum";
    String clientName = "ClientName";
    String owner = "owner";
    String seeAlso = "seeAlso";
    ClientStatus clientstatus = ClientStatus.ACTIVE;

    CustomerStatus customerStatus = CustomerStatus.ACTIVE;
    String uniqueId = "UniqueId";

    RoleStatus rolestatus = RoleStatus.ACTIVE;
    String type = "type";

    int offset = 0;
    int limit = 100;
    int totalRecords = 0;

    PasswordComplexityResult passingResult = new PasswordComplexityResult();
    PasswordComplexityResult failingResult = new PasswordComplexityResult();

    com.rackspace.idm.jaxb.User jaxbUser;

    @Before
    public void setUp() {

        Validator validator = Validation.buildDefaultValidatorFactory()
            .getValidator();

        mockOAuthService = EasyMock.createMock(OAuthService.class);
        mockCustomerService = EasyMock.createMock(CustomerService.class);
        mockUserService = EasyMock.createMock(UserService.class);
        mockRoleService = EasyMock.createMock(RoleService.class);
        mockClientService = EasyMock.createMock(ClientService.class);
        mockAuthorizationService = EasyMock
            .createMock(SunAuthorizationService.class);
        mockComplexityService = EasyMock
            .createMock(PasswordComplexityService.class);

        controller = new CustomerController(mockOAuthService,
            mockCustomerService, mockUserService, mockRoleService,
            mockClientService, new IDMAuthorizationHelper(mockOAuthService,
                mockAuthorizationService, mockRoleService, mockClientService,
                LoggerFactory.getLogger(IDMAuthorizationHelper.class)),
            new InputValidator(validator), clientConverter, userConverter,
            customerConverter, roleConverter, mockComplexityService,
            new LoggerFactoryWrapper());

        testUser = new User(username);
        testUser.setCustomerId(customerId);
        testUser.setPassword(password);
        testUser.setFirstname(firstname);
        testUser.setLastname(lastname);
        testUser.setEmail(email);
        testUser.setMiddlename(middlename);
        testUser.setSecretQuestion(secretQuestion);
        testUser.setSecretAnswer(secretAnswer);
        testUser.setPrefferedLang(preferredLang);
        testUser.setTimeZone(timeZone);
        testUser.setStatus(userstatus);
        testUser.setSeeAlso(seeAlso);
        testUser.setCountry(country);
        testUser.setDisplayName(displayName);
        testUser.setApiKey(apiKey);
        testUser.setPersonId(personId);
        testUser.setUniqueId(uniqueId);
        testUser.setSoftDeleted(false);

        testClient = new Client();
        testClient.setClientId(clientId);
        testClient.setClientSecretObj(clientSecret);
        testClient.setCustomerId(customerId);
        testClient.setIname(iname);
        testClient.setInum(inum);
        testClient.setName(clientName);
        testClient.setOwner(owner);
        testClient.setSeeAlso(seeAlso);
        testClient.setStatus(clientstatus);
        testClient.setUniqueId(uniqueId);

        testCustomer = new Customer();
        testCustomer.setCustomerId(customerId);
        testCustomer.setIname(iname);
        testCustomer.setInum(inum);
        testCustomer.setOwner(owner);
        testCustomer.setSeeAlso(seeAlso);
        testCustomer.setStatus(customerStatus);
        testCustomer.setUniqueId(uniqueId);
        testCustomer.setDefaults();

        testRole = new Role();
        testRole.setCountry(country);
        testRole.setCustomerId(customerId);
        testRole.setIname(iname);
        testRole.setInum(inum);
        testRole.setName(roleName);
        testRole.setOrgInum(inum);
        testRole.setOwner(owner);
        testRole.setSeeAlso(seeAlso);
        testRole.setStatus(rolestatus);
        testRole.setType(type);
        testRole.setUniqueId(uniqueId);

        passingResult.addPasswordRuleResult(new PasswordRuleResult(true, 1,
            "RuleName", "RuleMessage"));
        failingResult.addPasswordRuleResult(new PasswordRuleResult(false, 1,
            "RuleName", "RuleMessage"));

        jaxbUser = userConverter.toUserJaxb(testUser);
    }

    @Test
    public void shouldAddFirstUser() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockUserService.isUsernameUnique(username)).andReturn(
            true);
        mockUserService.addUser(EasyMock.anyObject(testUser.getClass()));
        EasyMock.expect(mockUserService.getUser(username)).andReturn(testUser);
        EasyMock.expect(mockUserService.getUser(username)).andReturn(testUser);
        EasyMock.replay(mockUserService);

        mockCustomerService.addCustomer(EasyMock.anyObject(Customer.class));
        EasyMock.replay(mockCustomerService);

        EasyMock.expect(mockRoleService.getRole(roleName, customerId))
            .andReturn(testRole);
        mockRoleService.addUserToRole(EasyMock.anyObject(User.class),
            EasyMock.anyObject(Role.class));
        EasyMock.replay(mockRoleService);

        EasyMock.expect(mockComplexityService.checkPassword(password))
            .andReturn(passingResult);
        EasyMock.replay(mockComplexityService);

        controller.addFirstUser(response, authHeader, jaxbUser);

        Assert
            .assertTrue(response.getStatus() == HttpServletResponse.SC_CREATED);
    }

    @Test(expected = DuplicateUsernameException.class)
    public void shouldNotAddFirstUserForDuplicateUsername() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockUserService.isUsernameUnique(username)).andReturn(
            false);
        EasyMock.replay(mockUserService);

        controller.addFirstUser(response, authHeader, jaxbUser);

        Assert
            .assertTrue(response.getStatus() == HttpServletResponse.SC_CREATED);
    }

    @Test(expected = DuplicateUsernameException.class)
    public void shouldNotAddFirstUserForSecondDuplicateUserCheck() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockUserService.isUsernameUnique(username)).andReturn(
            true);
        mockUserService.addUser(EasyMock.anyObject(testUser.getClass()));
        EasyMock.expectLastCall().andThrow(new DuplicateException());
        EasyMock.replay(mockUserService);

        mockCustomerService.addCustomer(EasyMock.anyObject(Customer.class));
        mockCustomerService.deleteCustomer(customerId);
        EasyMock.replay(mockCustomerService);

        EasyMock.expect(mockRoleService.getRole(roleName, customerId))
            .andReturn(testRole);
        mockRoleService.addUserToRole(EasyMock.anyObject(User.class),
            EasyMock.anyObject(Role.class));
        EasyMock.replay(mockRoleService);

        EasyMock.expect(mockComplexityService.checkPassword(password))
            .andReturn(passingResult);
        EasyMock.replay(mockComplexityService);

        controller.addFirstUser(response, authHeader, jaxbUser);

        Assert
            .assertTrue(response.getStatus() == HttpServletResponse.SC_CREATED);
    }

    @Test(expected = CustomerConflictException.class)
    public void shouldNotAddFirstUserForDuplicateCustomer() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockUserService.isUsernameUnique(username)).andReturn(
            true);
        EasyMock.replay(mockUserService);

        mockCustomerService.addCustomer(EasyMock.anyObject(Customer.class));
        EasyMock.expectLastCall().andThrow(new DuplicateException());
        EasyMock.replay(mockCustomerService);

        EasyMock.expect(mockComplexityService.checkPassword(password))
            .andReturn(passingResult);
        EasyMock.replay(mockComplexityService);

        controller.addFirstUser(response, authHeader, jaxbUser);

        Assert
            .assertTrue(response.getStatus() == HttpServletResponse.SC_CREATED);
    }

    @Test(expected = PasswordValidationException.class)
    public void shouldNotAddFirstUserForInvalidPassword() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockUserService.isUsernameUnique(username)).andReturn(
            true);
        mockUserService.addUser(EasyMock.anyObject(testUser.getClass()));
        EasyMock.replay(mockUserService);

        mockCustomerService.addCustomer(EasyMock.anyObject(Customer.class));
        EasyMock.replay(mockCustomerService);

        EasyMock.expect(mockRoleService.getRole(roleName, customerId))
            .andReturn(testRole);
        mockRoleService.addUserToRole(EasyMock.anyObject(User.class),
            EasyMock.anyObject(Role.class));
        EasyMock.replay(mockRoleService);

        EasyMock.expect(mockComplexityService.checkPassword(password))
            .andReturn(failingResult);
        EasyMock.replay(mockComplexityService);

        controller.addFirstUser(response, authHeader, jaxbUser);

        Assert
            .assertTrue(response.getStatus() == HttpServletResponse.SC_CREATED);
    }

    @Test
    public void shouldAddCustomer() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        Customer customerToAdd = new Customer();
        customerToAdd.setCustomerId(testCustomer.getCustomerId());
        customerToAdd.setIname(testCustomer.getIname());
        customerToAdd.setInum(testCustomer.getInum());
        customerToAdd.setIsLocked(testCustomer.getIsLocked());
        customerToAdd.setSoftDeleted(testCustomer.getSoftDeleted());
        customerToAdd.setStatus(CustomerStatus.ACTIVE);

        mockCustomerService.addCustomer(customerToAdd);

        com.rackspace.idm.jaxb.Customer jaxbCustomer = customerConverter
            .toJaxbCustomer(customerToAdd);

        EasyMock.replay(mockCustomerService);

        setupCompanyAuthorizationReturnsTrue("Rackspace");

        controller.addCustomer(response, authHeader, jaxbCustomer);

        Assert
            .assertTrue(response.getStatus() == HttpServletResponse.SC_CREATED);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotAddCustomerBecauseNotAuthorized() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        Customer customerToAdd = new Customer();
        customerToAdd.setCustomerId(testCustomer.getCustomerId());
        customerToAdd.setIname(testCustomer.getIname());
        customerToAdd.setInum(testCustomer.getInum());
        customerToAdd.setIsLocked(testCustomer.getIsLocked());
        customerToAdd.setSoftDeleted(testCustomer.getSoftDeleted());

        customerToAdd.setSeeAlso("");
        customerToAdd.setOwner("");
        customerToAdd.setUniqueId("");

        mockCustomerService.addCustomer(customerToAdd);

        com.rackspace.idm.jaxb.Customer jaxbCustomer = customerConverter
            .toJaxbCustomer(customerToAdd);
        EasyMock.replay(mockCustomerService);

        boolean isTrusted = false;
        AccessToken testtoken = createTestToken(isTrusted);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);
        setupCompanyAuthorizationReturnsFalse("Rackspace");

        controller.addCustomer(response, authHeader, jaxbCustomer);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotAddCustomerBecauseNullCustomer() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        mockCustomerService.addCustomer(testCustomer);

        com.rackspace.idm.jaxb.Customer jaxbCustomer = customerConverter
            .toJaxbCustomer(testCustomer);

        EasyMock.replay(mockCustomerService);

        boolean isTrusted = false;
        AccessToken testtoken = createTestToken(isTrusted);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);
        setupCompanyAuthorizationReturnsFalse(null);

        try {
            controller.addCustomer(response, authHeader, jaxbCustomer);
        } catch (ApiException apiException) {
            Assert
                .assertTrue(apiException.getResponse().getStatus() == HttpServletResponse.SC_UNAUTHORIZED);
        }
    }

    @Test(expected = BadRequestException.class)
    public void shouldNotAddInvalidCustomer() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        Customer customerToAdd = new Customer();
        customerToAdd.setCustomerId(null);
        customerToAdd.setIname(testCustomer.getIname());
        customerToAdd.setInum(testCustomer.getInum());
        customerToAdd.setIsLocked(testCustomer.getIsLocked());
        customerToAdd.setSoftDeleted(testCustomer.getSoftDeleted());

        customerToAdd.setSeeAlso("");
        customerToAdd.setOwner("");
        customerToAdd.setUniqueId("");

        mockCustomerService.addCustomer(customerToAdd);

        com.rackspace.idm.jaxb.Customer jaxbCustomer = customerConverter
            .toJaxbCustomer(customerToAdd);

        EasyMock.replay(mockCustomerService);

        setupCompanyAuthorizationReturnsTrue("Rackspace");

        controller.addCustomer(response, authHeader, jaxbCustomer);
    }

    @Test
    public void shouldNotAddDuplicateCustomer() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        Customer customerToAdd = new Customer();
        customerToAdd.setCustomerId(testCustomer.getCustomerId());
        customerToAdd.setIname(testCustomer.getIname());
        customerToAdd.setInum(testCustomer.getInum());
        customerToAdd.setIsLocked(testCustomer.getIsLocked());
        customerToAdd.setSoftDeleted(testCustomer.getSoftDeleted());
        customerToAdd.setStatus(CustomerStatus.ACTIVE);

        mockCustomerService.addCustomer(customerToAdd);
        EasyMock.expectLastCall().andThrow(
            new DuplicateException("DuplicateCustomer"));
        EasyMock.replay(mockCustomerService);

        com.rackspace.idm.jaxb.Customer jaxbCustomer = customerConverter
            .toJaxbCustomer(customerToAdd);

        setupCompanyAuthorizationReturnsTrue("Rackspace");

        try {
            controller.addCustomer(response, authHeader, jaxbCustomer);
        } catch (ApiException e) {
            Assert.assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), e
                .getError().getCode());
        }
    }

    @Test
    public void shouldGetCustomer() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(
            testCustomer);
        EasyMock.replay(mockCustomerService);

        setupCompanyAuthorizationReturnsTrue("Rackspace");

        com.rackspace.idm.jaxb.Customer jaxbCustomer = controller.getCustomer(
            response, authHeader, customerId);
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
        Assert.assertEquals(testCustomer.getCustomerId(),
            jaxbCustomer.getCustomerId());
        Assert.assertEquals(testCustomer.getIname(), jaxbCustomer.getIname());
        Assert.assertEquals(testCustomer.getInum(), jaxbCustomer.getInum());
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotGetCustomerBecauseNotAuthorized() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(
            testCustomer);
        EasyMock.replay(mockCustomerService);

        boolean isTrusted = false;
        AccessToken testtoken = createTestToken(isTrusted);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);
        setupCompanyAuthorizationReturnsFalse("Rackspace");

        controller.getCustomer(response, authHeader, customerId);

    }

    @Test(expected = NotFoundException.class)
    public void shouldNotGetCustomer() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(
            null);
        EasyMock.replay(mockCustomerService);

        setupCompanyAuthorizationReturnsTrue("Rackspace");

        controller.getCustomer(response, authHeader, customerId);

    }

    @Test
    public void shouldGetCustomersUsers() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        List<User> users = new ArrayList<User>();
        users.add(testUser);

        Users returnedUsers = new Users();
        returnedUsers.setLimit(limit);
        returnedUsers.setOffset(offset);
        returnedUsers.setTotalRecords(totalRecords);
        returnedUsers.setUsers(users);

        setupAdminAuthorizationReturnsTrue(testUser);

        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(
            testCustomer);
        EasyMock.replay(mockCustomerService);

        EasyMock.expect(
            mockUserService.getByCustomerId(customerId, offset, limit))
            .andReturn(returnedUsers);
        EasyMock.replay(mockUserService);

        com.rackspace.idm.jaxb.Users returned = controller.getUsers(response,
            authHeader, customerId, offset, limit);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotGetCustomersUsersBecauseNotAuthorized()
        throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        List<User> users = new ArrayList<User>();
        users.add(testUser);

        Users returnedUsers = new Users();
        returnedUsers.setLimit(limit);
        returnedUsers.setOffset(offset);
        returnedUsers.setTotalRecords(totalRecords);
        returnedUsers.setUsers(users);

        boolean isTrusted = false;
        AccessToken testtoken = createTestToken(isTrusted);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);
        setupAdminAuthorizationReturnsFalse(testUser);

        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(
            testCustomer);

        EasyMock.expect(
            mockUserService.getByCustomerId(customerId, offset, limit))
            .andReturn(returnedUsers);

        EasyMock.replay(mockUserService);

        EasyMock.replay(mockCustomerService);

        com.rackspace.idm.jaxb.Users userlist = controller.getUsers(response,
            authHeader, customerId, offset, limit);

    }

    @Test(expected = NotFoundException.class)
    public void shouldNotGetUsersForNonExistentCustomer() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(
            null);
        EasyMock.replay(mockCustomerService);

        com.rackspace.idm.jaxb.Users users = controller.getUsers(response,
            authHeader, customerId, offset, limit);

    }

    @Test
    public void shouldGetCustomersRoles() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        List<Role> roles = new ArrayList<Role>();
        roles.add(testRole);

        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(
            testCustomer);
        EasyMock.replay(mockCustomerService);

        EasyMock.expect(mockRoleService.getByCustomerId(customerId)).andReturn(
            roles);
        EasyMock.replay(mockRoleService);

        com.rackspace.idm.jaxb.Roles jaxbRoles = controller.getRoles(response,
            customerId);

        List<com.rackspace.idm.jaxb.Role> returnedRoles = jaxbRoles.getRoles();

        Assert.assertTrue(returnedRoles.size() == 1);
        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotGetRolesForNonExistentCustomer() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(
            null);
        EasyMock.replay(mockCustomerService);

        controller.getRoles(response, customerId);

    }

    @Test
    public void shouldGetCustomersClients() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        List<Client> clients = new ArrayList<Client>();
        clients.add(testClient);

        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(
            testCustomer);
        EasyMock.replay(mockCustomerService);

        EasyMock.expect(mockClientService.getByCustomerId(customerId))
            .andReturn(clients);
        EasyMock.replay(mockClientService);

        com.rackspace.idm.jaxb.Clients returnedClients = controller.getClients(
            response, customerId);

        Assert.assertTrue(response.getStatus() == HttpServletResponse.SC_OK);
    }

    @Test(expected = NotFoundException.class)
    public void shouldNotGetClientsForNonExistentCustomer() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(
            null);
        EasyMock.replay(mockCustomerService);

        controller.getClients(response, customerId);

    }

    @Test
    public void shouldDeleteCustomer() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(
            testCustomer);
        mockCustomerService.softDeleteCustomer(customerId);
        EasyMock.replay(mockCustomerService);

        setupCompanyAuthorizationReturnsTrue("Rackspace");
        controller.deleteCustomer(response, authHeader, customerId);

        EasyMock.verify(mockCustomerService);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotDeleteCustomerBecauseNotAuthorized() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(
            testCustomer);
        mockCustomerService.softDeleteCustomer(customerId);
        EasyMock.replay(mockCustomerService);

        boolean isTrusted = false;
        AccessToken testtoken = createTestToken(isTrusted);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);
        setupCompanyAuthorizationReturnsFalse("Rackspace");

        controller.deleteCustomer(response, authHeader, customerId);

    }

    @Test
    public void shouldLockCustomer() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean locked = true;

        setupCompanyAuthorizationReturnsTrue("Rackspace");

        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(
            testCustomer);
        mockCustomerService.setCustomerLocked(testCustomer, locked);
        EasyMock.replay(mockCustomerService);

        com.rackspace.idm.jaxb.Customer inputCustomer = new com.rackspace.idm.jaxb.Customer();

        inputCustomer.setLocked(locked);

        controller.setCustomerLockStatus(response, customerId, authHeader,
            inputCustomer);
        EasyMock.verify(mockCustomerService);
    }

    @Test
    public void shouldUnLockCustomer() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean locked = false;

        setupCompanyAuthorizationReturnsTrue("Rackspace");

        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(
            testCustomer);
        mockCustomerService.setCustomerLocked(testCustomer, locked);
        EasyMock.replay(mockCustomerService);

        com.rackspace.idm.jaxb.Customer inputCustomer = new com.rackspace.idm.jaxb.Customer();

        inputCustomer.setLocked(locked);

        controller.setCustomerLockStatus(response, customerId, authHeader,
            inputCustomer);
        EasyMock.verify(mockCustomerService);
    }

    @Test(expected = ForbiddenException.class)
    public void shouldNotLockCustomerBecauseNotAuthorized() throws Exception {

        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean locked = true;

        boolean isTrusted = false;
        AccessToken testtoken = createTestToken(isTrusted);
        EasyMock.expect(mockOAuthService.getTokenFromAuthHeader(authHeader))
            .andReturn(testtoken);
        setupCompanyAuthorizationReturnsFalse("Rackspace");

        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(
            testCustomer);
        mockCustomerService.setCustomerLocked(testCustomer, locked);
        EasyMock.replay(mockCustomerService);

        com.rackspace.idm.jaxb.Customer inputCustomer = new com.rackspace.idm.jaxb.Customer();

        inputCustomer.setLocked(locked);

        controller.setCustomerLockStatus(response, customerId, authHeader,
            inputCustomer);

    }

    @Test(expected = NotFoundException.class)
    public void shouldNotLockNonExistentCustomer() {

        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean locked = true;

        EasyMock.expect(mockCustomerService.getCustomer(customerId)).andReturn(
            null);
        EasyMock.replay(mockCustomerService);

        com.rackspace.idm.jaxb.Customer inputCustomer = new com.rackspace.idm.jaxb.Customer();

        inputCustomer.setLocked(locked);

        controller.setCustomerLockStatus(response, customerId, authHeader,
            inputCustomer);

    }

    private void setupCompanyAuthorizationReturnsTrue(String companyName)
        throws Exception {
        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        EasyMock.expect(
            mockAuthorizationService.createRequest(EasyMock
                .<List<Entity>> anyObject())).andReturn(authorizationRequest);
        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(true);

        EasyMock.replay(mockAuthorizationService);

        EasyMock.expect(
            mockOAuthService.getCustomerIdFromAuthHeaderToken(authHeader))
            .andReturn(companyName);

        EasyMock.replay(mockOAuthService);

    }

    private void setupCompanyAuthorizationReturnsFalse(String companyName)
        throws Exception {
        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        EasyMock.expect(
            mockAuthorizationService.createRequest(EasyMock
                .<List<Entity>> anyObject())).andReturn(authorizationRequest);
        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(false);

        EasyMock.replay(mockAuthorizationService);

        EasyMock.expect(
            mockOAuthService.getCustomerIdFromAuthHeaderToken(authHeader))
            .andReturn(companyName);

        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn(testUser.getUsername());

        EasyMock.replay(mockOAuthService);
    }

    private void setupAdminAuthorizationReturnsTrue(User user) {

        String oldpassword = password;

        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);

        EasyMock.expect(
            mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn("testUser");
        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        try {
            EasyMock.expect(
                mockAuthorizationService.createRequest(EasyMock
                    .<List<Entity>> anyObject())).andReturn(
                authorizationRequest);
        } catch (XACMLRequestCreationException e) {
            throw new IllegalStateException("Test encountered an error.", e);
        }

        EasyMock.expect(mockUserService.getUser(username)).andReturn(user);

        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(true);
        EasyMock.replay(mockAuthorizationService);
        EasyMock.replay(mockOAuthService);

        List<Role> roleList = new Vector<Role>();

        EasyMock.expect(mockRoleService.getRolesForUser("testUser")).andReturn(
            roleList);
    }

    private void setupAdminAuthorizationReturnsFalse(User user)
        throws XACMLRequestCreationException {

        String oldpassword = password;

        EasyMock.expect(
            mockOAuthService.authenticateUser(username, oldpassword))
            .andReturn(true);

        EasyMock
            .expect(mockOAuthService.getUsernameFromAuthHeaderToken(authHeader))
            .andReturn("testUser").times(2);
        AuthorizationRequest authorizationRequest = new AuthorizationRequest();
        EasyMock.expect(
            mockAuthorizationService.createRequest(EasyMock
                .<List<Entity>> anyObject())).andReturn(authorizationRequest);
        EasyMock.expect(
            mockAuthorizationService.doAuthorization(authorizationRequest))
            .andReturn(false);

        EasyMock.replay(mockAuthorizationService);
        EasyMock.replay(mockOAuthService);

        EasyMock.expect(mockUserService.getUser(username)).andReturn(user);

        List<Role> roleList = new Vector<Role>();

        EasyMock.expect(mockRoleService.getRolesForUser("testUser")).andReturn(
            roleList);
    }

    private AccessToken createTestToken(boolean isTrusted) {
        String tokenString = "asdf1234";
        DateTime expiration = new DateTime().plusHours(1);
        AccessToken testToken = new AccessToken(tokenString, expiration, owner,
            username, IDM_SCOPE.FULL, isTrusted);
        return testToken;
    }
}
