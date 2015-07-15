package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.KeyMetadata;

public interface KeyCzarKeyMetadataDao {
    KeyMetadata getKeyMetadataByName(String name);
}
