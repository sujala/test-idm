package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.idm.GlobalConstants
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForArrayEntity
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.ApplicationService
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.impl.DefaultAuthorizationService
import com.sun.jersey.api.client.ClientResponse
import org.apache.commons.configuration.Configuration
import org.openstack.docs.identity.api.v2.AuthenticateResponse
import org.openstack.docs.identity.api.v2.Role
import org.openstack.docs.identity.api.v2.RoleList
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import spock.lang.Shared
import testHelpers.RootIntegrationTest

import javax.ws.rs.core.MediaType
import javax.ws.rs.core.MultivaluedMap
import java.lang.annotation.Annotation
import java.lang.reflect.Type

class ListUserGroupsIntegrationTest extends RootIntegrationTest {
    @Autowired
    ScopeAccessService scopeAccessService

    @Autowired
    AuthorizationService authorizationService

    def "identity:get-user-groups-global can be used to retrieve groups for users"() {
        //start with role enabled
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_GET_USER_GROUPS_GLOBAL_ROLE_PROP, true)
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin()
        AuthenticateResponse auth = utils.authenticate(userAdmin)

        UserScopeAccess token = scopeAccessService.getScopeAccessByAccessToken(auth.token.id)
        String uaToken = token.accessTokenString
        String iaToken = utils.getToken(users[0].username)

        def userRole = authorizationService.getCachedIdentityRoleByName(GlobalConstants.ROLE_NAME_GET_USER_GROUPS_GLOBAL)

        when: "user admin tries to load identity admin groups"
        def uaResponse = cloud20.listGroupsForUser(uaToken, users[0].id)

        then:
        uaResponse.status == org.apache.http.HttpStatus.SC_FORBIDDEN

        when: "give user global groups role"
        utils.addRoleToUser(userAdmin, userRole.id)
        uaResponse = cloud20.listGroupsForUser(uaToken, users[0].id)

        then: "user admin can now access groups"
        uaResponse != null
        uaResponse.status == org.apache.http.HttpStatus.SC_OK

        when: "identity admin tries to load groups of user admin"
        def iaResponse = cloud20.listGroupsForUser(iaToken, userAdmin.id)

        then: "allowed"
        iaResponse.status == org.apache.http.HttpStatus.SC_OK

