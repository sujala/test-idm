package com.rackspace.idm.api.error

import com.rackspace.idm.exception.CustomerConflictException
import spock.lang.Shared
import testHelpers.RootServiceTest

class CustomerConflictExceptionMapperTest extends RootServiceTest {
    @Shared CustomerConflictExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new CustomerConflictExceptionMapper()
    }

    def "exception gets mapped to conflict"() {
        when:
        def result = exceptionMapper.toResponse(new CustomerConflictException())

        then:
        result.status == 409
    }
}
