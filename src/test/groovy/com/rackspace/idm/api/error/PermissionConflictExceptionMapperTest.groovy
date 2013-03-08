package com.rackspace.idm.api.error

import com.rackspace.idm.exception.PermissionConflictException
import spock.lang.Shared
import testHelpers.RootServiceTest

class PermissionConflictExceptionMapperTest extends RootServiceTest {
    @Shared PermissionConflictExceptionMapper exceptionMapper

    def setupSpec() {
        exceptionMapper = new PermissionConflictExceptionMapper()
    }

    def "exception gets mapped to to conflict"() {
        when:
        def result = exceptionMapper.toResponse(new PermissionConflictException())

        then:
        result.status == 409
    }
}
