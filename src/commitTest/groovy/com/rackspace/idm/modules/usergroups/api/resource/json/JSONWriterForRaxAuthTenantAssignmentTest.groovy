package com.rackspace.idm.modules.usergroups.api.resource.json

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.idm.modules.usergroups.Constants
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import spock.lang.Specification

class JSONWriterForRaxAuthTenantAssignmentTest extends Specification {

    JSONWriterForRaxAuthTenantAssignment writer = new JSONWriterForRaxAuthTenantAssignment()

    def "write tenantAssignment to json"() {

        TenantAssignment assignment = new TenantAssignment().with {
            it.onRoleName = "roleName"
            it.onRole = "roleA"
            it.forTenants.add("tenantA")
            it.forTenants.add("tenantB")
            it
        }

        when:
        def out = new ByteArrayOutputStream()
        writer.writeTo(assignment, null, null, null, null, null, out)

        def json = out.toString()
        JSONObject outer = (JSONObject) new JSONParser().parse(json)

        then:
        json != null

        JSONObject ta = outer.get(Constants.RAX_AUTH_TENANT_ASSIGNMENT)

        ta != null

        ta.get("onRoleName") == "roleName"
        ta.get("onRole") == "roleA"
        ta.get("forTenants") == ["tenantA", "tenantB"]
    }

    def "write tenantAssignment with (*) forTenant to json"() {

        TenantAssignment assignment = new TenantAssignment().with {
            it.onRoleName = "roleName1"
            it.onRole = "roleB"
            it.forTenants.add("*")
            it
        }

        when:
        def out = new ByteArrayOutputStream()
        writer.writeTo(assignment, null, null, null, null, null, out)

        def json = out.toString()
        JSONObject outer = (JSONObject) new JSONParser().parse(json)

        then:
        json != null

        JSONObject ta = outer.get(Constants.RAX_AUTH_TENANT_ASSIGNMENT)

        ta != null
        ta.get("onRoleName") == "roleName1"
        ta.get("onRole") == "roleB"
        ta.get("forTenants") == ["*"]
    }
}
