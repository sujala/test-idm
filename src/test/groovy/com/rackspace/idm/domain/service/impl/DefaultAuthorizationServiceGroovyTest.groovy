package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.dao.ApplicationDao
import com.rackspace.idm.domain.dao.ScopeAccessDao
import com.rackspace.idm.domain.dao.TenantDao
import com.rackspace.idm.domain.entity.ClientRole
import com.rackspace.idm.domain.entity.TenantRole
import com.rackspace.idm.domain.entity.User
import com.rackspace.idm.domain.service.ScopeAccessService
import com.rackspace.idm.domain.service.TenantService
import org.apache.commons.configuration.Configuration
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 1/22/13
 * Time: 12:39 PM
 * To change this template use File | Settings | File Templates.
 */
class DefaultAuthorizationServiceGroovyTest extends Specification{
    @Shared ApplicationDao clientDao;
    @Shared Configuration config;
    @Shared ScopeAccessDao scopeAccessDao;
    @Shared TenantDao tenantDao;
    @Shared ScopeAccessService scopeAccessService;
    @Shared TenantService tenantService;
    @Shared DefaultAuthorizationService defaultAuthorizationService

    def setupSpec(){
        defaultAuthorizationService = new DefaultAuthorizationService()
    }

    def setup(){
        createMocks()
    }

    def "hasUserAdminRole - returns true" (){
        given:
        User user = new User()
        clientDao.getClientRoleByClientIdAndRoleName(_,_) >> new ClientRole().with {
            it.name = "identity:user-admin"
            it.id = "3"
            return it
        }
        config.getString("cloudAuth.userAdminRole") >> "identity:user-admin"
        config.getString("cloudAuth.clientId") >>  "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        tenantDao.getTenantRolesForUser(_,_) >> [createTenantRole("3", "bde1268ebabeeabb70a0e702a4626977c331d5c4")].asList()

        when:
        Boolean hasRole = defaultAuthorizationService.hasUserAdminRole(user)

        then:
        hasRole
    }

    def "hasUserAdminRole - returns false" (){
        given:
        User user = new User()
        clientDao.getClientRoleByClientIdAndRoleName(_,_) >> new ClientRole().with {
            it.name = "identity:user-admin"
            it.id = "3"
            return it
        }
        config.getString("cloudAuth.userAdminRole") >> "identity:user-admin"
        config.getString("cloudAuth.clientId") >>  "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        tenantDao.getTenantRolesForUser(_,_) >> [createTenantRole("1", "bde1268ebabeeabb70a0e702a4626977c331d5c4")].asList()

        when:
        Boolean hasRole = defaultAuthorizationService.hasUserAdminRole(user)

        then:
        !hasRole
    }

    def "hasUserAdminRole - user equals null"(){
        when:
        Boolean hasRole = defaultAuthorizationService.hasUserAdminRole(null)
        then:
        !hasRole
    }

    def "hasUserDefaultRole - returns true" (){
        given:
        User user = new User()
        clientDao.getClientRoleByClientIdAndRoleName(_,_) >> new ClientRole().with {
            it.name = "identity:default"
            it.id = "2"
            return it
        }
        config.getString("cloudAuth.userRole") >> "identity:default"
        config.getString("cloudAuth.clientId") >>  "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        tenantDao.getTenantRolesForUser(_,_) >> [createTenantRole("2", "bde1268ebabeeabb70a0e702a4626977c331d5c4")].asList()

        when:
        Boolean hasRole = defaultAuthorizationService.hasDefaultUserRole(user)

        then:
        hasRole
    }

    def "hasUserDefaultRole - returns false" (){
        given:
        User user = new User()
        clientDao.getClientRoleByClientIdAndRoleName(_,_) >> new ClientRole().with {
            it.name = "identity:default"
            it.id = "2"
            return it
        }
        config.getString("cloudAuth.userRole") >> "identity:default"
        config.getString("cloudAuth.clientId") >>  "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        tenantDao.getTenantRolesForUser(_,_) >> [createTenantRole("3", "bde1268ebabeeabb70a0e702a4626977c331d5c4")].asList()

        when:
        Boolean hasRole = defaultAuthorizationService.hasDefaultUserRole(user)

        then:
        !hasRole
    }

