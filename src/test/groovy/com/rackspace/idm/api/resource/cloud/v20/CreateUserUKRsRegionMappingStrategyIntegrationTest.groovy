package com.rackspace.idm.api.resource.cloud.v20

import spock.lang.Unroll

class CreateUserUKRsRegionMappingStrategyIntegrationTest extends CreateUserRegionMappingStrategyBaseIntegrationTest {

    static def settingsFile = "classpath:com/rackspace/idm/api/resource/cloud/v20/Feature_RegionlessEndpoints_UK_cloud_rsregion_strategy.xml"

    @Override
    def getSettingsLocation() {
        return settingsFile
    }

    @Unroll("add user UK - v2.0 'one user' and v1.1 adds non-global endpoints as necessary with rsregion strategy - baseUrlID: #baseUrlID, baseUrlrsRegion: #baseUrlrsRegion, expects endpoint to have been added == #shouldBaseUrlHaveBeenAdded")
    def "add user UK - v2.0 'one user' and v1.1 mosso tenant adds endpoint as necessary with rsregion strategy"() {
        expect:
        assertEndpointLogic(baseUrlID, baseUrlrsRegion, shouldBaseUrlHaveBeenAdded)

        /*
        The default data set in docker includes a number of pre-existing endpoints. Use very high and very low ids to avoid collisions.
        The associated DefaultEndpointServiceTest will verify edge cases based on the id
         */
        where:
        baseUrlID                           |   baseUrlrsRegion     |   shouldBaseUrlHaveBeenAdded
        getRandomIntegerLessThan(-10000)    |   null                |   false
        getRandomIntegerGreaterThan(10000)  |   null                |   false
        getRandomIntegerLessThan(-10000)    |   "DFW"               |   false
        getRandomIntegerGreaterThan(10000)  |   "DFW"               |   false
        getRandomIntegerGreaterThan(10000)  |   "LON"               |   true
        getRandomIntegerLessThan(-10000)    |   "LON"               |   true
    }

}
