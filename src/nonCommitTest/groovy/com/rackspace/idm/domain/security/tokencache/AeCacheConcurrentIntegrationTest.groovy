package com.rackspace.idm.domain.security.tokencache

import com.rackspace.idm.Constants
import com.rackspace.idm.domain.entity.*
import com.rackspace.idm.domain.security.tokencache.AETokenCache
import com.rackspace.idm.domain.security.tokenproviders.TokenProvider
import com.rackspace.idm.domain.service.IdentityPropertyService
import com.rackspace.idm.domain.service.impl.RootConcurrentIntegrationTest
import org.apache.commons.lang.StringUtils
import org.apache.commons.lang.math.RandomUtils
import org.joda.time.DateTime
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.MultiStageTask
import testHelpers.MultiStageTaskFactory

import java.util.concurrent.atomic.AtomicInteger

class AeCacheConcurrentIntegrationTest extends RootConcurrentIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired AETokenCache aeTokenCache

    @Autowired
    TokenProvider tokenProvider

    @Autowired
    IdentityPropertyService identityPropertyService

    // Establish set of user/authBys to use and expectations on whether the token is cached
    List<Tuple2> seedDataList = [new CacheTestData(entityFactory.createRandomUser("user1", "user1"), [AuthenticatedByMethodEnum.PASSWORD.value], true)
                                , new CacheTestData(entityFactory.createRandomUser("user1", "user1"), [AuthenticatedByMethodEnum.APIKEY.value], true)
                                 , new CacheTestData(entityFactory.createRandomUser("user1", "user1"), [AuthenticatedByMethodEnum.PASSWORD.value, AuthenticatedByMethodEnum.PASSCODE.value], true)
                                 , new CacheTestData(entityFactory.createRandomUser("user2", "user2"), [AuthenticatedByMethodEnum.APIKEY.value], true)
                                 , new CacheTestData(entityFactory.createRandomUser("user2", "user2"), [AuthenticatedByMethodEnum.PASSWORD.value], false)
                                 , new CacheTestData(entityFactory.createRandomUser("user3", "user3"), [AuthenticatedByMethodEnum.PASSWORD.value], true)
                                 , new CacheTestData(entityFactory.createRandomUser("user4", "user4"), [AuthenticatedByMethodEnum.PASSWORD.value], false)
    ]

    IdentityProperty tokenCacheProp

    String allUserCache = '{"tokenCacheConfig":{"enabled":true,"maxSize":1000,"cacheableUsers":[' +
            '{"type":"CID","minimumValidityDuration":"PT30S", "maximumCacheDuration":"PT3M","userIds":["user1"],"maximumCacheDuration":"PT10M","authenticatedByLists":[["PASSWORD","APIKEY"],["PASSWORD","PASSCODE"],["PASSWORD"],["APIKEY"]]}' +
            ',{"type":"CID","minimumValidityDuration":"PT30S", "maximumCacheDuration":"PT3M","userIds":["user2"],"maximumCacheDuration":"PT10M","authenticatedByLists":[["APIKEY"]]}' +
            ',{"type":"CID","minimumValidityDuration":"PT30S", "maximumCacheDuration":"PT3M","userIds":["user3"],"maximumCacheDuration":"PT10M","authenticatedByLists":[["PASSWORD"]]}' +
            ']}}'
    String previousCacheConfig

    def setup() {
        // Set cache for 60 seconds
        aeTokenCache.recreateCache() // Reload cache to pick up properties

        // Update cache to cache all users for this testing
        tokenCacheProp = identityPropertyService.getIdentityPropertyById(Constants.REPO_PROP_TOKEN_CACHE_CONFIG_ID)

        // Store the previous config so can reset it upon test completion
        previousCacheConfig = String.valueOf(tokenCacheProp.value)

        tokenCacheProp.setValueAsString(allUserCache)
        identityPropertyService.updateIdentityProperty(tokenCacheProp)
    }

    def cleanup() {
        // Reset the repo prop to original value
        tokenCacheProp.setValueAsString(previousCacheConfig)
        identityPropertyService.updateIdentityProperty(tokenCacheProp)
    }

    /**
     * This test runs a large number of threads - each of which randomly picks one entry in the seed data list to simulate "authenticating".
     *
     * It then verifies that all the simulated auths either reuse tokens or are issued new tokens appropriately based on the seed data.
     */
    def "AE cache returns appropriate tokens"() {
        Map<Integer, ConsolidatedCacheRunInfo> seedToUsedCount = new HashMap<>()

        when: "Run concurrent threads"
        List<TokenGeneratorStagedTask> runs = concurrentStageTaskRunner.runConcurrent(75, new MultiStageTaskFactory<MultiStageTask>() {
            @Override
            MultiStageTask createTask() {
                return new TokenGeneratorStagedTask()
            }
        })

        for (TokenGeneratorStagedTask run : runs) {
            int seedIndex = run.getSeedIndex()
            ConsolidatedCacheRunInfo runInfo = seedToUsedCount.computeIfAbsent(seedIndex, { t -> new ConsolidatedCacheRunInfo(seedDataList.get(seedIndex))})
            runInfo.addRunResult(run.token)
        }

        then: "No errors were encountered"
        for (TokenGeneratorStagedTask run : runs) {
            assert run.exceptionEncountered == null
        }

        and: "Cached tokens were used appropriately"
        for (ConsolidatedCacheRunInfo consolidatedCacheRunInfo : seedToUsedCount.values()) {
            assert consolidatedCacheRunInfo.validateRun()
        }

    }

    /**
     * This test starts a large number of threads where a small percentage of threads will call the "recreate" cache maintenance task.
     * The remaining threads randomly choose an entry from the seed data list to simulate "authenticating". The these
     * then verifies that each task completed successfully. Since the recreation of the cache will clear any cached
     * tokens, and the order of operations is indeterminate, we can't verify the count of cached tokens - just that a
     * token was issued.
     *
     */
    def "AE cache runs without error while cache is recreated"() {
        Map<Integer, String> seedToUsedCount = new HashMap<>()

        when: "Run concurrent threads"
        List<TokenGeneratorStagedTask> runs = concurrentStageTaskRunner.runConcurrent(75, new MultiStageTaskFactory<MultiStageTask>() {
            AtomicInteger counter = new AtomicInteger()

            @Override
            MultiStageTask createTask() {
                MultiStageTask task = counter.getAndIncrement() % 5 == 0 ? new DelayedCacheRecreationStagedTask() : new TokenGeneratorStagedTask()
                return task
            }
        })

        then:
        for (MultiStageTask mst : runs) {
            if (mst instanceof TokenGeneratorStagedTask) {
                TokenGeneratorStagedTask run = (TokenGeneratorStagedTask) mst
                assert run.exceptionEncountered == null && StringUtils.isNotBlank(run.token)
            } else if (mst instanceof DelayedCacheRecreationStagedTask) {
                DelayedCacheRecreationStagedTask run = (DelayedCacheRecreationStagedTask) mst
                assert run.exceptionEncountered == null
            }
        }
    }

    private class TokenGeneratorStagedTask implements MultiStageTask {
        User user
        UserScopeAccess userScopeAccess
        CacheTestData seedData

        int seedIndex

        Throwable exceptionEncountered
        String token

        TokenGeneratorStagedTask() {
            seedIndex = RandomUtils.nextInt(seedDataList.size())
            seedData = seedDataList.getAt(seedIndex)

            this.user = seedData.getUser()
            userScopeAccess = new UserScopeAccess()
            userScopeAccess.setUserRsId(user.getId())
            userScopeAccess.setAccessTokenString(UUID.randomUUID().toString().replaceAll('-', ""))
            userScopeAccess.setAccessTokenExp(new DateTime().plusDays(1).toDate())
            userScopeAccess.authenticatedBy = seedData.getAuthenticatedBy().collect()
        }

        @Override
        int getNumberConcurrentStages() {
            return 1
        }

        @Override
        void setup() {
        }

        @Override
        void runStage(int i) {
            switch (i) {
                case 1:
                    generateToken()
                    break
                default:
                    throw new IllegalStateException("This task does not support stage '" + i + "'")
            }
        }

        void generateToken() {
            try {
                logger.debug("Generating token")
                token = aeTokenCache.getOrCreateTokenForUser(user, userScopeAccess, tokenProvider)
            } catch (Exception ex) {
                logger.error("Exception caught generating token")
                ex.printStackTrace()
                exceptionEncountered = ex;
            }
        }

        @Override
        void cleanup() {

        }

        Throwable getExceptionEncountered() {
            return exceptionEncountered
        }
    }

    private class DelayedCacheRecreationStagedTask implements MultiStageTask {
        Throwable exceptionEncountered

        DelayedCacheRecreationStagedTask() {
        }

        @Override
        int getNumberConcurrentStages() {
            return 1
        }

        @Override
        void setup() {
        }

        @Override
        void runStage(int i) {
            switch (i) {
                case 1:
                    stage1()
                    break
                default:
                    throw new IllegalStateException("This task does not support stage '" + i + "'")
            }
        }

        void stage1() {
            //update the token. This will try to delete the expired tokens. When run concurrently, this will fail.
            try {
                logger.debug("Recreating cache")
                aeTokenCache.recreateCache()
            } catch (Exception ex) {
                logger.error("Exception caught recreating cache")
                ex.printStackTrace()
                exceptionEncountered = ex;
            }
        }

        @Override
        void cleanup() {

        }

        Throwable getExceptionEncountered() {
            return exceptionEncountered
        }
    }

    private class CacheTestData {
        BaseUser user
        List<String> authenticatedBy
        boolean tokenCacheable

        CacheTestData(BaseUser user, List<String> authenticatedBy, boolean tokenCacheable) {
            this.user = user
            this.authenticatedBy = authenticatedBy
            this.tokenCacheable = tokenCacheable
        }


        @Override
        public String toString() {
            return "CacheTestData{" +
                    "user=" + user +
                    ", authenticatedBy=" + authenticatedBy +
                    ", tokenCacheable=" + tokenCacheable +
                    '}';
        }
    }

    private class ConsolidatedCacheRunInfo {
        CacheTestData cacheTestData

        int numberRuns = 0
        Set<String> tokens = new HashSet<>()

        ConsolidatedCacheRunInfo(CacheTestData cacheTestData) {
            this.cacheTestData = cacheTestData
        }

        void addRunResult(token) {
            numberRuns++
            tokens.add(token)
        }

        boolean validateRun() {
            if (cacheTestData.tokenCacheable) {
                if (numberRuns > 0 && tokens.size() != 1) {
                    throw new RuntimeException(String.format("Cacheable token not returned for '%s'", cacheTestData))
                }
            } else if (tokens.size() != numberRuns) {
                throw new RuntimeException(String.format("Non-cacheable token was apparently reused for '%s'", cacheTestData))
            }
            return true
        }
    }
}
