package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.KeyVersion;

import java.util.List;

public interface KeyCzarKeyVersionDao {
    List<KeyVersion> getKeyVersionsForMetadata(String metadataName);
}
