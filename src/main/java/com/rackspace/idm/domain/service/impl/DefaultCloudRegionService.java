package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.domain.dao.RegionDao;
import com.rackspace.idm.domain.entity.Region;
import com.rackspace.idm.domain.service.CloudRegionService;
import com.rackspace.idm.exception.BadRequestException;
import com.rackspace.idm.exception.DuplicateException;
import com.rackspace.idm.exception.NotFoundException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Created with IntelliJ IDEA.
 * User: wmendiza
 * Date: 10/22/12
 * Time: 3:21 PM
 * To change this template use File | Settings | File Templates.
 */
@Component
public class DefaultCloudRegionService implements CloudRegionService {

    @Autowired
    private RegionDao regionDao;

    public static final String REGION_CANNOT_BE_NULL = "Region cannot be null";
    public static final String REGIONID_CANNOT_BE_NULL = "Region id cannot be null";
    public static final String REGIONID_MUST_BE_ALPHA_NUMERIC = "Region id must be alpha numeric";
    public static final String REGION_NAME_CANNOT_BE_NULL = "Region Name cannot be null";
    public static final String REGION_CLOUD_CANNOT_BE_NULL = "Region Cloud cannot be null";
    public static final String REGION_ISDEFAULT_CANNOT_BE_NULL = "Region isDefault cannot be null";
    public static final String REGION_ISENABLED_CANNOT_BE_NULL = "Region getEnabled cannot be null";
    public static final String REGION_NAME_CANNOT_BE_UPDATED = "Region Name cannot be updated";
    public static final String REGION_NAME_ALREADY_EXISTS = "Region Name already exists";
    public static final String DEFAULT_REGION_CANNOT_BE_SET_NONDEFAULT = "Default region cannot be set to non default";
    public static final String DEFAULT_REGION_CANNOT_BE_DELETED = "Default region cannot be deleted";
    public static final String DEFAULT_REGION_CANNOT_BE_DISABLED = "Default region cannot be disabled";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void addRegion(Region region) {
        if (region == null) {
            throw new BadRequestException(REGION_CANNOT_BE_NULL);
        }

        if (StringUtils.isBlank(region.getName())) {
            throw new BadRequestException(REGION_NAME_CANNOT_BE_NULL);
        }

        if (StringUtils.isBlank(region.getCloud())) {
            throw new BadRequestException(REGION_CLOUD_CANNOT_BE_NULL);
        }

        if (!StringUtils.isAlphanumeric(region.getName())) {
            throw new BadRequestException(REGIONID_MUST_BE_ALPHA_NUMERIC);
        }

        Region currentRegion = regionDao.getRegion(region.getName());
        if (currentRegion != null) {
            throw new DuplicateException(REGION_NAME_ALREADY_EXISTS);
        }

        setAsOnlyDefaultRegion(region);

        logger.info("Adding Region: {}", region);
        regionDao.addRegion(region);
    }

    private void setAsOnlyDefaultRegion(Region region) {
        if (region.getIsDefault() && region.getIsEnabled()) {
            Region defaultRegion = regionDao.getDefaultRegion(region.getCloud());

            if (defaultRegion != null && !defaultRegion.getName().equals(region.getName())) {
                defaultRegion.setIsDefault(false);
                regionDao.updateRegion(defaultRegion);
            }
        }
    }

    @Override
    public void updateRegion(String name, Region region) {
        if (name == null) {
            throw new BadRequestException(REGIONID_CANNOT_BE_NULL);
        }
        if (region == null) {
            throw new BadRequestException(REGION_CANNOT_BE_NULL);
        }

        Region oldRegion = checkAndGetRegion(name);

        if (!StringUtils.isBlank(region.getName()) && !region.getName().equals(oldRegion.getName())) {
            throw new BadRequestException(REGION_NAME_CANNOT_BE_UPDATED);
        }

        if (StringUtils.isBlank(region.getCloud())) {
            oldRegion.setCloud(null);
        }

        if (oldRegion.getIsDefault() && oldRegion.getIsEnabled()) {
            if (!region.getIsDefault()) {
                throw new BadRequestException(DEFAULT_REGION_CANNOT_BE_SET_NONDEFAULT);
            }

            if (!region.getIsEnabled()) {
                throw new BadRequestException(DEFAULT_REGION_CANNOT_BE_DISABLED);
            }
        }

        setAsOnlyDefaultRegion(region);

        oldRegion.setIsDefault(region.getIsDefault());
        oldRegion.setIsEnabled(region.getIsEnabled());

        logger.info("Updating Region: {}", oldRegion);
        regionDao.updateRegion(oldRegion);
    }

    @Override
    public void deleteRegion(String name) {
        if (name == null) {
            throw new BadRequestException(REGIONID_CANNOT_BE_NULL);
        }

        Region region = checkAndGetRegion(name);
        if (region.getIsDefault() && region.getIsEnabled()) {
            throw new BadRequestException(DEFAULT_REGION_CANNOT_BE_DELETED);
        }

        logger.info("Deleting Region: {}", name);
        regionDao.deleteRegion(name);
    }

    @Override
    public Iterable<Region> getRegions(String cloud) {
        return regionDao.getRegions(cloud);
    }

    @Override
    public Region getDefaultRegion(String cloud) {
        if (cloud == null) {
            throw new BadRequestException(REGION_CLOUD_CANNOT_BE_NULL);
        }
        return regionDao.getDefaultRegion(cloud);
    }

    @Override
    public Region checkAndGetRegion(String regionId) {
        Region region = regionDao.getRegion(regionId);

        if (region == null) {
            String err = String.format("Region with Id %s does not exist", regionId);
            throw new NotFoundException(err);
        }
        return region;
    }
}
