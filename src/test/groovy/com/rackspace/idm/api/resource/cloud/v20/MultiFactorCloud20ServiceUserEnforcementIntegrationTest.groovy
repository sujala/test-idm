package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactor
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserMultiFactorEnforcementLevelEnum
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.api.resource.cloud.v20.json.writers.JSONWriterForRaxAuthMultiFactor
import com.rackspace.idm.domain.dao.impl.LdapMobilePhoneRepository
import com.rackspace.idm.domain.dao.impl.LdapUserRepository
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import org.apache.commons.configuration.Configuration
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Unroll

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.SC_BAD_REQUEST
import static org.apache.http.HttpStatus.SC_NOT_FOUND
import static org.apache.http.HttpStatus.SC_NO_CONTENT

/**
 * Tests the multifactor user enforcement level
 */
@ContextConfiguration(locations = ["classpath:app-config.xml"])
class MultiFactorCloud20ServiceUserEnforcementIntegrationTest extends RootConcurrentIntegrationTest {
    @Autowired
    private LdapMobilePhoneRepository mobilePhoneRepository;

    @Autowired
    private LdapUserRepository userRepository;

    @Autowired
    private Configuration globalConfig;

    /**
     * Tests that a user-admin can set the MFA user enforcement level on their own account and that when setting to "REQUIRED"
     * existing tokens are revoked. Run the tests each time for the various supported media types.
     *
     * @return
     */
    @Unroll()
    def "Set user enforcement level: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        when: "create a new user"
        org.openstack.docs.identity.api.v2.User userAdmin = createUserAdmin()
        String userAdminToken = authenticate(userAdmin.username)

        then: "enforcement is null"
        userAdmin.getUserMultiFactorEnforcementLevel() == null

        when: "set level to OPTIONAL"
        MultiFactor settings = v2Factory.createMultiFactorSettings(false)
        settings.setUserMultiFactorEnforcementLevel(UserMultiFactorEnforcementLevelEnum.OPTIONAL)
        def response = cloud20.updateMultiFactorSettings(specificationIdentityAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)
        userAdmin = utils.getUserByName(userAdmin.getUsername(), userAdminToken)

        then: "changed setting and existing user admin token still works"
        utils.getUserByName(userAdmin.getUsername(), userAdminToken).getUserMultiFactorEnforcementLevel() == UserMultiFactorEnforcementLevelEnum.OPTIONAL

        when: "set level to REQUIRED"
        settings.setUserMultiFactorEnforcementLevel(UserMultiFactorEnforcementLevelEnum.REQUIRED)
        response = cloud20.updateMultiFactorSettings(specificationIdentityAdminToken, userAdmin.id, settings, requestContentMediaType, acceptMediaType)

        then: "changed to required and user admin token was revoked"
        response.status == SC_NO_CONTENT
        cloud20.getUserByName(userAdminToken, userAdmin.id).status == SC_NOT_FOUND  //old token doesn't work anymore

        //getting user via identity admin token to check resultant enforcement level
        utils.getUserByName(userAdmin.getUsername(), specificationIdentityAdminToken).getUserMultiFactorEnforcementLevel() == UserMultiFactorEnforcementLevelEnum.REQUIRED

        cleanup:
        deleteUserQuietly(userAdmin)

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_XML_TYPE   |   MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE   |   MediaType.APPLICATION_XML_TYPE
    }

    def "Setting user enforcement level to invalid value returns 400"() {
        org.openstack.docs.identity.api.v2.User userAdmin = createUserAdmin()
        String userAdminToken = authenticate(userAdmin.username)

        MultiFactor settings = v2Factory.createMultiFactorSettings(false)
        settings.setUserMultiFactorEnforcementLevel(UserMultiFactorEnforcementLevelEnum.DEFAULT)
        JSONWriterForRaxAuthMultiFactor writer = new JSONWriterForRaxAuthMultiFactor()
        ByteArrayOutputStream out = new ByteArrayOutputStream()
        writer.writeTo(settings, null, null, null, null, null, out)

        JsonSlurper jsonParser = new JsonSlurper()
        def settingsAsJson = jsonParser.parseText(out.toString())
        settingsAsJson[JSONConstants.RAX_AUTH_MULTIFACTOR].userMultiFactorEnforcementLevel = "INVALID"

        JsonBuilder invalidSettings = new JsonBuilder(settingsAsJson)

        when: "set level to invalid value"

        def response = cloud20.updateMultiFactorSettings(specificationIdentityAdminToken, userAdmin.id, invalidSettings.toString())

        then: "get bad request"
        response.status == SC_BAD_REQUEST

        cleanup:
        deleteUserQuietly(userAdmin)
    }


}
