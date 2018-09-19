package com.rackspace.idm.domain.service.impl;

import com.rackspace.idm.api.converter.cloudv20.IdentityPropertyValueConverter;
import com.rackspace.idm.domain.config.CacheConfiguration;
import com.rackspace.idm.domain.config.IdmProperty;
import com.rackspace.idm.domain.config.IdmPropertyType;
import com.rackspace.idm.domain.dao.IdentityPropertyDao;
import com.rackspace.idm.domain.entity.IdentityProperty;
import com.rackspace.idm.domain.entity.ImmutableIdentityProperty;
import com.rackspace.idm.domain.entity.ReadableIdentityProperty;
import com.rackspace.idm.domain.service.IdentityPropertyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DefaultIdentityPropertyService implements IdentityPropertyService {
    public static final String IDENTITY_PROPERTY_SOURCE = "directory";

    @Autowired
    IdentityPropertyDao identityPropertyDao;

    @Autowired
    private IdentityPropertyValueConverter propertyValueConverter;


    ConcurrentHashMap<String, ImmutableIdentityProperty> staticIdentityProperties = new ConcurrentHashMap<>();

    @Override
    public void addIdentityProperty(IdentityProperty identityProperty) {
        identityPropertyDao.addIdentityProperty(identityProperty);
    }

    @Override
    public void updateIdentityProperty(IdentityProperty identityProperty) {
        identityPropertyDao.updateIdentityProperty(identityProperty);
    }

    @Override
    public void deleteIdentityProperty(IdentityProperty identityProperty) {
        identityPropertyDao.deleteIdentityProperty(identityProperty);
    }

    @Override
    public IdentityProperty getIdentityPropertyById(String id) {
        return identityPropertyDao.getIdentityPropertyById(id);
    }

    @Override
    public IdentityProperty getIdentityPropertyByName(String name) {
        return identityPropertyDao.getIdentityPropertyByName(name);
    }

    @Override
    public Iterable<IdentityProperty> getIdentityPropertyByNameAndVersions(String name, Collection<String> idmVersions) {
        return identityPropertyDao.getIdentityPropertyByNameAndVersions(name, idmVersions);
    }

    /**
     * All properties are cached in the spring based cache. All properties are expired from this cache within configured
     * TTL. Static properties are stored internally as well. Once a static property is loaded it will never be
     * reloaded from the repository.
     *
     * @param name
     * @return
     */
    @Override
    @Cacheable(value = CacheConfiguration.REPOSITORY_PROPERTY_CACHE_BY_NAME, unless="#result == null")
    public ImmutableIdentityProperty getImmutableIdentityPropertyByName(String name) {
        Assert.hasText(name);

        ImmutableIdentityProperty immutableIdentityProperty = staticIdentityProperties.get(name);

        if (immutableIdentityProperty == null) {
            IdentityProperty prop = getIdentityPropertyByName(name);
            if (prop != null) {
                immutableIdentityProperty = new ImmutableIdentityProperty(prop);

                // Store static properties
                if (!prop.isReloadable()) {
                    ImmutableIdentityProperty existingValue = staticIdentityProperties.putIfAbsent(name, immutableIdentityProperty);

                    // If value was stored since check, use the stored value
                    if (existingValue != null) {
                        immutableIdentityProperty = existingValue;
                    }
                }
            }
        }

        return immutableIdentityProperty;
    }

    @Override
    public IdmProperty convertIdentityPropertyToIdmProperty(ReadableIdentityProperty asConfiguredIdentityProperty) {
        IdmProperty idmProperty = null;

        if (asConfiguredIdentityProperty != null) {
            idmProperty = new IdmProperty();
            idmProperty.setId(asConfiguredIdentityProperty.getId());
            idmProperty.setType(IdmPropertyType.DIRECTORY);
            idmProperty.setName(asConfiguredIdentityProperty.getName());
            idmProperty.setDescription(asConfiguredIdentityProperty.getDescription());
            try {
                // try to parse the value into a primitive type
                idmProperty.setAsConfiguredValue(propertyValueConverter.convertPropertyValue(asConfiguredIdentityProperty));
            } catch (Exception e) {
                // but fall back to a String if not parseable
                idmProperty.setAsConfiguredValue(asConfiguredIdentityProperty.getValue());
            }
            idmProperty.setValueType(asConfiguredIdentityProperty.getValueType());
            idmProperty.setVersionAdded(asConfiguredIdentityProperty.getIdmVersion());
            idmProperty.setSource(IDENTITY_PROPERTY_SOURCE);
            idmProperty.setReloadable(asConfiguredIdentityProperty.isReloadable());
        }
        return idmProperty;
    }

    @Override
    public IdmProperty convertIdentityPropertyToIdmProperty(ReadableIdentityProperty asConfiguredIdentityProperty, ReadableIdentityProperty asUsedIdentityProperty) {
        IdmProperty idmProperty = null;

        if (asConfiguredIdentityProperty != null) {
            idmProperty = new IdmProperty();
            idmProperty.setId(asConfiguredIdentityProperty.getId());
            idmProperty.setType(IdmPropertyType.DIRECTORY);
            idmProperty.setName(asConfiguredIdentityProperty.getName());
            idmProperty.setDescription(asConfiguredIdentityProperty.getDescription());
            try {
                // try to parse the value into a primitive type
                idmProperty.setValue(propertyValueConverter.convertPropertyValue(asUsedIdentityProperty));
            } catch (Exception e) {
                // but fall back to a String if not parseable
                idmProperty.setValue(asConfiguredIdentityProperty.getValue());
            }
            try {
                // try to parse the value into a primitive type
                idmProperty.setAsConfiguredValue(propertyValueConverter.convertPropertyValue(asConfiguredIdentityProperty));
            } catch (Exception e) {
                // but fall back to a String if not parseable
                idmProperty.setValue(asConfiguredIdentityProperty.getValue());
            }
            idmProperty.setValueType(asConfiguredIdentityProperty.getValueType());
            idmProperty.setVersionAdded(asConfiguredIdentityProperty.getIdmVersion());
            idmProperty.setSource(IDENTITY_PROPERTY_SOURCE);
            idmProperty.setReloadable(asConfiguredIdentityProperty.isReloadable());
        }
        return idmProperty;
    }
}
