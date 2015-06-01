package com.rackspace.idm.domain.security.signoff;

public interface KeyCzarAPINodeSignoffDao {
    APINodeSignoff getByNodeAndMetaName(String metaName, String nodeName);


    /**
     * Adds the object as a new entry if the id is null; otherwise updates the existing object
     *
     * @param ldapAPINodeSignoff
     */
    void addOrUpdateObject(LdapAPINodeSignoff ldapAPINodeSignoff);
}
