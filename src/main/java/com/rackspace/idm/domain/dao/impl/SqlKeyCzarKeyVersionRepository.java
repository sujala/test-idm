package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.KeyCzarKeyVersionDao;
import com.rackspace.idm.domain.entity.KeyVersion;
import com.rackspace.idm.domain.sql.dao.KeyCzarKeyVersionRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@SQLComponent
public class SqlKeyCzarKeyVersionRepository implements KeyCzarKeyVersionDao {

    @Autowired
    private KeyCzarKeyVersionRepository keyCzarKeyVersionRepository;

    @Override
    public List<KeyVersion> getKeyVersionsForMetadata(String metadataName) {
        return keyCzarKeyVersionRepository.getByMetadata(metadataName);
    }

}
