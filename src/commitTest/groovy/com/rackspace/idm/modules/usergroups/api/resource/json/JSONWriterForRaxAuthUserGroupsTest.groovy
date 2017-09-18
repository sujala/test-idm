package com.rackspace.idm.modules.usergroups.api.resource.json

import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroups
import com.rackspace.idm.modules.usergroups.Constants
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import spock.lang.Specification

class JSONWriterForRaxAuthUserGroupsTest extends Specification {

    JSONWriterForRaxAuthUserGroups writer = new JSONWriterForRaxAuthUserGroups()

    def "Write web user groups to json"() {
        UserGroup webGroup = new UserGroup().with {
            it.id = "id"
            it.domainId = "domainId"
            it.name = "name"
            it.description = "description"
            it
        }

        UserGroups webGroups = new UserGroups();
        webGroups.userGroup.add(webGroup)

        when:
        def out = new ByteArrayOutputStream()
        writer.writeTo(webGroups, null, null, null, null, null, out)

        then:
        def json = out.toString()
        json != null
        JSONObject outer = (JSONObject) new JSONParser().parse(json)
        JSONArray groups = (JSONArray) outer.get(Constants.RAX_AUTH_USER_GROUPS)
        groups != null
        groups.size() == 1

        JSONObject a  = (JSONObject) groups.get(0)
        a.id == webGroup.id
        a.domainId == webGroup.domainId
        a.name == webGroup.name
        a.description == webGroup.description
    }
}
