package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.exception.BadRequestException
import spock.lang.Shared
import testHelpers.RootServiceTest

class Cloud20VersionResourceTest extends RootServiceTest {

    @Shared Cloud20VersionResource service

    def setup() {
        service = new Cloud20VersionResource()
        mockConfiguration(service)
    }

    def "validateOffset null offset sets offset to 0"() {
        when:
        def offset = service.validateMarker(null)

        then:
        offset == 0
    }

    def "validateOffset negative offset throws bad request"() {
        when:
        service.validateMarker(-5)

        then:
        thrown(BadRequestException)
    }

    def "validateOffset valid offset sets offset"() {
        when:
        def offset = service.validateMarker(10)

        then:
        offset == 10
    }

    def "validateLimit null limit sets limit to default"() {
        when:
        config.getInt(_) >> 25
        def limit = service.validateLimit(null)

        then:
        limit == 25
    }

    def "validateLimit negative limit throws bad request"() {
        when:
        service.validateLimit(-5)

        then:
        thrown(BadRequestException)
    }

    def "validateLimit limit is 0 sets to default"() {
        when:
        config.getInt(_) >> 25
        def limit = service.validateLimit(0)

        then:
        limit == 25
    }

    def "validateLimit limit is too large sets to default max"() {
        when:
        config.getInt(_) >> 99
        def value = 100
        def limit = service.validateLimit(value)

        then:
        limit == 99
    }

    def "validateLimit limit is valid sets limit"() {
        when:
        config.getInt(_) >> 100
        def value = 99
        def limit = service.validateLimit(value)

        then:
        limit == value
    }
}
