package com.rackspace.idm.api.resource.cloud.v20

import com.google.gson.JsonObject
import com.rackspace.idm.exception.BadRequestException
import spock.lang.Shared
import testHelpers.RootServiceTest

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 3/8/13
 * Time: 4:30 PM
 * To change this template use File | Settings | File Templates.
 */
class JSONReaderForRoleTest extends RootServiceTest {

    @Shared JSONReaderForRole reader

    def setupSpec() {
        reader = new JSONReaderForRole()
    }

    def "a role with rsWeight and rsPropagate flags can be read by getRoleFromJSONString"() {
        given:
        def role = entityFactory.createClientRole().with {
            it.rsWeight = 500
            it.propagate = true
            return it
        }
        JsonObject outer = new JsonObject()
        JsonObject inner = new JsonObject()
        outer.add("role", inner)
        for (property in role.getProperties()) {
            inner.addProperty(property.getKey().toString(), property.getValue().toString())
        }
        outer.toString()

        when:
        def response = reader.getRoleFromJSONString(outer.toString())

        then:
        notThrown(BadRequestException)
        response.rsWeight == 500
        response.propagate == true
    }
}
