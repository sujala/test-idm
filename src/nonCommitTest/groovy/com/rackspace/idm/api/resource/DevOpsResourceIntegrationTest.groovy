package com.rackspace.idm.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.FactorTypeEnum
import com.rackspace.identity.multifactor.util.IdmPhoneNumberUtil
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.MobilePhoneDao
import com.rackspace.idm.domain.service.IdentityUserService
import org.apache.http.HttpStatus
import org.springframework.beans.factory.annotation.Autowired
import spock.lang.Unroll
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType

class DevOpsResourceIntegrationTest extends RootIntegrationTest {

    @Autowired
    IdentityUserService identityUserService

    @Autowired
    MobilePhoneDao mobilePhoneDao

    @Autowired
    IdentityConfig identityConfig

    def "test federation deletion call"() {
        given:
        def response = devops.getFederationDeletion(utils.getServiceAdminToken())

        when:
        def entity = new org.codehaus.jackson.map.ObjectMapper().readValue(response.getEntity(String), Map).federatedUsersDeletionResponse

        then:
        response.status == 200
        entity.id != null
    }

    def "test federation deletion call cannot be done by who doesnt have role"() {
        when:
        def response = devops.getFederationDeletion(utils.getIdentityAdminToken())

        then:
        response.status == 403
    }

    @Unroll
    def "test ability to upgrade MFA user under various scenarios: accept: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        def (user, users) = utils.createUserAdmin()
        def phone = v2Factory.createMobilePhone()

        def userToken = utils.getToken(user.username)

        when: "try to upgrade user w/ no phone"
        def missingNumber = v2Factory.createMobilePhone().with {
            it.number = null
            it
        }
        def missingPhoneResponse = devops.migrateSmsMfaOnUser(utils.serviceAdminToken, user.id, missingNumber, requestContentMediaType, acceptMediaType)

        then: "get 400"
        missingPhoneResponse.status == HttpStatus.SC_BAD_REQUEST

        when: "try to upgrade user w/ no phone"
        def invalidNumber = v2Factory.createMobilePhone().with {
            it.number = "-abcd"
            it
        }
        def invalidPhoneResponse = devops.migrateSmsMfaOnUser(utils.serviceAdminToken, user.id, invalidNumber, requestContentMediaType, acceptMediaType)

        then: "get 400"
        invalidPhoneResponse.status == HttpStatus.SC_BAD_REQUEST

        when: "try to migrate user using unauthorized caller"
        def forbiddenResponse = devops.migrateSmsMfaOnUser(userToken, user.id, phone, requestContentMediaType, acceptMediaType)

        then: "forbidden"
        forbiddenResponse.status == HttpStatus.SC_FORBIDDEN

        when: "try to migrate user using approved token"
        def response = devops.migrateSmsMfaOnUser(utils.serviceAdminToken, user.id, phone, requestContentMediaType, acceptMediaType)

        then: "can migrate"
        response.status == HttpStatus.SC_NO_CONTENT

        and: "user entity is setup"
        def userEntity = identityUserService.getProvisionedUserById(user.id)
        userEntity.isMultiFactorEnabled()
        userEntity.isMultiFactorDeviceVerified()
        userEntity.getMultiFactorTypeAsEnum() == FactorTypeEnum.SMS
        userEntity.externalMultiFactorUserId != null

        and: "phone is setup"
        def phoneEntity = mobilePhoneDao.getById(userEntity.multiFactorMobilePhoneRsId)
        phoneEntity.externalMultiFactorPhoneId != null
        phoneEntity.standardizedTelephoneNumber.equals(IdmPhoneNumberUtil.getInstance().parsePhoneNumber(phone.getNumber()))

        and: "user's previous password token was revoked"
        cloud20.validateToken(utils.serviceAdminToken, userToken).status == HttpStatus.SC_NOT_FOUND

        when: "try to upgrade user w/ MFA already on"
        def enabledResponse = devops.migrateSmsMfaOnUser(utils.serviceAdminToken, user.id, phone, requestContentMediaType, acceptMediaType)

        then: "get 400"
        enabledResponse.status == HttpStatus.SC_BAD_REQUEST

        when: "try to upgrade non-existant user"
        def noUserResponse = devops.migrateSmsMfaOnUser(utils.serviceAdminToken, UUID.randomUUID().toString(), phone, requestContentMediaType, acceptMediaType)

        then: "get 404"
        noUserResponse.status == HttpStatus.SC_NOT_FOUND

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

    @Unroll
    def "test ability to remove MFA from user under various scenarios: accept: requestContentType: #requestContentMediaType ; acceptMediaType=#acceptMediaType"() {
        def (user, users) = utils.createUserAdmin()
        def phone = v2Factory.createMobilePhone()

        def userToken = utils.getToken(user.username)

        when: "try to remove user w/o MFA"
        def resp1 = devops.removeSmsMfaFromUser(utils.serviceAdminToken, user.id, requestContentMediaType, acceptMediaType)

        then: "get 400"
        resp1.status == HttpStatus.SC_BAD_REQUEST

        when: "try to remove using invalid caller"
        def invalidCallerResponse = devops.removeSmsMfaFromUser(userToken, user.id, requestContentMediaType, acceptMediaType)

        then: "get 403"
        invalidCallerResponse.status == HttpStatus.SC_FORBIDDEN

        when: "try to remove from non-existant user"
        def resp2 = devops.removeSmsMfaFromUser(utils.serviceAdminToken, "abcd", requestContentMediaType, acceptMediaType)

        then: "get 404"
        resp2.status == HttpStatus.SC_NOT_FOUND

        when: "remove mfa"
        def setupResponse = devops.migrateSmsMfaOnUser(utils.serviceAdminToken, user.id, phone, requestContentMediaType, acceptMediaType)
        assert setupResponse.status == HttpStatus.SC_NO_CONTENT
        def resp3 = devops.removeSmsMfaFromUser(utils.serviceAdminToken, user.id, requestContentMediaType, acceptMediaType)

        then: "is success"
        resp3.status == HttpStatus.SC_NO_CONTENT

        and: "user entity is reset"
        def userEntity = identityUserService.getProvisionedUserById(user.id)
        userEntity.isMultiFactorEnabled() == false
        userEntity.isMultiFactorDeviceVerified() == false
        userEntity.getMultiFactorTypeAsEnum() == null
        userEntity.getExternalMultiFactorUserId() == null
        userEntity.getMultiFactorMobilePhoneRsId() == null

        and: "phone still exists"
        def phoneEntity = mobilePhoneDao.getByTelephoneNumber(IdmPhoneNumberUtil.getInstance().canonicalizePhoneNumberToString(IdmPhoneNumberUtil.getInstance().parsePhoneNumber(phone.getNumber())));
        phoneEntity.externalMultiFactorPhoneId != null

        where:
        requestContentMediaType | acceptMediaType
        MediaType.APPLICATION_XML_TYPE  | MediaType.APPLICATION_XML_TYPE
        MediaType.APPLICATION_JSON_TYPE | MediaType.APPLICATION_JSON_TYPE
    }

}
