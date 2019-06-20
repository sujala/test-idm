package com.rackspace.idm.api.resource.cloud.v20;

import com.rackspace.idm.ErrorCodes;
import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.entity.*;
import com.rackspace.idm.domain.service.*;
import com.rackspace.idm.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class DefaultRegionService {

    static String CLOUD_SERVERS_OPENSTACK = "cloudServersOpenStack";

    @Autowired
    private EndpointService endpointService;

    @Autowired
    private CloudRegionService cloudRegionService;

    @Autowired
    private ScopeAccessService scopeAccessService;

    @Autowired
    private DomainService domainService;

    @Autowired
    private UserService userService;

    @Autowired
    private IdentityConfig identityConfig;

    public void validateDefaultRegion(String defaultRegion) {
        Set<String> regions = this.getDefaultRegionsForCloudServersOpenStack();
        checkDefaultRegion(defaultRegion, regions);
    }

    public void validateDefaultRegion(String defaultRegion, User user) {
        Set<String> regions = this.getDefaultRegionsForUser(user);
        checkDefaultRegion(defaultRegion, regions);
    }

    public void checkDefaultRegion(String region, Set<String> defaultRegions) {
        String regionString = "";
        int size = defaultRegions.size();
        int counter = 0;
        for (String defaultRegion : defaultRegions) {
            counter = counter + 1;
            if (counter < size) {
                regionString += " " + defaultRegion + ",";
            } else {
                regionString += " " + defaultRegion;
            }
        }
        if (region != null && !defaultRegions.contains(region)) {
            throw new BadRequestException("Invalid defaultRegion value, accepted values are:" + regionString.toUpperCase() + ".");
        }
    }

    public Set<String> getDefaultRegionsForUser(User user) {
        Set<String> defaultRegions = getCloudServersOpenStackRegionsForUser(user);

        if(defaultRegions.size() == 0){
            defaultRegions.addAll(getDefaultRegionsForCloudServersOpenStack());
        }

        return defaultRegions;
    }

    private Set<String> getCloudServersOpenStackRegions(Iterable<CloudBaseUrl> baseUrls) {
        Set<String> defaultRegions = new HashSet<String>();
        for (CloudBaseUrl baseUrl : baseUrls) {
            if (baseUrl.getServiceName().equalsIgnoreCase(CLOUD_SERVERS_OPENSTACK)) {
                defaultRegions.add(baseUrl.getRegion());
            }
        }
        return defaultRegions;
    }

    private Set<String> getRegionsWithinCloud(Set<String> regionNames) {
        List<String> regionsInCloudRegion = new ArrayList<String>();

        for (Region region : cloudRegionService.getRegions(identityConfig.getStaticConfig().getCloudRegion())) {
            regionsInCloudRegion.add(region.getName());
        }

        regionNames.retainAll(regionsInCloudRegion);

        return regionNames;
    }

    public Set<String> getDefaultRegionsForCloudServersOpenStack() {
        Iterable<CloudBaseUrl> baseUrls = endpointService.getBaseUrlsByServiceName(CLOUD_SERVERS_OPENSTACK);

        return getRegionsWithinCloud(getCloudServersOpenStackRegions(baseUrls));
    }

    /**
     * Validates if a compute region is allowed in a cloud region.
     *
     * @param selectedRegion
     * @param cloud
     */
    public void validateComputeRegionForCloud(String selectedRegion, String cloud) {
        Set<String> validRegions = getComputeRegionsForCloud(cloud);
        checkDefaultRegion(selectedRegion, validRegions);
    }

    /**
     * Validates if a compute region is allowed for user. Base on legacy logic, the following steps are taken to
     * determine if a user has access to a compute region.
     *
     * 1. A valid compute region is based on the regions of the endpoints to which the user have access to.
     * 2. If user does not have access to any compute regions, fallback to compute regions based on the user's domain
     *    type.
     *  @param selectedRegion
     * @param user
     */
    public void validateComputeRegionForUser(String selectedRegion, User user) {
        Set<String> validRegions = getCloudServersOpenStackRegionsForUser(user);

        if(validRegions.size() == 0){
            Domain domain = domainService.getDomain(user.getDomainId());
            // Sanity check, all user should belong to an existing domain.
            if (domain == null) {
                throw new BadRequestException("Unable to update user's region. User's domain was not found.", ErrorCodes.ERROR_CODE_GENERIC_BAD_REQUEST);
            }
            String cloudRegion = userService.inferCloudBasedOnDomainType(domain.getType());

            validRegions = getComputeRegionsForCloud(cloudRegion);
        }

        checkDefaultRegion(selectedRegion, validRegions);
    }

    /**
     * Retrieves the compute regions allowed for cloud region.
     *
     * @param cloud
     * @return
     */
    private Set<String> getComputeRegionsForCloud(String cloud) {
        Set<String> validRegions = new HashSet<>();

        // 1. Finding all the regions in which a cloud servers openstack endpoint template exists.
        Iterable<CloudBaseUrl> baseUrls = endpointService.getBaseUrlsByServiceName(CLOUD_SERVERS_OPENSTACK);
        Set<String> cloudServersOpenStackRegions = getCloudServersOpenStackRegions(baseUrls);

        // 2. Retrieving all regions for the server's cloud region stored in the directory. (cause endpoints could point to regions that don't really exist)
        Iterable<Region> cloudRegionsBasedonDomain = cloudRegionService.getRegions(cloud);

        for (Region cloudRegion : cloudRegionsBasedonDomain) {
            validRegions.add(cloudRegion.getName());
        }

        //3. Taking the intersection of (1) and (2)
        validRegions.retainAll(cloudServersOpenStackRegions);

        return validRegions;
    }

    /**
     * Determines the cloud servers OpenStack regions accessible to user based on its tenant roles.
     *
     * @param user
     * @return
     */
    private Set<String> getCloudServersOpenStackRegionsForUser(User user) {
        List<OpenstackEndpoint> endpoints = scopeAccessService.getOpenstackEndpointsForUser(user);
        List<CloudBaseUrl> userBaseUrls = new ArrayList<>();
        for (OpenstackEndpoint endpoint : endpoints) {
            userBaseUrls.addAll(endpoint.getBaseUrls());
        }

        return getCloudServersOpenStackRegions(userBaseUrls);
    }
}
