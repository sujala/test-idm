
package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlEndpoint;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;

@SQLRepository
public interface EndpointRepository extends JpaSpecificationExecutor<SqlEndpoint>, JpaRepository<SqlEndpoint, String> {

    @Override
    @EntityGraph(value = "SqlEndpoint.rax", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlEndpoint> findAll();

    @EntityGraph(value = "SqlEndpoint.rax", type = EntityGraph.EntityGraphType.FETCH)
    public List<SqlEndpoint> findByLegacyEndpointId(String id);

    @EntityGraph(value = "SqlEndpoint.rax", type = EntityGraph.EntityGraphType.FETCH)
    public List<SqlEndpoint> findByRaxServiceName(String name);

    /*
     * Following code fails in Hibernate, due to bug:
     * https://hibernate.atlassian.net/browse/HHH-9230
     *
     * This means that more sql request are going to be made. Can affect
     * performance.
    */
    //@EntityGraph(value = "SqlEndpoint.rax", type = EntityGraph.EntityGraphType.FETCH)
    public List<SqlEndpoint> findByLegacyEndpointIdIn(Collection<String> baseUrlIds);

    @EntityGraph(value = "SqlEndpoint.rax", type = EntityGraph.EntityGraphType.FETCH)
    public List<SqlEndpoint> findByRegionNotAndRaxBaseUrlType(String region, String baseUrlType);

    @EntityGraph(value = "SqlEndpoint.rax", type = EntityGraph.EntityGraphType.FETCH)
    public List<SqlEndpoint> findByRegionAndRaxBaseUrlType(String region, String baseUrlType);

    @EntityGraph(value = "SqlEndpoint.rax", type = EntityGraph.EntityGraphType.FETCH)
    public List<SqlEndpoint> findByRaxBaseUrlTypeAndEnabledAndRaxDefTrue(String baseUrlType, boolean enabled);

    @EntityGraph(value = "SqlEndpoint.rax", type = EntityGraph.EntityGraphType.FETCH)
    public List<SqlEndpoint> findByRaxSqlPolicyPolicyId(String policyId);
}
