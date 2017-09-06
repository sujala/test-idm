package com.rackspace.idm.modules.usergroups.api.resource.json

import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.modules.usergroups.Constants
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import spock.lang.Specification

class JSONWriterForRaxAuthUserGroupTest extends Specification{
    JSONWriterForRaxAuthUserGroup writer = new JSONWriterForRaxAuthUserGroup()

    def "write web user group to json"() {
        UserGroup webgroup = new UserGroup().with {
            it.id = "id"
            it.domainId = "domainId"
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        def out = new ByteArrayOutputStream()
        writer.writeTo(webgroup, null, null, null, null, null, out)

        then:
        def json = out.toString()
        JSONObject outer = (JSONObject) new JSONParser().parse(json)
        json != null
        JSONObject a = outer.get(Constants.RAX_AUTH_USER_GROUP)
        a.id == webgroup.id
        a.domainId == webgroup.domainId
        a.name == webgroup.name
        a.description == webgroup.description
    }
}
