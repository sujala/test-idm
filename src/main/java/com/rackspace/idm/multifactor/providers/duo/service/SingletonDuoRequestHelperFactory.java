package com.rackspace.idm.multifactor.providers.duo.service;

import com.rackspace.idm.multifactor.providers.duo.config.DuoSecurityConfig;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Constructs a single helper for each unique config
 */
@Component
public class SingletonDuoRequestHelperFactory implements DuoRequestHelperFactory {

    ConcurrentHashMap<DuoSecurityConfig, DuoRequestHelper> configToHelper = new ConcurrentHashMap<DuoSecurityConfig, DuoRequestHelper>(4);

    @Override
    public DuoRequestHelper getInstance(DuoSecurityConfig config) {
        DuoRequestHelper helper = configToHelper.get(config);
        if (helper == null) {
            DuoRequestHelper newHelper = new DefaultDuoRequestHelper(config);
            helper = configToHelper.putIfAbsent(config, newHelper);
            if (helper == null) {
                helper = newHelper;
            }
        }
        return helper;
    }
}
