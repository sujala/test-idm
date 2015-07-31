package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.dao.APINodeSignoff;

public interface KeyCzarAPINodeSignoffDao {
    APINodeSignoff getByNodeAndMetaName(String metaName, String nodeName);


    /**
     * Adds the object as a new entry if the id is null; otherwise updates the existing object
     *
     * @param apiNodeSignoff
     */
    void addOrUpdateObject(APINodeSignoff apiNodeSignoff);

    void deleteApiNodeSignoff(APINodeSignoff apiNodeSignoff);

    public APINodeSignoff createApiNodeSignoff();
}
