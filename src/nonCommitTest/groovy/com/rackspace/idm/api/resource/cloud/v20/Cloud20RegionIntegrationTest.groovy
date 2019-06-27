package com.rackspace.idm.api.resource.cloud.v20

import com.rackspace.docs.identity.api.ext.rax_auth.v1.Region
import com.rackspace.docs.identity.api.ext.rax_auth.v1.Regions
import com.rackspace.idm.Constants
import com.rackspace.idm.domain.dao.RegionDao
import com.rackspace.idm.domain.service.impl.DefaultCloudRegionService
import org.openstack.docs.identity.api.v2.IdentityFault
import org.openstack.docs.identity.api.v2.User
import org.springframework.beans.factory.annotation.Autowired
import testHelpers.RootIntegrationTest
import testHelpers.saml.SamlFactory

import javax.ws.rs.core.MediaType

import static org.apache.http.HttpStatus.*


class Cloud20RegionIntegrationTest extends RootIntegrationTest {

    @Autowired
    RegionDao regionDao

    /**
     * In order for you to be able to create a user in a region you need to take the following steps:
     * 1) Create a region with enabled = false, default = false, rsCloud = <config value cloud.region>
     *     Note: The enabled and default attributes on the region are not used when determining if a region is "default".
     *           Default regions are determined based on the intersection of cloudServersOpenStack endpoint's region values
     *           and the Region entries in the directory that mach the cloud.region static property
     * 2) Create an endpoint template with service == 'cloudServersOpenStack' and rsRegion == <name of new region>
     * 3) Create the user with the defaultRegion = new region name
     */
    def "403 returned when trying to delete a region with a provisioned user associated with it"() {
        given:
        //1) create the region
        def regionToCreate = v1Factory.createRegion(testUtils.getRandomUUID("region"), false, false)
        cloud20.createRegion(utils.getServiceAdminToken(), regionToCreate)
        def region = regionDao.getRegion(regionToCreate.name)
        region.cloud = "US"
        regionDao.updateRegion(region)

        //2) create the endpoint to allow for the region to be  a default region
        def endpoint = v1Factory.createEndpointTemplate(testUtils.getRandomInteger().toString(), "compute", testUtils.getRandomUUID("http://public/"), DefaultRegionService.CLOUD_SERVERS_OPENSTACK, false, region.name)
        cloud20.addEndpointTemplate(utils.getServiceAdminToken(), endpoint)

        //3) create the user in the new region
        def userToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("user"), "display", "email@email.com", true, regionToCreate.name, null, Constants.DEFAULT_PASSWORD)
        def user = cloud20.createUser(utils.getServiceAdminToken(), userToCreate).getEntity(User).value
        assert user.defaultRegion == region.name

        when: "try to delete the region with the user in the region"
        def response = cloud20.deleteRegion(utils.getServiceAdminToken(), region.name)

        then: "forbidden"
        response.status == 403
        def fault = response.getEntity(IdentityFault).value
        fault.message == DefaultCloudRegionService.ERROR_USERS_WITHIN_REGION_MESSAGE

        when: "now delete the user and try to delete the region"
        utils.deleteUser(user)
        response = cloud20.deleteRegion(utils.getServiceAdminToken(), region.name)

