package com.rackspace.idm.domain.dao;

import com.rackspace.idm.domain.entity.ServiceApi;

public interface ServiceApiDao {
    Iterable<ServiceApi> getServiceApis();
}
