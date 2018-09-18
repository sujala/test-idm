package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.entity.IdentityProperty
import org.apache.commons.lang3.CharEncoding
import spock.lang.Unroll
import testHelpers.RootServiceTest

/**
 * Tests the service without regard to the cache as the spring context is not loaded.
 */
class DefaultIdentityPropertyServiceTest extends RootServiceTest {
    DefaultIdentityPropertyService service

    void setup() {
        service = new DefaultIdentityPropertyService()

        mockIdentityPropertyDao(service)
    }

    def "getImmutableIdentityPropertyByName: Returns null when no property exists"() {
        def propName = "aName"
        when:
        def result = service.getImmutableIdentityPropertyByName(propName)

        then:
        1 * identityPropertyDao.getIdentityPropertyByName(propName) >> null

        and:
        result == null
    }

    @Unroll
    def "getImmutableIdentityPropertyByName: Immutable property values are appropriately copied from the IdentityProperty"() {
        def propName = "irrelevant"

        when:
        def result = service.getImmutableIdentityPropertyByName(propName)

        then:
        1 * identityPropertyDao.getIdentityPropertyByName(propName) >> rawProp

        and:
        result.id == rawProp.id
        result.description == rawProp.description
        result.idmVersion == rawProp.idmVersion
        result.name == rawProp.name
        result.reloadable == rawProp.reloadable
        result.searchable == rawProp.searchable
        result.value == rawProp.value
        result.valueType == rawProp.valueType
        result.getValueAsString() == rawProp.getValueAsString()

        where:
        rawProp << [new IdentityProperty().with {
            it.id = "id"
            it.description = "description"
            it.idmVersion = "version"
            it.name = "name"
            it.reloadable = true
            it.searchable = true
            it.value = "value".getBytes(CharEncoding.UTF_8)
            it.valueType = "valueType"
            it
        },
        new IdentityProperty().with {
            it.id = "asdf"
            it.description = null
            it.idmVersion = ""
            it.name = "name"
            it.reloadable = false
            it.searchable = false
            it.value = "value".getBytes(CharEncoding.UTF_8)
            it.valueType = "valueType"
            it
        }
        ]
    }
}