        then: "success"
        response.status == 204
    }

    def "403 returned when trying to delete a region with a federated user associated with it"() {
        given:
        //1) create the region
        def regionToCreate = v1Factory.createRegion(testUtils.getRandomUUID("region"), false, false)
        cloud20.createRegion(utils.getServiceAdminToken(), regionToCreate)
        def region = regionDao.getRegion(regionToCreate.name)
        region.cloud = "US"
        regionDao.updateRegion(region)

        //2) create the endpoint to allow for the region to be  a default region
        def endpoint = v1Factory.createEndpointTemplate(testUtils.getRandomInteger().toString(), "compute", testUtils.getRandomUUID("http://public/"), DefaultRegionService.CLOUD_SERVERS_OPENSTACK, false, region.name)
        cloud20.addEndpointTemplate(utils.getServiceAdminToken(), endpoint)

        //3) create the user admin in the new region
        def domainId = utils.createDomain()
        def userToCreate = v2Factory.createUserForCreate(testUtils.getRandomUUID("user"), "display", "email@email.com", true, regionToCreate.name, domainId, Constants.DEFAULT_PASSWORD)
        def userAdmin = cloud20.createUser(utils.getIdentityAdminToken(), userToCreate).getEntity(User).value
        assert userAdmin.defaultRegion == region.name

        //now auth a federated user in the region
        def samlResponse = utils.authenticateFederatedUser(domainId)
        assert samlResponse.user != null

        //now delete the user admin so the only user in the region is the federated user
        utils.deleteUser(userAdmin)

        when: "try to delete the region with the federated user in the region"
        def response = cloud20.deleteRegion(utils.getServiceAdminToken(), region.name)

        then: "forbidden"
        response.status == 403
        def fault = response.getEntity(IdentityFault).value
        fault.message == DefaultCloudRegionService.ERROR_USERS_WITHIN_REGION_MESSAGE

        when: "now delete the user and try to delete the region"
        utils.deleteFederatedUserQuietly(samlResponse.user.name)
        response = cloud20.deleteRegion(utils.getServiceAdminToken(), region.name)

        then: "success"
        response.status == 204
    }

    def "Create region accepts cloud region in the request"() {
        when: "create region"
        def cloud = "US"
        def regionToCreate = v1Factory.createRegion(testUtils.getRandomUUID("region"), false, false, cloud)
        def response = cloud20.createRegion(utils.getServiceAdminToken(), regionToCreate, contentType)

        then: "created"
        response.status == SC_CREATED

        when: "get region"
        response = cloud20.getRegion(utils.getServiceAdminToken(), regionToCreate.name, contentType)

        then: "cloud is returned"
        response.status == SC_OK
        def region = response.getEntity(Region)
        region.cloud == cloud

        when: "list region"
        response = cloud20.getRegions(utils.getServiceAdminToken(), contentType)

        then: "cloud is returned"
        response.status == SC_OK
        def regions = response.getEntity(Regions)
        regions.region.find {it.name == regionToCreate.name}.cloud == cloud

        when: "update region"
        region.cloud = "UK"
        response = cloud20.updateRegion(utils.getServiceAdminToken(), region.name, region, contentType)

        then:
        response.status == SC_BAD_REQUEST

        when: "delete region"
        response = cloud20.deleteRegion(utils.getServiceAdminToken(), region.name, contentType)

        then: "success"
        response.status == SC_NO_CONTENT

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }

    def "Cloud region can be set to US and UK only"() {
        when: "create region"
        def cloud = "US"
        def regionToCreate = v1Factory.createRegion(testUtils.getRandomUUID("region"), false, false, cloud)
        def response = cloud20.createRegion(utils.getServiceAdminToken(), regionToCreate)

        then: "created"
        response.status == SC_CREATED

        when: "get region"
        response = cloud20.getRegion(utils.getServiceAdminToken(), regionToCreate.name)

        then: "cloud is returned"
        response.status == SC_OK
        def region = response.getEntity(Region)
        region.cloud == cloud

        when: "delete region"
        response = cloud20.deleteRegion(utils.getServiceAdminToken(), region.name)

        then: "success"
        response.status == SC_NO_CONTENT

        when: "create region"
        cloud = "UK"
        regionToCreate = v1Factory.createRegion(testUtils.getRandomUUID("region"), false, false, cloud)
        response = cloud20.createRegion(utils.getServiceAdminToken(), regionToCreate)

        then: "created"
        response.status == SC_CREATED

        when: "get region"
        response = cloud20.getRegion(utils.getServiceAdminToken(), regionToCreate.name)
        region = response.getEntity(Region)

        then: "cloud is returned"
        response.status == SC_OK
        region.cloud == cloud

        when: "delete region"
        response = cloud20.deleteRegion(utils.getServiceAdminToken(), region.name)

        then: "success"
        response.status == SC_NO_CONTENT

        when: "create region"
        cloud = "USSR"
        regionToCreate = v1Factory.createRegion(testUtils.getRandomUUID("region"), false, false, cloud)
        response = cloud20.createRegion(utils.getServiceAdminToken(), regionToCreate)

        then: "returns bad request"
        response.status == SC_BAD_REQUEST

    }

    def "Region services are only accesible to service-admins"() {
        when: "create region"
        def cloud = "US"
        def regionToCreate = v1Factory.createRegion(testUtils.getRandomUUID("region"), false, false, cloud)
        def response = cloud20.createRegion(utils.getIdentityAdminToken(), regionToCreate, contentType)

        then: "forbidden"
        response.status == SC_FORBIDDEN

        when: "create region"
        cloud = "US"
        regionToCreate = v1Factory.createRegion(testUtils.getRandomUUID("region"), false, false, cloud)
        response = cloud20.createRegion(utils.getServiceAdminToken(), regionToCreate, contentType)

        then: "created"
        response.status == SC_CREATED

        when: "get region"
        response = cloud20.getRegion(utils.getIdentityAdminToken(), regionToCreate.name, contentType)

        then: "forbidden"
        response.status == SC_FORBIDDEN

        when: "list region"
        response = cloud20.getRegions(utils.getIdentityAdminToken(), contentType)

        then: "forbidden"
        response.status == SC_FORBIDDEN

        when: "update region"
        response = cloud20.getRegion(utils.getServiceAdminToken(), regionToCreate.name, contentType)
        def region = response.getEntity(Region)
        region.enabled = false
        response = cloud20.updateRegion(utils.getIdentityAdminToken(), region.name, region, contentType)

        then:
        response.status == SC_FORBIDDEN

        when: "delete region"
        response = cloud20.deleteRegion(utils.getIdentityAdminToken(), region.name, contentType)

        then: "forbidden"
        response.status == SC_FORBIDDEN

        when: "delete region"
        response = cloud20.deleteRegion(utils.getServiceAdminToken(), region.name, contentType)

        then: "success"
        response.status == SC_NO_CONTENT

        where:
        contentType                     | _
        MediaType.APPLICATION_XML_TYPE  | _
        MediaType.APPLICATION_JSON_TYPE | _
    }
}
