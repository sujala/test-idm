package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.TenantRole
import spock.lang.Shared
import spock.lang.Specification

/**
 * Created with IntelliJ IDEA.
 * User: jorge
 * Date: 12/28/12
 * Time: 2:27 PM
 * To change this template use File | Settings | File Templates.
 */
class LdapTenantRepositoryGroovyTest extends Specification{
    @Shared LdapTenantRepository ldapTenantRepository

    def setupSpec(){
        ldapTenantRepository = new LdapTenantRepository()
    }

    def "Tenant Role For Parent By Id: null parentUniqueId returns null" () {
        when:
        TenantRole role = ldapTenantRepository.getTenantRoleForParentById(null,"1")

        then:
        role == null
    }

    def "Tenant Role For Parent By Id: empty parentUniqueId returns null" () {
        when:
        TenantRole role = ldapTenantRepository.getTenantRoleForParentById("","1")

        then:
        role == null
    }

    def "Tenant Role For Parent By Id: null Id returns null" () {
        when:
        TenantRole role = ldapTenantRepository.getTenantRoleForParentById("1",null)

        then:
        role == null
    }

    def "Tenant Role For Parent By Id: empty Id returns null" () {
        when:
        TenantRole role = ldapTenantRepository.getTenantRoleForParentById("1","")

        then:
        role == null
    }
}
