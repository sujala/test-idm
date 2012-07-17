package com.rackspace.api.idm.v1;

import org.junit.Before;
import org.junit.Test;
import org.w3._2005.atom.Link;

import javax.xml.bind.JAXBElement;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Created with IntelliJ IDEA.
 * User: Yung Lai
 * Date: 7/10/12
 * Time: 10:02 AM
 * To change this template use File | Settings | File Templates.
 */
public class ObjectFactoryTest {
    ObjectFactory objectFactory;
    UserList userList;
    Tenant tenant;
    ApplicationList applicationList;
    Racker racker;
    Application application;
    IdentityProfileList identityProfileList;
    PasswordRuleResult passwordRuleResult;
    RoleList roleList;
    User user;
    IdmFault idmFault;

    @Before
    public void setUp() throws Exception {
        objectFactory = new ObjectFactory();

        // Classes that needs to be tested
        userList = objectFactory.createUserList();
        tenant = objectFactory.createTenant();
        applicationList = objectFactory.createApplicationList();
        racker = objectFactory.createRacker();
        application = objectFactory.createApplication();
        identityProfileList = objectFactory.createIdentityProfileList();
        passwordRuleResult = objectFactory.createPasswordRuleResult();
        roleList = objectFactory.createRoleList();
        user = objectFactory.createUser();
        idmFault = objectFactory.createIdmFault();
    }

    @Test
    public void createUsernameConflictFault_returnsNewCreatedObject() throws Exception {
        UsernameConflictFault result = objectFactory.createUsernameConflictFault();
        assertThat("detail", result.getDetail(), equalTo(null));
    }

    @Test
    public void createClientGroupConflictFault_returnsNewCreatedObject() throws Exception {
        ClientGroupConflictFault result = objectFactory.createClientGroupConflictFault();
        assertThat("detail", result.getDetail(), equalTo(null));
    }

