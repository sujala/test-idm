package com.rackspace.idm.domain.sql.dao;

import com.rackspace.idm.domain.entity.EndUser;
import com.rackspace.idm.domain.entity.PaginatorContext;

public interface IdentityUserRepository {

    PaginatorContext<EndUser> getEndUsersByDomainIdPaged(String domainId, int offset, int limit);

    PaginatorContext<EndUser> getEnabledEndUsersPaged(int offset, int limit);

}
