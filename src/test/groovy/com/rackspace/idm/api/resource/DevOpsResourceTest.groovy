package com.rackspace.idm.api.resource

import com.fasterxml.jackson.databind.ObjectMapper
import testHelpers.RootIntegrationTest

class DevOpsResourceTest extends RootIntegrationTest {

    enum ConfigSection {
        configRoot("configPath"), staticConfig("idm.properties"), reloadableConfig("idm.reloadable.properties")

        def representation

        ConfigSection(representation) {
            this.representation = representation
        }

        @Override
        def String toString() {
            return representation
        }
    }

    enum PropKey {
        description, defaultValue, value, versionAdded
    }

    def "test get idm props"() {
        given:
        def response = devops.getIdmProps(utils.getServiceAdminToken())

        when:
        def stringResp = response.getEntity(String)
        def data = new ObjectMapper().readValue(stringResp, Map)

        then:
        response.status == 200
        data.containsKey(ConfigSection.configRoot.toString()) && data.containsKey(ConfigSection.staticConfig.toString()) && data.containsKey(ConfigSection.reloadableConfig.toString())
        def staticConfig = data.get(ConfigSection.staticConfig.toString())
        def reloadableConfig = data.get(ConfigSection.reloadableConfig.toString())
        assertFormat(staticConfig)
        assertFormat(reloadableConfig)
        assertTypeAndValueOfPropValue(staticConfig.get("ga.username").get(PropKey.value.toString()), "auth")
        assertTypeAndValueOfPropValue(staticConfig.get("reloadable.docs.cache.timeout").get(PropKey.value.toString()), 10)
        assertTypeAndValueOfPropValue(staticConfig.get("feature.use.reloadable.docs").get(PropKey.value.toString()), true)
    }

    def assertFormat(configSection) {
        configSection.each { prop, propSection ->
            assert propSection.containsKey(PropKey.description.toString()) &&
                    propSection.containsKey(PropKey.defaultValue.toString()) &&
                    propSection.containsKey(PropKey.value.toString()) &&
                    propSection.containsKey(PropKey.versionAdded.toString())
        }
        return true
    }

    def assertTypeAndValueOfPropValue(value, expectedValue) {
        assert value.getClass() == expectedValue.getClass()
        assert value == expectedValue
        return true
    }
}
