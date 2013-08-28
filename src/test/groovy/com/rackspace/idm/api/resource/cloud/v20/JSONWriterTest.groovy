package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.AuthenticatedBy
import com.rackspace.docs.identity.api.ext.rax_auth.v1.ObjectFactory
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.api.resource.cloud.v20.json.writers.JSONWriter
import com.rackspacecloud.docs.auth.api.v1.BaseURLRefList
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import spock.lang.Shared
import testHelpers.RootServiceTest

class JSONWriterTest extends RootServiceTest {
    @Shared JSONWriter writer = new JSONWriter();
    @Shared ObjectFactory objectFactory = new ObjectFactory()

    def "can write authenticatedBy using json"() {
        when:
        def response = v2Factory.createAuthenticateResponse()
        def authenticatedBy = new AuthenticatedBy().with {
            it.credential = input.asList()
            it
        }
        response.getToken().any.add(objectFactory.createAuthenticatedBy(authenticatedBy))

        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writer.writeTo(response, AuthenticateResponse.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()

        then:
        JSONParser parser = new JSONParser();
        JSONObject authResponse = (JSONObject) parser.parse(json);
        JSONObject access = (JSONObject)authResponse.get(JSONConstants.ACCESS)
        JSONObject token = (JSONObject)access.get(JSONConstants.TOKEN)
        JSONArray authenticatedByList = (JSONArray) token.get(JSONConstants.RAX_AUTH_AUTHENTICATED_BY)
        authenticatedByList as Set == input as Set


        where:
        input << [
            ["RSA"],
            ["RSA", "Password"]
        ]
    }

    def "can write baseUrlRefList"() {
        when:
        def baseUrlRefs = new ArrayList<BaseURLRefList>();
        baseUrlRefs.add(v1Factory.createBaseUrlRef(baseUrlId,publicUrl,v1Default));
        def baseUrlRefList = new BaseURLRefList();
        baseUrlRefList.baseURLRef = baseUrlRefs;

        ByteArrayOutputStream arrayOutputStream = new ByteArrayOutputStream()
        writer.writeTo(baseUrlRefList, BaseURLRefList.class, null, null, null, null, arrayOutputStream)
        def json = arrayOutputStream.toString()

        then:
        JSONParser parser = new JSONParser();
        JSONObject baseResponse = (JSONObject) parser.parse(json);
        baseResponse.size() == 1;
        baseResponse.get("baseURLRefs")[0]["id"] == baseUrlId;
        baseResponse.get("baseURLRefs")[0]["href"] == publicUrl;
        baseResponse.get("baseURLRefs")[0]["v1Default"] == v1Default;

        where:
        baseUrlId = 1;
        publicUrl = "http://public/";
        v1Default = false;
    }

}
