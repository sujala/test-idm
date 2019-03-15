package com.rackspace.idm.domain.entity

import com.rackspace.docs.identity.api.ext.rax_auth.v1.AssignmentTypeEnum
import com.rackspace.docs.identity.api.ext.rax_auth.v1.SourceTypeEnum
import com.rackspace.idm.api.security.ImmutableClientRole
import org.apache.commons.collections4.CollectionUtils
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll
import testHelpers.EntityFactory

class RackerSourcedRoleAssignmentsBuilderTest extends Specification {
    @Shared def entityFactory = new EntityFactory()

    def "Must specify a racker"() {
        when:
        RackerSourcedRoleAssignmentsBuilder.rackerBuilder(null)

        then:
        thrown(IllegalArgumentException)
    }

    @Unroll
    def "Supplied racker must be valid: Testing invalid case: #useCase"() {
        when:
        RackerSourcedRoleAssignmentsBuilder.rackerBuilder(racker)

        then:
        thrown(IllegalArgumentException)

        where:
        racker | useCase
        new Racker() | "no username or id"
        new Racker().with {it.id = "rackerId"; it} | "no username"
        new Racker().with {it.username = "rackerId"; it} | "no id"
    }

    def "hasBeenBuilt: Returns true after build method called"() {
        Racker racker = new Racker().with {
            it.id = "rackerId"
            it.username = "rackerUsername"
            it
        }
        RackerSourcedRoleAssignmentsBuilder builder = RackerSourcedRoleAssignmentsBuilder.rackerBuilder(racker)

        expect:
        !builder.hasBeenBuilt()

        when:
        builder.build()

        then:
        builder.hasBeenBuilt()
    }

    def "Can't add new sources after build called"() {
        Racker racker = new Racker().with {
            it.id = "rackerId"
            it.username = "rackerUsername"
            it
        }
        RackerSourcedRoleAssignmentsBuilder builder = RackerSourcedRoleAssignmentsBuilder.rackerBuilder(racker)
        builder.build()

        when:
        builder.addIdentitySystemSourcedAssignment(entityFactory.createImmutableClientRole())

        then:
        thrown(IllegalStateException)

        when:
        builder.addImplicitAssignment("name", entityFactory.createImmutableClientRole())

        then:
        thrown(IllegalStateException)

        when:
        builder.build()

        then:
        thrown(IllegalStateException)
    }

    def "Implicit assignments supported"() {
        Racker racker = new Racker().with {
            it.id = "rackerId"
            it.username = "rackerUsername"
            it
        }
        RackerSourcedRoleAssignmentsBuilder builder = RackerSourcedRoleAssignmentsBuilder.rackerBuilder(racker)

        ImmutableClientRole testRole = entityFactory.createImmutableClientRole()
        String sourceName = "sourceName"

        when:
        builder.addImplicitAssignment(sourceName, testRole)
        SourcedRoleAssignments assignments = builder.build()

        then:
        SourcedRoleAssignments.SourcedRoleAssignment assignment = assignments.getSourcedRoleAssignmentForRole(testRole.getId())
        assignment.role == testRole
        assignment.sources.size() == 1
        assignment.sources[0].sourceId == sourceName
        assignment.sources[0].assignmentType == RoleAssignmentType.DOMAIN
        assignment.sources[0].sourceType == RoleAssignmentSourceType.IMPLICIT
        CollectionUtils.isEmpty(assignment.sources[0].getTenantIds())
    }

    def "Identity system assignments supported"() {
        Racker racker = new Racker().with {
            it.id = "rackerId"
            it.username = "rackerUsername"
            it
        }
        RackerSourcedRoleAssignmentsBuilder builder = RackerSourcedRoleAssignmentsBuilder.rackerBuilder(racker)

        ImmutableClientRole testRole = entityFactory.createImmutableClientRole()
        String sourceName = "sourceName"

        when:
        builder.addIdentitySystemSourcedAssignment(testRole)
        SourcedRoleAssignments assignments = builder.build()

        then:
        SourcedRoleAssignments.SourcedRoleAssignment assignment = assignments.getSourcedRoleAssignmentForRole(testRole.getId())
        assignment.role == testRole
        assignment.sources.size() == 1
        assignment.sources[0].sourceId == "IDENTITY"
        assignment.sources[0].assignmentType == RoleAssignmentType.DOMAIN
        assignment.sources[0].sourceType == RoleAssignmentSourceType.SYSTEM
        CollectionUtils.isEmpty(assignment.sources[0].getTenantIds())
    }

}
