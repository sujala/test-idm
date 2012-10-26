package com.rackspace.idm.domain.service.impl

import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.Shared
import org.springframework.beans.factory.annotation.Autowired
import com.rackspace.idm.domain.service.EndpointService
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.entity.Capabilities
import com.rackspace.idm.domain.entity.Capability
import com.rackspace.idm.domain.service.CapabilityService
import com.rackspace.idm.exception.NotFoundException
import com.rackspace.idm.exception.BadRequestException;

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
    @Shared CloudBaseUrl cloudBaseUrl
    @Shared Integer baseUrlId

    @Autowired
    private CapabilityService capabilityService;

    @Autowired
    private EndpointService endpointService;

    def setupSpec() {
        random = ("$randomness").replace('-', "")
        baseUrlId = 1000332132
        cloudBaseUrl = getCloudBaseUrl(baseUrlId, "NAST", "test", "http://someUrl", true, true, false, "cloudServers")
    }

    def cleanupSpec() {

    }

    def "Crud Capabilities"() {
        when:
        endpointService.addBaseUrl(cloudBaseUrl)
        Capabilities capabilities1 = new Capabilities()
        List<String> list = new ArrayList<String>()
        capabilities1.capability.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", list))
        capabilityService.updateCapabilities(String.valueOf(baseUrlId), capabilities1)
        Capability capability = capabilityService.getCapability("get_server", String.valueOf(baseUrlId))
        Capabilities capabilities = capabilityService.getCapabilities(String.valueOf(baseUrlId))
        capabilityService.removeCapabilities(String.valueOf(baseUrlId))
        capabilityService.getCapability("get_server", String.valueOf(baseUrlId))

        then:
        capability.getAction() == "GET"
        capabilities.capabilities.get(0).name == "get_server"
        endpointService.deleteBaseUrl(baseUrlId)
        thrown(NotFoundException)
    }

    def "null values updateCapabilities"() {
        when: capabilityService.updateCapabilities(null, null)
        then: thrown(BadRequestException)
    }

    def "invalid value for endpoint template updateCapabilities"() {
        when: capabilityService.updateCapabilities("a", null)
        then: thrown(BadRequestException)
    }

    def "not found endpoint template updateCapabilities"() {
        when: capabilityService.updateCapabilities("1000000001", null)
        then: thrown(NotFoundException)
    }

    def "invalid action capabilities updateCapabilities"() {
        when:
        endpointService.addBaseUrl(cloudBaseUrl)
        Capabilities capabilities1 = new Capabilities()
        List<String> list = new ArrayList<String>()
        capabilities1.capability.add(getCapability(null, "get_server", "get_server", "description", "http://someUrl", list))
        capabilityService.updateCapabilities(String.valueOf(baseUrlId), capabilities1)

        then:
        endpointService.deleteBaseUrl(baseUrlId)
        thrown(BadRequestException)
    }

    def "invalid id capabilities updateCapabilities"() {
        when:
        endpointService.addBaseUrl(cloudBaseUrl)
        Capabilities capabilities1 = new Capabilities()
        List<String> list = new ArrayList<String>()
        capabilities1.capability.add(getCapability("GET", null, "get_server", "description", "http://someUrl", list))
        capabilityService.updateCapabilities(String.valueOf(baseUrlId), capabilities1)

        then:
        endpointService.deleteBaseUrl(baseUrlId)
        thrown(BadRequestException)
    }

    def "invalid name capabilities updateCapabilities"() {
        when:
        endpointService.addBaseUrl(cloudBaseUrl)
        Capabilities capabilities1 = new Capabilities()
        List<String> list = new ArrayList<String>()
        capabilities1.capability.add(getCapability("GET", "get_server", null, "description", "http://someUrl", list))
        capabilityService.updateCapabilities(String.valueOf(baseUrlId), capabilities1)

        then:
        endpointService.deleteBaseUrl(baseUrlId)
        thrown(BadRequestException)
    }

    def "valid capabilities updateCapabilities"() {
        when:
        endpointService.addBaseUrl(cloudBaseUrl)
        Capabilities capabilities1 = new Capabilities()
        List<String> list = new ArrayList<String>()
        list.add("123")
        list.add("321")
        capabilities1.capability.add(getCapability("GET", "get_server", "get_servers", null, "http://someUrl", list))
        capabilityService.updateCapabilities(String.valueOf(baseUrlId), capabilities1)
        Capabilities capabilities = capabilityService.getCapabilities(String.valueOf(baseUrlId))
        endpointService.deleteBaseUrl(baseUrlId)

        then:
        capabilities.getCapability().size() == 1
        capabilities.getCapability().get(0).resources.get(0) == "123"
    }

    def "null values on getCapabilities" () {
        when: capabilityService.getCapabilities(null)
        then: thrown(BadRequestException)
    }

    def "non-numeric values on getCapabilities" () {
        when: capabilityService.getCapabilities("a")
        then: thrown(BadRequestException)
    }

    def "not found on getCapabilities" () {
        when: capabilityService.getCapabilities("1000000001")
        then: thrown(NotFoundException)
    }

    def "not found on getCapability" () {
        when: capabilityService.getCapability("get_server","1000000001")
        then: thrown(NotFoundException)
    }

    def "null values on getCapability" () {
        when: capabilityService.getCapability(null,null)
        then: thrown(BadRequestException)
    }

    def "not found on removeCapabilities" () {
        when: capabilityService.removeCapabilities("1000000001")
        then: thrown(NotFoundException)
    }

    def "null value on removeCapabilities" () {
        when: capabilityService.removeCapabilities(null)
        then: thrown(BadRequestException)
    }

    //Helper Methods
    def getCloudBaseUrl(Integer id, String type, String serviceName, String publicUrl, Boolean defaultValue, Boolean enabled, Boolean global, String OSType) {
        new CloudBaseUrl().with {
            it.setBaseUrlId(id);
            it.setBaseUrlType(type);
            it.setServiceName(serviceName);
            it.setPublicUrl(publicUrl);
            it.setDef(defaultValue);
            it.setEnabled(enabled);
            it.setGlobal(global);
            it.setOpenstackType(OSType);
            return it
        }
    }

    def getCapability(String action, String id, String name, String description, String url, List<String> resources) {
        new Capability().with {
            it.action = action
            it.capabilityId = id
            it.name = name
            it.description = description
            it.url = url
            it.resources = resources
            return it
        }
    }
}
