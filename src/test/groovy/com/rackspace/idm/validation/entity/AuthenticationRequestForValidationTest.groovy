package com.rackspace.idm.validation.entity;

import com.rackspace.idm.aspect.ValidateAspect;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.validation.ObjectConverter;
import org.aspectj.lang.JoinPoint;
import spock.lang.Shared;
import testHelpers.RootServiceTest;

import javax.xml.namespace.QName;

class AuthenticationRequestForValidationTest extends RootServiceTest {
    @Shared
    ValidateAspect validateAspect
    @Shared
    ObjectConverter converter

    def setupSpec(){
        validateAspect = new ValidateAspect()
    }

    def setup() {
        setupMocks()
        validateAspect.setup()
    }

    /*
    def "validate fields for AuthenticationRequestForValidation"() {
        where:


        when:

        then:
    }
    */

    def setupMocks(){
        converter = Mock()
        validateAspect.converter = converter
    }
}