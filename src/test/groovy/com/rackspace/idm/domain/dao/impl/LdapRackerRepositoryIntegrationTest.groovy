package com.rackspace.idm.domain.dao.impl

import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum
import com.rackspace.idm.domain.entity.Racker
import org.junit.Rule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.junit.ConditionalIgnoreRule
import testHelpers.junit.IgnoreByRepositoryProfile

@IgnoreByRepositoryProfile(profile = SpringRepositoryProfileEnum.SQL)
@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapRackerRepositoryIntegrationTest extends Specification{

    @Autowired
    LdapRackerRepository ldapRackerRepository

    @Shared def random
    @Shared def username

    @Rule
    public ConditionalIgnoreRule role = new ConditionalIgnoreRule()

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
