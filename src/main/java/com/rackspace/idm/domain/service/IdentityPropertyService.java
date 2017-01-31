package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.entity.IdentityProperty;

import java.util.Collection;

public interface IdentityPropertyService {

    /**
     * Saves the identity property
     *
     * @param identityProperty
     */
    void addIdentityProperty(IdentityProperty identityProperty);

    /**
     * Updates the identity property
     *
     * @param identityProperty
     */
    void updateIdentityProperty(IdentityProperty identityProperty);

    /**
     * Deletes the identity property
     *
     * @param identityProperty
     */
    void deleteIdentityProperty(IdentityProperty identityProperty);

    /**
     * Loads the IdentityProperty by ID. This does a case-insensitive search on the ID
     *
     * @param id
     * @return
     */
    IdentityProperty getIdentityPropertyById(String id);

    /**
     * Loads the IdentityProperty by name. This does a case-insensitive search on the name
     *
     * @param name
     * @return
     */
    IdentityProperty getIdentityPropertyByName(String name);

    /**
     * Searches and returns all IdentityProperty entries found that match the identity property versions
     * and property name. The search by name and IDM version are case-insensitive. Both name and idmVersions
     * params are optional. If both are provided the property must match both name AND idmVersions. The idmVersions
     * are OR'd in the query if multiple versions are provided.
     *
     * NOTE: this does not return IdentityProperty objects if they are set to reloadable = false
     *
     * @param name
     * @param idmVersions
     * @return
     */
    Iterable<IdentityProperty> getIdentityPropertyByNameAndVersions(String name, Collection<String> idmVersions);

}
