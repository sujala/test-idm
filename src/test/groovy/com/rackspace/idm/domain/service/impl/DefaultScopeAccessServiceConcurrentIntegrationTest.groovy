package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.dao.impl.LdapScopeAccessRepository
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Ignore
import spock.lang.Shared
import testHelpers.ConcurrentStageTaskRunner
import testHelpers.MultiStageTask
import testHelpers.MultiStageTaskFactory
import testHelpers.RootIntegrationTest

/**
 */
class DefaultScopeAccessServiceConcurrentIntegrationTest extends RootIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired DefaultScopeAccessService scopeAccessService

    @Autowired LdapScopeAccessRepository saRepository

    @Autowired DefaultUserService userService;

    private static final String SERVICE_ADMIN_USERNAME = "authQE";
    private static final String SERVICE_ADMIN_PWD = "Auth1234"
    public static final String IDENTITY_ADMIN_USERNAME_PREFIX = "identityAdmin"
    public static final String DEFAULT_PASSWORD = "Password1"
    public static final String CLIENT_ID = "bde1268ebabeeabb70a0e702a4626977c331d5c4"

    /**
     * Random string generated for entire test class. Same for all feature methods.
     */
    @Shared String SPECIFICATION_RANDOM

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
        assert serviceAdminAuthResponse.value instanceof AuthenticateResponse
        specificationServiceAdminToken = serviceAdminAuthResponse.value.token.id
        specificationServiceAdmin = cloud20.getUserByName(specificationServiceAdminToken, SERVICE_ADMIN_USERNAME).getEntity(org.openstack.docs.identity.api.v2.User)

        //create a new shared identity admin for these tests
        specificationIdentityAdmin = createIdentityAdmin(IDENTITY_ADMIN_USERNAME_PREFIX + SPECIFICATION_RANDOM)

        def identityAdminAuthResponse = cloud20.authenticatePassword(specificationIdentityAdmin.getUsername(), DEFAULT_PASSWORD).getEntity(AuthenticateResponse)
        //verify the authentication worked before retrieving the token
        assert identityAdminAuthResponse.value instanceof AuthenticateResponse
        specificationIdentityAdminToken = identityAdminAuthResponse.value.token.id
    }

    def cleanupSpec() {
        deleteUserQuietly(specificationIdentityAdmin)
    }

    /**
     * This test seeds a user with an expired token, and then attempts to create 2 new scope accesses in different threads.
     */
    @Ignore("Ignored due to current concurrent bug where multi-threads will try to delete the expired scope access with the last thread failing")
    def "updating expired tokens"() {
        setup:
        def final domainUser = specificationIdentityAdmin = userService.checkAndGetUserById(specificationIdentityAdmin.getId());

        //seed the repo with an expired token
        def UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setUsername(specificationIdentityAdmin.username);
        userScopeAccess.setUserRsId(specificationIdentityAdmin.getId());
        userScopeAccess.setClientId(CLIENT_ID);
        userScopeAccess.setAccessTokenString(UUID.randomUUID().toString().replaceAll('-', ""));
        userScopeAccess.setAccessTokenExp(new DateTime().minusDays(1).toDate());  //yesterday
        def createdScopeAccess = saRepository.addDirectScopeAccess(specificationIdentityAdmin.getUniqueId(), userScopeAccess);
        logger.debug("Seeding user " + domainUser.getUniqueId() + "' with expired scope access for access token '" + createdScopeAccess.getAccessTokenString() + "'")

        when:
        def List<ScopeAccessStagedTask> runs = concurrentStageTaskRunner.runConcurrent(2, new MultiStageTaskFactory<ScopeAccessStagedTask>() {
            @Override
            ScopeAccessStagedTask createTask() {
                return new ScopeAccessStagedTask(domainUser, CLIENT_ID)
            }
        })

        then:
        for (ScopeAccessStagedTask run : runs) {
            assert run.exceptionEncountered == null
        }

        cleanup:
        if (createdScopeAccess != null) {
            try {
                saRepository.deleteScopeAccess(createdScopeAccess)
            }
            catch (Exception e) {
                //eat exceptions here cause should have already been deleted as part of test, but want to make sure it's gone
            }
        }
    }

    def createIdentityAdmin(String identityAdminUsername = IDENTITY_ADMIN_USERNAME_PREFIX + getNormalizedRandomString(), String domainId = getNormalizedRandomString()) {
        def createResponse = cloud20.createUser(specificationServiceAdminToken, v2Factory.createUserForCreate(identityAdminUsername, "display", "test@rackspace.com", true, null, null, DEFAULT_PASSWORD))
        def userAdmin = cloud20.getUserByName(specificationServiceAdminToken, identityAdminUsername).getEntity(org.openstack.docs.identity.api.v2.User)
        return userAdmin;
    }

    def authenticate(String userName) {
        def token = cloud20.authenticatePassword(userName, DEFAULT_PASSWORD).getEntity(AuthenticateResponse).value.token.id
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

    def String getNormalizedRandomString() {
        return UUID.randomUUID().toString().replaceAll('-', "")
    }

    private class ScopeAccessStagedTask implements MultiStageTask {
        User user;
        String clientId;

        UserScopeAccess updatedScopeAccess;

        Throwable exceptionEncountered;

        ScopeAccessStagedTask(User user, String clientId) {
            this.user = user
            this.clientId = clientId;
        }

        @Override
        int getNumberConcurrentStages() {
            return 1
        }

        @Override
        void setup() {
        }

        @Override
        void runStage(int i) {
            switch (i) {
                case 1:
                    updateExpiredUserScopeAccess()
                    break;
                default:
                    throw new IllegalStateException("This task does not support stage '" + i + "'")
            }
        }

        void updateExpiredUserScopeAccess() {
            //update the token. This will try to delete the expired tokens. When run concurrently, this will fail.
            try {
                logger.debug("Updating expired scope access")
                updatedScopeAccess = scopeAccessService.updateExpiredUserScopeAccess(user, clientId, null);
            } catch (Exception ex) {
                logger.error("Exception caught updating expired tokens")
                ex.printStackTrace()
                exceptionEncountered = ex;
            }
        }

        Throwable getExceptionEncountered() {
            return exceptionEncountered
        }

        void cleanup() {
            if (updatedScopeAccess != null) {
                try {
                    saRepository.deleteScopeAccess(updatedScopeAccess)
                }
                catch (Exception e) {
                    //eat exceptions here cause some should have already been deleted. An error here is fine.
                }
            }
        }

    }


}
