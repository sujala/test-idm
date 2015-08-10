package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.CapabilityDao
import com.rackspace.idm.domain.entity.Capability
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.service.EndpointService
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class CapabilityRepositoryIntegrationTest extends Specification {
    @Shared def randomness = UUID.randomUUID()
    @Shared def random
    @Shared CloudBaseUrl cloudBaseUrl

    @Autowired
    private CapabilityDao capabilityDao

    @Autowired
    private EndpointService endpointService;

    def setupSpec() {
        random = ("$randomness").replace('-', "")
    }

    def "CRUD capabilities"() {
        when:
        def baseUrlId = "10003321"
        cloudBaseUrl = getCloudBaseUrl(baseUrlId, "NAST", "test", "test", false, true, false, "cloudServers")
        endpointService.addBaseUrl(cloudBaseUrl)
        capabilityDao.addCapability(getCapability("100123321","GET", "get_server", "get_server", "description", "http://someUrl","compute","1", null))
        Capability capability1 = capabilityDao.getCapability("get_server","compute","1")
        capabilityDao.deleteCapability("get_server","compute","1")
        Capability capability2 = capabilityDao.getCapability("get_server","compute", "1")
        endpointService.deleteBaseUrl(baseUrlId)

        then:
        capability1.id == "get_server"
        capability1.version == "1"
        capability1.type == "compute"
        capability2 == null

    }

    //Helper Methods
    def getCloudBaseUrl(String id, String type, String serviceName, String publicUrl, Boolean defaultValue, Boolean enabled, Boolean global, String OSType) {
        new CloudBaseUrl().with {
            it.setBaseUrlId(id);
            it.setBaseUrlType(type);
            it.setServiceName(serviceName);
            it.setPublicUrl(publicUrl);
            it.setDef(defaultValue);
            it.setEnabled(enabled);
            it.setGlobal(global);
            it.setOpenstackType(OSType);
            it.adminUrlId = UUID.randomUUID().toString().replace("-", "");
            it.internalUrlId = UUID.randomUUID().toString().replace("-", "");
            it.publicUrlId = UUID.randomUUID().toString().replace("-", "");
            it.region = "ORD"
            it.def = false
            it.clientId = "18e7a7032733486cd32f472d7bd58f709ac0d221"
            return it
        }
    }

    def getCapability(String rsId ,String action, String id, String name, String description, String url, String type, String version, List<String> resources) {
        new Capability().with {
            it.rsId = rsId
            it.action = action
            it.id = id
            it.name = name
            it.description = description
            it.url = url
            it.type = type
            it.version = version
            it.resources = resources
            return it
        }
    }
}
