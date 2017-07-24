package com.rackspace.idm.api.error

import com.rackspace.idm.exception.ClientConflictException
import spock.lang.Shared
import testHelpers.RootServiceTest

class ClientConflictExceptionMapperTest extends RootServiceTest {
    @Shared ClientConflictExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new ClientConflictExceptionMapper()
    }

    def "exception gets mapped to conflict"() {
        when:
        def result = exceptionMapper.toResponse(new ClientConflictException())

        then:
        result.status == 409
    }
}
