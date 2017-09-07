package com.rackspace.idm.modules.usergroups.dao

import com.rackspace.idm.domain.dao.impl.LdapConnectionPools
import com.rackspace.idm.domain.dao.impl.LdapRepository
import com.rackspace.idm.modules.usergroups.Constants
import com.rackspace.idm.modules.usergroups.dao.LdapUserGroupRepository
import com.rackspace.idm.modules.usergroups.entity.UserGroup
import com.unboundid.ldap.sdk.AddRequest
import com.unboundid.ldap.sdk.DN
import com.unboundid.ldap.sdk.Filter
import com.unboundid.ldap.sdk.LDAPInterface
import com.unboundid.ldap.sdk.SearchResultEntry
import com.unboundid.ldap.sdk.SearchScope
import spock.lang.Unroll
import testHelpers.RootServiceTest

class LdapUserGroupRepositoryTest extends RootServiceTest {
    LdapUserGroupRepository dao

    LdapConnectionPools ldapConnectionPools
    LDAPInterface ldapInterface

    def setup() {
        dao = new LdapUserGroupRepository()
        mockIdentityConfig(dao)

        ldapConnectionPools = Mock()
        dao.connPools = ldapConnectionPools

        ldapInterface = Mock()
        ldapConnectionPools.getAppConnPoolInterface() >> ldapInterface
    }

    @Unroll
    def "addGroup: Sets id on object regardless of any existing value. Existing Val: #curid"() {
        UserGroup group = new UserGroup().with {
            it.id = curId
            it.domainId = "domainId"
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        dao.addGroup(group)

        then:
        group.id != null
        group.id != curId

        where:
        curId << [null, "123"]
    }

    /**
     * Verifies the unbound library is called as expected w/ attributes set
     */
    def "addGroup: Call unboundId to persist"() {
        UserGroup group = new UserGroup().with {
            it.domainId = "domainId"
            it.name = "name"
            it.description = "description"
            it
        }

        when:
        dao.addGroup(group)

        then:
        1 * ldapInterface.add(_ as AddRequest) >> { args ->
            AddRequest request = (AddRequest) args[0]
            assert request.hasAttributeValue(LdapRepository.ATTR_ID, group.id)
            assert request.hasAttributeValue(LdapRepository.ATTR_NAME, group.name)
            assert request.hasAttributeValue(LdapRepository.ATTR_DESCRIPTION, group.description)
            assert request.hasAttributeValue(LdapRepository.ATTR_DOMAIN_ID, group.domainId)
            assert request.DN.contains (dao.getBaseDn())
            null
        }
    }

    def "addGroup: Get Group by Id and name creates appropriate filter"() {
        def domainId = "domainId"
        def groupName = "groupName"

        when:
        dao.getGroupByDomainIdAndName(domainId, groupName)

        then:
        1 * ldapInterface.searchForEntry(dao.getBaseDn(), SearchScope.ONE, _ as Filter, _) >> { args ->
            Filter filter = args[2]
            assert filter.components.size() == 3
            assert filter.components.find {it.attributeName == LdapRepository.ATTR_NAME && it.assertionValue == groupName}
            assert filter.components.find {it.attributeName == LdapRepository.ATTR_DOMAIN_ID && it.assertionValue == domainId}
            assert filter.components.find {it.attributeName == LdapRepository.ATTR_OBJECT_CLASS && it.assertionValue == Constants.OBJECTCLASS_USER_GROUP}

            null // Just return null as we're testing filter creation not actual searching
        }
    }
}
