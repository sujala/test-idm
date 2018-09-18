package com.rackspace.idm.domain.service;

import com.rackspace.idm.domain.config.IdentityConfig;
import com.rackspace.idm.domain.config.IdmProperty;
import com.rackspace.idm.domain.entity.IdentityProperty;
import com.rackspace.idm.domain.entity.ImmutableIdentityProperty;
import com.rackspace.idm.domain.entity.ReadableIdentityProperty;

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
     * NOTE: this does not return IdentityProperty objects if they are set to searchable = false
     *
     * @param name
     * @param idmVersions
     * @return
     */
    Iterable<IdentityProperty> getIdentityPropertyByNameAndVersions(String name, Collection<String> idmVersions);

    /**
     * <p>
     * Retrieve a repository based property. There are two types of properties:
     * </p><br/>
     * <p>
     * Reloadable properties {@link IdentityProperty#isReloadable()} are cached
     * for a configured amount of time (TTL) via the filed based reloadable property {@link IdentityConfig#CACHE_REPOSITORY_PROPERTY_TTL_PROP}.
     * During this TTL the property value is retrieved from the cache. The repository is not hit. Once the TTL has expired
     * the property will be looked up again from the directory and cached on the next request to
     * retrieve the property.
     * </p>
     * <br/>
     * <p>
     * Non-reloadable (a.k.a. static) repository properties are also cached. However, they are not associated with a TTL.
     * They will not be reloaded from the repository until the entire application is restarted. Hence, while changes
     * may be made to the property stored in the directory (value updatd or even property deleted), the changes will not
     * be recognized by the application until it is restarted.
     * </p>
     *
     * @param name
     * @return
     */
    ImmutableIdentityProperty getImmutableIdentityPropertyByName(String name);

    IdmProperty convertIdentityPropertyToIdmProperty(ReadableIdentityProperty identityProperty);

    IdmProperty convertIdentityPropertyToIdmProperty(ReadableIdentityProperty asConfiguredIdentityProperty, ReadableIdentityProperty asUsedIdentityProperty);
}
