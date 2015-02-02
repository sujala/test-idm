package com.rackspace.idm.domain.security.encrypters.keyczar;

public interface KeyCzarKeyMetadataDao {
    KeyMetadata getKeyMetadataByName(String name);
}
