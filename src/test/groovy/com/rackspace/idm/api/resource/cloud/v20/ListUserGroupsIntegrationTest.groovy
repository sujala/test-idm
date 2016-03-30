package com.rackspace.idm.api.resource.cloud.v20
import com.rackspace.idm.JSONConstants
import com.rackspace.idm.api.resource.cloud.v20.json.readers.JSONReaderForArrayEntity
import com.rackspace.idm.api.security.IdentityRole
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AuthorizationService
import com.rackspace.idm.domain.service.ScopeAccessService
import org.openstack.docs.identity.api.v2.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
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

    def "test authorization for list user group"() {
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin()
        AuthenticateResponse auth = utils.authenticate(userAdmin)

        UserScopeAccess token = scopeAccessService.getScopeAccessByAccessToken(auth.token.id)
        String uaToken = token.accessTokenString
        String iaToken = utils.getToken(users[0].username)

        def userRole = authorizationService.getCachedIdentityRoleByName(IdentityRole.GET_USER_GROUPS_GLOBAL.getRoleName())

        when: "user admin tries to load identity admin groups"
        def uaResponse = cloud20.listGroupsForUser(uaToken, users[0].id)

        then: "forbidden"
        uaResponse.status == org.apache.http.HttpStatus.SC_FORBIDDEN

        when: "user admin tries to list groups for self (feature flag off)"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_LIST_GROUPS_FOR_SELF_PROP, false)
        uaResponse = cloud20.listGroupsForUser(uaToken, userAdmin.id)

        then: "forbidden"
        uaResponse.status == org.apache.http.HttpStatus.SC_FORBIDDEN

        when: "user admin tries to list groups for self (feature flag on)"
        reloadableConfiguration.setProperty(IdentityConfig.FEATURE_LIST_GROUPS_FOR_SELF_PROP, true)
        uaResponse = cloud20.listGroupsForUser(uaToken, userAdmin.id)

        then: "success"
        uaResponse != null
        uaResponse.status == org.apache.http.HttpStatus.SC_OK

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

        cleanup:
        reloadableConfiguration.reset()
        try {
            utils.deleteUsers(users)
        } catch (Exception ex) {/*ignore*/
        }
    }

    def "test list users in group with pagination"() {
        given:
        def group = utils.createGroup()
        def domain = utils.createDomain()
        def userAdmin, users
        (userAdmin, users) = utils.createUserAdmin(domain)
        utils.addUserToGroup(group, userAdmin)

        when: "index into the only user in the group"
        def response = cloud20.getUsersFromGroup(utils.getServiceAdminToken(), group.id, "10", "0")

        then:
        response.status == 200
        def userList = response.getEntity(UserList).value
        userList.user.size() == 1
        userList.user[0].id == userAdmin.id

        when: "index out of the list of users"
        response = cloud20.getUsersFromGroup(utils.getServiceAdminToken(), group.id, "10", "1")

        then:
        response.status == 200
        def userList2 = response.getEntity(UserList).value
        userList2.user.size() == 0

        cleanup:
        utils.deleteUsers(users)
        utils.deleteDomain(domain)
        utils.deleteGroup(group)
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
