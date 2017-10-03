package com.rackspace.idm.modules.usergroups.api.resource.json

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.idm.exception.BadRequestException
import spock.lang.Specification

class JSONReaderForRaxAuthTenantAssignmentTest extends Specification {

    JSONReaderForRaxAuthTenantAssignment reader = new JSONReaderForRaxAuthTenantAssignment()

    def "unmarshall tenantAssignment from json"() {
        def jsonTenantAssignment =
                '{' +
                        '  "RAX-AUTH:tenantAssignment": {\n' +
                        '    "onRoleName": "roleName",\n' +
                        '    "forTenants": [\n' +
                        '     "tenantA",\n' +
                        '     "tenantB"\n' +
                        '   ],\n' +
                        '    "onRole": "roleA"\n' +
                        ' }\n' +
                        '}'
        when:
        TenantAssignment tenantAssignment = reader.readFrom(TenantAssignment, null, null, null, null, new ByteArrayInputStream(jsonTenantAssignment.bytes))

        then:
        tenantAssignment != null
        tenantAssignment.onRole == "roleA"
        tenantAssignment.onRoleName == "roleName"
        tenantAssignment.forTenants.size() == 2
        tenantAssignment.forTenants.get(0) == "tenantA"
        tenantAssignment.forTenants.get(1) == "tenantB"
    }

    def "unmarshall tenantAssignment with (*) forTenant from json"() {
        def jsonTenantAssignment =
                '{' +
                        '  "RAX-AUTH:tenantAssignment": {\n' +
                        '    "onRoleName": "roleName1",\n' +
                        '    "forTenants": [\n' +
                        '     "*"\n' +
                        '   ],\n' +
                        '    "onRole": "roleB"\n' +
                        ' }\n' +
                        '}'
        when:
        TenantAssignment tenantAssignment = reader.readFrom(TenantAssignment, null, null, null, null, new ByteArrayInputStream(jsonTenantAssignment.bytes))

        then:
        tenantAssignment != null
        tenantAssignment.onRole == "roleB"
        tenantAssignment.onRoleName == "roleName1"
        tenantAssignment.forTenants.size() == 1
        tenantAssignment.forTenants.get(0) == "*"
    }

    def "fails without prefix json"() {
        def jsonTenantAssignment =
                '{' +
                        '  ":tenantAssignment": {\n' +
                        '    "onRoleName": "roleName",\n' +
                        '    "forTenants": [\n' +
                        '     "tenantA",\n' +
                        '     "tenantB"\n' +
                        '   ],\n' +
                        '    "onRole": "roleA"\n' +
                        ' }\n' +
                        '}'
        when:
        TenantAssignment tenantAssignment = reader.readFrom(TenantAssignment, null, null, null, null, new ByteArrayInputStream(jsonTenantAssignment.bytes))

        then:
        BadRequestException ex = thrown()
        ex.message == "Invalid json request body"
    }
}
