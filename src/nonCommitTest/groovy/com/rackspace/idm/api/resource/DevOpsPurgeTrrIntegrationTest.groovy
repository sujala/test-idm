package com.rackspace.idm.api.resource

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenRevocationRecordDeletionRequest
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenRevocationRecordDeletionResponse
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.domain.dao.TokenRevocationRecordPersistenceStrategy
import com.rackspace.idm.domain.dao.impl.LdapRepository
import com.rackspace.idm.domain.dao.impl.LdapTokenRevocationRecordRepository
import com.rackspace.idm.domain.entity.AuthenticatedByMethodGroup
import com.rackspace.idm.domain.entity.LdapTokenRevocationRecord
import com.rackspace.idm.domain.entity.UserScopeAccess
import com.rackspace.idm.domain.service.AETokenRevocationService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import com.unboundid.ldap.sdk.Filter
import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.RandomStringUtils
import org.apache.http.HttpStatus
import org.joda.time.DateTime
import org.openstack.docs.identity.api.v2.ForbiddenFault
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.IdmAssert

/**
 * These tests depend upon creating obsolete TRRs for testing purposes. As such, it deletes all TRRs prior to every test.
 */
class DevOpsPurgeTrrIntegrationTest extends RootConcurrentIntegrationTest {

    @Autowired
    IdentityConfig identityConfig

    @Autowired
    TokenRevocationRecordPersistenceStrategy trrRepository

    @Autowired
    AETokenRevocationService aeTokenRevocationService

    def TRR_TOKEN_NAME = "devOpsPurgeTrr_"
    def TRR_USER_ID_PREFIX = "devOpsPurgeTrrUser_"

    def setupSpec() {
        //assign the role to the test generated identity admin for use across all tests requiring authorized user
        cloud20.addUserRole(specificationServiceAdminToken, specificationIdentityAdmin.id, Constants.PURGE_TRR_ROLE_ID)
    }

    /**
     * self clean TRRs the various tests create so there's no cross test pollution in the event of a test failure
     */
    def setup() {
        /*
          Ignore tests unless trrRepository is the LdapTokenRevocationRecordRepository. This class makes assumption that
          trrRepository is a proxy around the actual class due to being used as part of a pointcut
          */
        boolean ldapStrategy = trrRepository instanceof LdapTokenRevocationRecordRepository || (trrRepository.getTargetSource().getTarget() instanceof LdapTokenRevocationRecordRepository)
        org.junit.Assume.assumeTrue("DevOpsPurgeTrrIntegrationTest being skipped due to trrRepository not being of type LdapTokenRevocationRecordRepository.", ldapStrategy)

        removeExistingTrrs()
        reloadableConfiguration.reset()
        reloadableConfiguration.setProperty(IdentityConfig.PURGE_TRRS_OBSOLETE_AFTER_PROP, 25)
    }

    def removeExistingTrrs(){
        Filter filter = new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRepository.OBJECTCLASS_TOKEN_REVOCATION_RECORD).build();

