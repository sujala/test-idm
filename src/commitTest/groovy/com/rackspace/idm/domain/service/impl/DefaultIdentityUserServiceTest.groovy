package com.rackspace.idm.domain.service.impl

import com.rackspace.idm.domain.dao.IdentityUserDao
import org.slf4j.Logger
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.EntityFactory

import static com.rackspace.idm.domain.service.impl.DefaultIdentityUserService.DELETE_FEDERATED_USER_FORMAT

class DefaultIdentityUserServiceTest extends Specification {
    @Shared DefaultIdentityUserService service

    IdentityUserDao identityUserRepository;

    Logger deleteUserLogger = Mock(Logger)

    EntityFactory entityFactory = new EntityFactory()

    def setupSpec() {
        //service being tested
        service = new DefaultIdentityUserService()
    }

    def setup() {
        identityUserRepository = Mock(IdentityUserDao)
        service.identityUserRepository = identityUserRepository
        deleteUserLogger = Mock(Logger)
        service.deleteUserLogger = deleteUserLogger
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

    def "Delete user adds delete user log entry"() {
        given:
        def user = entityFactory.createFederatedUser()

        when:
        service.deleteUser(user)

        then:
        1 * deleteUserLogger.warn(DELETE_FEDERATED_USER_FORMAT, _)
    }
}