package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.APINodeSignoff;
import com.rackspace.idm.domain.dao.KeyCzarAPINodeSignoffDao;
import com.rackspace.idm.domain.sql.dao.KeyCzarAPINodeSignoffRepository;
import com.rackspace.idm.domain.sql.entity.SqlAPINodeSignoff;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.UUID;

@SQLComponent
public class SqlKeyCzarAPINodeSignoffRepository implements KeyCzarAPINodeSignoffDao {

    @Autowired
    KeyCzarAPINodeSignoffRepository repository;

    @Override
    public APINodeSignoff getByNodeAndMetaName(String metaName, String nodeName) {
        return repository.getByKeyMetadataIdAndNodeName(metaName, nodeName);
    }

    @Override
    public void addOrUpdateObject(APINodeSignoff apiNodeSignoff) {
        SqlAPINodeSignoff sqlAPINodeSignoff = (SqlAPINodeSignoff) apiNodeSignoff;

        if(sqlAPINodeSignoff.getId() == null){
            sqlAPINodeSignoff.setId(UUID.randomUUID().toString().replace("-", ""));
        }

        repository.save(sqlAPINodeSignoff);

    }

    @Override
    public void deleteApiNodeSignoff(APINodeSignoff apiNodeSignoff) {
        repository.delete((SqlAPINodeSignoff) apiNodeSignoff);
    }

    @Override
    public APINodeSignoff createApiNodeSignoff() {
        return new SqlAPINodeSignoff();
    }
}
