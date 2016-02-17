package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum;
import com.rackspace.idm.domain.entity.IdentityProvider;
import com.rackspace.idm.domain.migration.ChangeType;
import com.rackspace.idm.domain.migration.sql.event.SqlMigrationChangeApplicationEvent;
import com.rackspace.idm.domain.sql.dao.IdentityProviderRepository;
import com.rackspace.idm.domain.sql.entity.SqlIdentityProvider;
import com.rackspace.idm.domain.sql.mapper.impl.IdentityProviderMapper;
import org.apache.commons.lang.NotImplementedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

@SQLComponent
public class SqlIdentityProviderRepository implements IdentityProviderDao {
    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private IdentityProviderMapper mapper;

    @Autowired
    private IdentityProviderRepository repository;

    @Override
    public IdentityProvider getIdentityProviderByUri(String uri) {
        return mapper.fromSQL(repository.findByUri(uri));
    }

    @Override
    public IdentityProvider getIdentityProviderByName(String name) {
        return mapper.fromSQL(repository.findOne(name));
    }

    @Override
    public List<IdentityProvider> findIdentityProvidersApprovedForDomain(String domainId) {
        //TODO - force failure when > max result
        return mapper.fromSQL(repository.findByApprovedDomainIdsContainsOrApprovedDomainGroupEquals(domainId, ApprovedDomainGroupEnum.GLOBAL.getStoredVal()));
    }

    @Override
    public List<IdentityProvider> findIdentityProvidersExplicitlyApprovedForDomain(String domainId) {
        throw new NotImplementedException();
    }

    @Override
    public List<IdentityProvider> findIdentityProvidersExplicitlyApprovedForAnyDomain() {
        throw new NotImplementedException();
    }

    @Override
    public List<IdentityProvider> findAllIdentityProviders() {
        //TODO - force failure when > max result
        return mapper.fromSQL(repository.findAll());
    }

    @Override
    public void addIdentityProvider(IdentityProvider identityProvider) {
        SqlIdentityProvider sqlProvider = mapper.toSQL(identityProvider);
        sqlProvider = repository.save(sqlProvider);

        final IdentityProvider newProvider = mapper.fromSQL(sqlProvider, identityProvider);
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.ADD, newProvider.getUniqueId(), mapper.toLDIF(newProvider)));
    }

    @Override
    public void updateIdentityProvider(IdentityProvider identityProvider) {
        SqlIdentityProvider sqlProvider = mapper.toSQL(identityProvider);
        sqlProvider = repository.save(sqlProvider);

        final IdentityProvider newProvider = mapper.fromSQL(sqlProvider, identityProvider);
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.MODIFY, newProvider.getUniqueId(), mapper.toLDIF(newProvider)));
    }

    @Override
    public void deleteIdentityProviderById(String identityProviderName) {
        final SqlIdentityProvider sqlProvider = repository.findOne(identityProviderName);
        repository.delete(identityProviderName);

        final IdentityProvider newProvider = mapper.fromSQL(sqlProvider);
        applicationEventPublisher.publishEvent(new SqlMigrationChangeApplicationEvent(this, ChangeType.DELETE, newProvider.getUniqueId(), null));
    }
}
