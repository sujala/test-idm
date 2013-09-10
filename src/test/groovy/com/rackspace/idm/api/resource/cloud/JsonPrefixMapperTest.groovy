package com.rackspace.idm.api.resource.cloud

import com.rackspace.docs.identity.api.ext.rax_auth.v1.AuthenticatedBy
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.domain.config.JAXBContextResolver
import com.sun.jersey.api.json.JSONJAXBContext
import org.json.simple.JSONArray
import org.json.simple.JSONObject
import org.json.simple.parser.JSONParser
import spock.lang.Shared
import testHelpers.RootServiceTest

import javax.xml.bind.JAXBException

import static com.rackspace.idm.JSONConstants.*

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 8/12/13
 * Time: 4:58 PM
 * To change this template use File | Settings | File Templates.
 */
class JsonPrefixMapperTest extends RootServiceTest{
    @Shared JsonPrefixMapper prefixMapper

    def setupSpec() {
       prefixMapper = new JsonPrefixMapper()
    }

    def "Create new Object with correct prefix"(){
        given:
        def authResponse = v2Factory.createAuthenticateResponse().with {
            it.token.authenticatedBy = new AuthenticatedBy().with {
                it.credential = [GlobalConstants.AUTHENTICATED_BY_PASSWORD].asList()
                it
            }
            it
        }
        OutputStream outputStream = new org.apache.commons.io.output.ByteArrayOutputStream();
        getMarshaller().marshallToJSON(authResponse, outputStream)
        String jsonString = outputStream.toString();

        JSONParser parser = new JSONParser()
        JSONObject outer = (JSONObject) parser.parse(jsonString)
        JSONObject inner = (JSONObject) outer.get(ACCESS);
        HashMap<String, String[]> hashMap = new LinkedHashMap<>()
        hashMap.put(TOKEN_AUTHENTICATED_BY, RAX_AUTH_AUTHENTICATED_BY)

        when:
        JSONObject object = prefixMapper.mapPrefix(inner, hashMap)


        then:
        JSONObject raxAuthBy = ((JSONObject)object.get(TOKEN)).get(RAX_AUTH_AUTHENTICATED_BY)
        raxAuthBy != null
        JSONArray array = raxAuthBy.get(CREDENTIAL)
        array != null
        array.get(0) == GlobalConstants.AUTHENTICATED_BY_PASSWORD
    }

    def "Create new Object with correct prefix with null value"(){
        given:
        def authResponse = v2Factory.createAuthenticateResponse().with {
            it.token.authenticatedBy = new AuthenticatedBy().with {
                it.credential = null
                it
            }
            it
        }
        OutputStream outputStream = new org.apache.commons.io.output.ByteArrayOutputStream();
        getMarshaller().marshallToJSON(authResponse, outputStream)
        String jsonString = outputStream.toString();

        JSONParser parser = new JSONParser()
        JSONObject outer = (JSONObject) parser.parse(jsonString)
        JSONObject inner = (JSONObject) outer.get(ACCESS);
        HashMap<String, String[]> hashMap = new LinkedHashMap<>()
        hashMap.put(TOKEN_AUTHENTICATED_BY, RAX_AUTH_AUTHENTICATED_BY)

        when:
        JSONObject object = prefixMapper.mapPrefix(inner, hashMap)


        then:
        ((JSONObject)object.get(TOKEN)).containsKey(RAX_AUTH_AUTHENTICATED_BY)
        JSONObject raxAuthBy = ((JSONObject)object.get(TOKEN)).get(RAX_AUTH_AUTHENTICATED_BY)
        JSONObject authBy = ((JSONObject)object.get(TOKEN)).get(AUTHENTICATED_BY)
        raxAuthBy == null
        authBy == null
    }

    def "Do not replace element if it exist somewhere else"(){
        given:
        String jsonString = "{\"access\":{\"token\":{\"expires\":\"2013-08-13T15:49:53.706-05:00\",\"id\":\"id\",\"tenant\":null,\"authenticatedBy\":{\"credential\":[\"PASSWORD\"]}},\"user\":null,\"serviceCatalog\":null, \"authenticatedBy\":null}}"

        JSONParser parser = new JSONParser()
        JSONObject outer = (JSONObject) parser.parse(jsonString)
        JSONObject inner = (JSONObject) outer.get(ACCESS);
        HashMap<String, String[]> hashMap = new LinkedHashMap<>()
        hashMap.put(TOKEN_AUTHENTICATED_BY, RAX_AUTH_AUTHENTICATED_BY)

        when:
        JSONObject object = prefixMapper.mapPrefix(inner, hashMap)


        then:
        JSONObject raxAuthBy = ((JSONObject)object.get(TOKEN).get(RAX_AUTH_AUTHENTICATED_BY))
        raxAuthBy != null
        JSONArray array = raxAuthBy.get(CREDENTIAL)
        array != null
        array.get(0) == GlobalConstants.AUTHENTICATED_BY_PASSWORD
        object.containsKey(JSONConstants.AUTHENTICATED_BY)

    }


    def getMarshaller() throws JAXBException {
        return ((JSONJAXBContext) JAXBContextResolver.get()).createJSONMarshaller()
    }
}
