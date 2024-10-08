package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Domain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProvider
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MobilePhone
import com.rackspace.docs.identity.api.ext.rax_auth.v1.OTPDevice
import com.rackspace.docs.identity.api.ext.rax_auth.v1.RoleAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignment
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TenantAssignments
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.docs.identity.api.ext.rax_auth.v1.VerificationCode
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.ApplicationRoleDao
import com.rackspace.idm.domain.dao.TenantRoleDao
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.modules.usergroups.dao.UserGroupDao
import com.rackspace.docs.identity.api.ext.rax_auth.v1.DomainMultiFactorEnforcementLevelEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.IdentityProviderFederationTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.MultiFactorDomain
import com.rackspace.docs.identity.api.ext.rax_auth.v1.UserGroup
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum
import com.rackspace.idm.util.OTPHelper
import com.unboundid.util.Base32
import org.apache.commons.lang.RandomStringUtils
import org.apache.http.client.utils.URLEncodedUtils
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory

import javax.xml.datatype.DatatypeFactory

import static org.apache.http.HttpStatus.*
import static com.rackspace.idm.Constants.*

@ContextConfiguration(locations = "classpath:app-config.xml")
class FederatedUserManageIntegrationTest extends RootIntegrationTest {

    @Shared
    def sharedServiceAdminToken

    @Shared
    def token

    @Shared
    def domainId

    @Shared
    def otherDomainId

    @Shared
    def sharedUserGroup

    @Shared
    def user

    @Shared
    def users

    @Shared
    def tenant

    @Shared
    def role

    @Autowired
    TenantRoleDao tenantRoleDao

    @Autowired
    UserGroupDao userGroupDao

    @Autowired
    ApplicationRoleDao roleDao

    @Autowired
    OTPHelper otpHelper

