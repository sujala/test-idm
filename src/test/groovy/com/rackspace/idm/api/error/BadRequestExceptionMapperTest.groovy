package com.rackspace.idm.api.error

import com.rackspace.idm.exception.BadRequestException
import spock.lang.Shared
import testHelpers.RootServiceTest

class BadRequestExceptionMapperTest extends RootServiceTest {
    @Shared BadRequestExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new BadRequestExceptionMapper()
    }

    def "exception gets mapped to badRequest"() {
        when:
        def result = exceptionMapper.toResponse(new BadRequestException())

        then:
        result.status == 400
    }
}
