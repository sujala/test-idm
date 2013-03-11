package com.rackspace.idm.api.error

import com.rackspace.idm.exception.DuplicateClientException
import spock.lang.Shared
import testHelpers.RootServiceTest

class DuplicateClientExceptionMapperTest extends RootServiceTest {
    @Shared DuplicateClientExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new DuplicateClientExceptionMapper()
    }

    def "exception gets mapped to conflict"() {
        when:
        def result = exceptionMapper.toResponse(new DuplicateClientException())

        then:
        result.status == 409
    }
}
