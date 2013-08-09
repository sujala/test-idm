package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.api.resource.cloud.v20.JSONReaders.JSONReaderForTenant
import com.sun.jersey.api.json.JSONConfiguration
import com.sun.jersey.api.json.JSONJAXBContext
import com.sun.jersey.api.json.JSONMarshaller
import org.openstack.docs.identity.api.v2.Tenant
import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/7/13
 * Time: 11:33 AM
 * To change this template use File | Settings | File Templates.
 */
class JSONReaderForTenantTest extends RootServiceTest{

    @Shared
    JSONReaderForTenant jsonReader

    def setupSpec() {
        jsonReader = new JSONReaderForTenant()
    }

    def "Read from Json String" () {
        given:
        def tenantId = "id"
        def tenantName = "tenantName"
        def tenantDesc = "tenantDesc"
        def enabled = true
        def displayName = "displayName"

        JSONConfiguration jc = JSONConfiguration.natural().rootUnwrapping(false).build()
        JSONJAXBContext jjc = new JSONJAXBContext(jc, Tenant.class)
        JSONMarshaller jm = jjc.createJSONMarshaller()

        Tenant tenant = v2Factory.createTenant(tenantName, displayName, enabled).with {
            it.id = tenantId
            it.description = tenantDesc
            it
        }

        StringWriter sw = new StringWriter()
        jm.marshallToJSON(tenant, sw)

        InputStream inputStream = new ByteArrayInputStream(sw.toString().bytes)

        when:
        Tenant tenantObject = jsonReader.readFrom(null, null, null, null, null, inputStream)

        then:
        tenantObject.id == tenantId
        tenantObject.name == tenantName
        tenantObject.description == tenantDesc
        tenantObject.enabled == enabled
        tenantObject.displayName == displayName
    }
}
