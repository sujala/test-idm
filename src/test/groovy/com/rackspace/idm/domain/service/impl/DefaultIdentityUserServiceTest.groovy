package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperClient
import com.rackspace.idm.api.resource.cloud.atomHopper.AtomHopperConstants
import com.rackspace.idm.domain.dao.IdentityUserDao
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.EntityFactory

class DefaultIdentityUserServiceTest extends Specification {
    @Shared DefaultIdentityUserService service

    IdentityUserDao identityUserRepository;

    EntityFactory entityFactory = new EntityFactory()

    def setupSpec() {
        //service being tested
        service = new DefaultIdentityUserService()
    }

    def setup() {
        identityUserRepository = Mock(IdentityUserDao)
        service.identityUserRepository = identityUserRepository
    }

    def "Add Group to user includes feed event"() {
        given:
        def groupid = "1234509"
        def user = entityFactory.createUser()

        when:
        service.addGroupToEndUser(groupid, user.id)

        then:
        1 * identityUserRepository.getEndUserById(user.id) >> user
        1 * identityUserRepository.updateIdentityUser(user)
        user.getRsGroupId().contains(groupid)
    }

    def "Add an existing Group to user doesn't update user or send feed event"() {
        def groupid = "1234509"
        def user = entityFactory.createUser().with {it.getRsGroupId().add(groupid); return it;}

        when:
        service.addGroupToEndUser(groupid, user.id)

        then:
        1 * identityUserRepository.getEndUserById(user.id) >> user
        0 * identityUserRepository.updateIdentityUser(user)
        user.getRsGroupId().contains(groupid)
    }

    def "Remove Group from user removes from user and sends feed event"() {
        given:
        def groupid = "1234509"
        def user = entityFactory.createUser().with {it.getRsGroupId().add(groupid); return it;}

        when:
        service.removeGroupFromEndUser(groupid, user.id)

        then:
        1 * identityUserRepository.getEndUserById(user.id) >> user
        1 * identityUserRepository.updateIdentityUser(user)
        !user.getRsGroupId().contains(groupid)
    }

    def "Remove non-existing group from user doesn't update user or send feed event"(){
        given:
        def groupid = "1234509"
        def user = entityFactory.createUser()

        when:
        service.removeGroupFromEndUser(groupid, user.id)

        then:
        1 * identityUserRepository.getEndUserById(user.id) >> user
        0 * identityUserRepository.updateIdentityUser(user)
        !user.getRsGroupId().contains(groupid)
    }

}