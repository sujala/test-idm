package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.entity.TenantType
import org.dozer.DozerBeanMapper
import spock.lang.Shared
import spock.lang.Specification


class TenantTypeConverterCloudV20Test extends Specification {

    @Shared TenantTypeConverterCloudV20 converterCloudV20

    def setupSpec() {
        converterCloudV20 = new TenantTypeConverterCloudV20().with {
            it.objFactories = new JAXBObjectFactories()
            it.mapper = new DozerBeanMapper()
            return it
        }
    }

    def "convert tenantType from ldap to jersey object"() {
        given:
        TenantType tenantTypeEntity = tenantTypeEntity()

        when:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantType tenantType = converterCloudV20.toTenantType(tenantTypeEntity)

        then:
        tenantType.name == tenantTypeEntity.name
        tenantType.description == tenantTypeEntity.description
    }

    def "convert tenantType from jersey object to ldap"() {
        given:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantType tenantType = tenantType()

        when:
        TenantType tenantTypeEntity = converterCloudV20.fromTenantType(tenantType)

        then:
        tenantType.name == tenantTypeEntity.name
        tenantType.description == tenantTypeEntity.description
    }

    def "convert tenantTypes from ldap to jersey object"() {
        given:
        TenantType tenantTypeEntity = tenantTypeEntity()
        List<TenantType> tenantTypeList = new ArrayList<>();
        tenantTypeList.add(tenantTypeEntity)

        when:
        com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantTypes tenantTypes = converterCloudV20.toTenantType(tenantTypeList)

        then:
        tenantTypes.getTenantType().size() == tenantTypeList.size()
        def tenantType = tenantTypes.getTenantType().get(0)
        tenantType.name == tenantTypeEntity.name
        tenantType.description == tenantTypeEntity.description
    }


    def tenantTypeEntity(String name = "name", String description = "description") {
        new TenantType().with {
            it.name = name
            it.description = description
            return it
        }
    }

    def tenantType(String name = "name", String description = "description") {
        new com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantType().with {
            it.name = name
            it.description = description
            it
        }
    }
}
