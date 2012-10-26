package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.audit.Audit;
import com.rackspace.idm.domain.dao.RegionDao;
import com.rackspace.idm.domain.entity.Region;
import com.rackspace.idm.exception.NotFoundException;
import com.unboundid.ldap.sdk.*;
import com.unboundid.ldap.sdk.persist.LDAPPersistException;
import com.unboundid.ldap.sdk.persist.LDAPPersister;
import org.springframework.stereotype.Component;
import org.tuckey.web.filters.urlrewrite.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: wmendiza
 * Date: 10/22/12
 * Time: 1:20 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class LdapRegionRepository extends LdapRepository implements RegionDao {

    public static final String NULL_OR_EMPTY_NAME_PARAMETER = "Null or Empty name parameter";
    public static final String NULL_OR_EMPTY_CLOUD_ID_PARAMETER = "Null or Empty cloud parameter";
    public static final String ERROR_GETTING_REGION_OBJECT = "Error getting region object";

    @Override
    public void addRegion(Region region) {
        if (region == null) {
            getLogger().error(ERROR_GETTING_REGION_OBJECT);
            throw new IllegalArgumentException(ERROR_GETTING_REGION_OBJECT);
        }
        getLogger().info("Adding region: {}", region);
        Audit audit = Audit.log(region).add();

        try {
            LDAPPersister<Region> persister = LDAPPersister.getInstance(Region.class);
            persister.add(region, getAppInterface(), REGION_BASE_DN);
            audit.succeed();
            getLogger().info("Added region: {}", region);
        } catch (LDAPException e) {
            getLogger().error("Error adding region object", e);
            audit.fail(e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void updateRegion(Region region) {
        if (region == null || StringUtils.isBlank(region.getUniqueId())) {
            getLogger().error(NULL_OR_EMPTY_NAME_PARAMETER);
            getLogger().info("Invalid region parameter.");
        }
        Audit audit = Audit.log(region).modify();

        try {
            LDAPPersister<Region> persister = LDAPPersister.getInstance(Region.class);
            List<Modification> mods = persister.getModifications(region, true);
            audit.modify(mods);

            if (mods.size() > 0) {
                persister.modify(region, getAppInterface(), null, true);
            }
        } catch (LDAPException ldapEx) {
            getLogger().error("Error updating region {} - {}", region.getName(), ldapEx);
            audit.fail("Error updating region");
            throw new IllegalStateException(ldapEx.getMessage(), ldapEx);
        }
        audit.succeed();
        getLogger().info("Updated region - {}", region);
    }

    @Override
    public void deleteRegion(String name) {
        if (StringUtils.isBlank(name)) {
            getLogger().error(NULL_OR_EMPTY_NAME_PARAMETER);
            throw new IllegalArgumentException(
                    "Null or Empty name parameter.");
        }
        Region region = getRegion(name);
        if (region == null) {
            String errMsg = String.format("region %s not found", name);
            getLogger().warn(errMsg);
            throw new NotFoundException(errMsg);
        }
        getLogger().debug("Deleting Region: {}", region);
        final String dn = region.getUniqueId();
        final Audit audit = Audit.log(region).delete();
        deleteEntryAndSubtree(dn, audit);
        audit.succeed();
        getLogger().debug("Deleted Region: {}", region);
    }

    @Override
    public Region getRegion(String name) {
        if (StringUtils.isBlank(name)) {
            getLogger().error(NULL_OR_EMPTY_NAME_PARAMETER);
            getLogger().info("Invalid name parameter.");
            return null;
        }
        getLogger().debug("Doing search for region with name " + name);

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_NAME, name)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_REGION).build();

        return getRegion(searchFilter);
    }

    @Override
    public Region getDefaultRegion(String cloud) {
        if (StringUtils.isBlank(cloud)) {
            getLogger().error(NULL_OR_EMPTY_CLOUD_ID_PARAMETER);
            getLogger().info("Invalid cloud parameter.");
            return null;
        }

        getLogger().debug("Doing search for region with cloud " + cloud);

        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_CLOUD, cloud)
                .addEqualAttribute(ATTR_USE_FOR_DEFAULT_REGION, "true")
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_REGION).build();

        return getRegion(searchFilter);
    }

    @Override
    public List<Region> getRegions() {
        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_REGION).build();
        return getRegions(searchFilter);
    }

    @Override
    public List<Region> getRegions(String cloud) {
        Filter searchFilter = new LdapSearchBuilder()
                .addEqualAttribute(ATTR_CLOUD, cloud)
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_REGION).build();

        return getRegions(searchFilter);
    }

    private Region getSingleRegion(Filter searchFilter) throws LDAPPersistException {
        SearchResultEntry entry = this.getSingleEntry(REGION_BASE_DN, SearchScope.ONE, searchFilter, ATTR_REGION_SEARCH_ATTRIBUTES);
        if (entry == null) {
            return null;
        }
        Region region = null;
        region = LDAPPersister.getInstance(Region.class).decode(entry);
        return region;
    }

    private List<Region> getRegions(Filter searchFilter) {
        getLogger().debug("Getting regions");

        List<Region> regions = new ArrayList<Region>();
        SearchResult searchResult = null;

        try {
            searchResult = getAppInterface().search(REGION_BASE_DN, SearchScope.ONE, searchFilter);
            getLogger().info("Got regions");
        } catch (LDAPSearchException ldapEx) {
            getLogger().error("Error searching for regions - {}", ldapEx);
            throw new IllegalStateException(ldapEx);
        }

        if (searchResult.getEntryCount() > 0) {
            for (SearchResultEntry entry : searchResult.getSearchEntries()) {
                Region region = null;

                try {
                    region = LDAPPersister.getInstance(Region.class).decode(entry);
                } catch (LDAPPersistException e) {
                    getLogger().error(ERROR_GETTING_REGION_OBJECT, e);
                    throw new IllegalStateException(e);
                }
                regions.add(region);
            }
        }
        return regions;
    }

    private Region getRegion(Filter searchFilter) {
        Region region = null;

        try {
            region = getSingleRegion(searchFilter);
        } catch (LDAPPersistException e) {
            getLogger().error(ERROR_GETTING_REGION_OBJECT, e);
            throw new IllegalStateException(e);
        }
        getLogger().debug("Found Region - {}", region);

        return region;
    }
}
