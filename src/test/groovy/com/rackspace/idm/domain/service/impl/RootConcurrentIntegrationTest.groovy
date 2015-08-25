package com.rackspace.idm.domain.service.impl

import org.junit.Rule
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.IdentityFault
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import spock.lang.Shared
import testHelpers.ConcurrentStageTaskRunner
import testHelpers.RootIntegrationTest
import testHelpers.junit.ConditionalIgnoreRule

/**
 */
abstract class RootConcurrentIntegrationTest extends RootIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(this.getClass());

    public static final String SERVICE_ADMIN_USERNAME = "authQE";
    public static final String SERVICE_ADMIN_PWD = "Auth1234"

    public static final String IDENTITY_ADMIN_USERNAME_PREFIX = "identityAdmin"
    public static final String USER_ADMIN_USERNAME_PREFIX = "userAdmin"
    public static final String DEFAULT_USER_USERNAME_PREFIX = "defaultUser"

    public static final String DEFAULT_PASSWORD = "Password1"
    public static final String DEFAULT_APIKEY = "0123456789"


    /**
     * Random string generated for entire test class. Same for all feature methods.
     */
    @Shared String SPECIFICATION_RANDOM

    /**
     * Random string that is unique for each feature method
     */
    @Shared String FEATURE_RANDOM

    @Shared
    def specificationServiceAdmin
    @Shared
    def specificationServiceAdminToken
    @Shared
    def specificationIdentityAdmin
    @Shared
    def specificationIdentityAdminToken

    @Shared
    def ConcurrentStageTaskRunner concurrentStageTaskRunner = new ConcurrentStageTaskRunner()

    @Rule
    public ConditionalIgnoreRule role = new ConditionalIgnoreRule()


    /**
     * Like most other tests, this test class depends on a pre-existing service admin (authQE)
     *
     * @return
     */
    def setupSpec() {
        SPECIFICATION_RANDOM = UUID.randomUUID().toString().replaceAll('-', "")

        //login via the already existing service admin user
        def serviceAdminAuthResponse = cloud20.authenticatePassword(SERVICE_ADMIN_USERNAME, SERVICE_ADMIN_PWD).getEntity(AuthenticateResponse)
        //verify the authentication worked before retrieving the token
        if (serviceAdminAuthResponse.value instanceof IdentityFault) {
            def fault = (IdentityFault)serviceAdminAuthResponse.value
            logger.error("Error authenticating service admin to setup test run. '" + fault.getMessage() + "'", fault)
        }
        assert serviceAdminAuthResponse.value instanceof AuthenticateResponse
        specificationServiceAdminToken = serviceAdminAuthResponse.value.token.id
        specificationServiceAdmin = cloud20.getUserByName(specificationServiceAdminToken, SERVICE_ADMIN_USERNAME).getEntity(org.openstack.docs.identity.api.v2.User).value

        //create a new shared identity admin for these tests
        specificationIdentityAdmin = createIdentityAdmin(IDENTITY_ADMIN_USERNAME_PREFIX + SPECIFICATION_RANDOM)
        def identityAdminAuthResponse = cloud20.authenticatePassword(specificationIdentityAdmin.getUsername(), DEFAULT_PASSWORD).getEntity(AuthenticateResponse)
        //verify the authentication worked before retrieving the token
        assert identityAdminAuthResponse.value instanceof AuthenticateResponse
        specificationIdentityAdminToken = identityAdminAuthResponse.value.token.id

        concurrentStageTaskRunner = new ConcurrentStageTaskRunner()
    }

    def cleanupSpec() {
        deleteUserQuietly(specificationIdentityAdmin)
    }

    def setup() {
        utils.resetServiceAdminToken()
        FEATURE_RANDOM = getNormalizedRandomString()
    }

    def createIdentityAdmin(String identityAdminUsername = IDENTITY_ADMIN_USERNAME_PREFIX + getNormalizedRandomString(), String domainId = getNormalizedRandomString()) {
        def createResponse = cloud20.createUser(specificationServiceAdminToken, v2Factory.createUserForCreate(identityAdminUsername, "display", "test@rackspace.com", true, null, null, DEFAULT_PASSWORD))
        def userAdmin = cloud20.getUserByName(specificationServiceAdminToken, identityAdminUsername).getEntity(org.openstack.docs.identity.api.v2.User).value
        return userAdmin;
    }

    def authenticate(String userName, String password = DEFAULT_PASSWORD) {
        def token = cloud20.authenticatePassword(userName, password).getEntity(AuthenticateResponse).value.token.id
        return token;
    }

    def authenticateApiKey(String userName, String apiKey = DEFAULT_APIKEY) {
        def token = cloud20.authenticateApiKey(userName, apiKey).getEntity(AuthenticateResponse).value.token.id
        return token;
    }

    def deleteUserQuietly(user) {
        if (user != null) {
            try {
                cloud20.destroyUser(specificationServiceAdminToken, user.getId())
            } catch (all) {
                //ignore
            }
        }
    }

    def createUserAdmin(String callerToken = specificationIdentityAdminToken, String adminUsername = USER_ADMIN_USERNAME_PREFIX + getNormalizedRandomString(), String domainId = getNormalizedRandomString()) {
        cloud20.createUser(callerToken, v2Factory.createUserForCreate(adminUsername, "display", adminUsername + "@rackspace.com", true, null, domainId, DEFAULT_PASSWORD))
        def userAdmin = cloud20.getUserByName(callerToken, adminUsername).getEntity(org.openstack.docs.identity.api.v2.User).value
        def createApiKey = cloud20.addApiKeyToUser(callerToken, userAdmin.id, v2Factory.createApiKeyCredentials(userAdmin.username, DEFAULT_APIKEY))
        return userAdmin;
    }

    def createDefaultUser(String callerToken, String userName = DEFAULT_USER_USERNAME_PREFIX + getNormalizedRandomString()) {
        cloud20.createUser(callerToken, v2Factory.createUserForCreate(userName, "display", "test@rackspace.com", true, null, null, DEFAULT_PASSWORD))
        def user = cloud20.getUserByName(callerToken, userName).getEntity(org.openstack.docs.identity.api.v2.User).value
        return user
    }

    def String getNormalizedRandomString() {
        return UUID.randomUUID().toString().replaceAll('-', "")
    }




}
