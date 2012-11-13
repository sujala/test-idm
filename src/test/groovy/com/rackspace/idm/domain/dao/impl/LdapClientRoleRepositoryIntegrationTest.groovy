package com.rackspace.idm.domain.dao.impl

import spock.lang.Specification
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import org.springframework.beans.factory.annotation.Autowired
import com.rackspace.idm.domain.dao.ClientRoleDao
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.FilterParam

/**
 * Created with IntelliJ IDEA.
 * User: jacob
 * Date: 11/12/12
 * Time: 5:22 PM
 * To change this template use File | Settings | File Templates.
 */

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapClientRoleRepositoryIntegrationTest extends Specification {

    @Shared def randomness = UUID.randomUUID()
    @Shared def sharedName1 = "sharedName1"
    @Shared def sharedName2 = "sharedName2"
    @Shared def sharedName3 = "sharedName3"
    @Shared def clientUniqueId = "clientId=bde1268ebabeeabb70a0e702a4626977c331d5c4,ou=applications,o=rackspace,dc=rackspace,dc=com"
    @Shared def sharedRandom

    @Shared def sharedRole1
    @Shared def sharedRole2
    @Shared def sharedRole3

    @Autowired
    private ClientRoleDao clientRoleDao

    def setupSpec() {
        sharedRandom = ("$randomness").replace('-', "")
        sharedRole1 = clientRole(sharedName1, sharedRandom + "1")
        sharedRole2 = clientRole(sharedName2, sharedRandom + "2")
        sharedRole3 = clientRole(sharedName3, sharedRandom + "3")
    }

    def "adding and deleting client roles"() {
        when:
        ClientRole role = clientRole(sharedName1, sharedRandom)
        List<ClientRole> beforeClientRoleList = clientRoleDao.getClientRoles()
        clientRoleDao.addClientRole(clientUniqueId, role)
        List<ClientRole> clientRoleList = clientRoleDao.getClientRoles()
        clientRoleDao.deleteClientRole(role)
        List<ClientRole> afterClientRoleList = clientRoleDao.getClientRoles()

        then:
        clientRoleList.size() == beforeClientRoleList.size() + 1
        beforeClientRoleList.size() == afterClientRoleList.size()
    }

    def "updating existing clientRoles"() {
        given:
        ClientRole clientRole = sharedRole1
        clientRole.name = "gibberish"
        List<FilterParam> filters = new ArrayList<FilterParam>();
        filters.add(new FilterParam(FilterParam.FilterParamName.ROLE_ID, sharedRole1.id))

        when:
        clientRoleDao.updateClientRole(clientRole)
        ClientRole foundRole = clientRoleDao.getClientRole(filters)

        then:
        foundRole.name.equalsIgnoreCase("gibberish")
    }

    def "getting clientRoles with filters specified"() {
        given:
        ClientRole clientRole1 = clientRole(sharedName1)
        ClientRole clientRole2 = clientRole(sharedName2)
        clientRoleDao.addClientRole(clientUniqueId, clientRole1)
        clientRoleDao.addClientRole(clientUniqueId, clientRole2)

        when:
        List<ClientRole> clientRoleList1 = clientRoleDao.getClientRoles(clientIdFilter("bde1268ebabeeabb70a0e702a4626977c331d5c4"))
        ClientRole foundClientRole = clientRoleDao.getClientRole(roleNameAndClientIdFilter(sharedName1, "bde1268ebabeeabb70a0e702a4626977c331d5c4"))

        then:
        clientRoleList1.size() == 2
        foundClientRole.id.equals(clientRole1.id)
        foundClientRole.name.equals(clientRole1.name)
    }

    def clientRole(String name, String id) {
        new ClientRole().with {
            it.id = id
            it.clientId = "bde1268ebabeeabb70a0e702a4626977c331d5c4"
            it.name = name
            return it
        }
    }

    def clientIdFilter(String clientId) {
        def List<FilterParam> filters = new ArrayList<FilterParam>()
        def FilterParam filterParam = new FilterParam(FilterParam.FilterParamName.APPLICATION_ID, clientId)
        filters.add(filterParam)
        return filters
    }

    def roleNameAndClientIdFilter(String name, String clientId) {
        def List<FilterParam> filters = new ArrayList<FilterParam>()
        def FilterParam filterParam1 = new FilterParam(FilterParam.FilterParamName.APPLICATION_ID, clientId)
        def FilterParam filterParam2 = new FilterParam(FilterParam.FilterParamName.ROLE_NAME, name)
        filters.add(filterParam1)
        filters.add(filterParam2)
        return filters
    }
}
