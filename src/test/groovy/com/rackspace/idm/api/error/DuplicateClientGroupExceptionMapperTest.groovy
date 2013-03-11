package com.rackspace.idm.api.error

import com.rackspace.idm.exception.DuplicateClientGroupException
import spock.lang.Shared
import testHelpers.RootServiceTest

class DuplicateClientGroupExceptionMapperTest extends RootServiceTest {
    @Shared DuplicateClientGroupExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new DuplicateClientGroupExceptionMapper()
    }

    def "exception gets mapped to conflict"() {
        when:
        def result = exceptionMapper.toResponse(new DuplicateClientGroupException())

        then:
        result.status == 409
    }
}
