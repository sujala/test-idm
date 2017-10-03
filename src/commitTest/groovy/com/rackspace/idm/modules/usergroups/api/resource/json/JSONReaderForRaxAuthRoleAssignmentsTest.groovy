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

    def "unmarshall user group from json w/ * forTenants"() {
        def json =
                '{' +
                        '  "RAX-AUTH:roleAssignments": {\n' +
                        '     "tenantAssignments": [' +
                        '       {\n' +
                        '         "onRole": "22776",\n' +
                        '         "forTenants": [\n' +
                        '           "*"\n' +
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
        roleAssignments.tenantAssignments.tenantAssignment[0].forTenants == ["*"]
    }

    def "unmarshall user group from json w/ various 'empty' forTenants"() {
        def json =
                '{' +
                        '  "RAX-AUTH:roleAssignments": {\n' +
                        '     "tenantAssignments": [' +
                        '       {\n' +
                        '         "onRole": "22776",\n' +
                        '         "forTenants": []' +
                        '       }' +
                        '       , {\n' +
                        '         "onRole": "22776",\n' +
                        '         "forTenants": [""]' +
                        '       }' +
                        '       , {\n' +
                        '         "onRole": "22776",\n' +
                        '         "forTenants": ["", "a"]' +
                        '       }' +
                        '       , {\n' +
                        '         "onRole": "22776",\n' +
                        '         "forTenants": null' +
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
        roleAssignments.tenantAssignments.tenantAssignment.size() == 4
        roleAssignments.tenantAssignments.tenantAssignment[0].onRole == "22776"
        roleAssignments.tenantAssignments.tenantAssignment[0].onRoleName == null
        roleAssignments.tenantAssignments.tenantAssignment[0].forTenants != null
        roleAssignments.tenantAssignments.tenantAssignment[0].forTenants.size() == 0

        roleAssignments.tenantAssignments.tenantAssignment[1].forTenants.size() == 1
        roleAssignments.tenantAssignments.tenantAssignment[2].forTenants.size() == 2
        roleAssignments.tenantAssignments.tenantAssignment[3].forTenants.size() == 0

    }

    def "unmarshall no tenant assignments"() {
        def json =
                '{' +
                        '  "RAX-AUTH:roleAssignments": {\n' +
                        '     "tenantAssignments": []\n' +
                        '   }' +
                        '}'

        when:
        RoleAssignments roleAssignments = reader.readFrom(RoleAssignments, null, null, null, null, new ByteArrayInputStream(json.bytes))

        then:
        roleAssignments != null
        roleAssignments.tenantAssignments != null
        roleAssignments.tenantAssignments.tenantAssignment != null
        roleAssignments.tenantAssignments.tenantAssignment.size() == 0
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
        reader.readFrom(RoleAssignments, null, null, null, null, new ByteArrayInputStream(json.bytes))

        then:
        BadRequestException ex = thrown()
        ex.message == "Invalid json request body"
    }

    def "unmarshall user group from json fails if forTenants is not an array"() {
        def json =
                '{' +
                        '  "RAX-AUTH:roleAssignments": {\n' +
                        '     "tenantAssignments": [' +
                        '       {\n' +
                        '         "onRole": "22776",\n' +
                        '         "forTenants": "12345"' +
                        '       }' +
                        '     ]\n' +
                        '   }' +
                        '}'

        when:
        reader.readFrom(RoleAssignments, null, null, null, null, new ByteArrayInputStream(json.bytes))

        then:
        BadRequestException ex = thrown()
        ex.message == "Invalid json request body"
    }

}
