package com.rackspace.idm.domain.entity

import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/25/13
 * Time: 12:38 PM
 * To change this template use File | Settings | File Templates.
 */
class ServiceApiTest extends RootServiceTest{

    def "Two different serviceAPI can not be equal" () {
        given:
        ServiceApi serviceApi1 = new ServiceApi().with {
            it.type = "type"
            it.version = "1"
            return it
        }

        ServiceApi serviceApi2 = new ServiceApi().with {
            it.type = "type2"
            it.version = "1"
            return it
        }

        when:
        def result = serviceApi1.equals(serviceApi2)

        then:
        result == false
    }

    def "Two serviceAPI with same attributes are equal" () {
        given:
        ServiceApi serviceApi1 = new ServiceApi().with {
            it.type = "type"
            it.version = "1"
            it.description = "desc"
            return it
        }

        ServiceApi serviceApi2 = new ServiceApi().with {
            it.type = "type"
            it.version = "1"
            it.description = "desc2"
            return it
        }

        when:
        def result = serviceApi1.equals(serviceApi2)

        then:
        result == true
    }
}
