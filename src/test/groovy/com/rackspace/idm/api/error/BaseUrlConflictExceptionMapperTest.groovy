package com.rackspace.idm.api.error

import com.rackspace.idm.exception.BaseUrlConflictException
import spock.lang.Shared
import testHelpers.RootServiceTest

class BaseUrlConflictExceptionMapperTest extends RootServiceTest {
    @Shared BaseUrlConflictExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new BaseUrlConflictExceptionMapper()
    }

    def "exception gets mapped to Conflict"() {
        when:
        def result = exceptionMapper.toResponse(new BaseUrlConflictException())

        then:
        result.status == 409
    }
}
