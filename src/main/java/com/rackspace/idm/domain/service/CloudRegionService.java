package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.Region;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: wmendiza
 * Date: 10/22/12
 * Time: 3:18 PM
 * To change this template use File | Settings | File Templates.
 */
public interface CloudRegionService {
    void addRegion(Region region);
    void updateRegion(String regionId, Region region);
    void deleteRegion(String regionId);
    List<Region> getRegions();
    Region getDefaultRegion(String cloud);
    Region checkAndGetRegion(String name);
}
