package com.rackspace.idm.api.error

import com.rackspace.idm.exception.IdmException
import spock.lang.Shared
import testHelpers.RootServiceTest

class IdmExceptionMapperTest extends RootServiceTest {
    @Shared IdmExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new IdmExceptionMapper()
    }

    def "exception gets mapped to internal server error"() {
        when:
        def result = exceptionMapper.toResponse(new IdmException())

        then:
        result.status == 500
    }
}
