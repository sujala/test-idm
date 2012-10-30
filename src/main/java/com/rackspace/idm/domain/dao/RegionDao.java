package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.Region;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: wmendiza
 * Date: 10/22/12
 * Time: 1:19 PM
 * To change this template use File | Settings | File Templates.
 */
public interface RegionDao {
    void addRegion(Region region);
    void updateRegion(Region region);
    void deleteRegion(String name);
    Region getRegion(String name);
    List<Region> getRegions();
    List<Region> getRegions(String cloud);
    Region getDefaultRegion(String cloud);
}