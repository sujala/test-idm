package com.rackspace.idm.multifactor.providers.duo.service;

import com.rackspace.idm.multifactor.providers.duo.config.DuoSecurityConfig;

/**
 */
public interface DuoRequestHelperFactory {
    DuoRequestHelper getInstance(DuoSecurityConfig config);
}
