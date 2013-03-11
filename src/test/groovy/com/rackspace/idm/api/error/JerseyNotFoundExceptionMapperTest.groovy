package com.rackspace.idm.api.error

import com.sun.jersey.api.NotFoundException
import spock.lang.Shared
import testHelpers.RootServiceTest

class JerseyNotFoundExceptionMapperTest extends RootServiceTest {
    @Shared JerseyNotFoundExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new JerseyNotFoundExceptionMapper()
    }

    def "exception gets mapped to not found"() {
        when:
        def result = exceptionMapper.toResponse(new NotFoundException())

        then:
        result.status == 404
    }
}
