package com.rackspace.idm.domain.dao.impl;

import org.apache.commons.configuration.Configuration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.rackspace.idm.domain.config.LdapConfiguration;
import com.rackspace.idm.domain.config.PropertyFileConfiguration;
import com.rackspace.idm.domain.dao.LdapStatusRepository;
import com.rackspace.idm.test.stub.StubLogger;

public class LdapStatusRepositoryTest {

    private LdapStatusRepository repo;
    private LdapConnectionPools connPools;
    
    private static LdapStatusRepository getRepo(LdapConnectionPools connPools) {
        Configuration appConfig = new PropertyFileConfiguration().getConfigFromClasspath();
        return new LdapStatusRepository(connPools, appConfig);
    }

    private static LdapConnectionPools getConnPools() {
        LdapConfiguration config = new LdapConfiguration(
            new PropertyFileConfiguration().getConfigFromClasspath());
        return config.connectionPools();
    }
    
    @Before
    public void setUp() {
        connPools = getConnPools();
        repo = getRepo(connPools);
    }
    
    @Test
    public void ShouldBeAlive() {
        Assert.assertTrue(repo.isAlive());
    }
}
