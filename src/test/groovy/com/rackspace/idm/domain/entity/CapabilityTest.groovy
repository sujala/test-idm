package com.rackspace.idm.domain.entity

import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/25/13
 * Time: 12:20 PM
 * To change this template use File | Settings | File Templates.
 */
class CapabilityTest extends RootServiceTest{

    def "Two capability with different attributes can not be equal"(){
        given:
        Capability capability1 = new Capability().with {
            it.id = "1"
            it.name = "capability1"
            it.action = "GET"
            it.version = "1"
            it.type = "type"
            return it
        }
        Capability capability2 = new Capability().with {
            it.id = "1"
            it.name = "capability1"
            it.action = "POST"
            it.version = "1"
            it.type = "type"
            return it
        }

        when:
        def result = capability1.equals(capability2)

        then:
        result == false
    }

    def "Two capability with same attributes should be equal"(){
        given:
        Capability capability1 = new Capability().with {
            it.id = "1"
            it.name = "capability1"
            it.action = "GET"
            it.version = "1"
            it.type = "type"
            return it
        }
        Capability capability2 = new Capability().with {
            it.id = "1"
            it.name = "capability1"
            it.action = "GET"
            it.version = "1"
            it.type = "type"
            return it
        }

        when:
        def result = capability1.equals(capability2)

        then:
        result == true
    }
}
