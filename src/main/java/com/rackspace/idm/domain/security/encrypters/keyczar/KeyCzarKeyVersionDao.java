package com.rackspace.idm.domain.security.encrypters.keyczar;

import java.util.List;

public interface KeyCzarKeyVersionDao {
    KeyVersion getKeyVersionForMetadata(String metadataName, String version);
    List<KeyVersion> getKeyVersionsForMetadata(String metadataName);
}
