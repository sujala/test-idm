
package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.annotation.SQLRepository;
import com.rackspace.idm.domain.entity.KeyMetadata;
import com.rackspace.idm.domain.sql.entity.SqlKeyMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

@SQLRepository
public interface KeyCzarKeyMetadataRepository extends JpaSpecificationExecutor<SqlKeyMetadata>, JpaRepository<SqlKeyMetadata, String> {
    public KeyMetadata getByName(String name);
}
