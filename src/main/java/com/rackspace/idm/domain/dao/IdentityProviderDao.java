package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.IdentityProvider;

public interface IdentityProviderDao {

    public IdentityProvider getIdentityProviderByUri(String uri);

    public IdentityProvider getIdentityProviderByName(String name);
}