    def setup() {
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)
        reloadableConfiguration.setProperty(IdentityConfig.EMAIL_SEND_TO_ONLY_RACKSPACE_ADDRESSES, false)

        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_USER_GROUPS_GLOBALLY_PROP, true)
        sharedServiceAdminToken = cloud20.authenticateForToken(Constants.SERVICE_ADMIN_USERNAME, Constants.SERVICE_ADMIN_PASSWORD)
        domainId = utils.createDomain()
        (user, users) = utils.createUserAdmin(domainId)
        sharedUserGroup = utils.createUserGroup(domainId)
        otherDomainId = utils.createDomain()
        utils.createDomain(v2Factory.createDomain(otherDomainId, otherDomainId))

        utils.grantRoleAssignmentsOnUserGroup(sharedUserGroup, v2Factory.createSingleRoleAssignment(Constants.USER_MANAGE_ROLE_ID, ['*']))

        tenant = utils.createTenant()
        utils.addTenantToDomain(domainId, tenant.id)
        role = utils.createRole()

        AuthenticateResponse fedAuthResponse = utils.authenticateFederatedUser(domainId, [sharedUserGroup.name] as Set)
        token = fedAuthResponse.token.id
    }

    def cleanup() {
        utils.deleteUserQuietly(user)
        utils.deleteUsersQuietly(users)
        utils.deleteTestDomainQuietly(domainId)
    }

    def "federated user with user-manage - role" () {
        when: "lists roles"
        def response = cloud20.listRoles(token)

        then:
        response.status == SC_OK

        when: "gets role by id"
        response = cloud20.getRole(token, Constants.DEFAULT_USER_ROLE_ID)

        then:
        response.status == SC_OK
    }

    def "federated user with user-manage - domain" () {
        when: "get domain by id"
        def response = cloud20.getDomain(token, domainId)
        def domainEntity = response.getEntity(Domain)

        then:
        response.status == SC_OK

        when: "update domain"
        def newDuration = DatatypeFactory.newInstance().newDuration(300 * 1000)
        domainEntity.description = "updated"
        domainEntity.sessionInactivityTimeout = newDuration
        response = cloud20.updateDomain(token, domainId, domainEntity)
        def updatedDomainEntity = response.getEntity(Domain)

        then:
        response.status == SC_OK
        updatedDomainEntity.sessionInactivityTimeout == newDuration

        when: "update domain user does not belong access"
        def otherDomainEntity = utils.getDomain(otherDomainId)
        otherDomainEntity.description = "can not updated"
        response = cloud20.updateDomain(token, otherDomainId, otherDomainEntity)

        then:
        response.status == SC_FORBIDDEN
    }

    def "federated user with user-manage - user-group" () {
        when: "create user group"
        def response = cloud20.createUserGroup(token, v2Factory.createUserGroup(domainId))
        def userGroup = response.getEntity(UserGroup)

        then:
        response.status == SC_CREATED

        when: "list user groups"
        response = cloud20.listUserGroupsForDomain(token, domainId)

        then:
        response.status == SC_OK

        when: "get user-group by id"
        response = cloud20.getUserGroup(token, userGroup)

        then:
        response.status == SC_OK

        when: "update user-group"
        userGroup.description = "updated"
        response = cloud20.updateUserGroup(token, userGroup.id, domainId, userGroup)

        then:
        response.status == SC_OK

        when: "grant role to user-group"
        response = cloud20.grantRoleAssignmentsOnUserGroup(token, userGroup, v2Factory.createSingleRoleAssignment(Constants.ADMIN_ROLE_ID, ['*']))

        then:
        response.status == SC_OK

        when: "list roles on user-group"
        response = cloud20.listRoleAssignmentsOnUserGroup(token, userGroup)

        then:
        response.status == SC_OK

        when: "get roles on user-group"
        cloud20.getRoleAssignmentOnUserGroup(token, userGroup, Constants.ADMIN_ROLE_ID)

        then:
        response.status == SC_OK

        when: "revoke roles on user-group"
        cloud20.revokeRoleAssignmentFromUserGroup(token, userGroup, Constants.ADMIN_ROLE_ID)

        then:
        response.status == SC_OK

        when: "grant role on tenant to user-group"
        response = cloud20.grantRoleOnTenantToGroup(token, userGroup, Constants.ADMIN_ROLE_ID, tenant.id)

        then:
        response.status == SC_NO_CONTENT

        when: "revoke role on tenant from user-group"
        response = cloud20.revokeRoleOnTenantToGroup(token, userGroup, Constants.ADMIN_ROLE_ID, tenant.id)

        then:
        response.status == SC_NO_CONTENT

        when: "add user to user-group"
        response = cloud20.addUserToUserGroup(token, domainId, userGroup.id, user.id)

        then:
        response.status == SC_NO_CONTENT

        when: "get user from user-group"
        response = cloud20.getUsersInUserGroup(token, domainId, userGroup.id)

        then:
        response.status == SC_OK

        when: "remove user from user-group"
        response = cloud20.removeUserFromUserGroup(token, domainId, userGroup.id, user.id)

        then:
        response.status == SC_NO_CONTENT

        when: "delete user-group"
        response = cloud20.deleteUserGroup(token, userGroup)

        then:
        response.status == SC_NO_CONTENT
    }

    def "federated user with user-manage - multi-factor" () {
        when: "update multi-factor settings"
        MultiFactorDomain settings = v2Factory.createMultiFactorDomainSettings()
        settings.domainMultiFactorEnforcementLevel = DomainMultiFactorEnforcementLevelEnum.REQUIRED
        def response = cloud20.updateMultiFactorDomainSettings(token, domainId, settings)

        then:
        response.status == SC_NO_CONTENT
    }

    def "federated user with user-manage - identity-provider" () {
        given:
        def identityProvider = utils.createIdentityProvider(sharedServiceAdminToken, IdentityProviderFederationTypeEnum.DOMAIN, domainId)
        String metadata = new SamlFactory().generateMetadataXMLForIDP(testUtils.getRandomUUID("http://example.com/"), RandomStringUtils.randomAlphabetic(8))

        when: "create identity-provider"
        def identityProviderToCreate = v2Factory.createIdentityProvider(getRandomUUID(), "blah", getRandomUUID(), IdentityProviderFederationTypeEnum.DOMAIN, ApprovedDomainGroupEnum.GLOBAL.storedVal, [domainId] as List)
        def response = cloud20.createIdentityProvider(token, identityProviderToCreate)

        then:
        response.status == SC_FORBIDDEN

        when: "create identity-provider with metadata"
        response = cloud20.createIdentityProviderWithMetadata(token, metadata)
        def idp = response.getEntity(IdentityProvider)

        then:
        response.status == SC_CREATED

        when: "delete identity-provider created from metadata"
        response = cloud20.deleteIdentityProvider(token, idp.id)

        then:
        response.status == SC_NO_CONTENT

        when: "list identity-providers"
        response = cloud20.listIdentityProviders(token)

        then:
        response.status == SC_OK

        when: "get identity-provider"
        response = cloud20.getIdentityProvider(token, identityProvider.id)

        then:
        response.status == SC_OK

        when: "update identity-provider"
        identityProvider.description = "updated"
        response = cloud20.updateIdentityProvider(token, identityProvider.id, identityProvider)

        then:
        response.status == SC_OK

        when: "get identity-provider policy"
        response = cloud20.getIdentityProviderPolicy(token, identityProvider.id)

        then:
        response.status == SC_OK

        when: "update identity-provider policy"
        def policy = '{"policy": "updated"}'
        response = cloud20.updateIdentityProviderPolicy(token, identityProvider.id, policy)

        then:
        response.status == SC_NO_CONTENT

        when: "update identity-provider metadata"
        metadata = new SamlFactory().generateMetadataXMLForIDP(identityProvider.issuer, RandomStringUtils.randomAlphabetic(8))
        response = cloud20.updateIdentityProviderUsingMetadata(token, identityProvider.id, metadata)

        then:
        response.status == SC_OK

        when: "get identity-provider metadata"
        response = cloud20.getIdentityProviderMetadata(token, identityProvider.id)

        then:
        response.status == SC_OK

        when: "delete identity-provider"
        response = cloud20.deleteIdentityProvider(token, identityProvider.id)

        then:
        response.status == SC_NO_CONTENT
    }

    def "federated user with user-manage - user crud" () {
        given:
        utils.domainRcnSwitch(domainId, Constants.RCN_ALLOWED_FOR_INVITE_USERS)

        when: "add user invite"
        def response = cloud20.createUnverifiedUser(token, v2Factory.createUnverifiedUser(domainId))
        def unverifiedUser = response.getEntity(User).value

        then:
        response.status == SC_CREATED

        when: "add user invite"
        response = cloud20.createUnverifiedUser(token, v2Factory.createUnverifiedUser(otherDomainId))

        then:
        response.status == SC_FORBIDDEN

        when: "send unverified user invite"
        response = cloud20.sendUnverifiedUserInvite(token, unverifiedUser.id)

        then:
        response.status == SC_OK

        when: "create user"
        def userToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("user"), "display", "email@email.com", true, null, null, "Password1")
        response = cloud20.createUser(token, userToCreate)
        def user = response.getEntity(User).value

        then:
        response.status == SC_CREATED

        when: "add role to user on tenant"
        response = cloud20.addRoleToUserOnTenant(token, tenant.id, user.id, role.id)

        then:
        response.status == SC_OK

        when: "delete role from user on tenant"
        response = cloud20.deleteRoleFromUserOnTenant(token, tenant.id, user.id, role.id)

        then:
        response.status == SC_NO_CONTENT

        when: "get accesible domains endpoints for user"
        response = cloud20.getAccessibleDomainEndpointsForUser(token, user.id, domainId)

        then:
        response.status == SC_OK

        when: "grant roles to user"
        RoleAssignments assignments = new RoleAssignments().with {
            TenantAssignments ta = new TenantAssignments()
            ta.tenantAssignment.add(createTenantAssignment(Constants.ROLE_RBAC1_ID, [tenant.id]))
            it.tenantAssignments = ta
            it
        }

        response = cloud20.grantRoleAssignmentsOnUser(token, user, assignments)

        then:
        response.status == SC_OK

        when: "delete user product roles"
        response = cloud20.deleteUserProductRoles(token, user.id, 'rbac')

        then:
        response.status == SC_NO_CONTENT

        when: "add user to role"
        response = cloud20.addApplicationRoleToUser(token, role.id, user.id)

        then:
        response.status == SC_OK

        when: "remove user from role"
        response = cloud20.deleteApplicationRoleOnUser(token, role.id, user.id)

        then:
        response.status == SC_NO_CONTENT

        when: "delete user"
        response = cloud20.deleteUser(token, user.id)

        then:
        response.status == SC_NO_CONTENT
    }


    def "federated user with user-manage - multifactor" () {
        when: "create user"
        def userToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("user"), "display", "email@email.com", true, null, null, "Password1")
        def response = cloud20.createUser(token, userToCreate)
        def user = response.getEntity(User).value

        then:
        response.status == SC_CREATED


        when: "add otp device to user"
        def otpDeviceToCreate = new OTPDevice().with {
            it.name = "myOtp"
            it
        }
        response = cloud20.addOTPDeviceToUser(token, user.id, otpDeviceToCreate)
        def otpDevice = response.getEntity(OTPDevice)

        then:
        response.status == SC_CREATED

        when: "add otp device to user for delete"
        def otpDeviceToCreate2 = new OTPDevice().with {
            it.name = "myOtp2"
            it
        }
        response = cloud20.addOTPDeviceToUser(token, user.id, otpDeviceToCreate2)
        def otpDevice2 = response.getEntity(OTPDevice)

        then:
        response.status == SC_CREATED

        when: "delete otp device from user"
        response = cloud20.deleteOTPDeviceFromUser(token, user.id, otpDevice2.id)

        then:
        response.status == SC_NO_CONTENT

        when: "list otp devices for user"
        response = cloud20.getOTPDevicesFromUser(token, user.id)

        then:
        response.status == SC_OK

        when: "get otp device for user by id"
        response = cloud20.getOTPDeviceFromUser(token, user.id, otpDevice.id)

        then:
        response.status == SC_OK

        when: "verify otp device"
        def code = new VerificationCode()
        def secret = Base32.decode(URLEncodedUtils.parse(new URI(otpDevice.getKeyUri()), "UTF-8").find { it.name == 'secret' }.value)
        code.setCode(otpHelper.TOTP(secret))
        response = cloud20.verifyOTPDevice(token, user.id, otpDevice.id, code)

        then:
        response.status == SC_NO_CONTENT

        when: "update multifactor settings"
        def multiFactorSettings = v2Factory.createMultiFactorSettings(true)
        response = cloud20.updateMultiFactorSettings(token, user.id, multiFactorSettings)

        then:
        response.status == SC_NO_CONTENT

        when: "get multifactor devices from user"
        response = cloud20.getMultiFactorDevicesFromUser(token, user.id)

        then:
        response.status == SC_OK

        when: "add mobile phone to user"
        def requestMobilePhone = v2Factory.createMobilePhone();
        response = cloud20.addPhoneToUser(token, user.id, requestMobilePhone)
        def mobilePhone = response.getEntity(MobilePhone)

        then:
        response.status == SC_CREATED

        when: "send verification code"
        response = cloud20.sendVerificationCode(token, user.id, mobilePhone.id)

        then:
        response.status == SC_ACCEPTED

        when: "verify verification code"
        def constantVerificationCode = v2Factory.createVerificationCode(Constants.MFA_DEFAULT_PIN)
        response = cloud20.verifyVerificationCode(token, user.id, mobilePhone.id, constantVerificationCode)

        then:
        response.status == SC_NO_CONTENT

        when: "get phone from user"
        response = cloud20.getPhoneFromUser(token, user.id, mobilePhone.id)

        then:
        response.status == SC_OK

        when: "delete phone from user"
        response = cloud20.deletePhoneFromUser(token, user.id, mobilePhone.id)

        then:
        response.status == SC_NO_CONTENT

        when: "delete multifactor"
        response = cloud20.deleteMultiFactor(token, user.id)

        then:
        response.status == SC_NO_CONTENT

        when: "delete user"
        response = cloud20.deleteUser(token, user.id)

        then:
        response.status == SC_NO_CONTENT
    }

    def "grantRolesToUser: federated user-managers can assign/remove the user-manage role from users"() {
        given:
        RoleAssignments assignments = new RoleAssignments().with {
            it.tenantAssignments = new TenantAssignments().with {
                tas ->
                    tas.tenantAssignment.add(createTenantAssignment(USER_MANAGE_ROLE_ID, ['*']))
                    tas
            }
            it
        }

        when: "create user"
        def userToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("user"), "display", "email@email.com", true, null, null, "Password1")
        def response = cloud20.createUser(token, userToCreate)
        def defaultUser = response.getEntity(User).value

        then:
        response.status == SC_CREATED

        when: "grant domain role to user"
        response = cloud20.addUserRole(token, defaultUser.id, USER_MANAGE_ROLE_ID)

        then:
        response.status == SC_OK

        when: "revoke domain role from user"
        response = cloud20.deleteApplicationRoleOnUser(token, USER_MANAGE_ROLE_ID, defaultUser.id)

        then:
        response.status == SC_NO_CONTENT

        when: "grant role on tenant to user"
        response = cloud20.addRoleToUserOnTenant(token, tenant.id, defaultUser.id, role.id)

        then:
        response.status == SC_OK

        when: "revoke role on tenant from user"
        response = cloud20.deleteRoleFromUserOnTenant(token, tenant.id, defaultUser.id, role.id)

        then:
        response.status == SC_NO_CONTENT

        when: "grant role to user"
        response = cloud20.grantRoleAssignmentsOnUser(token, defaultUser, assignments)

        then:
        response.status == SC_OK

        when: "delete user"
        response = cloud20.deleteUser(utils.getServiceAdminToken(), defaultUser.id)

        then:
        response.status == SC_NO_CONTENT
    }


    TenantAssignment createTenantAssignment(String roleId, List<String> tenants) {
        def assignment = new TenantAssignment().with {
            ta ->
                ta.onRole = roleId
                ta.forTenants.addAll(tenants)
                ta
        }
        return assignment
    }

}
