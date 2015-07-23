package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.sql.entity.SqlProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@SQLRepository
public interface ProjectRepository extends JpaSpecificationExecutor<SqlProject>, JpaRepository<SqlProject, String> {

    SqlProject findByName(String name);
}