        // Remove all trrs matching the trrs this test will create
        Iterable<LdapTokenRevocationRecord> trrs = trrRepository.getObjects(filter)
        for (LdapTokenRevocationRecord tokenRevocationRecord : trrs) {
            trrRepository.deleteTokenRevocationRecord(tokenRevocationRecord)
        }
    }

    def "must have authorized role to call"() {
        def userAdmin = createUserAdmin()
        def userAdminToken = authenticate(userAdmin.username)

        when: "user without role calls"
        def response = devops.purgeObsoleteTrrs(userAdminToken)

        then: "get 403"
        IdmAssert.assertOpenStackV2FaultResponseWithMessagePattern(response, ForbiddenFault, 403, IdmAssert.PATTERN_ALL)

        when: "assign role and retry"
        utils.addRoleToUser(userAdmin, Constants.PURGE_TRR_ROLE_ID, specificationServiceAdminToken)
        def authResponse = devops.purgeObsoleteTrrs(userAdminToken)

        then: "success"
        //just verify status. Other tests will verify response body
        authResponse.status == HttpStatus.SC_OK
    }

    def "When no expired trrs, still returns successful"() {
        when:
        def response = devops.purgeObsoleteTrrs(specificationIdentityAdminToken)

        then:
        response.status == HttpStatus.SC_OK
        TokenRevocationRecordDeletionResponse dr = response.getEntity(TokenRevocationRecordDeletionResponse)
        dr.deleted == 0
        dr.errors == 0
        dr.id != null
    }

    def "When there is an obsolete TRR, it is deleted, while others are not"() {
        def obsoleteTrr = createTokenTrrWithCreationDate(new DateTime().minusHours(26))
        def validTrr = createTokenTrrWithCreationDate(new DateTime().minusHours(24)) //not yet obsolete

        // Verify the 2 TRRs exist
        assert CollectionUtils.isNotEmpty(aeTokenRevocationService.findTokenRevocationRecordsMatchingToken(createTokenMatchingRecord(obsoleteTrr)))
        assert CollectionUtils.isNotEmpty(aeTokenRevocationService.findTokenRevocationRecordsMatchingToken(createTokenMatchingRecord(validTrr)))

        when:
        def response = devops.purgeObsoleteTrrs(specificationIdentityAdminToken)

        then:
        response.status == HttpStatus.SC_OK
        TokenRevocationRecordDeletionResponse dr = response.getEntity(TokenRevocationRecordDeletionResponse)
        dr.deleted == 1
        dr.errors == 0
        dr.id != null

        and: "The obsolete TRR is actually gone"
        CollectionUtils.isEmpty(aeTokenRevocationService.findTokenRevocationRecordsMatchingToken(createTokenMatchingRecord(obsoleteTrr)))

        and: "The valid TRR remains"
        CollectionUtils.isNotEmpty(aeTokenRevocationService.findTokenRevocationRecordsMatchingToken(createTokenMatchingRecord(validTrr)))
    }

    def "Can specify no delay when deleting TRRs"() {
        def o1 = createTokenTrrWithCreationDate(new DateTime().minusHours(26))
        def o2 = createTokenTrrWithCreationDate(new DateTime().minusHours(26))
        def o3 = createUserTrrWithCreationDate(new DateTime().minusHours(26))
        def v1 = createUserTrrWithCreationDate(new DateTime().minusHours(22))

        when: "delete multiple w/o delay"
        def response = devops.purgeObsoleteTrrs(specificationIdentityAdminToken, createTRRDeleteRequest(5, 0))
        TokenRevocationRecordDeletionResponse dr = response.getEntity(TokenRevocationRecordDeletionResponse)

        then:
        dr.deleted == 3
        dr.errors == 0
        dr.id != null

        and: "Obsolete TRRS appropriately deleted"
        CollectionUtils.isEmpty(aeTokenRevocationService.findTokenRevocationRecordsMatchingToken(createTokenMatchingRecord(o1)))
        CollectionUtils.isEmpty(aeTokenRevocationService.findTokenRevocationRecordsMatchingToken(createTokenMatchingRecord(o2)))
        CollectionUtils.isEmpty(aeTokenRevocationService.findTokenRevocationRecordsMatchingToken(createTokenMatchingRecord(o3)))

        and: "Valid TRRs are not deleted"
        CollectionUtils.isNotEmpty(aeTokenRevocationService.findTokenRevocationRecordsMatchingToken(createTokenMatchingRecord(v1)))
    }

    def "If specify a lower limit than the number of obsolete TRRs, service will only delete up to limit"() {
        createTokenTrrWithCreationDate(new DateTime().minusHours(26))
        createTokenTrrWithCreationDate(new DateTime().minusHours(26))
        createUserTrrWithCreationDate(new DateTime().minusHours(26))
        createUserTrrWithCreationDate(new DateTime().minusHours(22))

        when:
        def response = devops.purgeObsoleteTrrs(specificationIdentityAdminToken, createTRRDeleteRequest(2, 0))
        TokenRevocationRecordDeletionResponse dr = response.getEntity(TokenRevocationRecordDeletionResponse)

        then:
        dr.deleted == 2
        dr.errors == 0
        dr.id != null
    }

    /**
     * the delay adds 1 second to processing, but theoretically the second deletions could be faster than first, so
     * the actual difference could be less than 50ms (e.g. assume 1000ms delay; A took 5 seconds; B took 4.5 + 1 second delay so 5.5)
     * So even if delay is working, a direct comparison is tricky and would result in a fragile test. While could bump
     * the delay to a very large value that would dwarf process time, this would negatively impact overall test time.
     * Instead just test the code works when specifying a delay value and leave for more manual testing.
     */
    def "specifying a delay with multiple obsolete trrs"() {
        when: "when add 50ms delay between deletes"
        createTokenTrrWithCreationDate(new DateTime().minusHours(26))
        createTokenTrrWithCreationDate(new DateTime().minusHours(26))
        def response2 = devops.purgeObsoleteTrrs(specificationIdentityAdminToken, createTRRDeleteRequest(5, 50))
        TokenRevocationRecordDeletionResponse dr2 = response2.getEntity(TokenRevocationRecordDeletionResponse)

        then:
        dr2.deleted == 2
        dr2.errors == 0
        dr2.id != null
    }

    def createTokenTrrWithCreationDate(DateTime dateTime) {
        LdapTokenRevocationRecord record = new LdapTokenRevocationRecord()
        record.setTargetToken(TRR_TOKEN_NAME + RandomStringUtils.randomAlphanumeric(6))
        record.setTargetCreatedBefore(dateTime.toDate())  //this is what purge keys off of. Always set for token TRRs
        record.setId(UUID.randomUUID().toString())

        trrRepository.addObject(record)
        return record
    }

    def createUserTrrWithCreationDate(DateTime dateTime) {
        LdapTokenRevocationRecord record = new LdapTokenRevocationRecord()
        record.setTargetCreatedBefore(dateTime.toDate()) //this is what purge keys off of. Always set for user TRRs
        record.setTargetIssuedToId(TRR_USER_ID_PREFIX + RandomStringUtils.randomAlphanumeric(6))
        record.setId(UUID.randomUUID().toString())
        record.setTargetAuthenticatedByMethodGroups(Arrays.asList(AuthenticatedByMethodGroup.ALL))

        trrRepository.addObject(record)
        return record
    }

    def TokenRevocationRecordDeletionRequest createTRRDeleteRequest(int limit, int delay) {
        new TokenRevocationRecordDeletionRequest().with {
            it.limit = limit
            it.delay = delay
            it
        }
    }

    /**
     * Generates a fake token useful to search for matching TRR records. The generated token would be matched by the
     * specified TRR.
     *
     * A token is created for a random user when a token TRR is supplied in order to avoid collisions with User TRRs.
     *
     * If a User TRR is supplied, the token is issued for the user specified in the TRR
     *
     * @param record
     * @return
     */
    def createTokenMatchingRecord(LdapTokenRevocationRecord record) {
        return new UserScopeAccess().with {
            // User TRRs don't have a token value so need to generate a new token string
            it.accessTokenString = record.targetToken != null ? record.targetToken : TRR_TOKEN_NAME + RandomStringUtils.randomAlphanumeric(6)
            it.accessTokenExp = new DateTime().plusDays(1).toDate()
            it.createTimestamp = new DateTime(record.targetCreatedBefore).minusHours(5).toDate()

            // Token TRRs do not necessarily have target users, so specify the standard user if token trr
            it.userRsId = record.targetIssuedToId != null ? record.targetIssuedToId : TRR_USER_ID_PREFIX + RandomStringUtils.randomAlphanumeric(6)
            it
        }
    }
}
