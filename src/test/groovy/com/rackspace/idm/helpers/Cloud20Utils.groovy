package com.rackspace.idm.helpers

import com.rackspace.docs.identity.api.ext.rax_auth.v1.ImpersonationResponse
import com.rackspace.idm.Constants
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import spock.lang.Shared
import testHelpers.Cloud20Methods
import testHelpers.V2Factory

import javax.annotation.PostConstruct

import static com.rackspace.idm.Constants.*

@Component
class Cloud20Utils {

    @Autowired
    Cloud20Methods methods

    @Autowired
    V2Factory factory

    @Shared
    def serviceAdminToken

    @PostConstruct
    def init() {
        methods.init()
    }

    def getToken(username, password) {
        def response = methods.authenticatePassword(username, password)
        assert (response.status == 200)
        def entity = response.getEntity(AuthenticateResponse).value
        assert (entity != null)
        entity.token.id
    }

    def createUser(token, username=getRandomUUID(), domainId=null) {
        def response = methods.createUser(token, factory.createUserForCreate(username, "display", "email@email.com", true, null, domainId, DEFAULT_PASSWORD))
        assert (response.status == 201)
        def entity = response.getEntity(User)
        assert (entity != null)
        return entity
    }

    def addRoleToUser(user, roleId, token=getServiceAdminToken()) {
        def response = methods.addApplicationRoleToUser(token, roleId, user.id)
        assert (response.status == 200)
    }

    def deleteUser(user) {
        def response = methods.deleteUser(getServiceAdminToken(), user.id)
        assert (response.status == 204)
    }

    def createDomain() {
        getRandomUUID("domain")
    }

    def deleteDomain(domainId) {
        if (domainId == null) {
            return
        }
        def response = methods.deleteDomain(getServiceAdminToken(), domainId)
        assert (response.status == 204)
    }

    def getServiceAdminToken() {
        if (serviceAdminToken == null) {
            serviceAdminToken = getToken(SERVICE_ADMIN_USERNAME, SERVICE_ADMIN_PASSWORD)
        }
        return serviceAdminToken
    }

    def getIdentityAdminToken() {
        getToken(IDENTITY_ADMIN_USERNAME, IDENTITY_ADMIN_PASSWORD)
    }

    def createUsers(domainId) {
        def serviceAdminToken = getServiceAdminToken()
        def identityAdmin = createUser(serviceAdminToken, getRandomUUID("identityAdmin"))

        def identityAdminToken = getToken(identityAdmin.username, DEFAULT_PASSWORD)

        def userAdmin = createUser(identityAdminToken, getRandomUUID("userAdmin"), domainId)
        def userAdminToken = getToken(userAdmin.username, DEFAULT_PASSWORD)

        def userManage = createUser(userAdminToken, getRandomUUID("userManage"), domainId)
        addRoleToUser(userManage, USER_MANAGE_ROLE_ID)

        def defaultUser = createUser(userAdminToken, getRandomUUID("defaultUser"), domainId)

        return [identityAdmin, userAdmin, userManage, defaultUser]
    }

    def getRandomUUID(prefix='') {
        String.format("%s%s", prefix, UUID.randomUUID().toString().replace('-', ''))
    }

    //delete users order matters.  pass default users first followed by user-managed, etc...
    def deleteUsers(... users) {
        for (User user : users) {
            if (user == null) {
                continue
            }
            deleteUser(user)
        }
    }

    def impersonate(impersonator, user) {
        methods.impersonate(getToken(impersonator.username, DEFAULT_PASSWORD), user)
    }

    def getImpersonatedToken(impersonator, user) {
        def response = impersonate(impersonator, user)
        assert (response.status == 200)
        def entity = response.getEntity(ImpersonationResponse)
        assert (entity != null)
        entity.token.id
    }

    def updateUser(user) {
        def response = methods.updateUser(getServiceAdminToken(), user.id, user)
        assert (response.status == 200)
    }

    def disableUser(user) {
        user.enabled = false
        updateUser(user)
    }

    def validateToken(token) {
        def response = methods.validateToken(getServiceAdminToken(), token)
        response.status
    }
}
