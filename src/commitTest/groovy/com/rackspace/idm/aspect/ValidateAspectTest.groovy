package com.rackspace.idm.aspect

import com.rackspace.idm.validation.DefaultObjectValidator
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.validation.entity.AuthenticationRequestForValidation
import org.aspectj.lang.JoinPoint
import spock.lang.Shared
import testHelpers.RootServiceTest

import javax.xml.namespace.QName

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 2/12/13
 * Time: 12:18 PM
 * To change this template use File | Settings | File Templates.
 */
class ValidateAspectTest extends RootServiceTest {
    @Shared ValidateAspect validateAspect
    @Shared DefaultObjectValidator objectValidator

    def setupSpec(){
        validateAspect = new ValidateAspect()
    }

    def setup() {
        setupMocks()
    }

    def "Validate throws exception if not valid" (){
        given:
        JoinPoint joinPoint  = Mock()
        Object arg = v2Factory.createAuthenticationRequest()

        joinPoint.args >> [arg]

        when:
        validateAspect.validateObject(joinPoint)

        then:
        thrown(BadRequestException)
        1 * config.getBoolean("validate.entities") >> true
        1 * objectValidator.validate(arg) >> { throw new BadRequestException() }
    }

    def "Validate does not thorw exception if valid" (){
        given:
        JoinPoint joinPoint  = Mock()
        Object arg = v2Factory.createAuthenticationRequest()

        joinPoint.args >> [arg]

        when:
        validateAspect.validateObject(joinPoint)

        then:
        notThrown(BadRequestException)
        1 * config.getBoolean("validate.entities") >> true
        1 * objectValidator.validate(arg)
    }

    def "skip null args to avoid null pointer exception"() {
        given:
        JoinPoint joinPoint  = Mock()
        joinPoint.args >> [null]

        when:
        validateAspect.validateObject(joinPoint)

        then:
        0 * objectValidator.validate(_)
    }

    def "validateObject does not validate entities if disabled"() {
        given:
        JoinPoint joinPoint  = Mock()
        Object arg = v2Factory.createAuthenticationRequest()

        joinPoint.args >> [arg]

        when:
        validateAspect.validateObject(joinPoint)

        then:
        1 * config.getBoolean("validate.entities") >> false
        0 * objectValidator.validate(_)
    }

    def "validateObject validates entities if enabled"() {
        given:
        JoinPoint joinPoint  = Mock()
        Object arg = v2Factory.createAuthenticationRequest()

        joinPoint.args >> [arg]

        when:
        validateAspect.validateObject(joinPoint)

        then:
        1 * config.getBoolean("validate.entities") >> true
        1 * objectValidator.validate(_)
    }

    def setupMocks(){
        objectValidator = Mock()
        validateAspect.objectValidator = objectValidator
        mockConfiguration(validateAspect)
    }
}
