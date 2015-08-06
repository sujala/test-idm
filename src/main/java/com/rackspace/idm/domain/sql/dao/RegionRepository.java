package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlRegion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

@SQLRepository
public interface RegionRepository extends JpaSpecificationExecutor<SqlRegion>, JpaRepository<SqlRegion, String> {

    @EntityGraph(value = "SqlRegion.rax", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlRegion> findByRaxCloud(String cloud);

    @EntityGraph(value = "SqlRegion.rax", type = EntityGraph.EntityGraphType.FETCH)
    SqlRegion findByRaxCloudAndRaxIsDefaultAndRaxIsEnabledTrue(String cloud, Boolean defaultRegion);

}
