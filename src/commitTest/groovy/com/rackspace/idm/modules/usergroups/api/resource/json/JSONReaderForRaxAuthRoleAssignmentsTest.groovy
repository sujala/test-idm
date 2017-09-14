package com.rackspace.idm.modules.usergroups.api.resource.json

import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.idm.exception.BadRequestException
import spock.lang.Specification

class JSONReaderForRaxAuthRoleAssignmentsTest extends Specification{
    JSONReaderForRaxAuthRoleAssignments reader = new JSONReaderForRaxAuthRoleAssignments()

    def "unmarshall user group from json"() {
        def json =
                '{' +
                '  "RAX-AUTH:roleAssignments": {\n' +
                '     "tenantAssignments": [' +
                '       {\n' +
                '         "onRole": "22776",\n' +
                '         "forTenants": [\n' +
                '           "t1",\n' +
                '           "t2"\n' +
                '         ]' +
                '       }' +
                '     ]\n' +
                '   }' +
                '}'

        when:
        RoleAssignments roleAssignments = reader.readFrom(RoleAssignments, null, null, null, null, new ByteArrayInputStream(json.bytes))

        then:
        roleAssignments != null
        roleAssignments.tenantAssignments != null
        roleAssignments.tenantAssignments.tenantAssignment != null
        roleAssignments.tenantAssignments.tenantAssignment.size() == 1
        roleAssignments.tenantAssignments.tenantAssignment[0].onRole == "22776"
        roleAssignments.tenantAssignments.tenantAssignment[0].onRoleName == null
        roleAssignments.tenantAssignments.tenantAssignment[0].forTenants == ["t1", "t2"]
    }

    def "fails without prefix json"() {
        def json =
                '{' +
                '  "roleAssignments": {\n' +
                '     "tenantAssignments": [' +
                '       {\n' +
                '         "onRole": "22776",\n' +
                '         "forTenants": [\n' +
                '           "t1",\n' +
                '           "t2"\n' +
                '         ]' +
                '       }' +
                '     ]\n' +
                '   }' +
                '}'

        when:
        RoleAssignments roleAssignments = reader.readFrom(RoleAssignments, null, null, null, null, new ByteArrayInputStream(json.bytes))

        then:
        BadRequestException ex = thrown()
        ex.message == "Invalid json request body"
    }

}
