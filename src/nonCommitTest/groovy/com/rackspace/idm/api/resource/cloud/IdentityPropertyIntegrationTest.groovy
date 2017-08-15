package com.rackspace.idm.api.resource.cloud

import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProperty
import com.rackspace.idm.api.resource.cloud.devops.DefaultDevOpsService
import com.rackspace.idm.api.resource.cloud.devops.JsonWriterForIdmProperty
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.IdentityPropertyValueType
import com.rackspace.idm.validation.entity.IdentityPropertyBooleanValueTypeValidator
import com.rackspace.idm.validation.entity.IdentityPropertyIntValueTypeValidator
import com.rackspace.idm.validation.entity.IdentityPropertyJsonValueTypeValidator
import com.rackspace.idm.validation.entity.IdentityPropertyStringValueTypeValidator
import groovy.json.JsonSlurper
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpStatus
import org.openstack.docs.identity.api.v2.BadRequestFault
import org.openstack.docs.identity.api.v2.IdentityFault
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.IdmAssert
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class IdentityPropertyIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityConfig identityConfig

    def "test get idm props"() {
        when:
        def response = devops.getIdmProps(utils.getIdentityAdminToken())

        then: "success"
        response.status == 200
        def data = new JsonSlurper().parseText(response.getEntity(String))

        and: "config path set"
        data[JsonWriterForIdmProperty.JSON_PROP_CONFIG_PATH] == identityConfig.getConfigRoot()

        and: "string configs are returned as strings"
        data[JsonWriterForIdmProperty.JSON_PROP_PROPERTIES].find{it.name == "ga.username"}[JsonWriterForIdmProperty.JSON_PROP_VALUE] == "auth"

        and: "int configs are returned as int"
        data[JsonWriterForIdmProperty.JSON_PROP_PROPERTIES].find{it.name == "reloadable.docs.cache.timeout"}[JsonWriterForIdmProperty.JSON_PROP_VALUE] == 10
    }

    def "test get eDir props"() {
        when:
        def response = devops.getIdmProps(utils.getIdentityAdminToken())

        then: "success"
        response.status == 200
        def data = new JsonSlurper().parseText(response.getEntity(String))

        and: "eDir bind DN visible"
        data[JsonWriterForIdmProperty.JSON_PROP_PROPERTIES].find{it.name == IdentityConfig.EDIR_BIND_DN} != null

        and: "eDir password is not visible"
        data[JsonWriterForIdmProperty.JSON_PROP_PROPERTIES].find{it.name == IdentityConfig.EDIR_BIND_PASSWORD} == null
    }

    @Unroll
    def "get idm props can be called by user with #role role"() {
        given:
        def idmAdmin = utils.createIdentityAdmin()
        def idmAdminToken = utils.getToken(idmAdmin.username)

        when: "call w/o query props role"
        def response = devops.getIdmProps(idmAdminToken)

        then:
        response.status == HttpStatus.SC_FORBIDDEN

        when: "call w/ query props role"
        def roleResponse = utils.getRoleByName(role.roleName)
        utils.addRoleToUser(idmAdmin, roleResponse.id)
        idmAdminToken = utils.getToken(idmAdmin.username)
        def responseWRole = devops.getIdmProps(idmAdminToken)

        then:
        responseWRole.status == HttpStatus.SC_OK

        cleanup:
        utils.deleteUserQuietly(idmAdmin)

        where:
        role                                 | _
        IdentityRole.IDENTITY_QUERY_PROPS    | _
        IdentityRole.IDENTITY_PROPERTY_ADMIN | _
    }

    def "test case-insensitive search for Identity properties"() {
        when: "static config is case-insensitive"
        def response = devops.getIdmProps(utils.getServiceAdminToken(), StringUtils.swapCase(IdentityConfig.EMAIL_HOST))
        def data = new JsonSlurper().parseText(response.getEntity(String))

        then:
        data[JsonWriterForIdmProperty.JSON_PROP_PROPERTIES].find{it.name == IdentityConfig.EMAIL_HOST}[JsonWriterForIdmProperty.JSON_PROP_VALUE] == identityConfig.getStaticConfig().getEmailHost()

        when: "reloadable config is case-insensitive"
        response = devops.getIdmProps(utils.getServiceAdminToken(), StringUtils.swapCase(IdentityConfig.AE_NODE_NAME_FOR_SIGNOFF_PROP))
        data = new JsonSlurper().parseText(response.getEntity(String))

        then:
        data[JsonWriterForIdmProperty.JSON_PROP_PROPERTIES].find{it.name == IdentityConfig.AE_NODE_NAME_FOR_SIGNOFF_PROP}[JsonWriterForIdmProperty.JSON_PROP_VALUE] == identityConfig.getReloadableConfig().getAENodeNameForSignoff()

        when: "directory properties are case-insensitive"
        def property = utils.createIdentityProperty()
        response = devops.getIdmProps(utils.getServiceAdminToken(), StringUtils.swapCase(property.name))
        data = new JsonSlurper().parseText(response.getEntity(String))

        then:
        data[JsonWriterForIdmProperty.JSON_PROP_PROPERTIES].find{it.name == property.name}[JsonWriterForIdmProperty.JSON_PROP_VALUE] == property.value

        cleanup:
        utils.deleteIdentityProperty(property.id)
    }

    def "test identity property CRUD API calls callable with identity:property-admin"() {
        given:
        def idmAdmin = utils.createIdentityAdmin()
        def idmAdminToken = utils.getToken(idmAdmin.username)
        def idmProperty = v2Factory.createIdentityProperty(testUtils.getRandomUUID("prop"), "foo", IdentityPropertyValueType.STRING.getTypeName())

        when: "call CREATE w/o prop manage role"
        def response = devops.createIdentityProperty(idmAdminToken, idmProperty)

        then:
        response.status == HttpStatus.SC_FORBIDDEN

        when: "call CREATE w/ prop manage role"
        def propManageRole = utils.getRoleByName(IdentityRole.IDENTITY_PROPERTY_ADMIN.roleName)
        utils.addRoleToUser(idmAdmin, propManageRole.id)
        idmAdminToken = utils.getToken(idmAdmin.username)
        response = devops.createIdentityProperty(idmAdminToken, idmProperty)

        then:
        response.status == HttpStatus.SC_CREATED

        when: "call UPDATE w/o prop manage role"
        idmProperty = response.getEntity(IdentityProperty)
        utils.deleteRoleOnUser(idmAdmin, propManageRole.id)
        response = devops.updateIdentityProperty(idmAdminToken, idmProperty.id, idmProperty)

        then:
        response.status == HttpStatus.SC_FORBIDDEN

        when: "call UPDATE w/ prop manage role"
        utils.addRoleToUser(idmAdmin, propManageRole.id)
        idmAdminToken = utils.getToken(idmAdmin.username)
        response = devops.updateIdentityProperty(idmAdminToken, idmProperty.id, idmProperty)

        then:
        response.status == HttpStatus.SC_OK

        when: "call DELETE w/o prop manage role"
        idmProperty = response.getEntity(IdentityProperty)
        utils.deleteRoleOnUser(idmAdmin, propManageRole.id)
        response = devops.deleteIdentityProperty(idmAdminToken, idmProperty.id)

        then:
        response.status == HttpStatus.SC_FORBIDDEN

        when: "call DELETE w/ prop manage role"
        utils.addRoleToUser(idmAdmin, propManageRole.id)
        idmAdminToken = utils.getToken(idmAdmin.username)
        response = devops.deleteIdentityProperty(idmAdminToken, idmProperty.id)

        then:
        response.status == HttpStatus.SC_NO_CONTENT

        cleanup:
        utils.deleteUserQuietly(idmAdmin)
    }

    @Unroll
    def "test CREATE identity property: accept = #accept, request = #request"() {
        given:
        def idmProperty = v2Factory.createIdentityProperty(testUtils.getRandomUUID("prop"), "foo", IdentityPropertyValueType.STRING.getTypeName())

        when: "create the property"
        def response = devops.createIdentityProperty(utils.getIdentityAdminToken(), idmProperty, request, accept)

        then: "success"
        response.status == HttpStatus.SC_CREATED

        and: "the properties match in the response"
        def responseEntity = response.getEntity(IdentityProperty)
        responseEntity.id != null
        idmProperty.name == responseEntity.name
        idmProperty.description == responseEntity.description
        idmProperty.idmVersion == responseEntity.idmVersion
        idmProperty.value == responseEntity.value
        idmProperty.valueType == responseEntity.valueType
        idmProperty.searchable == responseEntity.searchable
        idmProperty.reloadable == responseEntity.reloadable

        and: "the properties match in the GET props call"
        def getPropsResponse = devops.getIdmProps(utils.getIdentityAdminToken(), idmProperty.name)
        def data = new JsonSlurper().parseText(getPropsResponse.getEntity(String))
        def getProp = data[JsonWriterForIdmProperty.JSON_PROP_PROPERTIES].find{it.name == idmProperty.name}
        getProp[JsonWriterForIdmProperty.JSON_PROP_NAME] == idmProperty.name
        getProp[JsonWriterForIdmProperty.JSON_PROP_DESCRIPTION] == idmProperty.description
        getProp[JsonWriterForIdmProperty.JSON_PROP_VERSION_ADDED] == idmProperty.idmVersion
        getProp[JsonWriterForIdmProperty.JSON_PROP_VALUE] == idmProperty.value
        getProp[JsonWriterForIdmProperty.JSON_PROP_VALUE_TYPE] == idmProperty.valueType
        getProp[JsonWriterForIdmProperty.JSON_PROP_RELOADABLE] == idmProperty.reloadable

        cleanup:
        devops.deleteIdentityProperty(utils.getIdentityAdminToken(), responseEntity.id)

        where:
        accept | request
        MediaType.APPLICATION_XML | MediaType.APPLICATION_XML
        MediaType.APPLICATION_XML | MediaType.APPLICATION_JSON
        MediaType.APPLICATION_JSON | MediaType.APPLICATION_XML
        MediaType.APPLICATION_JSON | MediaType.APPLICATION_JSON
    }

    @Unroll
    def "test UPDATE identity property: accept = #accept, request = #request"() {
        given:
        def idmProperty = utils.createIdentityProperty()
        //change the values on all the properties
        idmProperty.description = testUtils.getRandomUUID("propDesc")
        idmProperty.value = testUtils.getRandomUUID("value")
        idmProperty.idmVersion = "3.1.0"
        idmProperty.searchable = !idmProperty.searchable

        when: "update the property"
        def response = devops.updateIdentityProperty(utils.getIdentityAdminToken(), idmProperty.id, idmProperty)

        then: "success"
        response.status == HttpStatus.SC_OK

        and: "the properties match in the response"
        def responseEntity = response.getEntity(IdentityProperty)
        responseEntity.id != null
        idmProperty.description == responseEntity.description
        idmProperty.idmVersion == responseEntity.idmVersion
        idmProperty.searchable == responseEntity.searchable

        and: "value is no returned b/c property is non-reloadable"
        responseEntity.value == null

        when: "make the property searchable again"
        def expectedValue = idmProperty.value
        idmProperty.value = null
        idmProperty.searchable = true
        response = devops.updateIdentityProperty(utils.getIdentityAdminToken(), idmProperty.id, idmProperty)
        responseEntity = response.getEntity(IdentityProperty)

        then: "property value is now shown to the updated value"
        responseEntity.value == expectedValue

        and: "the properties match in the GET props call"
        def getPropsResponse = devops.getIdmProps(utils.getIdentityAdminToken(), idmProperty.name)
        def data = new JsonSlurper().parseText(getPropsResponse.getEntity(String))
        def getProp = data[JsonWriterForIdmProperty.JSON_PROP_PROPERTIES].find{it.name == idmProperty.name}
        getProp[JsonWriterForIdmProperty.JSON_PROP_NAME] == idmProperty.name
        getProp[JsonWriterForIdmProperty.JSON_PROP_DESCRIPTION] == idmProperty.description
        getProp[JsonWriterForIdmProperty.JSON_PROP_VERSION_ADDED] == idmProperty.idmVersion
        getProp[JsonWriterForIdmProperty.JSON_PROP_VALUE] == expectedValue
        getProp[JsonWriterForIdmProperty.JSON_PROP_VALUE_TYPE] == idmProperty.valueType
        getProp[JsonWriterForIdmProperty.JSON_PROP_RELOADABLE] == idmProperty.reloadable

        cleanup:
        devops.deleteIdentityProperty(utils.getIdentityAdminToken(), responseEntity.id)

        where:
        accept | request
        MediaType.APPLICATION_XML | MediaType.APPLICATION_XML
        MediaType.APPLICATION_XML | MediaType.APPLICATION_JSON
        MediaType.APPLICATION_JSON | MediaType.APPLICATION_XML
        MediaType.APPLICATION_JSON | MediaType.APPLICATION_JSON
    }

    def "test CREATE identity property call returns 409 (conflict) when creating a property with a duplicate name"() {
        given:
        def property = utils.createIdentityProperty()
        def propertyForCreate = v2Factory.createIdentityProperty(property.name, testUtils.getRandomUUID("propValue"), IdentityPropertyValueType.STRING.getTypeName())

        when: "create the property"
        def response = devops.createIdentityProperty(utils.getIdentityAdminToken(), propertyForCreate)

        then: "409 - conflict"
        response.status == HttpStatus.SC_CONFLICT
        response.getEntity(IdentityFault).value.message == DefaultDevOpsService.IDENTITY_PROPERTY_NAME_CONFLICT_MSG

        cleanup:
        utils.deleteIdentityProperty(property.id)
    }

    def "test UPDATE & DELETE identity property returns 404 when trying to update an identity property that does not exist"() {
        given:
        def prop = v2Factory.createIdentityProperty(testUtils.getRandomUUID("propName"), testUtils.getRandomUUID("propValue"))

        when: "call update with invalid ID"
        def response = devops.updateIdentityProperty(utils.getIdentityAdminToken(), "invalid", prop)

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.getEntity(IdentityFault).value.message == DefaultDevOpsService.IDENTITY_PROPERTY_NOT_FOUND_MSG

        when: "call delete with invalid ID"
        response = devops.deleteIdentityProperty(utils.getIdentityAdminToken(), "invalid")

        then:
        response.status == HttpStatus.SC_NOT_FOUND
        response.getEntity(IdentityFault).value.message == DefaultDevOpsService.IDENTITY_PROPERTY_NOT_FOUND_MSG
    }

    def "test CREATE does not return the value for non-searchable properties"() {
        given:
        def idmProperty = v2Factory.createIdentityProperty(testUtils.getRandomUUID("propName"), testUtils.getRandomUUID("propValue"))
        idmProperty.searchable = false

        when: "create the property"
        def response = devops.createIdentityProperty(utils.getIdentityAdminToken(), idmProperty)

        then: "success"
        response.status == HttpStatus.SC_CREATED

        and: "the properties match in the response"
        def responseEntity = response.getEntity(IdentityProperty)
        responseEntity.value == null

        cleanup:
        utils.deleteIdentityProperty(responseEntity.id)
    }

    def "test UPDATE ignores name, reloadable, and value type attributes"() {
        given:
        def property = utils.createIdentityProperty()
        def originalName = property.name
        def originalReloadable = property.reloadable
        def originalValueType = property.valueType
        property.name = testUtils.getRandomUUID("newPropName")
        property.reloadable = !property.reloadable
        property.valueType = IdentityPropertyValueType.JSON.typeName

        when:
        def response = devops.updateIdentityProperty(utils.getIdentityAdminToken(), property.id, property)

        then:
        response.status == HttpStatus.SC_OK
        def responseProperty = response.getEntity(IdentityProperty)
        responseProperty.name == originalName
        responseProperty.reloadable == originalReloadable
        responseProperty.valueType == originalValueType

        cleanup:
        utils.deleteIdentityProperty(property.id)
    }

    def "test identity property STRING value type validation"() {
        given:
        def propertyData = v2Factory.createIdentityProperty(testUtils.getRandomUUID("propName"))
        propertyData.valueType = IdentityPropertyValueType.STRING.getTypeName()

        when: "try to create the property with string too long"
        propertyData.value = testUtils.randomAlphaStringWithLengthInBytes(IdentityPropertyStringValueTypeValidator.STRING_VALUE_MAX_LENGTH_KB * 1024 + 1)
        def response = devops.createIdentityProperty(utils.getIdentityAdminToken(), propertyData)

        then: "error"
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, IdentityPropertyStringValueTypeValidator.VALUE_LENGTH_EXCEEDED_MSG)

        when: "try to create the property with string equal to the max length"
        propertyData.value = testUtils.randomAlphaStringWithLengthInBytes(IdentityPropertyStringValueTypeValidator.STRING_VALUE_MAX_LENGTH_KB * 1024)
        response = devops.createIdentityProperty(utils.getIdentityAdminToken(), propertyData)

        then:
        response.status == HttpStatus.SC_CREATED
        def property = response.getEntity(IdentityProperty)

        cleanup:
        utils.deleteIdentityProperty(property.id)
    }

    def "test identity property JSON value type validation"() {
        given:
        def propertyData = v2Factory.createIdentityProperty(testUtils.getRandomUUID("propName"))
        propertyData.valueType = IdentityPropertyValueType.JSON.getTypeName()

        when: "try to create the property with malformed json"
        propertyData.value = "{{\"foo}"
        def response = devops.createIdentityProperty(utils.getIdentityAdminToken(), propertyData)

        then: "error"
        response.status == HttpStatus.SC_BAD_REQUEST
        def errorResponse = response.getEntity(IdentityFault).value
        errorResponse.message == IdentityPropertyJsonValueTypeValidator.VALUE_INVALID_JSON_MSG

        when: "create the property and then try to update with invalid json"
        propertyData.value = '{"fizz" : {"value": "buzz"} }'
        def createResponse = devops.createIdentityProperty(utils.getIdentityAdminToken(), propertyData)
        def property = createResponse.getEntity(IdentityProperty)
        propertyData.value = "{{\"foo}"
        response = devops.updateIdentityProperty(utils.getIdentityAdminToken(), property.id, propertyData)

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.getEntity(IdentityFault).value.message == IdentityPropertyJsonValueTypeValidator.VALUE_INVALID_JSON_MSG

        when: "json value equal to the max length"
        propertyData.value = "{\"fizz\" : {\"value\": \"\"} }"
        def numOfBytes = IdentityPropertyJsonValueTypeValidator.JSON_VALUE_MAX_LENGTH_KB * 1024 - testUtils.getStringLenghtInBytes(propertyData.value)
        def jsonValue = testUtils.randomAlphaStringWithLengthInBytes(numOfBytes)
        propertyData.value = "{\"fizz\" : {\"value\": \"$jsonValue\"} }"
        response = devops.updateIdentityProperty(utils.getIdentityAdminToken(), property.id, propertyData)

        then:
        response.status == HttpStatus.SC_OK

        when: "json value one byte over the max length"
        propertyData.value = "{\"fizz\" : {\"value\": \"\"} }"
        numOfBytes = IdentityPropertyJsonValueTypeValidator.JSON_VALUE_MAX_LENGTH_KB * 1024 - testUtils.getStringLenghtInBytes(propertyData.value)
        numOfBytes++
        jsonValue = testUtils.randomAlphaStringWithLengthInBytes(numOfBytes)
        propertyData.value = "{\"fizz\" : {\"value\": \"$jsonValue\"} }"
        response = devops.updateIdentityProperty(utils.getIdentityAdminToken(), property.id, propertyData)

        then:
        IdmAssert.assertOpenStackV2FaultResponse(response, BadRequestFault, HttpStatus.SC_BAD_REQUEST, IdentityPropertyJsonValueTypeValidator.VALUE_LENGTH_EXCEEDED_MSG)

        cleanup:
        utils.deleteIdentityProperty(property.id)
    }

    def "test identity property BOOLEAN value type validation"() {
        given:
        def propertyData = v2Factory.createIdentityProperty(testUtils.getRandomUUID("propName"))
        propertyData.valueType = IdentityPropertyValueType.BOOLEAN.getTypeName()

        when: "try to create the property with invalid boolean"
        propertyData.value = "nope"
        def response = devops.createIdentityProperty(utils.getIdentityAdminToken(), propertyData)

        then: "error"
        response.status == HttpStatus.SC_BAD_REQUEST
        def errorResponse = response.getEntity(IdentityFault).value
        errorResponse.message == IdentityPropertyBooleanValueTypeValidator.VALUE_INVALID_BOOLEAN_MSG

        when: "create the property and then try to update with invalid boolean"
        propertyData.value = 'true'
        def createResponse = devops.createIdentityProperty(utils.getIdentityAdminToken(), propertyData)
        def property = createResponse.getEntity(IdentityProperty)
        propertyData.value = "truez"
        response = devops.updateIdentityProperty(utils.getIdentityAdminToken(), property.id, propertyData)

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.getEntity(IdentityFault).value.message == IdentityPropertyBooleanValueTypeValidator.VALUE_INVALID_BOOLEAN_MSG

        cleanup:
        utils.deleteIdentityProperty(property.id)
    }

    def "test identity property BOOLEAN values are case insensitive"() {
        given:
        def propertyData = v2Factory.createIdentityProperty(testUtils.getRandomUUID("propName"))
        propertyData.valueType = IdentityPropertyValueType.BOOLEAN.getTypeName()
        propertyData.value = "false"
        def createResponse = devops.createIdentityProperty(utils.getIdentityAdminToken(), propertyData)
        def property = createResponse.getEntity(IdentityProperty)

        when: "create the value with all upper case"
        propertyData.value = "true".toUpperCase()
        def response = devops.updateIdentityProperty(utils.getIdentityAdminToken(), property.id, propertyData)

        then: "success"
        response.status == HttpStatus.SC_OK

        and: "value is set correctly"
        def getResponse = devops.getIdmProps(utils.getIdentityAdminToken())
        def props = new JsonSlurper().parseText(getResponse.getEntity(String))
        props[JsonWriterForIdmProperty.JSON_PROP_PROPERTIES].find{it.name == property.name}[JsonWriterForIdmProperty.JSON_PROP_VALUE] == true

        when: "create the value with all lower case"
        propertyData.value = "false".toUpperCase()
        response = devops.updateIdentityProperty(utils.getIdentityAdminToken(), property.id, propertyData)
        getResponse = devops.getIdmProps(utils.getIdentityAdminToken())
        props = new JsonSlurper().parseText(getResponse.getEntity(String))

        then: "success"
        response.status == HttpStatus.SC_OK

        and: "value is set correctly"
        props[JsonWriterForIdmProperty.JSON_PROP_PROPERTIES].find{it.name == property.name}[JsonWriterForIdmProperty.JSON_PROP_VALUE] == false

        cleanup:
        utils.deleteIdentityProperty(property.id)
    }

    def "test identity property INT value type validation"() {
        given:
        def propertyData = v2Factory.createIdentityProperty(testUtils.getRandomUUID("propName"))
        propertyData.valueType = IdentityPropertyValueType.INT.getTypeName()

        when: "try to create the property with invalid int"
        propertyData.value = "" + Integer.MAX_VALUE + "0"
        def response = devops.createIdentityProperty(utils.getIdentityAdminToken(), propertyData)

        then: "error"
        response.status == HttpStatus.SC_BAD_REQUEST
        def errorResponse = response.getEntity(IdentityFault).value
        errorResponse.message == IdentityPropertyIntValueTypeValidator.VALUE_INVALID_INT_MSG

        when: "create the property and then try to update with invalid int"
        propertyData.value = '123'
        def createResponse = devops.createIdentityProperty(utils.getIdentityAdminToken(), propertyData)
        def property = createResponse.getEntity(IdentityProperty)
        propertyData.value = "" + Integer.MIN_VALUE + "0"
        response = devops.updateIdentityProperty(utils.getIdentityAdminToken(), property.id, propertyData)

        then:
        response.status == HttpStatus.SC_BAD_REQUEST
        response.getEntity(IdentityFault).value.message == IdentityPropertyIntValueTypeValidator.VALUE_INVALID_INT_MSG

        cleanup:
        utils.deleteIdentityProperty(property.id)
    }

    def "test GET props call does not return non-searchable properties"() {
        given:
        def propertyData = v2Factory.createIdentityProperty(testUtils.getRandomUUID("propName"), testUtils.getRandomUUID("propValue"))
        propertyData.searchable = false
        def createResponse = devops.createIdentityProperty(utils.getIdentityAdminToken(), propertyData)
        def property = createResponse.getEntity(IdentityProperty)

        when:
        def response = devops.getIdmProps(utils.getIdentityAdminToken(), property.name)
        def responseData = new JsonSlurper().parseText(response.getEntity(String))

        then: "the property is not returned"
        responseData[JsonWriterForIdmProperty.JSON_PROP_PROPERTIES].find{it.name == property.name} == null

        cleanup:
        utils.deleteIdentityProperty(property.id)
    }

    def "test DELETE identity property"() {
        given:
        def property = utils.createIdentityProperty()

        when:
        def response = devops.deleteIdentityProperty(utils.getIdentityAdminToken(), property.id)

        then: "success"
        response.status == HttpStatus.SC_NO_CONTENT

        and: "property not returned in GET props call"
        def getPropsResponse = devops.getIdmProps(utils.getIdentityAdminToken())
        def data = new JsonSlurper().parseText(getPropsResponse.getEntity(String))
        data[JsonWriterForIdmProperty.JSON_PROP_PROPERTIES].find{it.name == property.name} == null
    }

}
