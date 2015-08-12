package com.rackspace.idm.util
import com.rackspace.idm.domain.config.LdapConfiguration
import com.rackspace.idm.domain.config.SpringRepositoryProfileEnum
import org.apache.commons.configuration.Configuration
import org.apache.commons.lang.StringUtils
import org.junit.Rule
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.ContextConfiguration
import spock.lang.Shared
import spock.lang.Specification
import testHelpers.junit.ConditionalIgnoreRule
import testHelpers.junit.IgnoreByRepositoryProfile

import static org.hamcrest.CoreMatchers.equalTo
import static org.junit.Assert.assertThat

@IgnoreByRepositoryProfile(profile = SpringRepositoryProfileEnum.SQL)
@ContextConfiguration(locations = "classpath:app-config.xml")
class LdapRouterMBeanIntegrationTest extends Specification {
    @Autowired
    private LdapRouterMBean ldapRouterMBean

    @Autowired
    private Configuration config

    @Shared def initPoolSize
    @Shared def maxPoolSize

    @Rule
    public ConditionalIgnoreRule role = new ConditionalIgnoreRule()

    def setup() {
        initPoolSize = config.getInt("ldap.server.pool.size.init", LdapConfiguration.SERVER_POOL_SIZE_INIT);
        maxPoolSize = config.getInt("ldap.server.pool.size.max", LdapConfiguration.SERVER_POOL_SIZE_MAX);
    }

    def "get number of application available connections" (){
        when:
        def result = ldapRouterMBean.getNumApplicationAvailableConnections()

        then:
        result == initPoolSize
    }

    def "get number of application failed checkouts" (){
        when:
        def result =  ldapRouterMBean.getNumApplicationFailedCheckouts()

        then:
        result == 0
    }

    def "get number of bind available connections" (){
        when:
        def result = ldapRouterMBean.getNumBindAvailableConnections()

        then:
        result == initPoolSize
    }


    def "get number of bind failed checkouts" (){
        when:
        def result =  ldapRouterMBean.getNumBindFailedCheckouts()

        then:
        result == 0
    }

    def "get bind configuration" (){
        when:
        def result =  ldapRouterMBean.getBindConfiguration()

        then:
        assert StringUtils.isNotBlank(result)
    }

    def "get app configuration" (){
        when:
        def result =  ldapRouterMBean.getAppConfiguration()

        then:
        assert StringUtils.isNotBlank(result)
    }

    def "get server connection status" (){
        when:
        Map<String,String> serverConnectionStatus = ldapRouterMBean.getServerConnectionStatus()

        then:
        assertThat("server connection status", serverConnectionStatus.size(), equalTo(1));
        assertThat("server connection status", serverConnectionStatus.values().contains("up"), equalTo(true));
        assertThat("server connection status", serverConnectionStatus.values().contains("down"), equalTo(false));
    }
}