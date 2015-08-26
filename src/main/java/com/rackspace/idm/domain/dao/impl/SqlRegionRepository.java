package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.RegionDao;
import com.rackspace.idm.domain.entity.Region;
import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.sql.event.SqlMigrationChangeApplicationEvent;
import com.rackspace.idm.domain.sql.dao.RegionRepository;
import com.rackspace.idm.domain.sql.entity.SqlRegion;
import com.rackspace.idm.domain.sql.mapper.impl.RegionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@SQLComponent
public class SqlRegionRepository implements RegionDao {

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlBypassDeviceRepository.class);

    @Autowired
    private RegionMapper mapper;

    @Autowired
    private RegionRepository regionRepository;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Override
    @Transactional
    public void addRegion(Region region) {
        try {
            SqlRegion sqlRegion = mapper.toSQL(region);
            if (sqlRegion.getDescription() == null) {
                sqlRegion.setDescription("");
            }
            sqlRegion = regionRepository.save(sqlRegion);

            final Region newRegion = mapper.fromSQL(sqlRegion, region);
            applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.ADD, newRegion.getUniqueId(), mapper.toLDIF(newRegion)));
        } catch (Exception e) {
            LOGGER.error("Cannot add region: " + region.getName(), e);
        }
    }

    @Override
    @Transactional
    public void updateRegion(com.rackspace.idm.domain.entity.Region region) {
        try {
            SqlRegion sqlRegion = regionRepository.findOne(region.getName());
            sqlRegion = mapper.toSQL(region, sqlRegion);
            if (sqlRegion.getDescription() == null) {
                sqlRegion.setDescription("");
            }
            sqlRegion = regionRepository.save(sqlRegion);

            final Region newRegion = mapper.fromSQL(sqlRegion, region);
            applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.MODIFY, newRegion.getUniqueId(), mapper.toLDIF(newRegion)));
        } catch (Exception e) {
            LOGGER.error("Cannot update region: " + region.getName(), e);
        }
    }

    @Override
    @Transactional
    public void deleteRegion(String name) {
        try {
            final SqlRegion sqlRegion = regionRepository.findOne(name);
            regionRepository.delete(name);

            final Region newRegion = mapper.fromSQL(sqlRegion);
            applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.DELETE, newRegion.getUniqueId(), null));
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
            return mapper.fromSQL(regionRepository.findByRaxCloudAndRaxIsDefaultAndRaxIsEnabledTrue(cloud, true));
        } catch (Exception e) {
            LOGGER.error("Cannot retrieve default region for cloud:" + cloud, e);
        }
        return null;
    }

}
