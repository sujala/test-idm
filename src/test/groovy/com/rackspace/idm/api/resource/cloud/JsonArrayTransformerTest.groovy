package com.rackspace.idm.api.resource.cloud

import org.json.simple.parser.JSONParser
import spock.lang.Shared
import testHelpers.RootServiceTest

class JsonArrayTransformerTest extends RootServiceTest {
    @Shared JsonArrayTransformer arrayTransformer

    def setupSpec() {
        arrayTransformer = new JsonArrayTransformer()
    }

    def "Transform json by including wrapper element if non existent"(){
        given:
        String jsonString = "{\"user\":{\"roles\":[{\"name\":\"managed\"}],\"username\":\"jqsmith\"}}"
        String transformedJsonString = "{\"user\":{\"roles\": { \"role\" : [{\"name\":\"managed\"}] },\"username\":\"jqsmith\"}}"

        def parser = new JSONParser()
        def jsonObject = parser.parse(jsonString)
        def transformedJsonObject = parser.parse(transformedJsonString)
        assert(!jsonObject.equals(transformedJsonObject))

        when:
        arrayTransformer.transformIncludeWrapper(jsonObject)

        then:
        assert(jsonObject.equals(transformedJsonObject));
    }

    def "Transform json by removing wrapper element if existent"(){
        given:
        String jsonString = "{\"user\":{\"roles\": { \"role\" : [{\"name\":\"managed\"}] },\"username\":\"jqsmith\"}}"
        String transformedJsonString = "{\"user\":{\"roles\":[{\"name\":\"managed\"}],\"username\":\"jqsmith\"}}"

        def parser = new JSONParser()
        def jsonObject = parser.parse(jsonString)
        def transformedJsonObject = parser.parse(transformedJsonString)
        assert(!jsonObject.equals(transformedJsonObject))

        when:
        arrayTransformer.transformRemoveWrapper(jsonObject, null)

        then:
        assert(jsonObject.equals(transformedJsonObject));
    }
}
