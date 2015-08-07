package com.rackspace.idm.domain.config;

import com.rackspace.idm.domain.dao.AEScopeAccessDao;
import com.rackspace.idm.domain.dao.ScopeAccessDao;
import com.rackspace.idm.domain.dao.UUIDScopeAccessDao;
import com.rackspace.idm.domain.dao.impl.AEScopeAccessRepository;
import com.rackspace.idm.domain.dao.impl.LdapScopeAccessRepository;
import com.rackspace.idm.domain.dao.impl.RouterScopeAccessRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("SQL")
@Configuration
public class SqlScopeAccessDaoConfiguration {

    @Bean(name = "scopeAccessDao")
    public AEScopeAccessDao getAEScopeAccessDao() {
        return new AEScopeAccessRepository();
    }

}
