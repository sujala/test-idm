package com.rackspace.idm.aspect

import com.rackspace.idm.validation.ObjectConverter
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
    @Shared ObjectConverter converter

    def setupSpec(){
        validateAspect = new ValidateAspect()
    }

    def setup() {
        setupMocks()
        validateAspect.setup()
    }

    def "Validate does not return exception" (){
        given:
        JoinPoint joinPoint  = Mock()
        Object arg = v2Factory.createAuthenticationRequest()

        joinPoint.args >> [arg]

        converter.isConvertible(_) >> true
        converter.convert(_) >> new AuthenticationRequestForValidation().with {
            it.tenantId = "tenantId"
            it.otherAttributes.put(new QName("http://localhost"), "h123")
            return it
        }

        when:
        validateAspect.validateObject(joinPoint)

        then:
        notThrown(BadRequestException)
    }

    def "Validate throws exception" (){
        given:
        JoinPoint joinPoint  = Mock()
        Object arg = v2Factory.createAuthenticationRequest()

        joinPoint.args >> [arg]

        converter.isConvertible(_) >> true
        converter.convert(_) >> new AuthenticationRequestForValidation().with {
            it.tenantId = "zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz"
            return it
        }

        when:
        validateAspect.validateObject(joinPoint)

        then:
        thrown(BadRequestException)
    }

    def "non convertible object does not get converted"() {
        given:
        JoinPoint joinPoint  = Mock()
        Object arg = v2Factory.createAuthenticationRequest()

        joinPoint.args >> [arg]
        converter.isConvertible(_) >> false

        when:
        validateAspect.validateObject(joinPoint)

        then:
        0 * converter.convert(_)
    }

    def setupMocks(){
        converter = Mock()
        validateAspect.converter = converter
    }
}
