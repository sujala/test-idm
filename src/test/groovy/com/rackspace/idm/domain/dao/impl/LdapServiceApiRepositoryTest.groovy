package com.rackspace.idm.domain.dao.impl;

import org.springframework.beans.factory.annotation.Autowired
import com.rackspace.idm.domain.entity.CloudBaseUrl
import com.unboundid.ldap.sdk.*
import com.unboundid.ldap.sdk.migrate.ldapjdk.LDAPSearchResults
import com.rackspace.idm.domain.dao.impl.LdapRepository.LdapSearchBuilder
import spock.lang.Specification
import org.springframework.test.context.ContextConfiguration
import com.rackspace.idm.domain.entity.ServiceApi

/**
 * Created by IntelliJ IDEA.
 * User: jorge
 * Date: 11/7/12
 * Time: 3:15 PM
 * To change this template use File | Settings | File Templates.
 */
@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapServiceApiRepositoryTest extends Specification{

    @Autowired
    private LdapServiceApiRepository serviceApiRepository

    def "list serviceApis"(){
        when:
        List<ServiceApi> baseUrlList = serviceApiRepository.getServiceApis().collect()

        then:
        baseUrlList != null

    }
}
