package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.entity.ProvisionedUserDelegate
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.security.TokenFormat
import com.rackspace.idm.domain.service.DomainSubUserDefaults
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import org.apache.commons.lang3.RandomStringUtils
import spock.lang.Shared
import spock.lang.Unroll
import testHelpers.RootServiceTest

class SimpleAETokenRevocationServiceTest extends RootServiceTest {

    @Shared
    SimpleAETokenRevocationService service

    def setup() {
        service = new SimpleAETokenRevocationService()

        mockTokenFormatSelector(service)
        tokenFormatSelector.formatForExistingToken(_) >> TokenFormat.AE
        mockUserService(service)
        mockDomainService(service)
        mockDelegationService(service)
        mockTokenRevocationRecordPersistenceStrategy(service)
    }

    @Unroll
    def "isTokenRevoked: revokes delegation tokens for disabled users - userEnabled = #userEnabled"() {
        given:
        def domain = entityFactory.createDomain()
        def user = entityFactory.createUser().with {
            it.enabled = userEnabled
            it
        }
        def da = entityFactory.createDelegationAgreement(domain.domainId).with {
            it.delegates = [user.dn] as Set
            it
        }
        def userDelegate = new ProvisionedUserDelegate(new DomainSubUserDefaults(domain, [] as Set, "ORD", []), da, user)
        def token = new UserScopeAccess().with {
            it.delegationAgreementId = da.id
            it
        }

        if (userEnabled) {
            1 * delegationService.getDelegationAgreementById(da.id) >> da
            1 * domainService.getDomain(domain.domainId) >> domain
        } else {
            0 * delegationService.getDelegationAgreementById(_)
            0 * domainService.getDomain(_)
        }

        when:
        def tokenIsRevoked = service.isTokenRevoked(token)

        then: "the token is revoked"
        tokenIsRevoked == !userEnabled

        and: "correct services are called"
        1 * userService.getUserByScopeAccess(token, false) >> userDelegate

        where:
        userEnabled << [true, false]
    }

    /**
     * Users can either be an explicit delegate for an implicit delegate through user group membership.
     * This test tests a user that has obtained a DA token through being an explicit delegate and then
     * checking to see if the token is revoked based on if they are still an effective delegate.
     */
    @Unroll
    def "isTokenRevoked: revokes delegation tokens for users that are not an explicit delegate, userIsExplicitDelegate = #userIsExplicitDelegate"() {
        given:
        def domain = entityFactory.createDomain().with {
            it.enabled = true
            it
        }
        def user = entityFactory.createUser().with {
            it.enabled = true
            it
        }
        def da = entityFactory.createDelegationAgreement(domain.domainId)
        def userDelegate = new ProvisionedUserDelegate(new DomainSubUserDefaults(domain, [] as Set, "ORD", []), da, user)
        def token = new UserScopeAccess().with {
            it.delegationAgreementId = da.id
            it
        }

        if (userIsExplicitDelegate) {
            da.delegates = [user.dn] as Set
            1 * domainService.getDomain(domain.domainId) >> domain
        } else {
            da.delegates = [] as Set
            0 * domainService.getDomain(_)
        }

        when:
        def tokenIsRevoked = service.isTokenRevoked(token)

        then: "the token is revoked"
        tokenIsRevoked == !userIsExplicitDelegate

        and: "correct services are called"
        1 * userService.getUserByScopeAccess(token, false) >> userDelegate
        1 * delegationService.getDelegationAgreementById(da.id) >> da

        where:
        userIsExplicitDelegate << [true, false]
    }

    /**
     * Users can either be an explicit delegate for an implicit delegate through user group membership.
     * This test tests a user that has obtained a DA token through being an implicit delegate through
     * user group membership and then checking to see if the token is revoked based on if they are
     * still an effective delegate.
     */
    @Unroll
    def "isTokenRevoked: revokes delegation tokens for users that are no longer part of a user group delegate, userMemberOfUserGroup = #userMemberOfUserGroup"() {
        given:
        def domain = entityFactory.createDomain().with {
            it.enabled = true
            it
        }
        def user = entityFactory.createUser().with {
            it.enabled = true
            it
        }
        def userGroup = new UserGroup().with {
            it.domainId = domain.domainId
            it.id = RandomStringUtils.randomAlphanumeric(8)
            it.uniqueId = "rsId=${it.id},ou=userGroups,ou=groups,ou=cloud,o=rackspace,dc=rackspace,dc=com"
            it
        }
        def da = entityFactory.createDelegationAgreement(domain.domainId).with {
            it.delegates = [userGroup.dn] as Set
            it
        }
        def userDelegate = new ProvisionedUserDelegate(new DomainSubUserDefaults(domain, [] as Set, "ORD", []), da, user)
        def token = new UserScopeAccess().with {
            it.delegationAgreementId = da.id
            it
        }

        if (userMemberOfUserGroup) {
            user.userGroupDNs = [userGroup.dn] as Set
            1 * domainService.getDomain(domain.domainId) >> domain
        } else {
            0 * domainService.getDomain(_)
        }

        when:
        def tokenIsRevoked = service.isTokenRevoked(token)

        then: "the token is revoked"
        tokenIsRevoked == !userMemberOfUserGroup

        and: "correct services are called"
        1 * userService.getUserByScopeAccess(token, false) >> userDelegate
        1 * delegationService.getDelegationAgreementById(da.id) >> da

        where:
        userMemberOfUserGroup << [true, false]
    }

    @Unroll
    def "isTokenRevoked: revokes delegation tokens for disabled domains - domainEnabled = #domainEnabled"() {
        given:
        def domain = entityFactory.createDomain().with {
            it.enabled = domainEnabled
            it
        }
        def user = entityFactory.createUser().with {
            it.enabled = true
            it
        }
        def da = entityFactory.createDelegationAgreement(domain.domainId).with {
            it.delegates = [user.dn] as Set
            it
        }
        def userDelegate = new ProvisionedUserDelegate(new DomainSubUserDefaults(domain, [] as Set, "ORD", []), da, user)
        def token = new UserScopeAccess().with {
            it.delegationAgreementId = da.id
            it
        }

        when:
        def tokenIsRevoked = service.isTokenRevoked(token)

        then: "the token is revoked"
        tokenIsRevoked == !domainEnabled

        and: "correct services are called"
        1 * userService.getUserByScopeAccess(token, false) >> userDelegate
        1 * delegationService.getDelegationAgreementById(da.id) >> da
        1 * domainService.getDomain(domain.domainId) >> domain

        where:
        domainEnabled << [true, false]
    }

}
