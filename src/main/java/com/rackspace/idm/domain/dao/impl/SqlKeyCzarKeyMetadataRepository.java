package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.KeyCzarKeyMetadataDao;
import com.rackspace.idm.domain.entity.KeyMetadata;
import com.rackspace.idm.domain.sql.dao.KeyCzarKeyMetadataRepository;
import org.springframework.beans.factory.annotation.Autowired;

@SQLComponent
public class SqlKeyCzarKeyMetadataRepository implements KeyCzarKeyMetadataDao {

    @Autowired
    private KeyCzarKeyMetadataRepository keyCzarKeyMetadataRepository;

    @Override
    public KeyMetadata getKeyMetadataByName(String name) {
        return keyCzarKeyMetadataRepository.getByName(name);
    }

}
