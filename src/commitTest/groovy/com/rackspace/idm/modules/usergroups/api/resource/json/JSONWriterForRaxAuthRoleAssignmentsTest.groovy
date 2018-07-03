package com.rackspace.idm.modules.usergroups.api.resource.json

import com.rackspace.docs.identity.api.ext.rax_auth.v1.AssignmentSource
import com.rackspace.docs.identity.api.ext.rax_auth.v1.AssignmentSources
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.idm.domain.entity.RoleAssignmentSourceType
import com.rackspace.idm.domain.entity.RoleAssignmentType
import com.rackspace.idm.modules.usergroups.Constants
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import spock.lang.Specification

class JSONWriterForRaxAuthRoleAssignmentsTest extends Specification {

    JSONWriterForRaxAuthRoleAssignments writer = new JSONWriterForRaxAuthRoleAssignments()

    def "write tenantAssignment to json"() {
        TenantAssignment assignment = new TenantAssignment().with {
            it.onRoleName = "roleName"
            it.onRole = "roleA"
            it.forTenants.add("tenantA")
            it.forTenants.add("tenantB")
            it
        }

        RoleAssignments roleAssignments = createRoleAssignmentsWrapper(assignment)

        when:
        def out = new ByteArrayOutputStream()
        writer.writeTo(roleAssignments, null, null, null, null, null, out)

        then:
        JSONObject ta = getTenantAssignmentFromJson(out.toString())
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
        RoleAssignments roleAssignments = createRoleAssignmentsWrapper(assignment)

        when:
        def out = new ByteArrayOutputStream()
        writer.writeTo(roleAssignments, null, null, null, null, null, out)

        then:
        JSONObject ta = getTenantAssignmentFromJson(out.toString())

        ta != null
        ta.get("onRoleName") == "roleName1"
        ta.get("onRole") == "roleB"
        ta.get("forTenants") == ["*"]
    }

    def "write tenantAssignment with sources to json"() {

        TenantAssignment assignment = new TenantAssignment().with {
            it.onRoleName = "roleName"
            it.onRole = "roleA"
            it.forTenants.add("tenantA")
            it.forTenants.add("tenantB")

            it.sources = new AssignmentSources()
            AssignmentSource aSource = new AssignmentSource();
            aSource.sourceId = "sourceId"
            aSource.assignmentType = RoleAssignmentType.DOMAIN.name()
            aSource.sourceType = RoleAssignmentSourceType.USER.name()
            aSource.forTenants.addAll(["tenantA", "tenantB"])
            it.sources.source.add(aSource)

            it
        }
        RoleAssignments roleAssignments = createRoleAssignmentsWrapper(assignment)

        when:
        def out = new ByteArrayOutputStream()
        writer.writeTo(roleAssignments, null, null, null, null, null, out)

        then:
        JSONObject ta = getTenantAssignmentFromJson(out.toString())

        ta != null
        ta.get("onRoleName") == "roleName"
        ta.get("onRole") == "roleA"
        ta.get("forTenants") == ["tenantA", "tenantB"]
        JSONArray sources = (JSONArray) ta.get("sources")
        sources.size() == 1
        JSONObject source = sources.get(0)
        source.get("sourceId") == "sourceId"
        source.get("assignmentType") == RoleAssignmentType.DOMAIN.name()
        source.get("sourceType") == RoleAssignmentSourceType.USER.name()
        source.get("forTenants") == ["tenantA", "tenantB"]
    }

    RoleAssignments createRoleAssignmentsWrapper(TenantAssignment ta) {
        RoleAssignments roleAssignments = new RoleAssignments()
        roleAssignments.tenantAssignments = new TenantAssignments()

        roleAssignments.tenantAssignments.tenantAssignment.add(ta)
        return roleAssignments
    }

    JSONObject getTenantAssignmentFromJson(String json) {
        JSONObject outer = (JSONObject) new JSONParser().parse(json)

        JSONObject ras = outer.get(Constants.RAX_AUTH_ROLE_ASSIGNMENTS)
        assert ras != null

        JSONArray tas = (JSONArray) ras.get(Constants.TENANT_ASSIGNMENTS)
        assert tas != null

        JSONObject ta = tas[0]
        assert ta != null

        return ta
    }
}
