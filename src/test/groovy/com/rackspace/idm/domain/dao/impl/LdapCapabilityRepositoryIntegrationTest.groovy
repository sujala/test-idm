package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.dao.impl.LdapRepository.LdapSearchBuilder
import com.rackspace.idm.domain.entity.Capability
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.rackspace.idm.domain.service.EndpointService
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapCapabilityRepositoryIntegrationTest extends Specification {
    @Shared def randomness = UUID.randomUUID()
    @Shared def random
    @Shared CloudBaseUrl cloudBaseUrl

    @Autowired
    private LdapCapabilityRepository ldapCapabilityRepository;

    @Autowired Configuration config

    @Autowired
    private EndpointService endpointService;

    def setupSpec() {
        random = ("$randomness").replace('-', "")
    }

    def "getNextId returns UUID"() {
        given:
        def success = false
        ldapCapabilityRepository.config = config
        def originalVal = config.getBoolean("rsid.uuid.enabled", false)
        config.setProperty("rsid.uuid.enabled",true)

        when:
        def id = ldapCapabilityRepository.getNextId(LdapRepository.NEXT_CAPABILITY_ID)
        try {
            Long.parseLong(id)
        } catch (Exception) {
            success = true
        }

        then:
        success == true

        cleanup:
        config.setProperty("rsid.uuid.enabled",originalVal)
    }

    def "getNextId returns Long"() {
        given:
        def success = false
        ldapCapabilityRepository.config = config
        def originalVal = config.getBoolean("rsid.uuid.enabled", false)
        config.setProperty("rsid.uuid.enabled",false)

        when:
        def id = ldapCapabilityRepository.getNextId(LdapRepository.NEXT_CAPABILITY_ID)
        try {
            Long.parseLong(id)
            success = true
        } catch (Exception) {
            //no-op
        }

        then:
        success == true

        cleanup:
        config.setProperty("rsid.uuid.enabled",originalVal)
    }

    def "CRUD capabilities"() {
        when:
        def baseUrlId = "10003321"
        cloudBaseUrl = getCloudBaseUrl(baseUrlId, "NAST", "test", "test", false, true, false, "cloudServers")
        endpointService.addBaseUrl(cloudBaseUrl)
        ldapCapabilityRepository.addObject(getCapability("100123321","GET", "get_server", "get_server", "description", "http://someUrl","compute","1", null))
        Capability capability1 = ldapCapabilityRepository.getObject(createSearchFilter("get_server","1","compute").build())
        ldapCapabilityRepository.deleteObject(createSearchFilter("get_server","1","compute").build())
        Capability capability2 = ldapCapabilityRepository.getObject(createSearchFilter("get_server","1","compute").build())
        endpointService.deleteBaseUrl(baseUrlId)

        then:
        capability1.id == "get_server"
        capability1.version == "1"
        capability1.type == "compute"
        capability2 == null

    }

    def "IllegalStateException getCapabilities"() {
        when: ldapCapabilityRepository.getObject(null)
        then: thrown(IllegalStateException)
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
            it.def = false
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

    def createSearchFilter(String id, String version, String type){
         new LdapSearchBuilder().with {
             it.addEqualAttribute("objectClass","rsCapability")
             it.addEqualAttribute("capabilityId", id)
             it.addEqualAttribute("versionId",version)
             it.addEqualAttribute("openStackType", type)
             return it
         }
    }
}
