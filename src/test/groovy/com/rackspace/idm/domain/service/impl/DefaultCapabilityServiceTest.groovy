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
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Capabilities

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/22/12
 * Time: 4:57 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultCapabilityServiceTest extends Specification {
    @Shared def randomness = UUID.randomUUID()
    @Shared def random

    @Shared LdapCapabilityRepository ldapCapabilityRepository
    @Shared DefaultCapabilityService defaultCapabilityService;

    def setupSpec() {
        random = ("$randomness").replace('-', "")
        defaultCapabilityService = new DefaultCapabilityService()
    }

    def cleanupSpec() {
    }

    def "Crud Capabilities"() {
        given:
        setupMocks()
        List<Capability> capabilities1 = new ArrayList<Capability>()
        List<String> list = new ArrayList<String>()
        capabilities1.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", list, null, null))
        capabilities1.get(0).type = "comnpute"
        capabilities1.get(0).version = "1"
        ldapCapabilityRepository.getObjects(_) >> capabilities1


        when:
        defaultCapabilityService.updateCapabilities(capabilities1, "computeTest", "1")
        List<Capability> capabilities = defaultCapabilityService.getCapabilities("computeTest", "1")
        defaultCapabilityService.removeCapabilities("computeTest", "1")


        then:
        capabilities.get(0).getId() == "get_server"
        capabilities.get(0).type == "computeTest"
    }

    def "null values updateCapabilities"() {
        when: defaultCapabilityService.updateCapabilities(null, null, null)
        then: thrown(BadRequestException)
    }

    def "duplicate capabilities updateCapabilities"() {
        given:
        setupMocks()
        List<Capability> capabilities = new ArrayList<Capability>()
        capabilities.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", null, null, null))
        capabilities.add(getCapability("GET", "get_server", "get_server", null, "http://someUrl", null, null, null))
        capabilities.get(0).type = "comnputeTest"
        capabilities.get(0).version = "1"
        capabilities.get(1).type = "comnputeTest"
        capabilities.get(1).version = "1"
        ldapCapabilityRepository.getObjects(_) >> capabilities

        when:
        defaultCapabilityService.updateCapabilities(capabilities, "computeTest", "1")

        then:
        thrown(DuplicateException)
    }

    def "invalid action capabilities updateCapabilities"() {
        given:
        setupMocks()
        List<Capability> capabilities1 = new ArrayList<Capability>()
        capabilities1.add(getCapability(null, "get_server", "get_server", "description", "http://someUrl", null, null, null))
        ldapCapabilityRepository.getObjects(_) >> new ArrayList()

        when:
        defaultCapabilityService.updateCapabilities(capabilities1, "computeTest", "1")

        then:
        thrown(BadRequestException)
    }

    def "invalid id capabilities updateCapabilities"() {
        given:
        setupMocks()
        List<Capability> capabilities1 = new ArrayList<Capability>()
        capabilities1.add(getCapability("GET", null, "get_server", "description", "http://someUrl", null, null, null))
        ldapCapabilityRepository.getObjects(_) >> new ArrayList()

        when:
        defaultCapabilityService.updateCapabilities(capabilities1, "computeTest", "1")

        then:
        thrown(BadRequestException)
    }

    def "invalid name capabilities updateCapabilities"() {
        given:
        setupMocks()
        List<Capability> capabilities1 = new ArrayList<Capability>()
        capabilities1.add(getCapability("GET", "get_server", null, "description", "http://someUrl", null, null, null))
        ldapCapabilityRepository.getObjects(_) >> new ArrayList()

        when:
        defaultCapabilityService.updateCapabilities(capabilities1, "computeTest", "1")

        then:
        thrown(BadRequestException)
    }

    def "invalid version capabilities updateCapabilities"() {
        when:
        List<Capability> capabilities1 = new ArrayList<Capability>()
        capabilities1.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", null, null, null))
        defaultCapabilityService.updateCapabilities(capabilities1, "computeTest", null)

        then:
        thrown(BadRequestException)
    }

    def "invalid type capabilities updateCapabilities"() {
        when:
        List<Capability> capabilities1 = new ArrayList<Capability>()
        capabilities1.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", null, null, null))
        defaultCapabilityService.updateCapabilities(capabilities1, null, "1")

        then:
        thrown(BadRequestException)
    }



    def "valid capabilities updateCapabilities"() {
        given:
        setupMocks()
        List<Capability> capabilities1 = new ArrayList<Capability>()
        List<String> list = new ArrayList<String>()
        list.add("123")
        list.add("321")
        capabilities1.add(getCapability("GET", "get_server", "get_servers", null, "http://someUrl", list, null, null))
        capabilities1.get(0).type = "computeTest"
        capabilities1.get(0).version = "1"
        ldapCapabilityRepository.getObjects(_) >> capabilities1

        when:
        defaultCapabilityService.updateCapabilities(capabilities1, "computeTest", "1")
        List<Capability> capabilities = defaultCapabilityService.getCapabilities("computeTest", "1")
        defaultCapabilityService.removeCapabilities("computeTest", "1")

        then:
        capabilities.size() == 1
        capabilities.get(0).resources.get(0) == "123"
    }

    def "null values on getCapabilities" () {
        when: defaultCapabilityService.getCapabilities(null, null)
        then: thrown(BadRequestException)
    }

    def "null value on getCapabilities" () {
        when: defaultCapabilityService.getCapabilities("1", null)
        then: thrown(BadRequestException)
    }

    def "Get all correct values on getCapabilities" () {
        given:
        setupMocks()
        List<Capability> capabilities = new ArrayList<Capability>()
        capabilities.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", null, null, null))
        capabilities.add(getCapability("POST", "post_server", "get_server", null, "http://someUrl", null, null, null))
        capabilities.get(0).type = "computeTest"
        capabilities.get(0).version = "1"
        capabilities.get(1).type = "computeTest"
        capabilities.get(1).version = "1"
        ldapCapabilityRepository.getObjects(_) >> capabilities

        when:
        defaultCapabilityService.updateCapabilities(capabilities, "computeTest", "1")
        List<Capability> capabilities1 = defaultCapabilityService.getCapabilities("computeTest", "1")
        defaultCapabilityService.removeCapabilities("computeTest", "1")

        then:
        capabilities1.size() == 2
        capabilities1.get(0).version == "1"
        capabilities1.get(0).action == "GET"
        capabilities1.get(1).version == "1"
        capabilities1.get(1).action == "POST"
    }

    def "not found on getCapabilities" () {
        given:
        setupMocks()
        ldapCapabilityRepository.getObjects(_) >> new ArrayList();

        when:
        List<Capability> capabilities = defaultCapabilityService.getCapabilities("computeTest","10")
        then:
        capabilities.size() == 0
    }

    def "not found on removeCapabilities" () {
        given:
        setupMocks()
        List<Capability> capabilityList = new ArrayList<Capability>()
        ldapCapabilityRepository.getObjects(_) >> capabilityList

        when:
        defaultCapabilityService.removeCapabilities("someType", "10")

        then:
        0 * ldapCapabilityRepository.deleteObject(_)
    }

    def "null values on removeCapabilities" () {
        when: defaultCapabilityService.removeCapabilities(null, null)
        then: thrown(BadRequestException)
    }

    def "null type value on removeCapabilities" () {
        when: defaultCapabilityService.removeCapabilities(null, "1")
        then: thrown(BadRequestException)
    }

    //Helper Methods
    def getCapability(String action, String id, String name, String description, String url, List<String> resources, String type, String version) {
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
