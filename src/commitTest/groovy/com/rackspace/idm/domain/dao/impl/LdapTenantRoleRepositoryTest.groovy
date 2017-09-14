package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.modules.usergroups.Constants
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import com.unboundid.ldap.sdk.*
import testHelpers.RootServiceTest

class LdapTenantRoleRepositoryTest extends RootServiceTest {
    LdapTenantRoleRepository dao

    LdapConnectionPools ldapConnectionPools
    LDAPInterface ldapInterface

    def setup() {
        dao = new LdapTenantRoleRepository()

        ldapConnectionPools = Mock()
        dao.connPools = ldapConnectionPools

        ldapInterface = Mock()
        ldapConnectionPools.getAppConnPoolInterface() >> ldapInterface
    }

    def "addRoleAssignmentOnGroup: adding new assignment checks for container"() {
        UserGroup userGroup = new UserGroup().with {
            it.id = "groupId"
            it.uniqueId = "rsId=" + it.id + "," + Constants.USER_GROUP_BASE_DN
            it
        }

        def containerDN = "cn=ROLES," + userGroup.uniqueId

        TenantRole tenantRole = new TenantRole().with {
            it.roleRsId = "roleRsId"
            it.clientId = "clientId"
            it.tenantIds = ["tenant1"] as Set
            it
        }

        when:
        dao.addRoleAssignmentOnGroup(userGroup, tenantRole)

        then:
        // Will always first search for ROLES container
        1 * ldapInterface.searchForEntry(userGroup.uniqueId, SearchScope.ONE, _ as Filter, _) >> { args ->
            Filter filter = args[2]
            assert filter.components.size() == 2
            assert filter.components.find {it.attributeName == LdapRepository.ATTR_NAME && it.assertionValue == "ROLES"}
            assert filter.components.find {it.attributeName == LdapRepository.ATTR_OBJECT_CLASS && it.assertionValue == LdapRepository.OBJECTCLASS_RACKSPACE_CONTAINER}
            return new SearchResultEntry(containerDN, new Attribute[0], new Control[0])  // Just return mock result
        }

        // Sends in an add request for values filled out
        1 * ldapInterface.add(_ as AddRequest) >> { args ->
            AddRequest request = (AddRequest) args[0]
            assert request.hasAttributeValue(LdapRepository.ATTR_ROLE_RS_ID, tenantRole.roleRsId)
            assert request.hasAttributeValue(LdapRepository.ATTR_CLIENT_ID, tenantRole.clientId)
            for (String tenantId : tenantRole.tenantIds) {
                assert request.hasAttributeValue(LdapRepository.ATTR_TENANT_RS_ID, tenantId)
            }
            assert !request.hasAttributeValue(LdapRepository.ATTR_USER_RS_ID)
            assert request.DN.contains (containerDN)
            null
        }
    }

    def "updateRoleAssignmentOnGroup: adding new assignment checks for container"() {
        UserGroup userGroup = new UserGroup().with {
            it.id = "groupId"
            it.uniqueId = "rsId=" + it.id + "," + Constants.USER_GROUP_BASE_DN
            it
        }

        def containerDN = "cn=ROLES," + userGroup.uniqueId

        TenantRole tenantRole = new TenantRole().with {
            it.roleRsId = "roleRsId"
            it.clientId = "clientId"
            it.tenantIds = ["tenant1"] as Set
            it.uniqueId = "roleRsId=2134," + containerDN
            it
        }

        when:
        dao.updateRoleAssignmentOnGroup(userGroup, tenantRole)

        then:
        // Sends in a modify request for values filled out
        1 * ldapInterface.modify(_ as ModifyRequest) >> { args ->
            ModifyRequest request = (ModifyRequest) args[0]
            assert request.DN.contains (containerDN)
            null
        }
    }
}