    @Test
    public void userList_getLink_linkIsNull_returnsNewList() throws Exception {
        List<Link> result = userList.getLink();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void userList_getLink_linkExists_returnsExistingList() throws Exception {
        List<Link> link = userList.getLink();
        link.add(new Link());
        List<Link> result = userList.getLink();
        assertThat("list", result.size(), equalTo(1));
    }

    @Test
    public void createNotProvisionedFault_returnsNewCreatedObject() throws Exception {
        NotProvisionedFault result = objectFactory.createNotProvisionedFault();
        assertThat("detail", result.getDetail(), equalTo(null));
    }

    @Test
    public void createRackerCredentials_returnsNewCreatedObject() throws Exception {
        RackerCredentials result = objectFactory.createRackerCredentials();
        assertThat("detail", result.authorizationCode, equalTo(null));
    }

    @Test
    public void tenant_getAny_anyIsNull_returnsNewList() throws Exception {
        List<Object> result = tenant.getAny();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void tenant_getAny_anyExists_returnsExistingList() throws Exception {
        List<Object> any = tenant.getAny();
        any.add("test");
        List<Object> result = tenant.getAny();
        assertThat("list", result.size(), equalTo(1));
    }

    @Test
    public void createEmailConflictFault_returnsNewCreatedObject() throws Exception {
        EmailConflictFault result = objectFactory.createEmailConflictFault();
        assertThat("detail", result.getDetail(), equalTo(null));
    }

    @Test
    public void createApplicationNameConflictFault_returnsNewCreatedObject() throws Exception {
        ApplicationNameConflictFault result = objectFactory.createApplicationNameConflictFault();
        assertThat("detail", result.getDetail(), equalTo(null));
    }

    @Test
    public void createUserNotFoundFault_returnsNewCreatedObject() throws Exception {
        UserNotFoundFault result = objectFactory.createUserNotFoundFault();
        assertThat("detail", result.getDetail(), equalTo(null));
    }

    @Test
    public void applicationList_getLink_linkIsNull_returnsNewList() throws Exception {
        List<Link> result = applicationList.getLink();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void applicationList_getLink_linkExists_returnsExistingList() throws Exception {
        List<Link> link = applicationList.getLink();
        link.add(new Link());
        List<Link> result = applicationList.getLink();
        assertThat("list", result.size(), equalTo(1));
    }

    @Test
    public void racker_getFirstName_returnsFirstName() throws Exception {
        racker.setFirstName("test");
        String result = racker.getFirstName();
        assertThat("first name", result, equalTo("test"));
    }

    @Test
    public void racker_getLastName_returnsLastName() throws Exception {
        racker.setLastName("test");
        String result = racker.getLastName();
        assertThat("Last name", result, equalTo("test"));
    }

    @Test
    public void racker_getDisplayName_returnsDisplayName() throws Exception {
        racker.setDisplayName("test");
        String result = racker.getDisplayName();
        assertThat("Display name", result, equalTo("test"));
    }

    @Test
    public void application_getRoles_returnsRoles() throws Exception {
        RoleList result = application.getRoles();
        assertThat("role list", result, equalTo(null));
    }

    @Test
    public void identityProfileList_getIdentityProfile_identityProfileIsNull_returnsNewList() throws Exception {
        List<IdentityProfile> result = identityProfileList.getIdentityProfile();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void identityProfileList_getIdentityProfile_identityProfileExists_returnsExistingList() throws Exception {
        List<IdentityProfile> IdentityProfile = identityProfileList.getIdentityProfile();
        IdentityProfile.add(new IdentityProfile());
        List<IdentityProfile> result = identityProfileList.getIdentityProfile();
        assertThat("list", result.size(), equalTo(1));
    }

    @Test
    public void createStalePasswordFault_returnsNewCreatedObject() throws Exception {
        StalePasswordFault result = objectFactory.createStalePasswordFault();
        assertThat("details", result.getDetail(), equalTo(null));
    }

    @Test
    public void createBaseUrlIdConflictFault_returnsNewCreatedObject() throws Exception {
        BaseUrlIdConflictFault result = objectFactory.createBaseUrlIdConflictFault();
        assertThat("detail", result.getDetail(), equalTo(null));
    }

    @Test
    public void createPermisionIdConflictFault_returnsNewCreatedObject() throws Exception {
        PermisionIdConflictFault result = objectFactory.createPermisionIdConflictFault();
        assertThat("detail", result.getDetail(), equalTo(null));
    }

    @Test
    public void createPasswordSelfUpdateTooSoonFault_returnsNewCreatedObject() throws Exception {
        PasswordSelfUpdateTooSoonFault result = objectFactory.createPasswordSelfUpdateTooSoonFault();
        assertThat("detail", result.getDetail(), equalTo(null));
    }

    @Test
    public void createAuthCredentials_returnsNewCreatedObject() throws Exception {
        AuthCredentials result = objectFactory.createAuthCredentials();
        assertThat("detail", result.authorizationCode, equalTo(null));
    }

    @Test
    public void passwordRuleResult_isPassed_returnsBoolean() throws Exception {
        boolean result = passwordRuleResult.isPassed();
        assertThat("boolean", result, equalTo(false));
    }

    @Test
    public void createCustomerIdConflictFault_returnsNewCreatedObject() throws Exception {
        CustomerIdConflictFault result = objectFactory.createCustomerIdConflictFault();
        assertThat("detail", result.getDetail(), equalTo(null));
    }

    @Test
    public void createPasswordValidationFault_returnsNewCreatedObject() throws Exception {
        PasswordValidationFault result = objectFactory.createPasswordValidationFault();
        assertThat("detail", result.getDetail(), equalTo(null));
    }

    @Test
    public void createUserDisabledFault_returnsNewCreatedObject() throws Exception {
        UserDisabledFault result = objectFactory.createUserDisabledFault();
        assertThat("detail", result.getDetail(), equalTo(null));
    }

    @Test
    public void createRSACredentials_returnsNewCreatedObject() throws Exception {
        RSACredentials result = objectFactory.createRSACredentials();
        assertThat("detail", result.authorizationCode, equalTo(null));
    }

    @Test
    public void roleList_getLink_linkIsNull_returnsNewList() throws Exception {
        List<Link> result = roleList.getLink();
        assertThat("list", result.isEmpty(), equalTo(true));
    }

    @Test
    public void roleList_getLink_linkExists_returnsExistingList() throws Exception {
        List<Link> link = roleList.getLink();
        link.add(new Link());
        List<Link> result = roleList.getLink();
        assertThat("list", result.size(), equalTo(1));
    }

    @Test
    public void roleList_getOffset_returnsOffset() throws Exception {
        roleList.setOffset(1);
        Integer result = roleList.getOffset();
        assertThat("offset", result, equalTo(1));
    }

    @Test
    public void roleList_getLimit_returnsLimit() throws Exception {
        roleList.setLimit(1);
        Integer result = roleList.getLimit();
        assertThat("Limit", result, equalTo(1));
    }

    @Test
    public void user_getRoles_returnRoles() throws Exception {
        RoleList result = user.getRoles();
        assertThat("role list", result, equalTo(null));
    }

    @Test
    public void createPasswordRotationPolicy_returnsNewCreatedObject() throws Exception {
        PasswordRotationPolicy result = objectFactory.createPasswordRotationPolicy();
        assertThat("duration", result.duration, equalTo(0));
    }

    @Test
    public void idmFault_getMessage_returnsMessage() throws Exception {
        idmFault.setMessage("test");
        String result = idmFault.getMessage();
        assertThat("message", result, equalTo("test"));
    }

    @Test
    public void idmFault_getDetails_returnsDetails() throws Exception {
        idmFault.setDetails("test");
        String result = idmFault.getDetails();
        assertThat("Details", result, equalTo("test"));
    }

    @Test
    public void idmFault_getCode_returnsCode() throws Exception {
        idmFault.setCode(1);
        int result = idmFault.getCode();
        assertThat("Code", result, equalTo(1));
    }

    @Test
    public void createTenants_returnsNewCreatedJAXBElementObject() throws Exception {
        JAXBElement<Tenants> result = objectFactory.createTenants(new Tenants());
        assertThat("tenant", result.getValue().tenant, equalTo(null));
    }

    @Test
    public void createPasswordRuleResult_returnsNewCreatedJAXBElementObject() throws Exception {
        JAXBElement<PasswordRuleResult> result = objectFactory.createPasswordRuleResult(new PasswordRuleResult());
        assertThat("rule message", result.getValue().ruleMessage, equalTo(null));
    }

    @Test
    public void createRackerCredentials_returnsNewCreatedJAXBElementObject() throws Exception {
        JAXBElement<RackerCredentials> result = objectFactory.createRackerCredentials(new RackerCredentials());
        assertThat("authorization code", result.getValue().authorizationCode, equalTo(null));
    }

    @Test
    public void createRole_returnsNewCreatedJAXBElementObject() throws Exception {
        JAXBElement<Role> result = objectFactory.createRole(new Role());
        assertThat("application id", result.getValue().applicationId, equalTo(null));
    }

    @Test
    public void createUserPassword_returnsNewCreatedJAXBElementObject() throws Exception {
        JAXBElement<UserPassword> result = objectFactory.createUserPassword(new UserPassword());
        assertThat("password", result.getValue().password, equalTo(null));
    }

    @Test
    public void createRSACredentials_returnsNewCreatedJAXBElementObject() throws Exception {
        JAXBElement<RSACredentials> result = objectFactory.createRsaCredentials(new RSACredentials());
        assertThat("authorization code", result.getValue().authorizationCode, equalTo(null));
    }

    @Test
    public void createDelegatedToken_returnsNewCreatedJAXBElementObject() throws Exception {
        JAXBElement<DelegatedToken> result = objectFactory.createDelegatedToken(new DelegatedToken());
        assertThat("client id", result.getValue().clientId, equalTo(null));
    }

    @Test
    public void createCustomerIdentityprofiles_returnsNewCreatedJAXBElementObject() throws Exception {
        JAXBElement<IdentityProfileList> result = objectFactory.createCustomeridentityprofiles(new IdentityProfileList());
        assertThat("identity profile", result.getValue().identityProfile, equalTo(null));
    }

    @Test
    public void createAuthCredentials_returnsNewCreatedJAXBElementObject() throws Exception {
        JAXBElement<AuthCredentials> result = objectFactory.createAuthCredentials(new AuthCredentials());
        assertThat("client id", result.getValue().clientId, equalTo(null));
    }

    @Test
    public void createEmailConflict_returnsNewCreatedJAXBElementObject() throws Exception {
        JAXBElement<EmailConflictFault> result = objectFactory.createEmailConflict(new EmailConflictFault());
        assertThat("detail", result.getValue().getDetail(), equalTo(null));
    }

    @Test
    public void createUserNotFound_returnsNewCreatedJAXBElementObject() throws Exception {
        JAXBElement<UserNotFoundFault> result = objectFactory.createMissingUsername(new UserNotFoundFault());
        assertThat("detail", result.getValue().getDetail(), equalTo(null));
    }

    @Test
    public void createPasswordRuleResults_returnsNewCreatedJAXBElementObject() throws Exception {
        JAXBElement<PasswordRuleResultList> result = objectFactory.createPasswordRuleResults(new PasswordRuleResultList());
        assertThat("password rule results", result.getValue().passwordRuleResults, equalTo(null));
    }

    @Test
    public void authGrantType_fromValue_returnsValue() throws Exception {
        AuthGrantType result = AuthGrantType.fromValue("RACKER");
        assertThat("racker", result.toString(), equalTo("RACKER"));
    }
}
