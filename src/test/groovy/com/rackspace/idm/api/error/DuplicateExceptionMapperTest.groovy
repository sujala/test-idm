package com.rackspace.idm.api.error

import com.rackspace.idm.exception.DuplicateException
import spock.lang.Shared
import testHelpers.RootServiceTest

class DuplicateExceptionMapperTest extends RootServiceTest {
    @Shared DuplicateExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new DuplicateExceptionMapper()
    }

    def "exception gets mapped to conflict"() {
        when:
        def result = exceptionMapper.toResponse(new DuplicateException())

        then:
        result.status == 409
    }
}