        when: "disable groups global role feature"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_ENABLE_GET_USER_GROUPS_GLOBAL_ROLE_PROP, false)
        uaResponse = cloud20.listGroupsForUser(uaToken, users[0].id)
        iaResponse = cloud20.listGroupsForUser(iaToken, userAdmin.id)

        then: "user admin can no longer access groups"
        uaResponse.status == org.apache.http.HttpStatus.SC_FORBIDDEN
        iaResponse.status == org.apache.http.HttpStatus.SC_OK

        cleanup:
        reloadableConfiguration.reset()
        try {
            utils.deleteUsers(users)
        } catch (Exception ex) {/*ignore*/
        }
    }

    private ClientRole getUserManageRole() {
/*
        get the identity admin role. Calling explicitly here rather than using DefaultAuthorizationService because in master branch the code was changed
        to no longer use static methods (required to avoid race conditions within tests). Rather than depend on something that will be changed once the code
        is merged from 2.0.0 branch to master, making the call directly. Note - once merged to master this method can safely use the DefaultAuthorizationService
        _instance_ method to retrieve the role.
         */
        ClientRole role = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthUserManageRole());
        role
    }

    private ClientRole getUserDefaultRole() {
/*
        get the identity admin role. Calling explicitly here rather than using DefaultAuthorizationService because in master branch the code was changed
        to no longer use static methods (required to avoid race conditions within tests). Rather than depend on something that will be changed once the code
        is merged from 2.0.0 branch to master, making the call directly. Note - once merged to master this method can safely use the DefaultAuthorizationService
        _instance_ method to retrieve the role.
         */
        ClientRole role = applicationService.getClientRoleByClientIdAndRoleName(getCloudAuthClientId(), getCloudAuthDefaultUserRole());
        role
    }

    //Helper Methods
    def deleteRoleQuietly(role) {
        if (role != null) {
            try {
                cloud20.deleteRole(specificationServiceAdminToken, role.getId())
            } catch (all) {
                //ignore
            }
        }
    }

    def deleteUserQuietly(user) {
        if (user != null) {
            try {
                cloud20.destroyUser(specificationServiceAdminToken, user.getId())
            } catch (all) {
                //ignore
            }
        }
    }

    def void assertUserHasRole(user, role) {
        assert cloud20.getUserApplicationRole(specificationServiceAdminToken, role.getId(), user.getId()).status == HttpStatus.OK.value()
    }

    def void assertUserDoesNotHaveRole(user, role) {
        assert cloud20.getUserApplicationRole(specificationServiceAdminToken, role.getId(), user.getId()).status == HttpStatus.NOT_FOUND.value()
    }

    def void addRoleToUser(callerToken, userToAddRoleTo, roleToAdd) {
        assert cloud20.addApplicationRoleToUser(callerToken, roleToAdd.getId(), userToAddRoleTo.getId()).status == HttpStatus.OK.value()
    }

    def void removeRoleFromUser(callerToken, userToAddRoleTo, roleToAdd) {
        assert cloud20.deleteApplicationRoleFromUser(callerToken, roleToAdd.getId(), userToAddRoleTo.getId()).status == HttpStatus.NO_CONTENT.value()
    }

    def createPropagateRole(boolean propagate = true, int weight = STANDARD_PROPAGATING_ROLE_WEIGHT, String roleName = ROLE_NAME_PREFIX + getNormalizedRandomString()) {
        def role = v2Factory.createRole(propagate).with {
            it.name = roleName
            it.propagate = propagate
            it.otherAttributes = null
            return it
        }
        def responsePropagateRole = cloud20.createRole(specificationServiceAdminToken, role)
        def propagatingRole = responsePropagateRole.getEntity(Role).value
        return propagatingRole
    }

    def createIdentityAdmin(String identityAdminUsername = IDENTITY_ADMIN_USERNAME_PREFIX + getNormalizedRandomString(), String domainId = getNormalizedRandomString()) {
        cloud20.createUser(specificationServiceAdminToken, v2Factory.createUserForCreate(identityAdminUsername, "display", "test@rackspace.com", true, null, null, DEFAULT_PASSWORD))
        def userAdmin = cloud20.getUserByName(specificationServiceAdminToken, identityAdminUsername).getEntity(User).value
        return userAdmin;
    }

    def createUserAdmin(String callerToken = specificationIdentityAdminToken, String adminUsername = USER_ADMIN_USERNAME_PREFIX + getNormalizedRandomString(), String domainId = getNormalizedRandomString()) {
        cloud20.createUser(callerToken, v2Factory.createUserForCreate(adminUsername, "display", "test@rackspace.com", true, null, domainId, DEFAULT_PASSWORD))
        def userAdmin = cloud20.getUserByName(callerToken, adminUsername).getEntity(User).value
        return userAdmin;
    }

    def createDefaultUser(String callerToken, String userName = DEFAULT_USER_USERNAME_PREFIX + getNormalizedRandomString()) {
        cloud20.createUser(callerToken, v2Factory.createUserForCreate(userName, "display", "test@rackspace.com", true, null, null, DEFAULT_PASSWORD))
        def user = cloud20.getUserByName(callerToken, userName).getEntity(User).value
        return user
    }

    def authenticate(String userName) {
        def token = cloud20.authenticatePassword(userName, DEFAULT_PASSWORD).getEntity(AuthenticateResponse).value.token.id
        return token;
    }

    def String getNormalizedRandomString() {
        return UUID.randomUUID().toString().replaceAll('-', "")
    }

    def String getCloudAuthUserManageRole() {
        return config.getString("cloudAuth.userManagedRole");
    }

    def String getCloudAuthDefaultUserRole() {
        return config.getString("cloudAuth.userRole");
    }

    private String getCloudAuthClientId() {
        return config.getString("cloudAuth.clientId");
    }

    private def listRolesForUserOnTenant(String token, String tenantId, String userId) {
        cloud20.resource.path(cloud20.path20).path("tenants").path(tenantId).path("users").path(userId).path("roles")
                .header(cloud20.X_AUTH_TOKEN, token).get(ClientResponse)
    }

    public class JSONReaderForRoles extends JSONReaderForArrayEntity<RoleList> {
        @Override
        public RoleList readFrom(Class<RoleList> type,
                                 Type genericType, Annotation[] annotations, MediaType mediaType,
                                 MultivaluedMap<String, String> httpHeaders, InputStream inputStream)
        throws IOException {

            return read(JSONConstants.ROLES, JSONConstants.ROLE, inputStream);
        }
    }
}
