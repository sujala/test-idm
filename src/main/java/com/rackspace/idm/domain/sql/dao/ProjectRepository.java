package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlProject;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

@SQLRepository
public interface ProjectRepository extends JpaSpecificationExecutor<SqlProject>, JpaRepository<SqlProject, String> {

    @Override
    @EntityGraph(value = "SqlProject.rax", type = EntityGraph.EntityGraphType.FETCH)
    List<SqlProject> findAll();

    @Override
    @EntityGraph(value = "SqlProject.rax", type = EntityGraph.EntityGraphType.FETCH)
    SqlProject findOne(String projectId);

    @EntityGraph(value = "SqlProject.rax", type = EntityGraph.EntityGraphType.FETCH)
    SqlProject findByName(String name);

}
