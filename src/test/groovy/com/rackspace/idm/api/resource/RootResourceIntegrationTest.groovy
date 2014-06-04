package com.rackspace.idm.api.resource

import spock.lang.Shared
import testHelpers.RootIntegrationTest

import static com.rackspace.idm.Constants.*
import static org.apache.http.HttpStatus.*


class RootResourceIntegrationTest extends RootIntegrationTest {

    @Shared def userAdmin, users

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

    def "Test invalid foundation call - /idm/v1/invalid - access false" () {
        given:
        staticIdmConfiguration.setProperty("feature.access.to.foundation.api", false)

        when:
        def response = foundation.invalidFoundationCall()

        then:
        response.status == SC_NOT_FOUND
    }

    def "Test invalid foundation call - /idm/v1/invalid - access true" () {
        given:
        staticIdmConfiguration.setProperty("feature.access.to.foundation.api", true)

        when:
        def response = foundation.invalidFoundationCall()

        then:
        response.status == SC_UNAUTHORIZED
    }

    def "Verify foundation user resource is not found - access false" () {
        given:
        staticIdmConfiguration.setProperty("feature.access.to.foundation.api", false)

        when:
        def response = foundation.getUser("token", "userId")

        then:
        response.status == SC_NOT_FOUND
    }


    def "Verify foundation user resource - access true" () {
        given:

        def domainId = utils.createDomain()
        staticIdmConfiguration.setProperty("feature.access.to.foundation.api", true)

        when:
        (userAdmin, users) = utils.createUserAdmin(domainId)
        def authData  = foundationUtils.authenticate(CLIENT_ID, CLIENT_SECRET)
        def response = foundation.getUser(authData.accessToken.id, userAdmin.id)

        then:
        response.status == SC_OK
    }

    def "Verify foundation user resource - access true - invalid token" () {
        given:

        staticIdmConfiguration.setProperty("feature.access.to.foundation.api", true)

        when:
        def response = foundation.getUser("badToken", "id")

        then:
        response.status == SC_UNAUTHORIZED
    }

    def "Verify foundation tenant resource is not found - access = false" () {
        given:
        staticIdmConfiguration.setProperty("feature.access.to.foundation.api", false)

        when:
        def response = foundation.getTenant("token", "tenantId")

        then:
        response.status == SC_NOT_FOUND
    }

    def "Verify foundation tenant resource - access = true" () {
        given:
        staticIdmConfiguration.setProperty("feature.access.to.foundation.api", true)

        when:
        def tenant = utils.createTenant()
        def authData  = foundationUtils.authenticate(CLIENT_ID, CLIENT_SECRET)
        def response = foundation.getTenant(authData.accessToken.id, tenant.id)

        then:
        response.status == SC_OK
    }

    def "Verify foundation tenant resource - access = true - invalid token" () {
        given:
        staticIdmConfiguration.setProperty("feature.access.to.foundation.api", true)

        when:
        def response = foundation.getTenant("badToken", "id")

        then:
        response.status == SC_UNAUTHORIZED
    }

    def "Test invalid idm call - /idm/invalid/invalid - access to foundation = true" () {
        given:
        staticIdmConfiguration.setProperty("feature.access.to.foundation.api", true)

        when:
        def response = foundation.invalidIdmCall()

        then:
        response.status == SC_UNAUTHORIZED
    }

    def "Test invalid resource call - /idm/invalid - access to foundation = true" () {
        given:
        staticIdmConfiguration.setProperty("feature.access.to.foundation.api", true)

        when:
        def response = foundation.invalidCall()

        then:
        response.status == SC_NOT_FOUND
    }

}
