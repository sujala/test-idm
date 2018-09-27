package com.rackspace.idm.domain.dao.impl


import com.rackspace.idm.domain.entity.Racker
import org.junit.Rule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.web.WebAppConfiguration
import spock.lang.Shared
import spock.lang.Specification

@WebAppConfiguration
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
            it
        }

        when:
        ldapRackerRepository.addRacker(user)
        def retrivedRacker = ldapRackerRepository.getRackerByRackerId(random)
        ldapRackerRepository.deleteRacker(random);
        def deletedRacker = ldapRackerRepository.getRackerByRackerId(random)

        then:
        retrivedRacker != null
        retrivedRacker.rackerId == random
        deletedRacker == null
    }
}
