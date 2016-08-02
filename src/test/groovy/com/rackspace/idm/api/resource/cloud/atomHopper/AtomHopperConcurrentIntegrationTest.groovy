package com.rackspace.idm.api.resource.cloud.atomHopper

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.UserService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.MultiStageTask
import testHelpers.MultiStageTaskFactory

/**
 */
class AtomHopperConcurrentIntegrationTest extends RootConcurrentIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired AtomHopperClient atomHopperClient

    @Autowired UserService userService

    User user;

    def setup() {
        user = userService.getUser(Constants.SERVICE_ADMIN_2_USERNAME)
    }

    def "concurrent feeds"() {
        when:
        def List<ForceFeedEvent> runs = concurrentStageTaskRunner.runConcurrent(5, new MultiStageTaskFactory<ForceFeedEvent>() {
            @Override
            ForceFeedEvent createTask() {
                return new ForceFeedEvent()
            }
        })

        then:
        for (ForceFeedEvent run : runs) {
            assert run.exceptionEncountered == null
        }
    }

    private class ForceFeedEvent implements MultiStageTask {

        Throwable exceptionEncountered;

        ForceFeedEvent() {
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
                    atomHopperClient.asyncTokenPost(user, UUID.randomUUID().toString())
                    break;
                default:
                    throw new IllegalStateException("This task does not support stage '" + i + "'")
            }
        }

        Throwable getExceptionEncountered() {
            return exceptionEncountered
        }

        void cleanup() {
        }

    }
}