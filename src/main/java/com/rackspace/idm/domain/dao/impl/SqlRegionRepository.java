package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.RegionDao;
import com.rackspace.idm.domain.entity.Region;
import com.rackspace.idm.domain.sql.dao.RegionRepository;
import com.rackspace.idm.domain.sql.entity.SqlRegion;
import com.rackspace.idm.domain.sql.mapper.impl.RegionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;


@SQLComponent
public class SqlRegionRepository implements RegionDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlBypassDeviceRepository.class);

    @Autowired
    RegionMapper mapper;

    @Autowired
    RegionRepository regionRepository;

    @Override
    public void addRegion(Region region) {
        try {
            SqlRegion sqlRegion = mapper.toSQL(region);
            if (sqlRegion.getDescription() == null) {
                sqlRegion.setDescription("");
            }
            regionRepository.save(sqlRegion);
        } catch (Exception e) {
            LOGGER.error("Cannot add region: " + region.getName(), e);
        }
    }

    @Override
    public void updateRegion(com.rackspace.idm.domain.entity.Region region) {
        try {
            SqlRegion sqlRegion = mapper.toSQL(region);
            if (sqlRegion.getDescription() == null) {
                sqlRegion.setDescription("");
            }
            regionRepository.save(sqlRegion);
        } catch (Exception e) {
            LOGGER.error("Cannot update region: " + region.getName(), e);
        }
    }

    @Override
    public void deleteRegion(String name) {
        try {
            regionRepository.delete(name);
        } catch (Exception e) {
            LOGGER.error("Cannot delete region: " + name, e);
        }
    }

    @Override
    public com.rackspace.idm.domain.entity.Region getRegion(String name) {
        try {
            return mapper.fromSQL(regionRepository.findOne(name));
        } catch (Exception e) {
            LOGGER.error("Cannot retrieve region: " + name, e);
        }
        return null;
    }

    @Override
    public Iterable<com.rackspace.idm.domain.entity.Region> getRegions() {
        try {
            return mapper.fromSQL(regionRepository.findAll());
        } catch (Exception e) {
            LOGGER.error("Cannot retrieve regions", e);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public Iterable<Region> getRegions(String cloud) {
        try {
            return mapper.fromSQL(regionRepository.findByRaxCloud(cloud));
        } catch (Exception e) {
            LOGGER.error("Cannot retrieve regions by cloud:" + cloud, e);
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public Region getDefaultRegion(String cloud) {
        try {
            return mapper.fromSQL(regionRepository.findByRaxCloudAndRaxIsDefault(cloud, true));
        } catch (Exception e) {
            LOGGER.error("Cannot retrieve default region for cloud:" + cloud, e);
        }
        return null;
    }
}
