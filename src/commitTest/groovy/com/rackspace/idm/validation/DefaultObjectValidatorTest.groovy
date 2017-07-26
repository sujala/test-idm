package com.rackspace.idm.validation

import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.validation.entity.UserForValidation
import spock.lang.Shared
import testHelpers.RootServiceTest

class DefaultObjectValidatorTest extends RootServiceTest {
    @Shared DefaultObjectValidator objectValidator

    def setupSpec() {
        objectValidator = new DefaultObjectValidator()
    }

    def setup() {
        mockObjectConverter(objectValidator)
        objectValidator.setup()
    }

    def "does not try to convert if not convertible"() {
        when:
        objectValidator.validate(null)

        then:
        1 * objectConverter.isConvertible(_) >> false
        0 * objectConverter.convert(_)
    }

    def "object is convertible and has no violations does not throw exception"() {
        given:
        def user = new UserForValidation().with {
            it.username = "valid"
            return it
        }

        when:
        objectValidator.validate(null)

        then:
        notThrown(BadRequestException)
        1 * objectConverter.isConvertible(_) >> true
        1 * objectConverter.convert(_) >> user
    }

        def "object is convertible and has violations throws exception"() {
        given:
        def user = new UserForValidation().with {
            it.username = "u".multiply(101)
            return it
        }

        when:
        objectValidator.validate(null)

        then:
        thrown(BadRequestException)
        1 * objectConverter.isConvertible(_) >> true
        1 * objectConverter.convert(_) >> user
    }
}
