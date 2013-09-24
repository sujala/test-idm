package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import org.dozer.DozerBeanMapper
import spock.lang.Shared
import testHelpers.RootServiceTest

import static com.rackspace.idm.RaxAuthConstants.*

class RoleConverterCloudV20Test extends RootServiceTest {
    @Shared RoleConverterCloudV20 converter
    @Shared def configWeight = 200

    def setupSpec() {
        converter = new RoleConverterCloudV20()
        converter.objFactories = new JAXBObjectFactories()
        converter.mapper = new DozerBeanMapper()
    }

    def setup() {
        mockConfiguration(converter)
        config.getInt("cloudAuth.special.rsWeight") >> configWeight
    }

    def "can convert tenantRole to jaxb role"() {
        given:
        def weight = 100
        def propagate = true
        def serviceId = "serviceId"
        def tenantRole = entityFactory.createTenantRole().with {
            it.propagate = propagate
            it.clientId = serviceId
            return it
        }

        when:
        def result = converter.toRole(tenantRole)

        then:
        result.propagate == propagate
        result.serviceId == serviceId
    }

    def "can convert clientRole to jaxb role"() {
        given:
        def weight = 100
        def propagate = true
        def serviceId = "serviceId"
        def clientRole = entityFactory.createClientRole().with {
            it.rsWeight = weight
            it.propagate = propagate
            it.clientId = serviceId
            return it
        }

        when:
        def result = converter.toRoleFromClientRole(clientRole)

        then:
        result.weight == "$weight"
        result.propagate == propagate
        result.serviceId == serviceId
    }

    def "can convert jaxb role to clientRole"() {
        given:
        def weight = 100
        def propagate = false
        def serviceId = "serviceId"

        def jaxbRole = v2Factory.createRole()
        jaxbRole.weight = weight
        jaxbRole.propagate = propagate
        jaxbRole.serviceId = serviceId

        when:
        def result = converter.fromRole(jaxbRole, serviceId);

        then:
        result.rsWeight == weight
        result.propagate == propagate
        result.clientId == serviceId
    }

    def "jaxb defaults to config role when converting from role to clientRole"() {
        given:
        def jaxbRole = v2Factory.createRole()

        when:
        def result = converter.fromRole(jaxbRole, "clientId");

        then:
        result.rsWeight == configWeight
    }
}
