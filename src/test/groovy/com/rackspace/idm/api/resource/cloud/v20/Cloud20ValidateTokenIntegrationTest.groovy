package com.rackspace.idm.api.resource.cloud.v20

import spock.lang.Shared
import testHelpers.RootIntegrationTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 11/15/13
 * Time: 12:23 PM
 * To change this template use File | Settings | File Templates.
 */
class Cloud20ValidateTokenIntegrationTest extends RootIntegrationTest{

    @Shared def racker = "test.racker"
    @Shared def rackerPassword = "password"

    def "AuthenticatedBy should be displayed for racker token" () {
        when:
        def response = utils.authenticateRacker(racker, rackerPassword)
        utils.revokeToken(response.token.id)
        response = utils.authenticateRacker(racker, rackerPassword)
        def validateResponse = utils.validateToken(response.token.id)

        then:
        validateResponse != null
        validateResponse.token.authenticatedBy.credential.contains("PASSWORD")
    }


}
