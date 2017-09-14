package com.rackspace.idm.modules.usergroups.api.resource.json

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.modules.usergroups.Constants
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import spock.lang.Specification

class JSONWriterForRaxAuthUserGroupRoleAssignmentsTest extends Specification{
    JSONWriterForRaxAuthRoleAssignments writer = new JSONWriterForRaxAuthRoleAssignments()

    def "write roles assignments to json"() {
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = "roleA"
                            ta.onRoleName = "roleAName"
                            ta.forTenants.add("*")
                            ta
                    })
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = "roleB"
                            ta.onRoleName = "roleBName"
                            ta.forTenants.add("tenantB")
                            ta
                    })
                    tas
            }
            it
        }

        when:
        def out = new ByteArrayOutputStream()
        writer.writeTo(assignments, null, null, null, null, null, out)

        def json = out.toString()
        JSONObject outer = (JSONObject) new JSONParser().parse(json)

        then:
        json != null

        // Must have a prefixed root name
        JSONObject ras = outer.get(Constants.RAX_AUTH_ROLE_ASSIGNMENTS)
        ras != null

        // Have non-prefixed ta object
        JSONArray tas = ras.get(Constants.TENANT_ASSIGNMENTS)
        tas.size() == 2

        // Verify array is correct
        tas[0].onRole == "roleA"
        tas[0].onRoleName == "roleAName"
        tas[0].forTenants == ["*"]

        tas[1].onRole == "roleB"
        tas[1].onRoleName == "roleBName"
        tas[1].forTenants == ["tenantB"]
    }

    def "Null returned for tenantAssignments when no tenant roles"() {
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments()
            it
        }

        when:
        def out = new ByteArrayOutputStream()
        writer.writeTo(assignments, null, null, null, null, null, out)

        def json = out.toString()
        JSONObject outer = (JSONObject) new JSONParser().parse(json)
        JSONArray tas = outer.get(Constants.RAX_AUTH_ROLE_ASSIGNMENTS).get(Constants.TENANT_ASSIGNMENTS)

        then:
        tas == null // In JSON will be a null value rather than an empty array
    }
}
