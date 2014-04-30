package com.rackspace.idm.api.resource.cloud.v20

import spock.lang.Unroll

class CreateUserUSHybridMappingStrategyIntegrationTest extends CreateUserRegionMappingStrategyBaseIntegrationTest {

    static def settingsFile = "classpath:com/rackspace/idm/api/resource/cloud/v20/Feature_RegionlessEndpoints_US_cloud_hybrid_strategy.xml"

    @Override
    def getSettingsLocation() {
        return settingsFile
    }

    @Unroll("add user UK - v2.0 'one user' and v1.1 adds non-global endpoints as necessary - baseUrlID: #baseUrlID, baseUrlrsRegion: #baseUrlrsRegion, expects endpoint to have been added == #shouldBaseUrlHaveBeenAdded")
    def "add user UK - v2.0 'one user' and v1.1 adds endpoint as necessary"() {
        expect:
        assertEndpointLogic(baseUrlID, baseUrlrsRegion, shouldBaseUrlHaveBeenAdded)

        /*
        The default data set in docker includes a number of pre-existing endpoints. Use very high and very low ids to avoid collisions.
        The associated DefaultEndpointServiceTest will verify edge cases based on the id
         */
        where:
        baseUrlID                           |   baseUrlrsRegion     |   shouldBaseUrlHaveBeenAdded
        getRandomIntegerLessThan(-10000)    |   null                |   true
        getRandomIntegerGreaterThan(10000)  |   null                |   false
        getRandomIntegerLessThan(-10000)    |   "DFW"               |   true
        getRandomIntegerGreaterThan(10000)  |   "DFW"               |   true
        getRandomIntegerGreaterThan(10000)  |   "LON"               |   false
        getRandomIntegerLessThan(-10000)    |   "LON"               |   false
    }

}
