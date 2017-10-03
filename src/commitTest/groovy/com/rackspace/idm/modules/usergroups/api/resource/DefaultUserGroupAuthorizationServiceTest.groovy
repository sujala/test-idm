package com.rackspace.idm.modules.usergroups.api.resource

import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.entity.Domain
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.IdentityUserTypeEnum
import com.rackspace.idm.exception.ForbiddenException
import com.rackspace.idm.exception.NotFoundException
import spock.lang.Unroll
import testHelpers.RootServiceTest

/**
 * This verifies service authorization requires the user to have the identity:user-manage, identity:user-admin,
 * identity:admin, identity:service-admin, or rcn:admin role. The implementation combines custom logic and pre-existing
 * authorization services so this service only validates the custom group specific authorization.
 *
 * @return
 */
class DefaultUserGroupAuthorizationServiceTest extends RootServiceTest {

    DefaultUserGroupAuthorizationService defaultUserGroupCloudAuthorizationService

    def setup() {
        defaultUserGroupCloudAuthorizationService = new DefaultUserGroupAuthorizationService()

        mockDomainService(defaultUserGroupCloudAuthorizationService)
        mockAuthorizationService(defaultUserGroupCloudAuthorizationService)
        mockRequestContextHolder(defaultUserGroupCloudAuthorizationService)
        mockIdentityConfig(defaultUserGroupCloudAuthorizationService)
    }

    def "verify authorization pulls domain and checks for existence"() {
        setup:
        def domainId = "123"
        when:
        defaultUserGroupCloudAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId)

