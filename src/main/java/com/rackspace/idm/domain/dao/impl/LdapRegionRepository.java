package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.domain.dao.RegionDao;
import com.rackspace.idm.domain.entity.Region;
import com.unboundid.ldap.sdk.Filter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class LdapRegionRepository extends LdapGenericRepository<Region> implements RegionDao {

    public String getBaseDn(){
        return REGION_BASE_DN;
    }

    public String getLdapEntityClass(){
        return OBJECTCLASS_REGION;
    }

    public String[] getSearchAttributes(){
        return ATTR_REGION_SEARCH_ATTRIBUTES;
    }

    @Override
    public void addRegion(Region region) {
        addObject(region);
    }

    @Override
    public void updateRegion(Region region) {
        updateObject(region);
    }

    @Override
    public void deleteRegion(String name) {
        deleteObject(searchFilterGetRegionByName(name));
    }

    @Override
    public Region getRegion(String name) {
        return getObject(searchFilterGetRegionByName(name));
    }

    @Override
    public List<Region> getRegions() {
        return getObjects(searchFilterGetRegions());
    }

    @Override
    public List<Region> getRegions(String cloud) {
        return getObjects(searchFilterGetRegionsByCloud(cloud));
    }

    @Override
    public Region getDefaultRegion(String cloud) {
        return getObject(searchFilterGetDefaultRegion(cloud));
    }

    private Filter searchFilterGetRegionByName(String name) {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRepository.ATTR_NAME, name)
                .addEqualAttribute(LdapRepository.ATTR_OBJECT_CLASS, LdapRegionRepository.OBJECTCLASS_REGION).build();
    }

    private Filter searchFilterGetDefaultRegion(String cloud) {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRegionRepository.ATTR_CLOUD, cloud)
                .addEqualAttribute(LdapRegionRepository.ATTR_USE_FOR_DEFAULT_REGION, "TRUE")
                .addEqualAttribute(LdapRegionRepository.ATTR_ENABLED, "TRUE")
                .addEqualAttribute(LdapRegionRepository.ATTR_OBJECT_CLASS, LdapRegionRepository.OBJECTCLASS_REGION).build();
    }

    private Filter searchFilterGetRegions() {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRegionRepository.ATTR_OBJECT_CLASS, LdapRegionRepository.OBJECTCLASS_REGION).build();
    }

    private Filter searchFilterGetRegionsByCloud(String cloud) {
        return new LdapRepository.LdapSearchBuilder()
                .addEqualAttribute(LdapRegionRepository.ATTR_CLOUD, cloud)
                .addEqualAttribute(LdapRegionRepository.ATTR_OBJECT_CLASS, LdapRegionRepository.OBJECTCLASS_REGION).build();
    }
}
