package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import spock.lang.Shared
import testHelpers.RootServiceTest

import javax.xml.namespace.QName

class RoleConverterCloudV20Test extends RootServiceTest {
    @Shared RoleConverterCloudV20 converter
    @Shared def configWeight = 200

    def setupSpec() {
        converter = new RoleConverterCloudV20()
        converter.objFactories = new JAXBObjectFactories()
    }

    def setup() {
        mockConfiguration(converter)
        config.getInt("cloudAuth.special.rsWeight") >> configWeight
    }

    def "can convert clientRole to jaxb role"() {
        given:
        def weight = 100
        def propagate = true
        def clientRole = entityFactory.createClientRole().with {
            it.rsWeight = weight
            it.propagate = propagate
            return it
        }

        when:
        def result = converter.toRoleFromClientRole(clientRole)

        then:
        result.otherAttributes.get(RoleConverterCloudV20.QNAME_WEIGHT) == weight.toString()
        result.otherAttributes.get(RoleConverterCloudV20.QNAME_PROPAGATE) == propagate.toString()
    }

    def "can convert jaxb role to clientRole"() {
        given:
        def weight = 100
        def propagate = true

        def jaxbRole = v2Factory.createRole()
        jaxbRole.otherAttributes.put(RoleConverterCloudV20.QNAME_WEIGHT, weight.toString())
        jaxbRole.otherAttributes.put(RoleConverterCloudV20.QNAME_PROPAGATE, propagate.toString())

        when:
        def result = converter.toClientRoleFromRole(jaxbRole, "clientId");

        then:
        result.rsWeight == weight
        result.propagate == propagate
    }

    def "jaxb defaults to config role when converting from role to clientRole"() {
        given:
        def jaxbRole = v2Factory.createRole()

        when:
        def result = converter.toClientRoleFromRole(jaxbRole, "clientId");

        then:
        result.rsWeight == configWeight
    }
}
