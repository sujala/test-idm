package com.rackspace.idm.domain.sql.mapper.test;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DummyRepository extends JpaSpecificationExecutor<SqlDummy>, JpaRepository<SqlDummy, String> {
}