    def "hasUserDefaultRole - user equals null"(){
        when:
        Boolean hasRole = defaultAuthorizationService.hasDefaultUserRole(null)
        then:
        !hasRole
    }

    def "hasIdentityAdminRole - returns true" (){
        given:
        User user = new User()
        clientDao.getClientRoleByClientIdAndRoleName(_,_) >> new ClientRole().with {
            it.name = "identity:admin"
            it.id = "1"
            return it
        }
        config.getString("cloudAuth.adminRole") >> "identity:admin"
        config.getString("cloudAuth.clientId") >>  "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        tenantDao.getTenantRolesForUser(_,_) >> [createTenantRole("1", "bde1268ebabeeabb70a0e702a4626977c331d5c4")].asList()

        when:
        Boolean hasRole = defaultAuthorizationService.hasIdentityAdminRole(user)

        then:
        hasRole
    }

    def "hasIdentityAdminRole - returns false" (){
        given:
        User user = new User()
        clientDao.getClientRoleByClientIdAndRoleName(_,_) >> new ClientRole().with {
            it.name = "identity:admin"
            it.id = "1"
            return it
        }
        config.getString("cloudAuth.adminRole") >> "identity:admin"
        config.getString("cloudAuth.clientId") >>  "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        tenantDao.getTenantRolesForUser(_,_) >> [createTenantRole("3", "bde1268ebabeeabb70a0e702a4626977c331d5c4")].asList()

        when:
        Boolean hasRole = defaultAuthorizationService.hasIdentityAdminRole(user)

        then:
        !hasRole
    }

    def "hasIdentityAdminRole - user equals null"(){
        when:
        Boolean hasRole = defaultAuthorizationService.hasIdentityAdminRole(null)
        then:
        !hasRole
    }

    def "hasServiceAdminRole - returns true" (){
        given:
        User user = new User()
        clientDao.getClientRoleByClientIdAndRoleName(_,_) >> new ClientRole().with {
            it.name = "identity:service-admin"
            it.id = "4"
            return it
        }
        config.getString("cloudAuth.serviceAdminRole") >> "identity:service-admin"
        config.getString("cloudAuth.clientId") >>  "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        tenantDao.getTenantRolesForUser(_,_) >> [createTenantRole("4", "bde1268ebabeeabb70a0e702a4626977c331d5c4")].asList()

        when:
        Boolean hasRole = defaultAuthorizationService.hasServiceAdminRole(user)

        then:
        hasRole
    }

    def "hasServiceAdminRole - returns false" (){
        given:
        User user = new User()
        clientDao.getClientRoleByClientIdAndRoleName(_,_) >> new ClientRole().with {
            it.name = "identity:service-admin"
            it.id = "4"
            return it
        }
        config.getString("cloudAuth.serviceAdminRole") >> "identity:service-admin"
        config.getString("cloudAuth.clientId") >>  "bde1268ebabeeabb70a0e702a4626977c331d5c4"
        tenantDao.getTenantRolesForUser(_,_) >> [createTenantRole("1", "bde1268ebabeeabb70a0e702a4626977c331d5c4")].asList()

        when:
        Boolean hasRole = defaultAuthorizationService.hasServiceAdminRole(user)

        then:
        !hasRole
    }

    def "hasServiceAdminRole - user equals null"(){
        when:
        Boolean hasRole = defaultAuthorizationService.hasServiceAdminRole(null)
        then:
        !hasRole
    }

    def createTenantRole(String roleRsId, String clientId) {
        new TenantRole().with {
            it.roleRsId = roleRsId
            it.clientId = clientId
            return it
        }
    }

    def createMocks() {
        clientDao = Mock()
        config = Mock()
        scopeAccessDao = Mock()
        tenantDao = Mock()
        scopeAccessService = Mock()
        tenantService = Mock()

        defaultAuthorizationService.applicationDao = clientDao
        defaultAuthorizationService.config = config
        defaultAuthorizationService.scopeAccessDao = scopeAccessDao
        defaultAuthorizationService.tenantDao = tenantDao
        defaultAuthorizationService.scopeAccessService = scopeAccessService
        defaultAuthorizationService.tenantService = tenantService
    }
}
