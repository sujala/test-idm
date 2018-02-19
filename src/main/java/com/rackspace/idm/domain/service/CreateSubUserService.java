package com.rackspace.idm.domain.service;

import com.rackspace.idm.exception.DomainDefaultException;

public interface CreateSubUserService extends CreateUserService {
    /**
     * Returns the defaults to use when creating a subuser within the specified domain.
     *
     * @param domainId
     * @return
     *
     * @throws DomainDefaultException If error calculating defaults for the domain.
     */
    DomainSubUserDefaults calculateDomainSubUserDefaults(String domainId);
}
