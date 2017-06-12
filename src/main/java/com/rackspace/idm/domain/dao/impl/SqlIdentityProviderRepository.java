package com.rackspace.idm.domain.dao.impl;

import com.rackspace.idm.annotation.SQLComponent;
import com.rackspace.idm.domain.dao.IdentityProviderDao;
import com.rackspace.idm.domain.entity.ApprovedDomainGroupEnum;
import com.rackspace.idm.domain.entity.IdentityProvider;
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
    public IdentityProvider getIdentityProviderById(String Id) {
        return mapper.fromSQL(repository.findOne(Id));
    }

    @Override
    public IdentityProvider getIdentityProviderByName(String name) {
        throw new NotImplementedException();
    }

    @Override
    public IdentityProvider getIdentityProviderWithMetadataById(String id) {
        throw new NotImplementedException();
    }

    @Override
    public IdentityProvider getIdentityProviderApprovedForDomain(String name, String domainId) {
        throw new NotImplementedException();
    }

    @Override
    public IdentityProvider getIdentityProviderExplicitlyApprovedForDomain(String name, String domainId) {
        throw new NotImplementedException();
    }

    @Override
    public IdentityProvider getIdentityProvidersExplicitlyApprovedForAnyDomain(String name) {
        throw new NotImplementedException();
    }

    @Override
    public IdentityProvider getIdentityProviderApprovedForDomainByIssuer(String issuer, String domainId) {
        throw new NotImplementedException();
    }

    @Override
    public IdentityProvider getIdentityProviderExplicitlyApprovedForDomainByIssuer(String issuer, String domainId) {
        throw new NotImplementedException();
    }

    @Override
    public IdentityProvider getIdentityProvidersExplicitlyApprovedForAnyDomainByIssuer(String issuer) {
        throw new NotImplementedException();
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
    }

    @Override
    public void updateIdentityProvider(IdentityProvider identityProvider) {
        SqlIdentityProvider sqlProvider = mapper.toSQL(identityProvider);
        sqlProvider = repository.save(sqlProvider);

        final IdentityProvider newProvider = mapper.fromSQL(sqlProvider, identityProvider);
    }

    @Override
    public void updateIdentityProviderAsIs(IdentityProvider identityProvider) {
        throw new NotImplementedException();
    }

    @Override
    public void deleteIdentityProviderById(String identityProviderId) {
        final SqlIdentityProvider sqlProvider = repository.findOne(identityProviderId);
        repository.delete(identityProviderId);

        final IdentityProvider newProvider = mapper.fromSQL(sqlProvider);
    }
}
