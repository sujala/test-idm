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
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = "roleC"
                            ta.onRoleName = "roleCName"
                            ta.forTenants.addAll(["tenantC1","tenantC2"])
                            ta
                    })
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = "roleD"
                            ta.onRoleName = "roleDName"
                            ta
                    })
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = "roleE"
                            ta.onRoleName = "roleEName"
                            ta.forTenants.addAll(["", "tenantE1", "  ", ""])
                            ta
                    })
                    tas.tenantAssignment.add(new TenantAssignment().with {
                        ta ->
                            ta.onRole = "roleF"
                            ta.onRoleName = "roleFName"
                            ta.forTenants.addAll([])
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
        tas.size() == 6

        // Verify array is correct
        tas[0].onRole == "roleA"
        tas[0].onRoleName == "roleAName"
        tas[0].forTenants == ["*"]

        tas[1].onRole == "roleB"
        tas[1].onRoleName == "roleBName"
        tas[1].forTenants == ["tenantB"]

        tas[2].onRole == "roleC"
        tas[2].onRoleName == "roleCName"
        tas[2].forTenants == ["tenantC1","tenantC2"]

        tas[3].onRole == "roleD"
        tas[3].onRoleName == "roleDName"
        !tas[3].containsKey("forTenants") // When writing out, does not include forTenants if none provided

        tas[4].onRole == "roleE"
        tas[4].onRoleName == "roleEName"
        tas[4].forTenants == ["tenantE1"] // When writing out, empty string/space only elements are ignored

        tas[5].onRole == "roleF"
        tas[5].onRoleName == "roleFName"
        tas[5].forTenants == [] // When writing out, empty string/space only elements are ignored

    }

    def "Empty array written for tenantAssignments when no tenant role assignments"() {
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
        tas != null
        tas.size() == 0
    }
}
