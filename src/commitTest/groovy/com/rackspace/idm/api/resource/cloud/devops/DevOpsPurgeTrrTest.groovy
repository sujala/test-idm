package com.rackspace.idm.api.resource.cloud.devops

import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenRevocationRecordDeletionRequest
import com.rackspace.docs.identity.api.ext.rax_auth.v1.TokenRevocationRecordDeletionResponse
import com.rackspace.idm.domain.config.IdentityConfig
import com.rackspace.idm.exception.BadRequestException
import spock.lang.Unroll
import testHelpers.RootServiceTest

/**
 * These tests depend upon creating obsolete TRRs for testing purposes. As such, it expects that no other trrs exist that
 * would be picked up by the purge. Unless other tests explicitly create obsolete TRRs, this should be fine.
 */
class DevOpsPurgeTrrTest extends RootServiceTest {

    DefaultDevOpsService devOpsService = new DefaultDevOpsService()

    def setupSpec() {
    }

    /**
     * self clean TRRs the various tests create so there's no cross test pollution in the event of a test failure
     */
    def setup() {
        mockRequestContextHolder(devOpsService)
        mockAuthorizationService(devOpsService)
        mockIdentityConfig(devOpsService)
        mockTokenRevocationService(devOpsService)
    }

    @Unroll
    def "limit calculated appropriately - requestedLimit: #requestLimit; configuredMaxLimit: #configuredMaxLimit; expectedLimitToBeUsed: #expectedLimitToBeUsed"() {
        reloadableConfig.getPurgeTokenRevocationRecordsMaxDelay() >> 1
        reloadableConfig.getPurgeTokenRevocationRecordsMaxLimit() >> configuredMaxLimit

        when:
        devOpsService.purgeObsoleteTrrs("token", new TokenRevocationRecordDeletionRequest().with {
            it.limit = requestedLimit
            it.delay = 0
            it
        })

        then:
        1 * tokenRevocationService.purgeObsoleteTokenRevocationRecords(expectedLimitToBeUsed, 0) >> new TokenRevocationRecordDeletionResponse()

        where:
        requestedLimit | configuredMaxLimit | expectedLimitToBeUsed
        null    | 1000  | 1000 //null >> max limit
        1       | 1000  | 1 //requested less than max limit >> requested
    }

    @Unroll
    def "invalid limit throws BadRequest - requestedLimit: #requestLimit; configuredMaxLimit: #configuredMaxLimit"() {
        reloadableConfig.getPurgeTokenRevocationRecordsMaxDelay() >> 1
        reloadableConfig.getPurgeTokenRevocationRecordsMaxLimit() >> configuredMaxLimit

        when:
        devOpsService.purgeObsoleteTrrs("token", new TokenRevocationRecordDeletionRequest().with {
            it.limit = requestedLimit
            it.delay = 0
            it
        })

        then:
        thrown(BadRequestException)

        where:
        requestedLimit | configuredMaxLimit
        -1      | 10
        1005    | 1000
        0       | 5
    }

    @Unroll
    def "delay calculated appropriately - requestedDelay: #requestedDelay; configuredMaxDelay: #configuredMaxDelay; expectedDelayToBeUsed: #expectedDelayToBeUsed"() {
        reloadableConfig.getPurgeTokenRevocationRecordsMaxDelay() >> configuredMaxDelay
        reloadableConfig.getPurgeTokenRevocationRecordsMaxLimit() >> 10

        when:
        devOpsService.purgeObsoleteTrrs("token", new TokenRevocationRecordDeletionRequest().with {
            it.limit = 5
            it.delay = requestedDelay
            it
        })

        then:
        1 * tokenRevocationService.purgeObsoleteTokenRevocationRecords(5, expectedDelayToBeUsed) >> new TokenRevocationRecordDeletionResponse()

        where:
        requestedDelay | configuredMaxDelay | expectedDelayToBeUsed
        null    | 1000  | IdentityConfig.PURGE_TRRS_DEFAULT_DELAY       //requested is null >> hardcoded default
        1       | 1000  | 1                                             //requested < max >> requested
        0       | 5     | 0                                             //request is 0 >> requested value
    }

    @Unroll
    def "invalid delay throws BadRequest - requestedDelay: #requestedDelay; configuredMaxDelay: #configuredMaxDelay"() {
        reloadableConfig.getPurgeTokenRevocationRecordsMaxDelay() >> configuredMaxDelay
        reloadableConfig.getPurgeTokenRevocationRecordsMaxLimit() >> 10

        when:
        devOpsService.purgeObsoleteTrrs("token", new TokenRevocationRecordDeletionRequest().with {
            it.limit = 5
            it.delay = requestedDelay
            it
        })

        then:
        thrown(BadRequestException)

        where:
        requestedDelay | configuredMaxDelay
        -1      | 10
        1005    | 1000
    }
}