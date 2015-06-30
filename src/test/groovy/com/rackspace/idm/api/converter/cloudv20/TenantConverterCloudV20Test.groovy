package com.rackspace.idm.api.converter.cloudv20

import com.rackspace.idm.api.resource.cloud.JAXBObjectFactories
import com.rackspace.idm.domain.entity.Tenant
import org.dozer.DozerBeanMapper
import spock.lang.Shared
import spock.lang.Specification

import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

/**
 * Created with IntelliJ IDEA.
 * User: matt.kovacs
 * Date: 8/15/13
 * Time: 10:29 AM
 * To change this template use File | Settings | File Templates.
 */
class TenantConverterCloudV20Test extends Specification {
    @Shared TenantConverterCloudV20 converterCloudV20

    def setupSpec() {
        converterCloudV20 = new TenantConverterCloudV20().with {
            it.objFactories = new JAXBObjectFactories()
            it.mapper = new DozerBeanMapper()
            return it
        }
    }

    def cleanupSpec() {
    }

    def "convert tenant from ldap to jersey object"() {
        given:
        Tenant tenant = tenant()

        when:
        org.openstack.docs.identity.api.v2.Tenant tenantEntity = converterCloudV20.toTenant(tenant)

        then:
        tenant.tenantId == tenantEntity.id
        tenant.name == tenantEntity.name
        tenant.enabled == tenantEntity.enabled
        tenant.description == tenantEntity.description
    }

    def "convert tenant from ldap to jersey object - verify display-name is null"() {
        given:
        Tenant tenant = tenant()
        tenant.displayName = "displayName"

        when:
        org.openstack.docs.identity.api.v2.Tenant tenantEntity = converterCloudV20.toTenant(tenant)

        then:
        tenantEntity.displayName == null
    }

    def "convert tenant from jersey object to ldap"() {
        given:
        org.openstack.docs.identity.api.v2.Tenant tenantEntity = tenantEntity()

        when:
        Tenant tenant = converterCloudV20.fromTenant(tenantEntity)

        then:
        tenant.tenantId == tenantEntity.id
        tenant.name == tenantEntity.name
        tenant.enabled == tenantEntity.enabled
        tenant.description == tenantEntity.description
    }

    def "convert tenant from jersey object to ldap - should set defaults"() {
        given:
        org.openstack.docs.identity.api.v2.Tenant tenantEntity = tenantEntity()
        tenantEntity.enabled = null

        when:
        Tenant tenant = converterCloudV20.fromTenant(tenantEntity)

        then:
        tenant.tenantId == tenantEntity.id
        tenant.name == tenantEntity.name
        tenant.description == tenantEntity.description
        tenantEntity.enabled == true
    }

    def "convert tenants to jersey object" () {
        given:
        Tenant tenant = tenant()
        List<Tenant> tenants = new ArrayList<Tenant>()
        tenants.add(tenant)

        when:
        org.openstack.docs.identity.api.v2.Tenants tenantsEntity = converterCloudV20.toTenantList(tenants)

        then:
        tenants.size() == tenantsEntity.getTenant().size()
        org.openstack.docs.identity.api.v2.Tenant tenantEntity = tenantsEntity.getTenant().get(0)
        tenant.tenantId == tenantEntity.id
        tenant.name == tenantEntity.name
        tenant.enabled == tenantEntity.enabled
        tenant.description == tenantEntity.description
    }

    def tenant() {
        tenant("id", "name", false, "description", "display")
    }

    def tenant(String id, String name, boolean enabled, String description, String displayName) {
        new Tenant().with {
            it.tenantId = id
            it.name = name
            it.enabled = enabled
            it.description = description
            it.displayName = displayName
            return it
        }
    }

    def tenantEntity() {
        tenantEntity("id", "name", false, "description", "display")
    }

    def created() {
        new Date(2013,1,1)
    }

    def createdXML() {
        GregorianCalendar gc = new GregorianCalendar()
        gc.setTime(created())
        DatatypeFactory.newInstance().newXMLGregorianCalendar(gc)
    }

    def tenantEntity(String id, String name, boolean enabled, String description, String displayName) {
        new org.openstack.docs.identity.api.v2.Tenant().with {
            it.id = id
            it.name = name
            it.enabled = enabled
            it.description = description
            it.displayName = displayName
            return it
        }
    }
}
