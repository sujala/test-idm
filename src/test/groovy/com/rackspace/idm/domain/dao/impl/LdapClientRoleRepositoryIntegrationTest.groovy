package com.rackspace.idm.domain.dao.impl

import spock.lang.Specification
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import org.springframework.beans.factory.annotation.Autowired

import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.FilterParam
import com.rackspace.idm.domain.entity.Application
import com.rackspace.idm.domain.dao.ApplicationRoleDao

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
    @Shared def sharedName4 = "sharedName4"
    @Shared def applicationId = "bde1268ebabeeabb70a0e702a4626977c331d5c4"
    @Shared def sharedApplication
    @Shared def sharedRandom

    @Shared def sharedRole1

    @Autowired
    private ApplicationRoleDao clientRoleDao

    def setupSpec() {
        sharedRandom = ("$randomness").replace('-', "")
        sharedRole1 = clientRole(sharedName1, sharedRandom + "1")
        sharedApplication = application(applicationId)
    }

    def "adding and deleting client roles"() {
        when:
        ClientRole role = clientRole(sharedName4, sharedRandom + "4")
        List<ClientRole> beforeClientRoleList = clientRoleDao.getClientRolesForApplication(sharedApplication)
        clientRoleDao.addClientRole(sharedApplication, role)
        List<ClientRole> clientRoleList = clientRoleDao.getClientRolesForApplication(sharedApplication)
        clientRoleDao.deleteClientRole(role)
        List<ClientRole> afterClientRoleList = clientRoleDao.getClientRolesForApplication(sharedApplication)

        then:
        clientRoleList.size() == beforeClientRoleList.size() + 1
        beforeClientRoleList.size() == afterClientRoleList.size()
    }

    def "updating existing clientRoles"() {
        given:
        clientRoleDao.addClientRole(sharedApplication, sharedRole1)
        ClientRole clientRole = sharedRole1
        clientRole.name = "wahoo"

        when:
        clientRoleDao.updateClientRole(clientRole)
        ClientRole foundRole = clientRoleDao.getClientRole(clientRole)
        clientRoleDao.deleteClientRole(sharedRole1)

        then:
        foundRole.name.equals("wahoo")

    }

    def "getting role by clientId and role name"() {
        when:
        clientRoleDao.addClientRole(sharedApplication, sharedRole1)
        ClientRole role = clientRoleDao.getClientRoleByApplicationAndName(sharedApplication, sharedRole1)
        clientRoleDao.deleteClientRole(sharedRole1)

        then:
        role.name == sharedRole1.name
        role.getClientId() == sharedRole1.getClientId()
        role.id == sharedRole1.id
    }

    def clientRole(String name, String id) {
        new ClientRole().with {
            it.id = id
            it.clientId = applicationId
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

    def application(String id) {
        new Application().with {
            it.clientId = id
            it.uniqueId = "clientId=$id,ou=applications,o=rackspace,dc=rackspace,dc=com"
            return it
        }
    }
}
