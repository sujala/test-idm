package com.rackspace.idm.domain.service.impl

import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.Shared
import org.springframework.beans.factory.annotation.Autowired

import com.rackspace.idm.domain.entity.Capabilities
import com.rackspace.idm.domain.entity.Capability
import com.rackspace.idm.domain.service.CapabilityService
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.exception.DuplicateException
import com.rackspace.idm.domain.dao.impl.LdapCapabilityRepository

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/22/12
 * Time: 4:57 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class DefaultCapabilityServiceTest extends Specification {
    @Shared def randomness = UUID.randomUUID()
    @Shared def random

    @Shared LdapCapabilityRepository ldapCapabilityRepository
    @Shared DefaultCapabilityService defaultCapabilityService;

    @Autowired
    private CapabilityService capabilityService;

    def setupSpec() {
        random = ("$randomness").replace('-', "")
        defaultCapabilityService = new DefaultCapabilityService()
    }

    def cleanupSpec() {
    }

    def "Crud Capabilities"() {
        when:
        Capabilities capabilities1 = new Capabilities()
        List<String> list = new ArrayList<String>()
        capabilities1.capability.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", list, "1", "computeTest"))
        capabilityService.updateCapabilities(capabilities1)
        Capability capability = capabilityService.getCapability("get_server", "1", "computeTest")
        Capabilities capabilities = capabilityService.getCapabilities("1", "computeTest")
        capabilityService.removeCapabilities("1","computeTest")
        capabilityService.getCapability("get_server", "1", "computeTest")


        then:
        capability.getAction() == "GET"
        capability.getId() == "get_server"
        capabilities.capability.get(0).getId() == "get_server"
        capabilities.capability.get(0).type == "computeTest"
        thrown(NotFoundException)
    }

    def "null values updateCapabilities"() {
        when: capabilityService.updateCapabilities(null)
        then: thrown(BadRequestException)
    }

    def "duplicate capabilities updateCapabilities"() {
        when:
        Capabilities capabilities = new Capabilities()
        capabilities.capability.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", null, "1", "computeTest"))
        capabilities.capability.add(getCapability("GET", "get_server", "get_server", null, "http://someUrl", null, "1", "computeTest"))
        capabilityService.updateCapabilities(capabilities)

        then:
        thrown(DuplicateException)
    }

    def "capability already exist - updateCapabilities"() {
        when:
        Capabilities capabilities = new Capabilities()
        capabilities.capability.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", null, "1", "computeTest"))
        capabilityService.updateCapabilities(capabilities)
        capabilityService.updateCapabilities(capabilities)

        then:
        capabilityService.removeCapabilities("1","computeTest")
        thrown(DuplicateException)
    }

    def "invalid action capabilities updateCapabilities"() {
        when:
        Capabilities capabilities1 = new Capabilities()
        capabilities1.capability.add(getCapability(null, "get_server", "get_server", "description", "http://someUrl", null, "1", "computeTest"))
        capabilityService.updateCapabilities(capabilities1)

        then:
        thrown(BadRequestException)
    }

    def "invalid id capabilities updateCapabilities"() {
        when:
        Capabilities capabilities1 = new Capabilities()
        capabilities1.capability.add(getCapability("GET", null, "get_server", "description", "http://someUrl", null, "1", "computeTest"))
        capabilityService.updateCapabilities(capabilities1)

        then:
        thrown(BadRequestException)
    }

    def "invalid name capabilities updateCapabilities"() {
        when:
        Capabilities capabilities1 = new Capabilities()
        capabilities1.capability.add(getCapability("GET", "get_server", null, "description", "http://someUrl", null, "1", "computeTest"))
        capabilityService.updateCapabilities(capabilities1)

        then:
        thrown(BadRequestException)
    }

    def "invalid version capabilities updateCapabilities"() {
        when:
        Capabilities capabilities1 = new Capabilities()
        capabilities1.capability.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", null, null, "computeTest"))
        capabilityService.updateCapabilities(capabilities1)

        then:
        thrown(BadRequestException)
    }

    def "invalid type capabilities updateCapabilities"() {
        when:
        Capabilities capabilities1 = new Capabilities()
        capabilities1.capability.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", null, "1", null))
        capabilityService.updateCapabilities(capabilities1)

        then:
        thrown(BadRequestException)
    }



    def "valid capabilities updateCapabilities"() {
        when:
        Capabilities capabilities1 = new Capabilities()
        List<String> list = new ArrayList<String>()
        list.add("123")
        list.add("321")
        capabilities1.capability.add(getCapability("GET", "get_server", "get_servers", null, "http://someUrl", list, "1", "computeTest"))
        capabilityService.updateCapabilities(capabilities1)
        Capabilities capabilities = capabilityService.getCapabilities("1","computeTest")
        capabilityService.removeCapabilities("1","computeTest")

        then:
        capabilities.getCapability().size() == 1
        capabilities.getCapability().get(0).resources.get(0) == "123"
    }

    def "null values on getCapabilities" () {
        when: capabilityService.getCapabilities(null, null)
        then: thrown(BadRequestException)
    }

    def "null value on getCapabilities" () {
        when: capabilityService.getCapabilities("1", null)
        then: thrown(BadRequestException)
    }

    def "Get all correct values on getCapabilities" () {
        when:
        Capabilities capabilities = new Capabilities()
        capabilities.capability.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", null, "1", "computeTest"))
        capabilities.capability.add(getCapability("POST", "post_server", "get_server", null, "http://someUrl", null, "1", "computeTest"))
        capabilityService.updateCapabilities(capabilities)
        Capabilities capabilities1 = capabilityService.getCapabilities("1", "computeTest")
        capabilityService.removeCapabilities("1","computeTest")

        then:
        capabilities1.capability.size() == 2
        capabilities1.capability.get(0).version == "1"
        capabilities1.capability.get(0).action == "GET"
        capabilities1.capability.get(1).version == "1"
        capabilities1.capability.get(1).action == "POST"
    }

    def "not found on getCapabilities" () {
        when:
        Capabilities capabilities = capabilityService.getCapabilities("10","computeTest")
        then:
        capabilities.capability.size() == 0
    }

    def "not found on getCapability" () {
        when: capabilityService.getCapability("get_server","10","computeTest")
        then: thrown(NotFoundException)
    }

    def "null values on getCapability" () {
        when: capabilityService.getCapability(null,null,null)
        then: thrown(BadRequestException)
    }

    def "null version value on getCapability" () {
        when: capabilityService.getCapability("get_servers",null,"computeTest")
        then: thrown(BadRequestException)
    }

    def "null type value on getCapability" () {
        when: capabilityService.getCapability("get_servers","1",null)
        then: thrown(BadRequestException)
    }

    def "empty string type value on getCapability" () {
        when: capabilityService.getCapability("","1","computeTest")
        then: thrown(BadRequestException)
    }

    def "not found on removeCapabilities" () {
        given:
        setupMocks()
        List<Capability> capabilityList = new ArrayList<Capability>()
        ldapCapabilityRepository.getObjects(_) >> capabilityList

        when:
        defaultCapabilityService.removeCapabilities("10","someType")

        then:
        0 * ldapCapabilityRepository.deleteObject(_)
    }

    def "null values on removeCapabilities" () {
        when: capabilityService.removeCapabilities(null, null)
        then: thrown(BadRequestException)
    }

    def "null type value on removeCapabilities" () {
        when: capabilityService.removeCapabilities("1", null)
        then: thrown(BadRequestException)
    }

    //Helper Methods
    def getCapability(String action, String id, String name, String description, String url, List<String> resources, String version, String type) {
        new Capability().with {
            it.action = action
            it.id = id
            it.name = name
            it.description = description
            it.url = url
            it.resources = resources
            it.version = version
            it.type = type
            return it
        }
    }

    def setupMocks() {
        ldapCapabilityRepository = Mock()
        defaultCapabilityService.ldapCapabilityRepository = ldapCapabilityRepository
    }
}
