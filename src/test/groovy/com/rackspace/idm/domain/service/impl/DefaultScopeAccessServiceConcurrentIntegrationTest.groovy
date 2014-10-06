package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.entity.UserScopeAccess
import org.joda.time.DateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import testHelpers.MultiStageTask
import testHelpers.MultiStageTaskFactory

/**
 */
class DefaultScopeAccessServiceConcurrentIntegrationTest extends RootConcurrentIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired DefaultScopeAccessService scopeAccessService

    @Autowired
    @Qualifier("scopeAccessDao")
    ScopeAccessDao saRepository

    @Autowired DefaultUserService userService;

    public static final String CLIENT_ID = "bde1268ebabeeabb70a0e702a4626977c331d5c4"

    /**
     * This test seeds a user with an expired token, and then attempts to create 2 new scope accesses in different threads.
     */
    def "updating expired tokens"() {
        setup:
        def final domainUser = specificationIdentityAdmin = userService.checkAndGetUserById(specificationIdentityAdmin.getId());

        //seed the repo with an expired token
        UserScopeAccess userScopeAccess = new UserScopeAccess();
        userScopeAccess.setUsername(specificationIdentityAdmin.username);
        userScopeAccess.setUserRsId(specificationIdentityAdmin.getId());
        userScopeAccess.setClientId(CLIENT_ID);
        userScopeAccess.setAccessTokenString(UUID.randomUUID().toString().replaceAll('-', ""));
        userScopeAccess.setAccessTokenExp(new DateTime().minusDays(1).toDate());  //yesterday
        saRepository.addScopeAccess(specificationIdentityAdmin, userScopeAccess);
        logger.debug("Seeding user " + domainUser.getUniqueId() + "' with expired scope access for access token '" + userScopeAccess.getAccessTokenString() + "'")

        when:
        def List<ScopeAccessStagedTask> runs = concurrentStageTaskRunner.runConcurrent(5, new MultiStageTaskFactory<ScopeAccessStagedTask>() {
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
        if (userScopeAccess != null) {
            try {
                saRepository.deleteScopeAccess(userScopeAccess)
            }
            catch (Exception e) {
                //eat exceptions here cause should have already been deleted as part of test, but want to make sure it's gone
            }
        }
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
