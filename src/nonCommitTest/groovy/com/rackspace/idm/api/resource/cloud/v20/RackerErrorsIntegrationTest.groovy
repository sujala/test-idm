package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.AEScopeAccessDao
import com.rackspace.idm.domain.entity.Racker
import com.rackspace.idm.domain.entity.RackerScopeAccess
import org.apache.http.HttpStatus
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.RootIntegrationTest

/**
 * Performs various negative testing when the racker is in an error state or missing from AD. The integration with
 * the racker repository (e.g. AD) only applies to 3 services:
 * <ul>
 *     <li>v2.0 Authentication</li>
 *     <li>v2.0 Validation</li>
 *     <li>v2.0 Impersonation</li>
 * <ul>
 *
 * These tests verify that these services appropriately deal with invalid states of users within the racker
 * repository.
 */
class RackerErrorsIntegrationTest extends RootIntegrationTest {

    @Autowired
    AEScopeAccessDao aeScopeAccessDao

    def "Racker auth with dup uid returns 502"() {
        when:
        def response = cloud20.authenticateRacker(Constants.RACKER_DUP_UID, Constants.RACKER_DUP_UID_PASSWORD)

        then:
        response.status == HttpStatus.SC_BAD_GATEWAY
    }

    def "Racker username not found returns 401"() {
        when:
        def response = cloud20.authenticateRacker("doesnotexit", Constants.RACKER_DUP_UID_PASSWORD)

        then:
        response.status == HttpStatus.SC_UNAUTHORIZED
    }

    /**
     * When a racker token is used as a caller the code does not validate that the
     * caller still exists/is valid within AD. It just verifies the issued token is still
     * valid and has not been revoked. The racker repository (AD) is not checked to verify the user still exists
     * within it - hence a 401 wouldn't be returned. However, rackers can only self validate their own token. The validation of
     * a racker token pulls the roles for the racker from AD. If the racker does not exist, a 404 is returned.
     *
     * While this situation should probably return a 401 instead of a 404 (since the caller is *not* valid), that's
     * not the legacy behavior. That said, these tests ensure that the service will fail in some manner when the calling
     * racker does not exist in AD.
     */
    def "Racker self validate with missing user returns 404"() {
        def nonExistantUserToken = generateTokenForRackerId("nonexistant")

        when:
        def response = cloud20.validateToken(nonExistantUserToken.accessTokenString, nonExistantUserToken.accessTokenString)

        then:
        response.status == HttpStatus.SC_NOT_FOUND
    }

    def "Validate token for non-existent Racker returns 404"() {
        def nonExistantUserToken = generateTokenForRackerId("nonexistant")

        when: "A service admin validates a token issued to a no longer existing racker"
        def response = cloud20.validateToken(utils.getServiceAdminToken(), nonExistantUserToken.accessTokenString)

        then:
        response.status == HttpStatus.SC_NOT_FOUND
    }

    /**
     * When a racker token is used as a caller the code does not validate that the
     * caller still exists/is valid within AD. It just verifies the issued token is still
     * valid and has not been revoked. The racker repository (AD) is not checked to verify the user still exists
     * within it - hence a 401 wouldn't be returned. However, rackers can only self validate their own token. The validation of
     * a racker token pulls the roles for the racker from AD. If the racker does not exist, a 404 is returned.
     *
     * While this situation should probably return a 401 instead of a 404 (since the caller is *not* valid), that's
     * not the legacy behavior. That said, these tests ensure that the service will fail in some manner when the calling
     * racker does not exist in AD.
     */
    def "Racker self validate for racker with dup uid returns 502"() {
        def rackerToken = generateTokenForRackerId(Constants.RACKER_DUP_UID)

        when:
        def response = cloud20.validateToken(rackerToken.accessTokenString, rackerToken.accessTokenString)

        then:
        response.status == HttpStatus.SC_BAD_GATEWAY
    }

    def "Validate token for Racker with dup UID attribute returns 502"() {
        def rackerToken = generateTokenForRackerId(Constants.RACKER_DUP_UID)

        when: "A service admin validates a token issued to a racker that has a duplicate uid"
        def response = cloud20.validateToken(utils.getServiceAdminToken(), rackerToken.accessTokenString)

        then:
        response.status == HttpStatus.SC_BAD_GATEWAY
    }

    /**
     * In order to impersonate the racker must have a specific role in AD. The roles are pulled real-time on every
     * impersonation so if the racker's roles can not be pulled, the call must fail.
     */
    def "Impersonate user with racker token for racker has dup uid will return 502"() {
        def rackerToken = generateTokenForRackerId(Constants.RACKER_DUP_UID)
        User impersonatedUser = new User().with {
            it.username = Constants.TEST_USER_USER_ADMIN_USERNAME
            it
        }

        when:
        def response = cloud20.impersonate(rackerToken.accessTokenString, impersonatedUser)

        then:
        response.status == HttpStatus.SC_BAD_GATEWAY
    }

    /**
     * When a racker token is used as a caller for a racker that no longer exists in AD the code should probably return
     * a 401. However, that's not the current behavior. Instead it returns a 404 since the user does not exist in AD
     */
    def "Impersonate user with racker token for non-existant racker returns 404"() {
        def rackerToken = generateTokenForRackerId("nonexistant")
        User impersonatedUser = new User().with {
            it.username = Constants.TEST_USER_USER_ADMIN_USERNAME
            it
        }

        when:
        def response = cloud20.impersonate(rackerToken.accessTokenString, impersonatedUser)

        then:
        response.status == HttpStatus.SC_NOT_FOUND
    }

    RackerScopeAccess generateTokenForRackerId(id) {
        // Can't authenticate using standard auth call for a user that doesn't exist. So hack generating
        // a token for a non-existant user
        Racker racker = new Racker().with {
            it.rackerId = id
            it.username = it.rackerId
            it
        }
        RackerScopeAccess token = new RackerScopeAccess().with {
            it.rackerId = racker.id
            it.accessTokenExp = new DateTime().plusDays(1).toDate()
            it.createTimestamp = new Date()
            it
        }
        aeScopeAccessDao.addScopeAccess(racker, token)
        token
    }
}
