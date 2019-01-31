package com.rackspace.idm.api.resource.cloud.devops

import com.rackspace.idm.Constants
import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.api.security.ImmutableClientRole
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.NotFoundException
import com.unboundid.ldap.sdk.DN
import groovy.json.JsonSlurper
import spock.lang.Shared
import testHelpers.RootServiceTest

class DefaultDevOpsServiceTest extends RootServiceTest{
    @Shared DevOpsService service

    def setupSpec(){
        service = new DefaultDevOpsService()
    }

    def "Verify that encrypt users is only allowed for a service admin" () {
        given:
        setupMocks()
        allowUserAccess()

        when:
        service.encryptUsers("token")

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.SERVICE_ADMIN)
        1 * userService.reEncryptUsers()
    }

    def "Verify that reset key metadata is invoked"() {
        given:
        setupMocks()
        allowUserAccess()

        when:
        service.resetKeyMetadata("token")

        then:
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(authToken)
        1 * requestContext.getAndVerifyEffectiveCallerIsEnabled()
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccess(IdentityUserTypeEnum.SERVICE_ADMIN)
        1 * keyCzarCrypterLocator.resetCache()
    }

    def "verify analyze token"() {
        given:
        setupMocks()
        allowUserAccess()

        when:

        def scopeAccess = createScopeAccess().with {
            it.clientId = "clientId"
            return it
        }

        aeTokenService.unmarshallToken(_) >> scopeAccess
        service.analyzeToken("admintoken", "tokenUnderSubject")

        then:
        1 * authorizationService.verifyEffectiveCallerHasRoleByName(_)
        1 * securityContext.getAndVerifyEffectiveCallerTokenAsBaseToken(_)
        1 * aeTokenService.unmarshallToken(_)
    }

    def "verify analyze token with deleted user does not get domain"() {
        given:
        def user = new User().with {
            it.id = 'id'
            it
        }

        setupMocks()
        allowUserAccess()

        when:
        def scopeAccess = createUserScopeAccess().with {
            it.clientId = "clientId"
            return it
        }


        aeTokenService.unmarshallToken(_) >> scopeAccess
        userService.getUserByScopeAccess(scopeAccess, false) >> { throw new NotFoundException()}
        aeTokenRevocationService.findTokenRevocationRecordsMatchingToken(_) >> [].asList()

        service.analyzeToken("admintoken", "tokenUnderSubject")

        then:
        0 * domainService.getDomain(_) >> user
    }

    def "verify analyze impersonated token with deleted user does not get domain"() {
        given:
        setupMocks()
        allowUserAccess()

        when:
        def scopeAccess = createImpersonatedScopeAccess().with {
            it.clientId = "clientId"
            it.rsImpersonatingRsId = "id"
            return it
        }

        def user = new User().with {
            it.id = 'id'
            it
        }

        aeTokenService.unmarshallToken(_) >> scopeAccess
        userService.getUserByScopeAccess(scopeAccess, false) >> user
        identityUserService.getEndUserById("id") >> null
        aeTokenRevocationService.findTokenRevocationRecordsMatchingToken(_) >> [].asList()

        service.analyzeToken("admintoken", "tokenUnderSubject")

        then:
        0 * domainService.getDomain(_)
    }

    def "getDeletedUserByScopeAccess with impersonated scopeAccess and racker id returns racker"() {
        given:
        def rackerId = "id"
        ImpersonatedScopeAccess scopeAccess = new ImpersonatedScopeAccess().with {
            it.rackerId = rackerId
            it
        }

        when:
        def user = ((DefaultDevOpsService)service).getDeletedUserByScopeAccess(scopeAccess)

        then:
        user instanceof Racker
        user.id == rackerId
    }

    def "getDeletedUserByScopeAccess with impersonated scopeAccess and user id returns user"() {
        given:
        def userId = "id"
        ImpersonatedScopeAccess scopeAccess = new ImpersonatedScopeAccess().with {
            it.userRsId = userId
            it
        }

        when:
        def user = ((DefaultDevOpsService)service).getDeletedUserByScopeAccess(scopeAccess)

        then:
        user instanceof User
        user.id == userId
    }

    def "getDeletedUserByScopeAccess with user scopeAccess and federated auth by returns federated user"() {
        given:
        def userId = "id"
        UserScopeAccess scopeAccess = new UserScopeAccess().with {
            it.authenticatedBy =[GlobalConstants.AUTHENTICATED_BY_FEDERATION].asList()
            it.userRsId = userId
            it
        }

        when:
        def user = ((DefaultDevOpsService)service).getDeletedUserByScopeAccess(scopeAccess)

        then:
        user instanceof FederatedUser
        user.id == userId
    }

    def "getDeletedUserByScopeAccess with user scopeAccess and not federated auth by returns user"() {
        given:
        def userId = "id"
        UserScopeAccess scopeAccess = new UserScopeAccess().with {
            it.userRsId = userId
            it
        }

        when:
        def user = ((DefaultDevOpsService)service).getDeletedUserByScopeAccess(scopeAccess)

        then:
        user instanceof User
        user.id == userId
    }

    def "migrateDomainAdmin: calls correct services"() {
        given:
        setupMocks()
        allowUserAccess()
        def domain = entityFactory.createDomain()
        def domainWithAdminSet = entityFactory.createDomain().with {
            it.userAdminDN = new DN("rsId=1")
            it
        }
        def userAdminCr = entityFactory.createClientRole(IdentityUserTypeEnum.IDENTITY_ADMIN.roleName).with {
            it.id = Constants.USER_ADMIN_ROLE_ID
            it
        }
        def userAdminIcr = new ImmutableClientRole(userAdminCr)

        // Create test users
        def userAdmin = entityFactory.createUser().with {
            it.id = "1"
            it
        }
        def userAdmin2 = entityFactory.createUser().with {
            it.id = "2"
            it
        }
        def disabledUserAdmin = entityFactory.createUser().with {
            it.id = "3"
            it.enabled = false
            it
        }

        // Create userAdmin tenant role
        def userAdminTr = entityFactory.createTenantRole(IdentityUserTypeEnum.IDENTITY_ADMIN.roleName).with {
            it.roleRsId = Constants.USER_ADMIN_ROLE_ID
            it
        }

        when: "no previous user-admin set"
        def response = service.migrateDomainAdmin("token", domain.domainId).build()
        def domainAdmin = new JsonSlurper().parseText(response.entity)["domain"]

        then:
        domainAdmin.id == domain.domainId
        domainAdmin.userAdminDN == userAdmin.getDn().toString()
        domainAdmin.previousUserAdminDN == ""

        1 * domainService.checkAndGetDomain(domain.domainId) >> domain
        1 * domainService.getUsersByDomainId(domain.domainId) >> [userAdmin]
        1 * applicationService.getCachedClientRoleByName(_) >> userAdminIcr
        1 * tenantService.getTenantRoleForUserById(userAdmin, userAdminCr.id) >> userAdminTr
        1 * domainService.updateDomain(domain)

        when: "previous user-admin set"
        response = service.migrateDomainAdmin("token", domainWithAdminSet.domainId).build()
        domainAdmin = new JsonSlurper().parseText(response.entity)["domain"]

        then:
        domainAdmin.id == domain.domainId
        domainAdmin.userAdminDN == userAdmin.getDn().toString()
        domainAdmin.previousUserAdminDN == "rsId=1"

        1 * domainService.checkAndGetDomain(domainWithAdminSet.domainId) >> domainWithAdminSet
        1 * domainService.getUsersByDomainId(domainWithAdminSet.domainId) >> [userAdmin]
        1 * applicationService.getCachedClientRoleByName(_) >> userAdminIcr
        1 * tenantService.getTenantRoleForUserById(userAdmin, userAdminCr.id) >> userAdminTr
        1 * domainService.updateDomain(domain)

        when: "domain with no users"
        response = service.migrateDomainAdmin("token", domain.domainId).build()
        domainAdmin = new JsonSlurper().parseText(response.entity)["domain"]

        then:
        domainAdmin.id == domain.domainId
        domainAdmin.userAdminDN == ""
        domainAdmin.previousUserAdminDN == ""

        1 * domainService.checkAndGetDomain(domain.domainId) >> domain
        1 * domainService.getUsersByDomainId(domain.domainId) >> []
        1 * applicationService.getCachedClientRoleByName(_) >> userAdminIcr
        0 * tenantService.getTenantRoleForUserById(userAdmin, userAdminCr.id) >> userAdminTr
        0 * domainService.updateDomain(domain)

        when: "domain with multiple user-admins"
        domain.userAdminDN = null
        response = service.migrateDomainAdmin("token", domain.domainId).build()
        domainAdmin = new JsonSlurper().parseText(response.entity)["domain"]

        then: "Verify first user-admin found was set"
        domainAdmin.id == domain.domainId
        domainAdmin.userAdminDN == userAdmin.getDn().toString()
        domainAdmin.previousUserAdminDN == ""

        1 * domainService.checkAndGetDomain(domain.domainId) >> domain
        1 * domainService.getUsersByDomainId(domain.domainId) >> [userAdmin, userAdmin2]
        1 * applicationService.getCachedClientRoleByName(_) >> userAdminIcr
        1 * tenantService.getTenantRoleForUserById(userAdmin, userAdminCr.id) >> userAdminTr
        1 * tenantService.getTenantRoleForUserById(userAdmin2, userAdminCr.id) >> userAdminTr
        1 * domainService.updateDomain(domain)

        when: "domain with multiple user-admins, but one is disabled"
        domain.userAdminDN = null
        response = service.migrateDomainAdmin("token", domain.domainId).build()
        domainAdmin = new JsonSlurper().parseText(response.entity)["domain"]

        then: "Verify enabled user-admin was set"
        domainAdmin.id == domain.domainId
        domainAdmin.userAdminDN == userAdmin2.getDn().toString()
        domainAdmin.previousUserAdminDN == ""

        1 * domainService.checkAndGetDomain(domain.domainId) >> domain
        1 * domainService.getUsersByDomainId(domain.domainId) >> [disabledUserAdmin, userAdmin2]
        1 * applicationService.getCachedClientRoleByName(_) >> userAdminIcr
        1 * tenantService.getTenantRoleForUserById(disabledUserAdmin, userAdminCr.id) >> userAdminTr
        1 * tenantService.getTenantRoleForUserById(userAdmin2, userAdminCr.id) >> userAdminTr
        1 * domainService.updateDomain(domain)

        when: "domain with disabled user-admin"
        domain.userAdminDN = null
        response = service.migrateDomainAdmin("token", domain.domainId).build()
        domainAdmin = new JsonSlurper().parseText(response.entity)["domain"]

        then: "Verify disabled user-admin was set"
        domainAdmin.id == domain.domainId
        domainAdmin.userAdminDN == disabledUserAdmin.getDn().toString()
        domainAdmin.previousUserAdminDN == ""

        1 * domainService.checkAndGetDomain(domain.domainId) >> domain
        1 * domainService.getUsersByDomainId(domain.domainId) >> [disabledUserAdmin]
        1 * applicationService.getCachedClientRoleByName(_) >> userAdminIcr
        1 * tenantService.getTenantRoleForUserById(disabledUserAdmin, userAdminCr.id) >> userAdminTr
        1 * domainService.updateDomain(domain)
    }


    def setupMocks() {
        mockAuthorizationService(service)
        mockApplicationService(service)
        mockUserService(service)
        mockScopeAccessService(service)
        mockCacheableKeyCzarCrypterLocator(service)
        mockRequestContextHolder(service)
        mockAeTokenService(service)
        mockAeTokenRevocationService(service)
        mockIdentityUserService(service)
        mockDomainService(service)
        mockTenantService(service)
    }
}
