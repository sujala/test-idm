package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_ksgrp.v1.Groups
import com.rackspace.idm.Constants
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.domain.config.IdentityConfig
import groovy.json.JsonSlurper
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.RoleList
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory

import javax.ws.rs.core.MediaType


class UpgradeUserToCloudIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityConfig identityConfig

    @Unroll
    def "fully upgrade a user to cloud (using all optional attributes): accept = #accept, request = #request"() {
        given:
        def originalDomainId = testUtils.getRandomUUID("upgradeDomain")
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), originalDomainId, "ORD")
        def group = utils.createGroup()
        def role = utils.createRole()
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        userUpgradeData.groups = new Groups()
        userUpgradeData.groups.group.add(v1Factory.createGroup(group.name, null, null))
        userUpgradeData.roles = new RoleList()
        userUpgradeData.roles.role.add(v1Factory.createRole(role.name))
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)
        utils.addRoleToUser(upgradeUser, Constants.UPGRADE_USER_ELIGIBILITY_ROLE_ID)

        /*
         create endpoint templates to look for in the service catalog
         these will be used to verify that global and default endpoints are assigned to the user similar to the "OneUser" call
        */
        //default endpoint template
        def defaultEndpointTemplateId = testUtils.getRandomInteger().toString()
        def defaultEndpointTemplate = v1Factory.createEndpointTemplate(defaultEndpointTemplateId, "compute", testUtils.getRandomUUID("http://public/"), "cloudServers", true, "ORD").with {
            it.default = true
            it
        }
        defaultEndpointTemplate = utils.createAndUpdateEndpointTemplate(defaultEndpointTemplate, defaultEndpointTemplateId)
        def defaultEndpoint = defaultEndpointTemplate.publicURL + "/" + newDomainId
        //global endpoint template
        def globalEndpointTemplateId = testUtils.getRandomInteger().toString()
        def endpointTemplate = v1Factory.createEndpointTemplate(globalEndpointTemplateId, "compute", testUtils.getRandomUUID("http://public/"), "cloudServers", true, "ORD").with {
            it.global = true
            it
        }
        endpointTemplate = utils.createAndUpdateEndpointTemplate(endpointTemplate, globalEndpointTemplateId)
        def globalEndpoint = endpointTemplate.publicURL + "/" + newDomainId

        when: "upgrade the user"
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData, request, accept)

        then:
        response.status == 200
        def userResponse
        if (accept == MediaType.APPLICATION_XML_TYPE) {
            userResponse = response.getEntity(User).value
            assert userResponse.domainId == "" + newDomainId
            assert userResponse.roles.role.name.contains(role.name)
            assert userResponse.groups.group.id.contains(group.id)
            assert userResponse.defaultRegion == newRegion
            assert userResponse.secretQA.question == secretQA.question
            assert userResponse.secretQA.answer == secretQA.answer
            assert userResponse.multiFactorEnabled == false
        } else {
            userResponse = new JsonSlurper().parseText(response.getEntity(String))['user']
            assert userResponse[JSONConstants.RAX_AUTH_DOMAIN_ID] == "" + newDomainId
            assert userResponse[JSONConstants.ROLES].name.contains(role.name)
            assert userResponse[JSONConstants.RAX_KSGRP_GROUPS].id.contains(group.id)
            assert userResponse[JSONConstants.RAX_AUTH_DEFAULT_REGION] == newRegion
            assert userResponse[JSONConstants.RAX_KSQA_SECRET_QA][JSONConstants.QUESTION] == secretQA.question
            assert userResponse[JSONConstants.RAX_KSQA_SECRET_QA][JSONConstants.ANSWER] == secretQA.answer
            assert userResponse[JSONConstants.RAX_AUTH_MULTI_FACTOR_ENABLED] == false
        }

        and: "the domain was deleted"
        def getOriginalDomainResponse = cloud20.getDomain(utils.getServiceAdminToken(), originalDomainId)
        getOriginalDomainResponse.status == 404

        when:
        def tenants = utils.listTenantsForToken(utils.getToken(upgradeUser.username))

        then:
        tenants.tenant.id.contains(utils.getNastTenant(newDomainId))
        tenants.tenant.id.contains("" + newDomainId)

        when: "authenticate to see that the endpoints show up correctly for the upgraded user"
        def authResponse = utils.authenticateUser(upgradeUser.username)

        then:
        authResponse.serviceCatalog.service.endpoint.flatten().publicURL.count({t -> t == defaultEndpoint}) == 1
        authResponse.serviceCatalog.service.endpoint.flatten().publicURL.count({t -> t == globalEndpoint}) == 1

        when: "the user also has a generated API key"
        def apiKeyResponse = utils.getUserApiKey(upgradeUser)

        then:
        apiKeyResponse.username == upgradeUser.username
        apiKeyResponse.apiKey != null

        when: "list roles for user"
        def roles = utils.listUserGlobalRoles(utils.getServiceAdminToken(), upgradeUser.id)

        then: "the user no longer has the eligibility role"
        !roles.role.name.contains(identityConfig.getReloadableConfig().getUpgradeUserEligibleRole())

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)

        where:
        request | accept
        MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_XML_TYPE | MediaType.APPLICATION_JSON_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "test feature flag for include/exclude prefixes on json user attributes: accept = #accept, request = #request, includePrefixes = #includePrefixes"() {
        given:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_INCLUDE_USER_ATTR_PREFIXES_PROP, includePrefixes)
        def originalDomainId = testUtils.getRandomUUID("upgradeDomain")
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), originalDomainId, "ORD")
        def group = utils.createGroup()
        def role = utils.createRole()
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        userUpgradeData.groups = new Groups()
        userUpgradeData.groups.group.add(v1Factory.createGroup(group.name, null, null))
        userUpgradeData.roles = new RoleList()
        userUpgradeData.roles.role.add(v1Factory.createRole(role.name))
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)
        utils.addRoleToUser(upgradeUser, Constants.UPGRADE_USER_ELIGIBILITY_ROLE_ID)

        when: "upgrade the user"
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData, request, accept)

        then:
        response.status == 200
        def userResponse
        if (accept == MediaType.APPLICATION_XML_TYPE) {
            userResponse = response.getEntity(User).value
            assert userResponse.domainId == "" + newDomainId
            assert userResponse.roles.role.name.contains(role.name)
            assert userResponse.groups.group.id.contains(group.id)
            assert userResponse.defaultRegion == newRegion
            assert userResponse.secretQA.question == secretQA.question
            assert userResponse.secretQA.answer == secretQA.answer
        } else if (includePrefixes) {
            def stringResponse = response.getEntity(String)
            userResponse = new JsonSlurper().parseText(stringResponse)['user']
            assert userResponse[JSONConstants.RAX_AUTH_DOMAIN_ID] == "" + newDomainId
            assert userResponse[JSONConstants.ROLES].name.contains(role.name)
            assert userResponse[JSONConstants.RAX_KSGRP_GROUPS].id.contains(group.id)
            assert userResponse[JSONConstants.RAX_AUTH_DEFAULT_REGION] == newRegion
            assert userResponse[JSONConstants.RAX_KSQA_SECRET_QA][JSONConstants.QUESTION] == secretQA.question
            assert userResponse[JSONConstants.RAX_KSQA_SECRET_QA][JSONConstants.ANSWER] == secretQA.answer
        } else {
            def stringResponse = response.getEntity(String)
            userResponse = new JsonSlurper().parseText(stringResponse)['user']
            assert userResponse[JSONConstants.RAX_AUTH_DOMAIN_ID] == "" + newDomainId
            assert userResponse[JSONConstants.ROLES].name.contains(role.name)
            assert userResponse[JSONConstants.GROUPS].id.contains(group.id)
            assert userResponse[JSONConstants.RAX_AUTH_DEFAULT_REGION] == newRegion
            assert userResponse[JSONConstants.SECRET_QA][JSONConstants.QUESTION] == secretQA.question
            assert userResponse[JSONConstants.SECRET_QA][JSONConstants.ANSWER] == secretQA.answer
        }

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)

        where:
        request | accept | includePrefixes
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | false
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | false
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | false
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_JSON_TYPE | true
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_XML_TYPE  | true
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE | true
    }

    def "upgrade service returns 400 when invalid default region is provided"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def group = utils.createGroup()
        def role = utils.createRole()
        def newRegion = "mordor"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        userUpgradeData.groups = new Groups()
        userUpgradeData.groups.group.add(v1Factory.createGroup(group.name, null, null))
        userUpgradeData.roles = new RoleList()
        userUpgradeData.roles.role.add(v1Factory.createRole(role.name))
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)

        when:
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 400

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
    }

    def "upgrade service does not require default region to upgrade a user"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def group = utils.createGroup()
        def role = utils.createRole()
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, null, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        userUpgradeData.groups = new Groups()
        userUpgradeData.groups.group.add(v1Factory.createGroup(group.name, null, null))
        userUpgradeData.roles = new RoleList()
        userUpgradeData.roles.role.add(v1Factory.createRole(role.name))
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)
        utils.addRoleToUser(upgradeUser, Constants.UPGRADE_USER_ELIGIBILITY_ROLE_ID)

        when:
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 200
        def userResponse = response.getEntity(User).value
        userResponse.domainId == "" + newDomainId
        userResponse.roles.role.name.contains(role.name)
        userResponse.groups.group.id.contains(group.id)
        userResponse.defaultRegion == upgradeUser.defaultRegion
        userResponse.secretQA.question == secretQA.question
        userResponse.secretQA.answer == secretQA.answer

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
    }

    def "upgrade service does not require groups to upgrade a user"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def role = utils.createRole()
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, null, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        userUpgradeData.roles = new RoleList()
        userUpgradeData.roles.role.add(v1Factory.createRole(role.name))
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)
        utils.addRoleToUser(upgradeUser, Constants.UPGRADE_USER_ELIGIBILITY_ROLE_ID)

        when:
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 200
        def userResponse = response.getEntity(User).value
        userResponse.domainId == "" + newDomainId
        userResponse.roles.role.name.contains(role.name)
        userResponse.groups == null
        userResponse.defaultRegion == upgradeUser.defaultRegion
        userResponse.secretQA.question == secretQA.question
        userResponse.secretQA.answer == secretQA.answer

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
    }

    def "upgrade service returns 403 when user being upgraded already has a group assigned"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def group = utils.createGroup()
        def role = utils.createRole()
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        userUpgradeData.roles = new RoleList()
        userUpgradeData.roles.role.add(v1Factory.createRole(role.name))
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)
        utils.addRoleToUser(upgradeUser, Constants.UPGRADE_USER_ELIGIBILITY_ROLE_ID)

        when:
        utils.addUserToGroup(group, upgradeUser)
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 403

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
    }

    def "upgrade service returns 400 when request specifies group that does not exist"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def role = utils.createRole()
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        userUpgradeData.groups = new Groups()
        userUpgradeData.groups.group.add(v1Factory.createGroup(testUtils.getRandomUUID("invalidGroup"), null, null))
        userUpgradeData.roles = new RoleList()
        userUpgradeData.roles.role.add(v1Factory.createRole(role.name))
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)

        when:
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 400

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
    }

    def "upgrade service returns 400 when request does not specify user ID"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def group = utils.createGroup()
        def role = utils.createRole()
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        userUpgradeData.groups = new Groups()
        userUpgradeData.groups.group.add(v1Factory.createGroup(group.name, null, null))
        userUpgradeData.roles = new RoleList()
        userUpgradeData.roles.role.add(v1Factory.createRole(role.name))
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)

        when:
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 400

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
    }

    def "test that upgrade service returns 400 when request does not include secretQA"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def group = utils.createGroup()
        def role = utils.createRole()
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        userUpgradeData.groups = new Groups()
        userUpgradeData.groups.group.add(v1Factory.createGroup(group.name, null, null))
        userUpgradeData.roles = new RoleList()
        userUpgradeData.roles.role.add(v1Factory.createRole(role.name))
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)

        when: "no question in secretQA"
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        secretQA.question = null
        userUpgradeData.secretQA = secretQA
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 400

        when: "no answer in secretQA"
        secretQA = v1Factory.createRaxKsQaSecretQA()
        secretQA.answer = null
        userUpgradeData.secretQA = secretQA
        response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 400

        when: "no secretQA"
        userUpgradeData.secretQA = null
        response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 400

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
    }

    def "test that upgrade service returns 400 when request specifies non-numeric domain"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def group = utils.createGroup()
        def role = utils.createRole()
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        userUpgradeData.groups = new Groups()
        userUpgradeData.groups.group.add(v1Factory.createGroup(group.name, null, null))
        userUpgradeData.roles = new RoleList()
        userUpgradeData.roles.role.add(v1Factory.createRole(role.name))
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)

        when: "no domain ID"
        userUpgradeData.domainId = null
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 400

        when: "non-numeric domain ID"
        def newDomainId = testUtils.getRandomUUID("domain")
        userUpgradeData.domainId = newDomainId
        response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 400

        when: "domain ID not parsable to an int"
        newDomainId = "" + 1 + Integer.MAX_VALUE
        userUpgradeData.domainId = newDomainId
        response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 400

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
    }

    def "upgrade service returns 403 if user being upgraded has a numeric domain ID"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), "" + testUtils.getRandomInteger(), "ORD")
        def group = utils.createGroup()
        def role = utils.createRole()
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        userUpgradeData.groups = new Groups()
        userUpgradeData.groups.group.add(v1Factory.createGroup(group.name, null, null))
        userUpgradeData.roles = new RoleList()
        userUpgradeData.roles.role.add(v1Factory.createRole(role.name))
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)
        utils.addRoleToUser(upgradeUser, Constants.UPGRADE_USER_ELIGIBILITY_ROLE_ID)

        when:
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 403

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
    }

    def "upgrade service returns 409 if domain in upgrade request already exists"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def group = utils.createGroup()
        def role = utils.createRole()
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        userUpgradeData.groups = new Groups()
        userUpgradeData.groups.group.add(v1Factory.createGroup(group.name, null, null))
        userUpgradeData.roles = new RoleList()
        userUpgradeData.roles.role.add(v1Factory.createRole(role.name))
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)

        when:
        utils.createDomainEntity("" + newDomainId)
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 409

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
    }

    def "upgrade service returns 409 if MOSSO or NAST tenants are already created for given domain"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def group = utils.createGroup()
        def role = utils.createRole()
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        userUpgradeData.groups = new Groups()
        userUpgradeData.groups.group.add(v1Factory.createGroup(group.name, null, null))
        userUpgradeData.roles = new RoleList()
        userUpgradeData.roles.role.add(v1Factory.createRole(role.name))
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)

        when: "create NAST tenant before upgrade"
        def nastTenant = utils.createTenant(utils.getNastTenant("" + newDomainId))
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 409

        when: "create MOSSO tenant before upgrade"
        utils.deleteTenant(nastTenant)
        def mossoTenant = utils.createTenant("" + newDomainId)
        response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 409

        cleanup:
        utils.deleteTenant(mossoTenant)
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
    }

    def "upgrade service returns 403 when trying to upgrade a user when user already has a non-identity access role"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def group = utils.createGroup()
        def role = utils.createRole()
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        userUpgradeData.groups = new Groups()
        userUpgradeData.groups.group.add(v1Factory.createGroup(group.name, null, null))
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)
        utils.addRoleToUser(upgradeUser, Constants.UPGRADE_USER_ELIGIBILITY_ROLE_ID)

        when:
        utils.addRoleToUser(upgradeUser, role.id)
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 403

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
    }

    def "upgrade service returns 403 when trying to upgrade a user that does not have the upgrade eligibility role"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def group = utils.createGroup()
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        userUpgradeData.groups = new Groups()
        userUpgradeData.groups.group.add(v1Factory.createGroup(group.name, null, null))
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)

        when:
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 403

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
    }

    def "upgrade service returns 503 when trying to upgrade a user and the eligibility role config is not defined"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def group = utils.createGroup()
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        userUpgradeData.groups = new Groups()
        userUpgradeData.groups.group.add(v1Factory.createGroup(group.name, null, null))
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)
        reloadableConfiguration.setProperty(IdentityConfig.UPGRADE_USER_ELIGIBLE_ROLE_PROP, null)

        when: "the user DOES NOT have the role and the property is undefined"
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 503

        when: "the user DOES have the role and the property is undefined"
        utils.addRoleToUser(upgradeUser, Constants.UPGRADE_USER_ELIGIBILITY_ROLE_ID)
        response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 503

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
        reloadableConfiguration.reset()
    }

    def "upgrade service returns 400 when specifying invalid roles in request"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def group = utils.createGroup()
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        userUpgradeData.groups = new Groups()
        userUpgradeData.groups.group.add(v1Factory.createGroup(group.name, null, null))
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)

        when: "specify a role that does not exist"
        userUpgradeData.roles = new RoleList()
        userUpgradeData.roles.role.add(v1Factory.createRole(testUtils.getRandomUUID("invalidRole")))
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 400

        when: "specify an identity access role"
        userUpgradeData.roles = new RoleList()
        userUpgradeData.roles.role.add(v1Factory.createRole(Constants.USER_MANAGE_ROLE_NAME))
        response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 400

        when: "specify an role you do not have access to (based on weight)"
        userUpgradeData.roles = new RoleList()
        userUpgradeData.roles.role.add(v1Factory.createRole(Constants.UPGRADE_USER_TO_CLOUD_ROLE_NAME))
        response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 400

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
    }

    def "upgrade service returns 403 when trying to upgrade a sub-user"() {
        given:
        def userAdmin = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def upgradeUser = utils.createUser(utils.getToken(userAdmin.username), testUtils.getRandomUUID("subUser"), null, null)
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)
        utils.addRoleToUser(upgradeUser, Constants.UPGRADE_USER_ELIGIBILITY_ROLE_ID)

        when:
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 403

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(userAdmin)
        utils.deleteUser(identityAdmin)
    }

    def "upgrade service returns 403 when trying to upgrade a user admin with a sub-user in the same domain"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def subUser = utils.createUser(utils.getToken(upgradeUser.username), testUtils.getRandomUUID("subUser"), null, null)
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)
        utils.addRoleToUser(upgradeUser, Constants.UPGRADE_USER_ELIGIBILITY_ROLE_ID)

        when:
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 403

        cleanup:
        utils.deleteUser(subUser)
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
    }

    def "upgrade service returns 403 when trying to upgrade a user with a token from a user without the upgrade role"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(upgradeUser, Constants.UPGRADE_USER_ELIGIBILITY_ROLE_ID)

        when:
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 403

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
    }

    def "upgrade service returns 404 when feature flag is disabled"() {
        given:
        def upgradeUser = utils.createUser(utils.getIdentityAdminToken(), testUtils.getRandomUUID("userAdmin"), testUtils.getRandomUUID("upgradeDomain"), "ORD")
        def newRegion = "DFW"
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, newRegion, upgradeUser.domainId, null)
        userUpgradeData.id = upgradeUser.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId
        def identityAdmin = utils.createIdentityAdmin()

        when:
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_UPGRADE_USER_TO_CLOUD_PROP_NAME, false)
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 404

        cleanup:
        utils.deleteUser(upgradeUser)
        utils.deleteUser(identityAdmin)
        reloadableConfiguration.reset()
    }

    def "upgrade service returns 403 when trying to upgrade a federated user"() {
        given:
        def domainId = utils.createDomain()
        def username = testUtils.getRandomUUID("userAdminForSaml")
        def expSecs = Constants.DEFAULT_SAML_EXP_SECS
        def email = "fedIntTest@invalid.rackspace.com"
        def samlAssertion = new SamlFactory().generateSamlAssertionStringForFederatedUser(Constants.DEFAULT_IDP_URI, username, expSecs, domainId, null, email);
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdminWithTenants(domainId)
        def samlResponse = cloud20.samlAuthenticate(samlAssertion)
        AuthenticateResponse authResponse = samlResponse.getEntity(AuthenticateResponse).value
        def identityAdmin = utils.createIdentityAdmin()
        utils.addRoleToUser(identityAdmin, Constants.UPGRADE_USER_TO_CLOUD_ROLE_ID)
        def userUpgradeData = v2Factory.createUserForCreate(null, null, null, true, null, domainId, null)
        userUpgradeData.id = authResponse.user.id
        def secretQA = v1Factory.createRaxKsQaSecretQA()
        userUpgradeData.secretQA = secretQA
        def newDomainId = testUtils.getRandomInteger()
        userUpgradeData.domainId = newDomainId

        when:
        def response = cloud20.upgradeUserToCloud(utils.getToken(identityAdmin.username), userUpgradeData)

        then:
        response.status == 403

        cleanup:
        utils.deleteUsers(users)
    }

}
