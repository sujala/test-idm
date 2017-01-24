package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.LDAPComponent;
import com.rackspace.idm.domain.dao.IdentityPropertyDao;
import com.rackspace.idm.domain.entity.IdentityProperty;
import com.unboundid.ldap.sdk.Filter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.IterableGet;
import org.apache.commons.lang.Validate;

import java.util.ArrayList;
import java.util.Collection;

@LDAPComponent
public class LdapIdentityPropertyRepository extends LdapGenericRepository<IdentityProperty> implements IdentityPropertyDao {

    @Override
    public String getBaseDn(){
        return IDENTITY_PROPERTIES_BASE_DN;
    }

    @Override
    public String getLdapEntityClass() {
        return OBJECTCLASS_IDENTITY_PROPERTY;
    }

    @Override
    public String getSortAttribute() {
        return ATTR_ID;
    }

    @Override
    public void addIdentityProperty(IdentityProperty identityProperty) {
        Validate.isTrue(identityProperty.getId() == null);

        identityProperty.setId(getUuid());
        addObject(identityProperty);
    }

    @Override
    public void updateIdentityProperty(IdentityProperty identityProperty) {
        updateObject(identityProperty);
    }

    @Override
    public void deleteIdentityProperty(IdentityProperty identityProperty) {
        deleteObject(identityProperty);
    }

    @Override
    public IdentityProperty getIdentityPropertyById(String id) {
        return getObject(searchFilterGetPropertyById(id));
    }

    @Override
    public IdentityProperty getIdentityPropertyByName(String name) {
        return getObject(searchFilterGetPropertyByName(name));
    }

    @Override
    public Iterable<IdentityProperty> getIdentityPropertyByNameAndVersions(String name, Collection<String> idmVersions) {
        return getObjects(searchFilterGetPropertyByNameAndVersions(name, idmVersions));
    }

    private Filter searchFilterGetPropertyById(String id) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_IDENTITY_PROPERTY)
                .addEqualAttribute(ATTR_ID, id)
                .build();
    }

    private Filter searchFilterGetPropertyByName(String name) {
        return new LdapSearchBuilder()
                .addEqualAttribute(ATTR_OBJECT_CLASS, OBJECTCLASS_IDENTITY_PROPERTY)
                .addEqualAttribute(ATTR_COMMON_NAME, name)
                .build();
    }

    private Filter searchFilterGetPropertyByNameAndVersions(String name, Collection<String> idmVersions) {
        Collection<Filter> filters = new ArrayList<>();
        filters.add(Filter.createEqualityFilter(ATTR_OBJECT_CLASS, OBJECTCLASS_IDENTITY_PROPERTY));
        filters.add(Filter.createEqualityFilter(ATTR_PROPERTY_SEARCHABLE, Boolean.TRUE.toString().toUpperCase()));

        if (name != null) {
            filters.add(Filter.createEqualityFilter(ATTR_COMMON_NAME, name));
        }

        if (CollectionUtils.isNotEmpty(idmVersions)) {
            Collection<Filter> versionFilters = new ArrayList<>();
            for (String idmVersion : idmVersions) {
                versionFilters.add(Filter.createEqualityFilter(ATTR_PROPERTY_VERSION, idmVersion));
            }
            filters.add(Filter.createORFilter(versionFilters));
        }

        return Filter.createANDFilter(filters);
    }

}
