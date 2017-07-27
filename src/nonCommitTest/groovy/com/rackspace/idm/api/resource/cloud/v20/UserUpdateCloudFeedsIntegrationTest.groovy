package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.core.event.EventType
import org.apache.commons.lang.RandomStringUtils
import org.mockserver.verify.VerificationTimes
import testHelpers.RootIntegrationTest


class UserUpdateCloudFeedsIntegrationTest extends RootIntegrationTest {

    def "test v1.1 update username creates only an update cloud feeds event"() {
        given:
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), domainId)

        when:
        resetCloudFeedsMock()
        def userForUpdate = v1Factory.createUser(user.username, null, null, null, true)
        def response = cloud11.updateUser(user.username, userForUpdate)

        then:
        response.status == 200

        and: "verify that only 1 event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(1)
        )

        and: "verify that the UPDATE event was posted"
        cloudFeedsMock.verify(
                testUtils.createUpdateUserFeedsRequest(user, EventType.UPDATE.name()),
                VerificationTimes.exactly(1)
        )

        cleanup:
        utils.deleteUser(user)
    }

    def "test v2.0 updating an attribute that is not the 'enabled' attribute creates only an update cloud feeds event"() {
        given:
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), domainId)

        when:
        resetCloudFeedsMock()
        user.email = "${RandomStringUtils.randomAlphabetic(8)}@example.com"
        def response = cloud20.updateUser(utils.getIdentityAdminToken(), user.id, user)

        then:
        response.status == 200

        and: "verify that only 1 event was posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.exactly(1)
        )

        and: "verify that the update event was posted"
        cloudFeedsMock.verify(
                testUtils.createUpdateUserFeedsRequest(user, EventType.UPDATE.name()),
                VerificationTimes.exactly(1)
        )

        cleanup:
        utils.deleteUser(user)
    }

    def "test disable user through v2.0 update user call creates 2 update user cloud feed events"() {
        given:
        def domainId = utils.createDomain()
        def user = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), domainId)

        when:
        resetCloudFeedsMock()
        user.enabled = false
        def response = cloud20.updateUser(utils.getIdentityAdminToken(), user.id, user)

        then:
        response.status == 200

        and: "verify that at least 2 events were posted"
        cloudFeedsMock.verify(
                testUtils.createFeedsRequest(),
                VerificationTimes.atLeast(2)
        )

        and: "verify that the UPDATE event was posted"
        cloudFeedsMock.verify(
                testUtils.createUpdateUserFeedsRequest(user, EventType.SUSPEND.name()),
                VerificationTimes.exactly(1)
        )

        and: "verify that the SUSPEND event was posted"
        cloudFeedsMock.verify(
                testUtils.createUpdateUserFeedsRequest(user, EventType.UPDATE.name()),
                VerificationTimes.exactly(1)
        )

        cleanup:
        utils.deleteUser(user)
    }

}
