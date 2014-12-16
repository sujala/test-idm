package com.rackspace.idm.domain.security.encrypters.keyczar;

import java.util.List;

public interface KeyCzarKeyVersionDao {
    List<KeyVersion> getKeyVersionsForMetadata(String metadataName);
}