        then:
        1 * domainService.checkAndGetDomain(domainId) >> {throw new NotFoundException()} // Throw exception just to stop rest of tests
        thrown(NotFoundException)
    }

    def "verify service authorization requires user-manage+ or rcn:admin role"() {
        setup:
        def domainId = "123"
        domainService.checkAndGetDomain(domainId) >> new Domain().with {
            it.domainId = domainId
            it
        }
        requestContext.getEffectiveCaller() >> new User().with {
            it.domainId
            it
        }

        when:
        defaultUserGroupCloudAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId)

        then:
        // Must call this method to ensure user has acceptable overall role
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.USER_MANAGER, IdentityRole.RCN_ADMIN.getRoleName())
        1 * requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.IDENTITY_ADMIN
    }

    def "verify service authorization requires user to have a user type"() {
        setup:
        def domainId = "123"
        domainService.checkAndGetDomain(domainId) >> new Domain().with {
            it.domainId = domainId
            it
        }
        requestContext.getEffectiveCaller() >> new User().with {
            it.domainId
            it
        }

        when:
        defaultUserGroupCloudAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId)

        then:
        // Must call this method to ensure user has acceptable overall role
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.USER_MANAGER, IdentityRole.RCN_ADMIN.getRoleName())
        1 * requestContext.getEffectiveCallersUserType() >> null
        thrown(ForbiddenException)
    }

    @Unroll
    def "identity:admin allowed regardless of domains"() {
        setup:
        def domainId = "A"
        domainService.checkAndGetDomain(domainId) >> new Domain().with {
            it.domainId = domainId
            it
        }
       User user = new User().with {
            it.domainId = "B"
            it
        }
        requestContext.getEffectiveCaller() >> user

        when:
        defaultUserGroupCloudAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId)

        then:
        // Must call this method to ensure user has acceptable overall role
        1 * authorizationService.verifyEffectiveCallerHasIdentityTypeLevelAccessOrRole(IdentityUserTypeEnum.USER_MANAGER, IdentityRole.RCN_ADMIN.getRoleName())
        1 * requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.IDENTITY_ADMIN
        0 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName(_)
    }

    @Unroll
    def "user-admin/user-manager allowed when belong to same domain: userTypee: #userType"() {
        setup:
        def domainId = "A"
        domainService.checkAndGetDomain(domainId) >> new Domain().with {
            it.domainId = domainId
            it
        }
        User user = new User().with {
            it.domainId = domainId
            it
        }
        requestContext.getEffectiveCaller() >> user

        when:
        defaultUserGroupCloudAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(domainId)

        then:
        1 * requestContext.getEffectiveCallersUserType() >> userType

        notThrown(ForbiddenException)

        where:
        userType << [IdentityUserTypeEnum.USER_ADMIN, IdentityUserTypeEnum.USER_MANAGER]
    }

    @Unroll
    def "user-admin/user-manager fail when belong to different domains: userType: #userType"() {
        setup:
        def targetDomainId = "A"
        domainService.checkAndGetDomain(targetDomainId) >> new Domain().with {
            it.domainId = targetDomainId
            it
        }
        User user = new User().with {
            it.domainId = "B"
            it
        }
        requestContext.getEffectiveCaller() >> user

        when:
        defaultUserGroupCloudAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(targetDomainId)

        then:
        // Must call this method to ensure user has acceptable overall role
        1 * authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName("rcn:admin") >> false
        1 * requestContext.getEffectiveCallersUserType() >> userType
        thrown(ForbiddenException)

        where:
        userType | _
        IdentityUserTypeEnum.USER_ADMIN | _
        IdentityUserTypeEnum.USER_MANAGER | _
    }

    @Unroll
    def "rcn:admins allowed when domains or rcns are same: User Domain/Rcn: '#userDomainId'/'#userRcn'; target Domain/Rcn: '#targetDomainId'/'#targetRcn' "() {
        setup:
        Domain targetDomain = new Domain().with {
            it.domainId = targetDomainId
            it.rackspaceCustomerNumber = targetRcn
            it
        }
        Domain userDomain = new Domain().with {
            it.domainId = userDomainId
            it.rackspaceCustomerNumber = userRcn
            it
        }

        User user = new User().with {
            it.domainId = userDomainId
            it
        }
        requestContext.getEffectiveCaller() >> user

        requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.DEFAULT_USER
        authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName("rcn:admin") >> true

        when:
        defaultUserGroupCloudAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(targetDomainId)

        then:
        if (userDomainId.equalsIgnoreCase(targetDomainId)) {
            // Only need to retrieve the domain once
            1 * domainService.checkAndGetDomain({targetDomainId.equalsIgnoreCase(it)}) >> targetDomain
        } else {
            1 * domainService.checkAndGetDomain({targetDomainId.equalsIgnoreCase(it)}) >> targetDomain
            1 * domainService.getDomain({userDomainId.equalsIgnoreCase(it)}) >> userDomain
        }
        notThrown()

        where:
        userDomainId | userRcn | targetDomainId | targetRcn
        "D1" | "R1" | "D1" | "R1"
        "D1" | "R1" | "d1" | "R1"  //case insensitive
        "D1" | null | "D1" | null
        "D1" | "R1" | "D2" | "R1"
        "D1" | "R1" | "d2" | "r1" //case insensitive
    }

    @Unroll
    def "rcn:admins not allowed when domains different and rcns are null/different: User Domain/Rcn: '#userDomainId'/'#userRcn'; target Domain/Rcn: '#targetDomainId'/'#targetRcn' "() {
        setup:
        Domain targetDomain = new Domain().with {
            it.domainId = targetDomainId
            it.rackspaceCustomerNumber = targetRcn
            it
        }
        Domain userDomain = new Domain().with {
            it.domainId = userDomainId
            it.rackspaceCustomerNumber = userRcn
            it
        }
        User user = new User().with {
            it.domainId = userDomainId
            it
        }
        requestContext.getEffectiveCaller() >> user

        requestContext.getEffectiveCallersUserType() >> IdentityUserTypeEnum.DEFAULT_USER
        authorizationService.authorizeEffectiveCallerHasAtLeastOneOfIdentityRolesByName("rcn:admin") >> true
        domainService.checkAndGetDomain({targetDomainId.equalsIgnoreCase(it)}) >> targetDomain

        when:
        defaultUserGroupCloudAuthorizationService.verifyEffectiveCallerHasManagementAccessToDomain(targetDomainId)

        then:
        1 * domainService.getDomain({userDomainId.equalsIgnoreCase(it)}) >> userDomain
        Throwable t = thrown()
        t instanceof ForbiddenException

        where:
        userDomainId | userRcn | targetDomainId | targetRcn
        "D1" | "R1" | "D2" | "R2"
        "D1" | "R1" | "D2" | null
        "D1" | null | "D2" | null
        "d1" | null | "d2" | null
    }

    def "areUserGroupsEnabledForDomain: When 'enable.user.groups.globally' true, always returns true regardless of explicit domains"() {
        when:
        def result = defaultUserGroupCloudAuthorizationService.areUserGroupsEnabledForDomain("anyDomain")

        then:
        result
        1 * reloadableConfig.areUserGroupsGloballyEnabled() >> true
        0 * repositoryConfig.getExplicitUserGroupEnabledDomains() >> []
    }

    @Unroll
    def "areUserGroupsEnabledForDomain: When 'enable.user.groups.globally' false, enabled depends whether in explicit list. Test Domain: #domainId; list: #list"() {
        when:
        def result = defaultUserGroupCloudAuthorizationService.areUserGroupsEnabledForDomain(domainId)

        then:
        result == expectation
        1 * reloadableConfig.areUserGroupsGloballyEnabled() >> false
        1 * repositoryConfig.getExplicitUserGroupEnabledDomains() >> list

        where:
        domainId   | list                   | expectation
        "abc"      | []                     | false
        "abc"      | ["a"]                  | false
        "abc"      | ["123", "abc"]         | true
        "abc"      | ["123", "abc", "glke"] | true
        "abc"      | ["ABC"]                | true
    }
}
