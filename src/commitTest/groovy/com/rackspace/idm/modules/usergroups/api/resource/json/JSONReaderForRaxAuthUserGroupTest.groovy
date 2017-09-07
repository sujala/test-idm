package com.rackspace.idm.modules.usergroups.api.resource.json

import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.exception.BadRequestException
import com.rackspace.idm.modules.usergroups.Constants
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import spock.lang.Specification

class JSONReaderForRaxAuthUserGroupTest extends Specification{
    JSONReaderForRaxAuthUserGroup reader = new JSONReaderForRaxAuthUserGroup()

    def "unmarshall user group from json"() {
        def jsonUserGroup =
                '{' +
                '  "RAX-AUTH:userGroup": {\n' +
                '    "name": "name",\n' +
                '    "description": "description",\n' +
                '    "id": "id",\n' +
                '    "domainId": "domainId"\n' +
                '  }\n' +
                '}'

        when:
        UserGroup userGroup = reader.readFrom(UserGroup, null, null, null, null, new ByteArrayInputStream(jsonUserGroup.bytes))

        then:
        userGroup != null
        userGroup.id == "id"
        userGroup.domainId == "domainId"
        userGroup.name == "name"
        userGroup.description == "description"
    }

    def "fails without prefix json"() {
        def jsonUserGroup =
                '{' +
                        '  "userGroup": {\n' +
                        '    "name": "name",\n' +
                        '    "description": "description",\n' +
                        '    "id": "id",\n' +
                        '    "domainId": "domainId"\n' +
                        '  }\n' +
                        '}'

        when:
        UserGroup userGroup = reader.readFrom(UserGroup, null, null, null, null, new ByteArrayInputStream(jsonUserGroup.bytes))

        then:
        BadRequestException ex = thrown()
        ex.message == "Invalid json request body"
    }

}
