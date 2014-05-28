package com.rackspace.idm.api.resource

import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*
import static org.apache.http.HttpStatus.*


class RootResourceIntegrationTest extends RootIntegrationTest {

    def "Verify that foundation resource is not accessible: feature.access.to.foundation.api=false" () {
        given:
        staticIdmConfiguration.setProperty("feature.access.to.foundation.api", false)

        when:
        def response = foundation.authenticate(CLIENT_ID, CLIENT_SECRET)

        then:
        response.status == SC_NOT_FOUND
    }

    def "Verify that foundation resource is accessible: feature.access.to.foundation.api=true" () {
        given:
        staticIdmConfiguration.setProperty("feature.access.to.foundation.api", true)

        when:
        def response = foundation.authenticate(CLIENT_ID, CLIENT_SECRET)

        then:
        response.status == SC_OK
    }
}
