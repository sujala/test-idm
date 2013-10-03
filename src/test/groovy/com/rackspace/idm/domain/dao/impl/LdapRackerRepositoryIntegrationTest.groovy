package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.entity.Racker
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification

@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapRackerRepositoryIntegrationTest extends Specification{

    @Autowired
    LdapRackerRepository ldapRackerRepository

    @Shared def random
    @Shared def username

    def setup() {
        def randomness = UUID.randomUUID()
        random = ("$randomness").replace('-', "")
        username = "someName"+random
    }

    def "racker crud"() {
        given:
        Racker user = new Racker().with {
            it.rackerId = random
            it.rackerRoles = ["role1", "role2"].asList()
            it
        }

        when:
        ldapRackerRepository.addRacker(user)
        def retrivedRacker = ldapRackerRepository.getRackerByRackerId(random)
        ldapRackerRepository.deleteRacker(random);
        def deletedRacker = ldapRackerRepository.getRackerByRackerId(random)

        then:
        retrivedRacker != null
        deletedRacker == null
    }
}
