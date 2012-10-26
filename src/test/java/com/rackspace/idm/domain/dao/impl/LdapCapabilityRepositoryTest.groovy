package com.rackspace.idm.domain.dao.impl

import org.springframework.test.context.ContextConfiguration
import spock.lang.Specification
import spock.lang.Shared
import org.springframework.beans.factory.annotation.Autowired
import com.rackspace.idm.domain.service.EndpointService
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.entity.Capabilities
import com.rackspace.idm.domain.entity.Capability


/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 10/22/12
 * Time: 4:57 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapCapabilityRepositoryTest extends Specification {
    @Shared def randomness = UUID.randomUUID()
    @Shared def random
    @Shared CloudBaseUrl cloudBaseUrl

    @Autowired
    private CapabilityDao capabilityDao;

    @Autowired
    private EndpointService endpointService;

    def setupSpec() {
        random = ("$randomness").replace('-', "")
    }

    def "CRUD capabilities"() {
        when:
        Integer baseUrlId = 10003321
        cloudBaseUrl = getCloudBaseUrl(baseUrlId, "NAST", "test", "test", false, true, false, "cloudServers")
        endpointService.addBaseUrl(cloudBaseUrl)
        Capabilities capabilities1 = new Capabilities();
        List<String> list = new ArrayList<String>();
        capabilities1.capability.add(getCapability("GET", "get_server", "get_server", "description", "http://someUrl", list))
        capabilityDao.updateCapabilities(String.valueOf(baseUrlId), capabilities1)
        Capabilities capabilities = capabilityDao.getCapabilities(String.valueOf(baseUrlId))
        Capability capability = capabilityDao.getCapability("get_server", String.valueOf(baseUrlId))
        capabilityDao.removeCapabilities(String.valueOf(baseUrlId))
        Capabilities capabilities2 = capabilityDao.getCapabilities(String.valueOf(baseUrlId))
        endpointService.deleteBaseUrl(baseUrlId)

        then:
        capabilities != null
        capabilities.capability.get(0).name == "get_server"
        capability != null
        capability.action == "GET"
        capabilities2.capability.size() == 0
    }

    def "IllegalStateException getCapabilities"() {
        when: capabilityDao.getCapabilities("notFound")

        then: thrown(IllegalStateException)
    }

    def "invalid operations on getCapability"() {
        when:
        def capability = capabilityDao.getCapability(null, null)
        def capability1 = capabilityDao.getCapability("11231231", null)
        def capability2 = capabilityDao.getCapability("112341234123", "3281746123487")
        then:
        capability == null
        capability1 == null
        capability2 == null
    }

    def "IllegalArgumentException on updateCapabilities"() {
        when: capabilityDao.updateCapabilities(null, null)
        then: thrown(IllegalArgumentException)
    }

    def "IllegalStateException on updateCapabilities"() {
        when: capabilityDao.updateCapabilities("badId",new Capabilities())
        then: thrown(IllegalStateException)
    }

    def "IllegalStateException on removeCapabilities" () {
        when: capabilityDao.removeCapabilities("badId")
        then: thrown(IllegalStateException)
    }

    def "IllegalArgumentException on removeCapabilities"() {
        when: capabilityDao.removeCapabilities(null)
        then: thrown(IllegalArgumentException)
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
